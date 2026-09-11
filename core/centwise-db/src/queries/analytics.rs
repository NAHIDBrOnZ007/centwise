use centwise_domain::{CategorySpendSummary, MerchantSpendSummary, MonthlySpend};
use rusqlite::params;

use crate::error::DbResult;
use crate::queries::{collect, now_epoch_ms, Queries};

impl<'a> Queries<'a> {
    /// Expense totals grouped by category for a period, biggest first.
    pub fn category_breakdown(
        &self,
        start_epoch_ms: i64,
        end_epoch_ms: i64,
    ) -> DbResult<Vec<CategorySpendSummary>> {
        self.category_breakdown_filtered(start_epoch_ms, end_epoch_ms, "debit")
    }

    pub fn category_breakdown_filtered(
        &self,
        start_epoch_ms: i64,
        end_epoch_ms: i64,
        type_filter: &str,
    ) -> DbResult<Vec<CategorySpendSummary>> {
        let mut statement = self.connection.prepare(
            "SELECT c.id, c.name, c.icon, c.color_hex,
                    COALESCE(SUM(t.amount_minor), 0) AS total,
                    COUNT(t.id) AS tx_count
             FROM categories c
             LEFT JOIN transactions t
                    ON t.category_id = c.id
                   AND (?3 = 'all' OR (?3 = 'debit' AND t.transaction_type = 'expense')
                        OR (?3 = 'credit' AND t.transaction_type = 'income'))
                   AND t.occurred_at_epoch_ms >= ?1
                   AND t.occurred_at_epoch_ms < ?2
             GROUP BY c.id
             HAVING total > 0
             ORDER BY total DESC",
        )?;

        let rows =
            statement.query_map(params![start_epoch_ms, end_epoch_ms, type_filter], |row| {
                Ok(CategorySpendSummary {
                    category_id: row.get(0)?,
                    category_name: row.get(1)?,
                    category_icon: row.get(2)?,
                    category_color_hex: row.get(3)?,
                    total_minor: row.get(4)?,
                    transaction_count: row.get(5)?,
                })
            })?;

        collect(rows)
    }

    /// Expense totals grouped by transaction title (merchant), biggest first.
    pub fn top_merchants(
        &self,
        start_epoch_ms: i64,
        end_epoch_ms: i64,
        limit: u32,
    ) -> DbResult<Vec<MerchantSpendSummary>> {
        self.top_merchants_filtered(start_epoch_ms, end_epoch_ms, limit, "debit")
    }

    pub fn top_merchants_filtered(
        &self,
        start_epoch_ms: i64,
        end_epoch_ms: i64,
        limit: u32,
        type_filter: &str,
    ) -> DbResult<Vec<MerchantSpendSummary>> {
        let mut statement = self.connection.prepare(
            "SELECT title, COALESCE(SUM(amount_minor), 0) AS total, COUNT(id) AS tx_count
             FROM transactions
             WHERE (?4 = 'all' OR (?4 = 'debit' AND transaction_type = 'expense')
                    OR (?4 = 'credit' AND transaction_type = 'income'))
               AND occurred_at_epoch_ms >= ?1
               AND occurred_at_epoch_ms < ?2
             GROUP BY title
             ORDER BY total DESC
             LIMIT ?3",
        )?;

        let rows = statement.query_map(
            params![start_epoch_ms, end_epoch_ms, limit as i64, type_filter],
            |row| {
                Ok(MerchantSpendSummary {
                    merchant: row.get(0)?,
                    total_minor: row.get(1)?,
                    transaction_count: row.get(2)?,
                })
            },
        )?;

        collect(rows)
    }

    pub fn analytics_snapshot(
        &self,
        start_epoch_ms: i64,
        end_epoch_ms: i64,
        months_back: u32,
        type_filter: &str,
    ) -> DbResult<centwise_domain::AnalyticsSnapshot> {
        let (total_income_minor, total_expense_minor, transaction_count) = self
            .connection
            .query_row(
                "SELECT
                    COALESCE(SUM(CASE WHEN transaction_type = 'income' THEN amount_minor ELSE 0 END), 0),
                    COALESCE(SUM(CASE WHEN transaction_type = 'expense' THEN amount_minor ELSE 0 END), 0),
                    COALESCE(SUM(CASE WHEN (?3 = 'all' OR (?3 = 'debit' AND transaction_type = 'expense')
                               OR (?3 = 'credit' AND transaction_type = 'income')) THEN 1 ELSE 0 END), 0)
                 FROM transactions
                 WHERE occurred_at_epoch_ms >= ?1 AND occurred_at_epoch_ms < ?2",
                params![start_epoch_ms, end_epoch_ms, type_filter],
                |row| Ok((row.get(0)?, row.get(1)?, row.get(2)?),),
            )?;

        Ok(centwise_domain::AnalyticsSnapshot {
            total_income_minor,
            total_expense_minor,
            transaction_count,
            category_breakdown: self.category_breakdown_filtered(
                start_epoch_ms,
                end_epoch_ms,
                type_filter,
            )?,
            top_merchants: self.top_merchants_filtered(
                start_epoch_ms,
                end_epoch_ms,
                5,
                type_filter,
            )?,
            monthly_trends: self.spending_by_month(months_back)?,
        })
    }

    /// Expense totals per calendar month within the last `months_back`
    /// months from now (oldest first). Only months with spending are
    /// returned; consumers zero-fill gaps for charting.
    pub fn spending_by_month(&self, months_back: u32) -> DbResult<Vec<MonthlySpend>> {
        self.spending_by_month_anchored(months_back, now_epoch_ms())
    }

    /// Same as [`spending_by_month`] but anchored to a fixed timestamp, so
    /// tests are independent of the wall clock.
    pub fn spending_by_month_anchored(
        &self,
        months_back: u32,
        anchor_epoch_ms: i64,
    ) -> DbResult<Vec<MonthlySpend>> {
        // Compute the cutoff in Rust using UTC arithmetic instead of SQLite strftime.
        let cutoff_epoch_ms =
            month_start_n_months_ago(anchor_epoch_ms, months_back.saturating_sub(1));

        let mut statement = self.connection.prepare(
            "SELECT occurred_at_epoch_ms, amount_minor
             FROM transactions
             WHERE transaction_type = 'expense'
               AND occurred_at_epoch_ms >= ?1
             ORDER BY occurred_at_epoch_ms ASC",
        )?;

        // Aggregate year/month buckets in Rust — avoids per-row strftime in SQLite.
        let mut buckets: std::collections::BTreeMap<(i32, u32), i64> =
            std::collections::BTreeMap::new();
        let mut rows = statement.query(params![cutoff_epoch_ms])?;
        while let Some(row) = rows.next()? {
            let epoch_ms: i64 = row.get(0)?;
            let amount: i64 = row.get(1)?;
            let (year, month) = epoch_ms_to_year_month(epoch_ms);
            *buckets.entry((year, month)).or_insert(0) += amount;
        }

        Ok(buckets
            .into_iter()
            .map(|((year, month), total)| MonthlySpend {
                year,
                month,
                total_expense_minor: total,
            })
            .collect())
    }
}

/// Extracts UTC (year, month) from an epoch-ms timestamp using pure arithmetic.
fn epoch_ms_to_year_month(epoch_ms: i64) -> (i32, u32) {
    // Days since Unix epoch (1970-01-01).
    let total_days = (epoch_ms / 86_400_000) as i32;
    // Civil date from day count using the algorithm from
    // Howard Hinnant (public domain).
    let z = total_days + 719_468;
    let era = (if z >= 0 { z } else { z - 146_096 }) / 146_097;
    let doe = (z - era * 146_097) as u32; // day of era [0, 146096]
    let yoe = (doe - doe / 1460 + doe / 36524 - doe / 146_096) / 365; // year of era [0, 399]
    let y = yoe as i32 + era * 400;
    let doy = doe - (365 * yoe + yoe / 4 - yoe / 100); // day of year [0, 365]
    let mp = (5 * doy + 2) / 153; // [0, 11]
    let m = if mp < 10 { mp + 3 } else { mp - 9 }; // [1, 12]
    let year = if m <= 2 { y + 1 } else { y };
    (year, m)
}

/// Returns the epoch-ms of the first millisecond of the month that is
/// `n` calendar months before the month containing `anchor_epoch_ms`.
fn month_start_n_months_ago(anchor_epoch_ms: i64, n: u32) -> i64 {
    let (year, month) = epoch_ms_to_year_month(anchor_epoch_ms);
    let n = n as i32;
    // Subtract n months.
    let total_months = year * 12 + month as i32 - 1 - n;
    let target_year = total_months.div_euclid(12);
    let target_month = (total_months.rem_euclid(12) + 1) as u32;
    // Convert back to epoch-ms using the inverse civil→days algorithm.
    let y = if target_month <= 2 {
        target_year - 1
    } else {
        target_year
    };
    let era = (if y >= 0 { y } else { y - 399 }) / 400;
    let yoe = (y - era * 400) as u32;
    let doy =
        (153 * (if target_month > 2 {
            target_month - 3
        } else {
            target_month + 9
        }) + 2)
            / 5;
    let doe = yoe * 365 + yoe / 4 - yoe / 100 + doy;
    let days = era * 146_097 + doe as i32 - 719_468;
    days as i64 * 86_400_000
}

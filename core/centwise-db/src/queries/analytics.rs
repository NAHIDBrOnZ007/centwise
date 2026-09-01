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
                    SUM(CASE WHEN (?3 = 'all' OR (?3 = 'debit' AND transaction_type = 'expense')
                               OR (?3 = 'credit' AND transaction_type = 'income')) THEN 1 ELSE 0 END)
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
        let window_modifier = format!("-{} months", months_back.saturating_sub(1));
        let anchor_seconds = anchor_epoch_ms / 1000;

        let mut statement = self.connection.prepare(
            "SELECT
                CAST(strftime('%Y', occurred_at_epoch_ms / 1000, 'unixepoch') AS INTEGER) AS y,
                CAST(strftime('%m', occurred_at_epoch_ms / 1000, 'unixepoch') AS INTEGER) AS m,
                COALESCE(SUM(amount_minor), 0) AS total
             FROM transactions
             WHERE transaction_type = 'expense'
               AND occurred_at_epoch_ms >= strftime('%s', ?1, 'unixepoch', 'start of month', ?2) * 1000
             GROUP BY y, m
             ORDER BY y ASC, m ASC",
        )?;

        let rows = statement.query_map(params![anchor_seconds, window_modifier], |row| {
            Ok(MonthlySpend {
                year: row.get::<_, i64>(0)? as i32,
                month: row.get::<_, i64>(1)? as u32,
                total_expense_minor: row.get(2)?,
            })
        })?;

        collect(rows)
    }
}

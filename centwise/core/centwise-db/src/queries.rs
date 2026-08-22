use centwise_domain::{
    Account, AccountSummary, BudgetWithProgress, CategorySpendSummary, HomeDashboard,
    MerchantSpendSummary, MonthlySpend, NewBudget, NewSubscription, NewTransaction,
    SubscriptionSummary, Transaction, TransactionSummary, TransactionType,
};
use rusqlite::{params, Connection, OptionalExtension};

use crate::error::{DbError, DbResult};

/// Writes and screen-shaped reads. All writes must go through this module so
/// balances stay transactional and the update hook fires for listeners.
pub struct Queries<'a> {
    connection: &'a Connection,
}

impl<'a> Queries<'a> {
    pub fn new(connection: &'a Connection) -> Self {
        Queries { connection }
    }

    /// Inserts an account.
    pub fn insert_account(&self, account: &Account) -> DbResult<()> {
        self.connection.execute(
            "INSERT INTO accounts (id, name, provider, last_four, balance_minor, archived, created_at_epoch_ms)
             VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7)",
            params![
                account.id,
                account.name,
                account.provider,
                account.last_four,
                account.balance_minor,
                account.archived as i64,
                now_epoch_ms()
            ],
        )?;
        Ok(())
    }

    /// Current balance of an account, or 0 when unknown.
    pub fn account_balance(&self, account_id: &str) -> DbResult<i64> {
        let balance = self
            .connection
            .query_row(
                "SELECT balance_minor FROM accounts WHERE id = ?1",
                params![account_id],
                |row| row.get::<_, i64>(0),
            )
            .optional()?;
        Ok(balance.unwrap_or(0))
    }

    /// Inserts a transaction and updates the account balance in one atomic step.
    pub fn insert_transaction(&self, transaction: &NewTransaction) -> DbResult<()> {
        if transaction.amount_minor < 0 {
            return Err(DbError::Invalid("amount_minor must not be negative".into()));
        }

        let now = now_epoch_ms();
        let affected = self.connection.execute(
            "INSERT INTO transactions (
                id, title, amount_minor, currency, transaction_type, category_id,
                occurred_at_epoch_ms, account_id, reference, balance_after_minor,
                notes, is_auto_tracked, created_at_epoch_ms
            ) VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11, ?12, ?13)",
            params![
                transaction.id,
                transaction.title,
                transaction.amount_minor,
                transaction.currency,
                transaction.transaction_type.as_str(),
                transaction.category_id,
                transaction.occurred_at_epoch_ms,
                transaction.account_id,
                transaction.reference,
                transaction.balance_after_minor,
                transaction.notes,
                transaction.is_auto_tracked as i64,
                now
            ],
        )?;

        if affected != 1 {
            return Err(DbError::Sqlite("insert affected no rows".into()));
        }

        // Adjust account balance in the same connection transaction context.
        let delta = match transaction.transaction_type {
            TransactionType::Income => transaction.amount_minor,
            TransactionType::Expense | TransactionType::Refund => -transaction.amount_minor,
            TransactionType::Transfer => 0,
        };

        if delta != 0 {
            self.connection.execute(
                "UPDATE accounts SET balance_minor = balance_minor + ?1 WHERE id = ?2",
                params![delta, transaction.account_id],
            )?;
        }

        Ok(())
    }

    /// Deletes a transaction and reverses its balance effect.
    pub fn delete_transaction(&self, id: &str) -> DbResult<bool> {
        let stored: Option<(i64, String, String)> = self
            .connection
            .query_row(
                "SELECT amount_minor, transaction_type, account_id FROM transactions WHERE id = ?1",
                params![id],
                |row| Ok((row.get(0)?, row.get(1)?, row.get(2)?)),
            )
            .optional()?;

        let Some((amount_minor, type_value, account_id)) = stored else {
            return Ok(false);
        };

        let transaction_type = TransactionType::from_str_value(&type_value)
            .ok_or_else(|| DbError::Corrupt(format!("unknown transaction type: {type_value}")))?;

        let delta = match transaction_type {
            TransactionType::Income => -amount_minor,
            TransactionType::Expense | TransactionType::Refund => amount_minor,
            TransactionType::Transfer => 0,
        };

        self.connection
            .execute("DELETE FROM transactions WHERE id = ?1", params![id])?;

        if delta != 0 {
            self.connection.execute(
                "UPDATE accounts SET balance_minor = balance_minor + ?1 WHERE id = ?2",
                params![delta, account_id],
            )?;
        }

        Ok(true)
    }

    /// Full transaction rows, newest first.
    pub fn list_transactions(&self, limit: u32) -> DbResult<Vec<Transaction>> {
        let mut statement = self.connection.prepare(
            "SELECT id, title, amount_minor, currency, transaction_type, category_id,
                    occurred_at_epoch_ms, account_id, reference, balance_after_minor,
                    notes, is_auto_tracked
             FROM transactions
             ORDER BY occurred_at_epoch_ms DESC
             LIMIT ?1",
        )?;

        let rows = statement.query_map(params![limit as i64], map_full_row)?;

        let mut transactions = Vec::new();
        for row in rows {
            transactions.push(row?);
        }
        Ok(transactions)
    }

    /// Everything the Home screen renders, computed in one pass.
    pub fn home_dashboard(
        &self,
        start_epoch_ms: i64,
        end_epoch_ms: i64,
        recent_limit: u32,
    ) -> DbResult<HomeDashboard> {
        let totals = self.connection.query_row(
            "SELECT
                COALESCE(SUM(CASE WHEN transaction_type = 'expense' THEN amount_minor ELSE 0 END), 0),
                COALESCE(SUM(CASE WHEN transaction_type = 'income' THEN amount_minor ELSE 0 END), 0)
             FROM transactions
             WHERE occurred_at_epoch_ms >= ?1 AND occurred_at_epoch_ms < ?2",
            params![start_epoch_ms, end_epoch_ms],
            |row| Ok((row.get::<_, i64>(0)?, row.get::<_, i64>(1)?)),
        )?;

        let recent = self.recent_summaries(recent_limit)?;

        Ok(HomeDashboard {
            period_expense_minor: totals.0,
            period_income_minor: totals.1,
            recent_transactions: recent,
        })
    }

    fn recent_summaries(&self, limit: u32) -> DbResult<Vec<TransactionSummary>> {
        let mut statement = self.connection.prepare(
            "SELECT t.id, t.title, t.amount_minor, t.transaction_type,
                    c.name, c.icon, c.color_hex,
                    t.occurred_at_epoch_ms, a.name
             FROM transactions t
             JOIN categories c ON c.id = t.category_id
             LEFT JOIN accounts a ON a.id = t.account_id
             ORDER BY t.occurred_at_epoch_ms DESC
             LIMIT ?1",
        )?;

        let rows = statement.query_map(params![limit as i64], |row| {
            let type_value: String = row.get(3)?;
            Ok(TransactionSummary {
                id: row.get(0)?,
                title: row.get(1)?,
                amount_minor: row.get(2)?,
                transaction_type: TransactionType::from_str_value(&type_value).ok_or_else(
                    || {
                        rusqlite::Error::FromSqlConversionFailure(
                            3,
                            rusqlite::types::Type::Text,
                            Box::new(std::io::Error::new(
                                std::io::ErrorKind::InvalidData,
                                format!("unknown transaction type: {type_value}"),
                            )),
                        )
                    },
                )?,
                category_name: row.get(4)?,
                category_icon: row.get(5)?,
                category_color_hex: row.get(6)?,
                occurred_at_epoch_ms: row.get(7)?,
                account_name: row.get(8).unwrap_or_else(|_| "Unknown account".to_string()),
            })
        })?;

        let mut summaries = Vec::new();
        for row in rows {
            summaries.push(row?);
        }
        Ok(summaries)
    }

    // MARK: - Accounts

    /// All accounts, active first, ordered by name.
    pub fn list_accounts(&self) -> DbResult<Vec<AccountSummary>> {
        let mut statement = self.connection.prepare(
            "SELECT id, name, provider, last_four, balance_minor, archived
             FROM accounts
             ORDER BY archived ASC, name ASC",
        )?;

        let rows = statement.query_map([], |row| {
            Ok(AccountSummary {
                id: row.get(0)?,
                name: row.get(1)?,
                provider: row.get(2)?,
                last_four: row.get(3)?,
                balance_minor: row.get(4)?,
                archived: row.get::<_, i64>(5)? != 0,
            })
        })?;

        collect(rows)
    }

    // MARK: - Budgets

    /// Inserts a budget for a category and period.
    pub fn insert_budget(&self, budget: &NewBudget) -> DbResult<()> {
        if budget.limit_minor <= 0 {
            return Err(DbError::Invalid("budget limit must be positive".into()));
        }

        self.connection.execute(
            "INSERT INTO budgets (id, category_id, limit_minor, period, start_epoch_ms, end_epoch_ms)
             VALUES (?1, ?2, ?3, ?4, ?5, ?6)",
            params![
                budget.id,
                budget.category_id,
                budget.limit_minor,
                budget.period,
                budget.start_epoch_ms,
                budget.end_epoch_ms
            ],
        )?;
        Ok(())
    }

    /// Budgets with live spending (expenses in the budget's own period).
    pub fn list_budgets(&self) -> DbResult<Vec<BudgetWithProgress>> {
        let mut statement = self.connection.prepare(
            "SELECT b.id, b.category_id, c.name, c.icon, c.color_hex,
                    b.limit_minor, b.period, b.start_epoch_ms, b.end_epoch_ms,
                    COALESCE((
                        SELECT SUM(t.amount_minor) FROM transactions t
                        WHERE t.category_id = b.category_id
                          AND t.transaction_type = 'expense'
                          AND t.occurred_at_epoch_ms >= b.start_epoch_ms
                          AND t.occurred_at_epoch_ms < b.end_epoch_ms
                    ), 0)
             FROM budgets b
             JOIN categories c ON c.id = b.category_id
             ORDER BY c.name ASC",
        )?;

        let rows = statement.query_map([], |row| {
            Ok(BudgetWithProgress {
                id: row.get(0)?,
                category_id: row.get(1)?,
                category_name: row.get(2)?,
                category_icon: row.get(3)?,
                category_color_hex: row.get(4)?,
                limit_minor: row.get(5)?,
                period: row.get(6)?,
                start_epoch_ms: row.get(7)?,
                end_epoch_ms: row.get(8)?,
                spent_minor: row.get(9)?,
            })
        })?;

        collect(rows)
    }

    // MARK: - Subscriptions

    /// Inserts a subscription.
    pub fn insert_subscription(&self, subscription: &NewSubscription) -> DbResult<()> {
        self.connection.execute(
            "INSERT INTO subscriptions (id, name, amount_minor, billing_cycle, next_due_epoch_ms, is_active)
             VALUES (?1, ?2, ?3, ?4, ?5, ?6)",
            params![
                subscription.id,
                subscription.name,
                subscription.amount_minor,
                subscription.billing_cycle,
                subscription.next_due_epoch_ms,
                subscription.is_active as i64
            ],
        )?;
        Ok(())
    }

    /// Subscriptions ordered by next due date, active first.
    pub fn list_subscriptions(&self) -> DbResult<Vec<SubscriptionSummary>> {
        let mut statement = self.connection.prepare(
            "SELECT id, name, amount_minor, billing_cycle, next_due_epoch_ms, is_active
             FROM subscriptions
             ORDER BY is_active DESC, next_due_epoch_ms ASC",
        )?;

        let rows = statement.query_map([], |row| {
            Ok(SubscriptionSummary {
                id: row.get(0)?,
                name: row.get(1)?,
                amount_minor: row.get(2)?,
                billing_cycle: row.get(3)?,
                next_due_epoch_ms: row.get(4)?,
                is_active: row.get::<_, i64>(5)? != 0,
            })
        })?;

        collect(rows)
    }

    // MARK: - Analytics

    /// Expense totals grouped by category for a period, biggest first.
    pub fn category_breakdown(
        &self,
        start_epoch_ms: i64,
        end_epoch_ms: i64,
    ) -> DbResult<Vec<CategorySpendSummary>> {
        let mut statement = self.connection.prepare(
            "SELECT c.id, c.name, c.icon, c.color_hex,
                    COALESCE(SUM(t.amount_minor), 0) AS total,
                    COUNT(t.id) AS tx_count
             FROM categories c
             LEFT JOIN transactions t
                    ON t.category_id = c.id
                   AND t.transaction_type = 'expense'
                   AND t.occurred_at_epoch_ms >= ?1
                   AND t.occurred_at_epoch_ms < ?2
             GROUP BY c.id
             HAVING total > 0
             ORDER BY total DESC",
        )?;

        let rows = statement.query_map(params![start_epoch_ms, end_epoch_ms], |row| {
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
        let mut statement = self.connection.prepare(
            "SELECT title, COALESCE(SUM(amount_minor), 0) AS total, COUNT(id) AS tx_count
             FROM transactions
             WHERE transaction_type = 'expense'
               AND occurred_at_epoch_ms >= ?1
               AND occurred_at_epoch_ms < ?2
             GROUP BY title
             ORDER BY total DESC
             LIMIT ?3",
        )?;

        let rows =
            statement.query_map(params![start_epoch_ms, end_epoch_ms, limit as i64], |row| {
                Ok(MerchantSpendSummary {
                    merchant: row.get(0)?,
                    total_minor: row.get(1)?,
                    transaction_count: row.get(2)?,
                })
            })?;

        collect(rows)
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

/// Collects a mapped row iterator into a Vec, converting errors.
fn collect<T>(
    rows: rusqlite::MappedRows<'_, impl FnMut(&rusqlite::Row<'_>) -> rusqlite::Result<T>>,
) -> DbResult<Vec<T>> {
    let mut results = Vec::new();
    for row in rows {
        results.push(row?);
    }
    Ok(results)
}

fn map_full_row(row: &rusqlite::Row<'_>) -> rusqlite::Result<Transaction> {
    let type_value: String = row.get(4)?;
    let transaction_type = TransactionType::from_str_value(&type_value).ok_or_else(|| {
        rusqlite::Error::FromSqlConversionFailure(
            4,
            rusqlite::types::Type::Text,
            Box::new(std::io::Error::new(
                std::io::ErrorKind::InvalidData,
                format!("unknown transaction type: {type_value}"),
            )),
        )
    })?;

    Ok(Transaction {
        id: row.get(0)?,
        title: row.get(1)?,
        amount_minor: row.get(2)?,
        currency: row.get(3)?,
        transaction_type,
        category_id: row.get(5)?,
        occurred_at_epoch_ms: row.get(6)?,
        account_id: row.get(7)?,
        reference: row.get(8)?,
        balance_after_minor: row.get(9)?,
        notes: row.get(10)?,
        is_auto_tracked: row.get::<_, i64>(11)? != 0,
    })
}

pub fn now_epoch_ms() -> i64 {
    std::time::SystemTime::now()
        .duration_since(std::time::UNIX_EPOCH)
        .map(|duration| duration.as_millis() as i64)
        .unwrap_or(0)
}

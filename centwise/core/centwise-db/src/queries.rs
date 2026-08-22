use centwise_domain::{
    Account, HomeDashboard, NewTransaction, Transaction, TransactionSummary, TransactionType,
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

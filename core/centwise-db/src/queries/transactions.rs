use centwise_domain::{
    HomeDashboard, NewTransaction, Transaction, TransactionSummary, TransactionType,
};
use rusqlite::{params, OptionalExtension};

use crate::error::{DbError, DbResult};
use crate::queries::{now_epoch_ms, Queries};

impl<'a> Queries<'a> {
    /// Inserts a transaction and updates the account balance in one atomic step.
    pub fn insert_transaction(&self, transaction: &NewTransaction) -> DbResult<()> {
        if transaction.amount_minor < 0 {
            return Err(DbError::Invalid("amount_minor must not be negative".into()));
        }

        let id_exists: i64 = self.connection.query_row(
            "SELECT EXISTS(SELECT 1 FROM transactions WHERE id = ?1)",
            params![transaction.id],
            |row| row.get(0),
        )?;
        if id_exists != 0 {
            return Err(DbError::DuplicateTransaction(transaction.id.clone()));
        }

        if let Some(reference) = transaction.reference.as_deref() {
            if self.reference_exists(reference)? {
                return Err(DbError::DuplicateReference(reference.to_string()));
            }
        }

        let now = now_epoch_ms();
        let affected = self.connection.execute(
            "INSERT INTO transactions (
                id, title, amount_minor, currency, transaction_type, category_id,
                occurred_at_epoch_ms, account_id, reference, balance_after_minor,
                notes, raw_sms, fee_minor, is_auto_tracked, created_at_epoch_ms
            ) VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11, ?12, ?13, ?14, ?15)",
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
                transaction.raw_sms,
                transaction.fee_minor,
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

    pub fn update_transaction(&self, transaction: &NewTransaction) -> DbResult<bool> {
        if transaction.amount_minor < 0 {
            return Err(DbError::Invalid("amount_minor must not be negative".into()));
        }

        let stored: Option<(i64, String, String)> = self
            .connection
            .query_row(
                "SELECT amount_minor, transaction_type, account_id
                 FROM transactions WHERE id = ?1",
                params![transaction.id],
                |row| Ok((row.get(0)?, row.get(1)?, row.get(2)?)),
            )
            .optional()?;
        let Some((old_amount, old_type, old_account_id)) = stored else {
            return Ok(false);
        };

        if let Some(reference) = transaction.reference.as_deref() {
            let duplicate: i64 = self.connection.query_row(
                "SELECT EXISTS(SELECT 1 FROM transactions
                 WHERE reference = ?1 AND id <> ?2)",
                params![reference, transaction.id],
                |row| row.get(0),
            )?;
            if duplicate != 0 {
                return Err(DbError::DuplicateReference(reference.to_string()));
            }
        }

        let old_type = TransactionType::from_str_value(&old_type)
            .ok_or_else(|| DbError::Corrupt(format!("unknown transaction type: {old_type}")))?;
        let old_delta = balance_delta(old_type, old_amount);
        let new_delta = balance_delta(transaction.transaction_type, transaction.amount_minor);

        let changed = self.connection.execute(
            "UPDATE transactions SET title = ?1, amount_minor = ?2, currency = ?3,
                    transaction_type = ?4, category_id = ?5, occurred_at_epoch_ms = ?6,
                    account_id = ?7, reference = ?8, balance_after_minor = ?9,
                    notes = ?10, raw_sms = ?11, fee_minor = ?12, is_auto_tracked = ?13
             WHERE id = ?14",
            params![
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
                transaction.raw_sms,
                transaction.fee_minor,
                transaction.is_auto_tracked as i64,
                transaction.id
            ],
        )?;
        if changed != 1 {
            return Ok(false);
        }

        if old_account_id == transaction.account_id {
            self.connection.execute(
                "UPDATE accounts SET balance_minor = balance_minor - ?1 + ?2 WHERE id = ?3",
                params![old_delta, new_delta, old_account_id],
            )?;
        } else {
            self.connection.execute(
                "UPDATE accounts SET balance_minor = balance_minor - ?1 WHERE id = ?2",
                params![old_delta, old_account_id],
            )?;
            self.connection.execute(
                "UPDATE accounts SET balance_minor = balance_minor + ?1 WHERE id = ?2",
                params![new_delta, transaction.account_id],
            )?;
        }

        Ok(true)
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
                    notes, raw_sms, fee_minor, is_auto_tracked
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

pub(crate) fn balance_delta(transaction_type: TransactionType, amount_minor: i64) -> i64 {
    match transaction_type {
        TransactionType::Income => amount_minor,
        TransactionType::Expense | TransactionType::Refund => -amount_minor,
        TransactionType::Transfer => 0,
    }
}

pub(crate) fn map_full_row(row: &rusqlite::Row<'_>) -> rusqlite::Result<Transaction> {
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
        raw_sms: row.get(11)?,
        fee_minor: row.get(12)?,
        is_auto_tracked: row.get::<_, i64>(13)? != 0,
    })
}

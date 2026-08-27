use centwise_domain::{NewTransaction, ReviewQueueItem, TransactionType};
use rusqlite::params;

use crate::error::{DbError, DbResult};
use crate::queries::{collect, now_epoch_ms, Queries};

impl<'a> Queries<'a> {
    /// Adds a pending review item unless the same reference is already active.
    pub fn insert_review_queue_item(&self, item: &ReviewQueueItem) -> DbResult<bool> {
        if item.raw_sms.trim().is_empty() {
            return Err(DbError::Invalid("raw_sms must not be empty".into()));
        }
        let id_exists: i64 = self.connection.query_row(
            "SELECT EXISTS(SELECT 1 FROM review_queue WHERE id = ?1)",
            params![item.id],
            |row| row.get(0),
        )?;
        if id_exists != 0 {
            return Ok(false);
        }
        if let Some(reference) = item.reference.as_deref() {
            if self.reference_exists(reference)? {
                return Ok(false);
            }
        }

        let affected = self.connection.execute(
            "INSERT INTO review_queue (
                id, sender, raw_sms, received_at_epoch_ms, provider_id, reason,
                candidate_amount_minor, candidate_type, fee_minor, balance_after_minor,
                reference, party, merchant, category_id, account_last4, account_hint,
                created_at_epoch_ms, status
            ) VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11, ?12, ?13, ?14, ?15, ?16, ?17, 'pending')",
            params![
                item.id,
                item.sender,
                item.raw_sms,
                item.received_at_epoch_ms,
                item.provider_id,
                item.reason,
                item.candidate_amount_minor,
                item.candidate_type.map(|kind| kind.as_str()),
                item.fee_minor,
                item.balance_after_minor,
                item.reference,
                item.party,
                item.merchant,
                item.category_id,
                item.account_last4,
                item.account_hint,
                now_epoch_ms()
            ],
        )?;

        Ok(affected == 1)
    }

    /// Lists pending review items, newest received message first.
    pub fn list_review_queue(&self, limit: u32) -> DbResult<Vec<ReviewQueueItem>> {
        let mut statement = self.connection.prepare(
            "SELECT id, sender, raw_sms, received_at_epoch_ms, provider_id, reason,
                    candidate_amount_minor, candidate_type, fee_minor, balance_after_minor,
                    reference, party, merchant, category_id, account_last4, account_hint
             FROM review_queue
             WHERE status = 'pending'
             ORDER BY received_at_epoch_ms DESC, created_at_epoch_ms DESC
             LIMIT ?1",
        )?;

        let rows = statement.query_map(params![limit as i64], map_review_queue_row)?;
        collect(rows)
    }

    /// Dismisses a pending review item without deleting its audit record.
    pub fn dismiss_review_queue_item(&self, id: &str) -> DbResult<bool> {
        Ok(self.connection.execute(
            "UPDATE review_queue SET status = 'dismissed' WHERE id = ?1 AND status = 'pending'",
            params![id],
        )? == 1)
    }

    /// Converts a pending review item and marks it converted in the same DB transaction.
    pub fn convert_review_queue_item(
        &self,
        id: &str,
        transaction: &NewTransaction,
    ) -> DbResult<bool> {
        let account_exists: i64 = self.connection.query_row(
            "SELECT COUNT(*) FROM accounts WHERE id = ?1 AND archived = 0",
            params![transaction.account_id],
            |row| row.get(0),
        )?;
        if account_exists == 0 {
            return Ok(false);
        }

        let affected = self.connection.execute(
            "UPDATE review_queue SET status = 'converted' WHERE id = ?1 AND status = 'pending'",
            params![id],
        )?;
        if affected == 0 {
            return Ok(false);
        }

        self.insert_transaction(transaction)?;
        Ok(true)
    }

    pub(crate) fn reference_exists(&self, reference: &str) -> DbResult<bool> {
        let transaction_exists: i64 = self.connection.query_row(
            "SELECT EXISTS(SELECT 1 FROM transactions WHERE reference = ?1)",
            params![reference],
            |row| row.get(0),
        )?;
        if transaction_exists != 0 {
            return Ok(true);
        }

        let queued_exists: i64 = self.connection.query_row(
            "SELECT EXISTS(SELECT 1 FROM review_queue WHERE reference = ?1 AND status = 'pending')",
            params![reference],
            |row| row.get(0),
        )?;
        Ok(queued_exists != 0)
    }
}

pub(crate) fn map_review_queue_row(row: &rusqlite::Row<'_>) -> rusqlite::Result<ReviewQueueItem> {
    let type_value: Option<String> = row.get(7)?;
    let candidate_type = match type_value.as_deref() {
        None => None,
        Some(value) => Some(TransactionType::from_str_value(value).ok_or_else(|| {
            rusqlite::Error::FromSqlConversionFailure(
                7,
                rusqlite::types::Type::Text,
                Box::new(std::io::Error::new(
                    std::io::ErrorKind::InvalidData,
                    "unknown review transaction type",
                )),
            )
        })?),
    };

    Ok(ReviewQueueItem {
        id: row.get(0)?,
        sender: row.get(1)?,
        raw_sms: row.get(2)?,
        received_at_epoch_ms: row.get(3)?,
        provider_id: row.get(4)?,
        reason: row.get(5)?,
        candidate_amount_minor: row.get(6)?,
        candidate_type,
        fee_minor: row.get(8)?,
        balance_after_minor: row.get(9)?,
        reference: row.get(10)?,
        party: row.get(11)?,
        merchant: row.get(12)?,
        category_id: row.get(13)?,
        account_last4: row.get(14)?,
        account_hint: row.get(15)?,
    })
}

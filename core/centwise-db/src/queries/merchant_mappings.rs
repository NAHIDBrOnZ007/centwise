use centwise_domain::TransactionType;
use rusqlite::{params, OptionalExtension};

use crate::error::{DbError, DbResult};
use crate::queries::{now_epoch_ms, Queries};

impl<'a> Queries<'a> {
    pub fn matching_merchant_category(
        &self,
        merchant: &str,
        transaction_type: TransactionType,
    ) -> DbResult<Option<String>> {
        let key = normalize_merchant_key(merchant);
        if key.is_empty() {
            return Ok(None);
        }
        self.connection
            .query_row(
                "SELECT category_id FROM merchant_category_mappings
                 WHERE normalized_merchant = ?1 AND transaction_type = ?2",
                params![key, transaction_type.as_str()],
                |row| row.get(0),
            )
            .optional()
            .map_err(Into::into)
    }

    pub fn upsert_merchant_category_mapping(
        &self,
        merchant: &str,
        transaction_type: TransactionType,
        category_id: &str,
    ) -> DbResult<()> {
        let key = normalize_merchant_key(merchant);
        if key.is_empty() {
            return Err(DbError::Invalid(
                "merchant mapping key must not be empty".into(),
            ));
        }
        let category_exists: i64 = self.connection.query_row(
            "SELECT EXISTS(SELECT 1 FROM categories WHERE id = ?1)",
            params![category_id],
            |row| row.get(0),
        )?;
        if category_exists == 0 {
            return Err(DbError::Invalid(
                "merchant mapping category does not exist".into(),
            ));
        }

        let now = now_epoch_ms();
        self.connection.execute(
            "INSERT INTO merchant_category_mappings
                (normalized_merchant, transaction_type, category_id,
                 created_at_epoch_ms, updated_at_epoch_ms)
             VALUES (?1, ?2, ?3, ?4, ?4)
             ON CONFLICT(normalized_merchant, transaction_type) DO UPDATE SET
                category_id = excluded.category_id,
                updated_at_epoch_ms = excluded.updated_at_epoch_ms",
            params![key, transaction_type.as_str(), category_id, now],
        )?;
        Ok(())
    }
}

pub(crate) fn normalize_merchant_key(value: &str) -> String {
    let mut normalized = String::with_capacity(value.len());
    let mut pending_space = false;
    for character in value.trim().to_lowercase().chars() {
        if character.is_alphanumeric() {
            if pending_space && !normalized.is_empty() {
                normalized.push(' ');
            }
            normalized.push(character);
            pending_space = false;
        } else {
            pending_space = true;
        }
    }
    normalized
}

#[cfg(test)]
mod tests {
    use super::normalize_merchant_key;

    #[test]
    fn merchant_keys_ignore_case_punctuation_and_spacing() {
        assert_eq!(normalize_merchant_key("  Cafe-DHAKA! "), "cafe dhaka");
    }
}

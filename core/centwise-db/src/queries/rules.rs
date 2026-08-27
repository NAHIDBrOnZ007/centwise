use centwise_domain::{NewSmartRule, RuleMatchType, SmartRule, TransactionType};
use rusqlite::{params, Connection};

use crate::error::{DbError, DbResult};
use crate::queries::Queries;

impl<'a> Queries<'a> {
    pub fn list_rules(&self) -> DbResult<Vec<SmartRule>> {
        let mut statement = self.connection.prepare(
            "SELECT id, name, keyword, match_type, category_id, transaction_type,
                    is_enabled, sort_order
             FROM rules
             ORDER BY sort_order ASC, id ASC",
        )?;
        let rows = statement.query_map([], |row| {
            let match_type: String = row.get(3)?;
            let transaction_type: String = row.get(5)?;
            Ok(SmartRule {
                id: row.get(0)?,
                name: row.get(1)?,
                keyword: row.get(2)?,
                match_type: RuleMatchType::from_str_value(&match_type).ok_or_else(|| {
                    rusqlite::Error::FromSqlConversionFailure(
                        3,
                        rusqlite::types::Type::Text,
                        Box::new(DbError::Corrupt(format!(
                            "unknown rule match type: {match_type}"
                        ))),
                    )
                })?,
                category_id: row.get(4)?,
                transaction_type: TransactionType::from_str_value(&transaction_type).ok_or_else(
                    || {
                        rusqlite::Error::FromSqlConversionFailure(
                            5,
                            rusqlite::types::Type::Text,
                            Box::new(DbError::Corrupt(format!(
                                "unknown rule transaction type: {transaction_type}"
                            ))),
                        )
                    },
                )?,
                is_enabled: row.get::<_, i64>(6)? != 0,
                sort_order: row.get(7)?,
            })
        })?;

        rows.collect::<Result<Vec<_>, _>>().map_err(Into::into)
    }

    pub fn insert_rule(&self, rule: &NewSmartRule) -> DbResult<()> {
        validate_rule(self.connection, rule)?;
        self.connection.execute(
            "INSERT INTO rules
                (id, name, keyword, match_type, category_id, transaction_type,
                 is_enabled, sort_order)
             VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7,
                 COALESCE((SELECT MAX(sort_order) + 1 FROM rules), 0))",
            params![
                rule.id,
                rule.name.trim(),
                rule.keyword.trim(),
                rule.match_type.as_str(),
                rule.category_id,
                rule.transaction_type.as_str(),
                rule.is_enabled as i64
            ],
        )?;
        Ok(())
    }

    pub fn update_rule(&self, rule: &NewSmartRule) -> DbResult<bool> {
        validate_rule(self.connection, rule)?;
        let changed = self.connection.execute(
            "UPDATE rules SET name = ?1, keyword = ?2, match_type = ?3,
                    category_id = ?4, transaction_type = ?5, is_enabled = ?6
             WHERE id = ?7",
            params![
                rule.name.trim(),
                rule.keyword.trim(),
                rule.match_type.as_str(),
                rule.category_id,
                rule.transaction_type.as_str(),
                rule.is_enabled as i64,
                rule.id
            ],
        )?;
        Ok(changed == 1)
    }

    pub fn delete_rule(&self, id: &str) -> DbResult<bool> {
        Ok(self
            .connection
            .execute("DELETE FROM rules WHERE id = ?1", params![id])?
            == 1)
    }

    pub fn matching_rule(
        &self,
        merchant_or_party: &str,
        transaction_type: TransactionType,
    ) -> DbResult<Option<SmartRule>> {
        Ok(self.list_rules()?.into_iter().find(|rule| {
            rule.is_enabled
                && rule.transaction_type == transaction_type
                && rule.match_type.matches(merchant_or_party, &rule.keyword)
        }))
    }
}

fn validate_rule(connection: &Connection, rule: &NewSmartRule) -> DbResult<()> {
    if rule.name.trim().is_empty() {
        return Err(DbError::Invalid("rule name must not be empty".into()));
    }
    if rule.keyword.trim().is_empty() {
        return Err(DbError::Invalid("rule keyword must not be empty".into()));
    }
    let category_exists: i64 = connection.query_row(
        "SELECT EXISTS(SELECT 1 FROM categories WHERE id = ?1)",
        params![rule.category_id],
        |row| row.get(0),
    )?;
    if category_exists == 0 {
        return Err(DbError::Invalid("rule category does not exist".into()));
    }
    Ok(())
}

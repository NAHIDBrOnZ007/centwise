use centwise_domain::{CategorySummary, NewCategory};
use rusqlite::{params, OptionalExtension};

use crate::error::{DbError, DbResult};
use crate::queries::Queries;

impl<'a> Queries<'a> {
    pub fn category_count(&self) -> DbResult<u32> {
        Ok(self
            .connection
            .query_row("SELECT COUNT(*) FROM categories", [], |row| {
                row.get::<_, i64>(0)
            })? as u32)
    }

    pub fn list_categories(&self) -> DbResult<Vec<CategorySummary>> {
        let mut statement = self.connection.prepare(
            "SELECT id, name, icon, color_hex, is_system, sort_order
             FROM categories
             ORDER BY sort_order ASC, id ASC",
        )?;
        let rows = statement.query_map([], |row| {
            Ok(CategorySummary {
                id: row.get(0)?,
                name: row.get(1)?,
                icon: row.get(2)?,
                color_hex: row.get(3)?,
                is_system: row.get::<_, i64>(4)? != 0,
                sort_order: row.get(5)?,
            })
        })?;
        rows.collect::<Result<Vec<_>, _>>().map_err(Into::into)
    }

    pub fn insert_category(&self, category: &NewCategory) -> DbResult<()> {
        if category.name.trim().is_empty() {
            return Err(DbError::Invalid("category name must not be empty".into()));
        }

        self.connection.execute(
            "INSERT INTO categories (id, name, icon, color_hex, is_system, sort_order)
             VALUES (?1, ?2, ?3, ?4, 0,
                 COALESCE((SELECT MAX(sort_order) + 1 FROM categories), 0))",
            params![
                category.id,
                category.name.trim(),
                category.icon,
                category.color_hex
            ],
        )?;
        Ok(())
    }

    pub fn update_category(&self, category: &NewCategory) -> DbResult<bool> {
        let system: Option<i64> = self
            .connection
            .query_row(
                "SELECT is_system FROM categories WHERE id = ?1",
                params![category.id],
                |row| row.get(0),
            )
            .optional()?;
        let Some(system) = system else {
            return Ok(false);
        };
        if system != 0 {
            return Err(DbError::Invalid(
                "system categories cannot be modified".into(),
            ));
        }
        if category.name.trim().is_empty() {
            return Err(DbError::Invalid("category name must not be empty".into()));
        }

        let changed = self.connection.execute(
            "UPDATE categories SET name = ?1, icon = ?2, color_hex = ?3 WHERE id = ?4",
            params![
                category.name.trim(),
                category.icon,
                category.color_hex,
                category.id
            ],
        )?;
        Ok(changed == 1)
    }

    pub fn delete_category(&self, id: &str) -> DbResult<bool> {
        let system: Option<i64> = self
            .connection
            .query_row(
                "SELECT is_system FROM categories WHERE id = ?1",
                params![id],
                |row| row.get(0),
            )
            .optional()?;
        let Some(system) = system else {
            return Ok(false);
        };
        if system != 0 {
            return Err(DbError::Invalid(
                "system categories cannot be deleted".into(),
            ));
        }

        let reference_count: i64 = self.connection.query_row(
            "SELECT
                (SELECT COUNT(*) FROM transactions WHERE category_id = ?1) +
                (SELECT COUNT(*) FROM budgets WHERE category_id = ?1) +
                (SELECT COUNT(*) FROM rules WHERE category_id = ?1) +
                (SELECT COUNT(*) FROM merchant_category_mappings WHERE category_id = ?1) +
                (SELECT COUNT(*) FROM review_queue WHERE category_id = ?1 AND status = 'pending')",
            params![id],
            |row| row.get(0),
        )?;
        if reference_count > 0 {
            return Err(DbError::Invalid(
                "category is still used by transactions, budgets, rules, or mappings".into(),
            ));
        }

        Ok(self
            .connection
            .execute("DELETE FROM categories WHERE id = ?1", params![id])?
            == 1)
    }
}

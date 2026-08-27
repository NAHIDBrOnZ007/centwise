use centwise_domain::{BudgetWithProgress, NewBudget};
use rusqlite::params;

use crate::error::{DbError, DbResult};
use crate::queries::{collect, Queries};

impl<'a> Queries<'a> {
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

    pub fn update_budget(&self, budget: &NewBudget) -> DbResult<bool> {
        if budget.limit_minor <= 0 {
            return Err(DbError::Invalid("budget limit must be positive".into()));
        }
        let changed = self.connection.execute(
            "UPDATE budgets SET category_id = ?1, limit_minor = ?2, period = ?3,
                    start_epoch_ms = ?4, end_epoch_ms = ?5
             WHERE id = ?6",
            params![
                budget.category_id,
                budget.limit_minor,
                budget.period,
                budget.start_epoch_ms,
                budget.end_epoch_ms,
                budget.id
            ],
        )?;
        Ok(changed == 1)
    }

    pub fn delete_budget(&self, id: &str) -> DbResult<bool> {
        Ok(self
            .connection
            .execute("DELETE FROM budgets WHERE id = ?1", params![id])?
            == 1)
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
}

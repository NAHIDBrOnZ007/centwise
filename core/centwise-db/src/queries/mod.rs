pub mod accounts;
pub mod analytics;
pub mod budgets;
pub mod categories;
pub mod review_queue;
pub mod rules;
pub mod subscriptions;
pub mod transactions;

use rusqlite::Connection;

use crate::error::DbResult;

/// Writes and screen-shaped reads. All writes must go through this module so
/// balances stay transactional and the update hook fires for listeners.
pub struct Queries<'a> {
    pub(crate) connection: &'a Connection,
}

impl<'a> Queries<'a> {
    pub fn new(connection: &'a Connection) -> Self {
        Queries { connection }
    }

    pub fn clear_user_records(&self) -> DbResult<()> {
        self.connection.execute_batch(
            "DELETE FROM review_queue;
             DELETE FROM transactions;
             DELETE FROM budgets;
             DELETE FROM subscriptions;
             DELETE FROM accounts;
             DELETE FROM rules;
             DELETE FROM categories WHERE is_system = 0;",
        )?;
        Ok(())
    }
}

pub(crate) fn now_epoch_ms() -> i64 {
    std::time::SystemTime::now()
        .duration_since(std::time::UNIX_EPOCH)
        .map(|duration| duration.as_millis() as i64)
        .unwrap_or(0)
}

/// Collects a mapped row iterator into a Vec, converting errors.
pub(crate) fn collect<T>(
    rows: rusqlite::MappedRows<'_, impl FnMut(&rusqlite::Row<'_>) -> rusqlite::Result<T>>,
) -> DbResult<Vec<T>> {
    let mut results = Vec::new();
    for row in rows {
        results.push(row?);
    }
    Ok(results)
}

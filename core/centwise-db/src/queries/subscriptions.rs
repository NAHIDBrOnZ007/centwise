use centwise_domain::{NewSubscription, SubscriptionSummary};
use rusqlite::params;

use crate::error::DbResult;
use crate::queries::{collect, Queries};

impl<'a> Queries<'a> {
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

    pub fn update_subscription(&self, subscription: &NewSubscription) -> DbResult<bool> {
        let changed = self.connection.execute(
            "UPDATE subscriptions SET name = ?1, amount_minor = ?2,
                    billing_cycle = ?3, next_due_epoch_ms = ?4, is_active = ?5
             WHERE id = ?6",
            params![
                subscription.name,
                subscription.amount_minor,
                subscription.billing_cycle,
                subscription.next_due_epoch_ms,
                subscription.is_active as i64,
                subscription.id
            ],
        )?;
        Ok(changed == 1)
    }

    pub fn delete_subscription(&self, id: &str) -> DbResult<bool> {
        Ok(self
            .connection
            .execute("DELETE FROM subscriptions WHERE id = ?1", params![id])?
            == 1)
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
}

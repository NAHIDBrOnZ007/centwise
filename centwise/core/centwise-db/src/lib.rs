//! Centwise shared SQLite database.
//!
//! One database, one migration runner, all writes through Rust — the single
//! source of truth for both Android and iOS (docs/decisions/0001).

mod demo;
pub mod error;
pub mod migrations;
pub mod notify;
pub mod queries;

use std::path::Path;
use std::sync::{Arc, Mutex, MutexGuard};

use notify::{DataObserver, ObserverRegistry};
use rusqlite::Connection;

use centwise_domain::{NewTransaction, ReviewQueueItem, Transaction};
pub use error::{DbError, DbResult};
pub use queries::Queries;

pub use demo::DemoDataSummary;

/// Open handle to the Centwise database. Cheap to clone via `Arc` at the FFI
/// boundary. Safe to use from any thread; writes serialize on one connection.
pub struct Database {
    connection: Mutex<Connection>,
    registry: Arc<ObserverRegistry>,
}

impl Database {
    /// Opens (creating if needed) the database at `path`, applies migrations,
    /// enables WAL and foreign keys, and installs the change hook.
    pub fn open(path: impl AsRef<Path>) -> DbResult<Database> {
        let connection = Connection::open(path)?;
        Self::initialize(connection)
    }

    /// In-memory variant for tests and previews.
    pub fn open_in_memory() -> DbResult<Database> {
        let connection = Connection::open_in_memory()?;
        Self::initialize(connection)
    }

    fn initialize(connection: Connection) -> DbResult<Database> {
        // WAL allows the app, extensions, and widgets to read concurrently.
        let _: String = connection.query_row("PRAGMA journal_mode=WAL", [], |row| row.get(0))?;
        connection.busy_timeout(std::time::Duration::from_millis(3_000))?;
        connection.pragma_update(None, "foreign_keys", "ON")?;

        migrations::run(&connection)?;

        let registry = Arc::new(ObserverRegistry::new());
        let hook_registry = Arc::clone(&registry);
        connection.update_hook(Some(
            move |_action: rusqlite::hooks::Action, _database: &str, table: &str, _row_id: i64| {
                if !table.starts_with("sqlite_") {
                    hook_registry.notify_all();
                }
            },
        ));

        Ok(Database {
            connection: Mutex::new(connection),
            registry,
        })
    }

    /// Registers a change observer. Called after every data write.
    pub fn add_observer(&self, observer: Arc<dyn DataObserver>) {
        self.registry.add(observer);
    }

    /// Runs reads on the connection.
    pub fn read<T>(&self, operation: impl FnOnce(&Queries<'_>) -> DbResult<T>) -> DbResult<T> {
        let guard = self.lock();
        let queries = Queries::new(&guard);
        operation(&queries)
    }

    /// Runs a write on the connection inside a transaction. Note the SQLite
    /// update hook fires as statements execute; listeners must re-query, never
    /// assume a specific write completed.
    pub fn write<T>(&self, operation: impl FnOnce(&Queries<'_>) -> DbResult<T>) -> DbResult<T> {
        let guard = self.lock();
        let transaction = guard.unchecked_transaction()?;
        let queries = Queries::new(&transaction);
        let result = operation(&queries)?;
        transaction.commit()?;
        Ok(result)
    }

    fn lock(&self) -> MutexGuard<'_, Connection> {
        self.connection
            .lock()
            .unwrap_or_else(|poisoned| poisoned.into_inner())
    }
}

/// Convenience write helpers used by the FFI layer.
impl Database {
    pub fn insert_transaction(&self, transaction: &NewTransaction) -> DbResult<()> {
        self.write(|queries| queries.insert_transaction(transaction))
    }

    pub fn delete_transaction(&self, id: &str) -> DbResult<bool> {
        self.write(|queries| queries.delete_transaction(id))
    }

    pub fn list_transactions(&self, limit: u32) -> DbResult<Vec<Transaction>> {
        self.read(|queries| queries.list_transactions(limit))
    }

    pub fn list_accounts(&self) -> DbResult<Vec<centwise_domain::AccountSummary>> {
        self.read(|queries| queries.list_accounts())
    }

    pub fn list_budgets(&self) -> DbResult<Vec<centwise_domain::BudgetWithProgress>> {
        self.read(|queries| queries.list_budgets())
    }

    pub fn list_subscriptions(&self) -> DbResult<Vec<centwise_domain::SubscriptionSummary>> {
        self.read(|queries| queries.list_subscriptions())
    }

    pub fn category_count(&self) -> DbResult<u32> {
        self.read(|queries| queries.category_count())
    }

    pub fn list_categories(&self) -> DbResult<Vec<centwise_domain::CategorySummary>> {
        self.read(|queries| queries.list_categories())
    }

    /// Replaces all user records with the deterministic Rust-owned demo set.
    /// This is explicit because it is destructive to the current local data.
    pub fn replace_with_demo_data(&self) -> DbResult<DemoDataSummary> {
        self.replace_with_demo_data_at(current_epoch_ms())
    }

    pub fn replace_with_demo_data_at(&self, now_epoch_ms: i64) -> DbResult<DemoDataSummary> {
        self.write(|queries| {
            queries.clear_user_records()?;
            demo::populate(queries, now_epoch_ms)
        })
    }

    /// Clears user records while preserving system categories.
    pub fn reset_to_empty(&self) -> DbResult<()> {
        self.write(|queries| queries.clear_user_records())
    }

    pub fn account_balance(&self, account_id: &str) -> DbResult<i64> {
        self.read(|queries| queries.account_balance(account_id))
    }

    pub fn dismiss_review_queue_item(&self, id: &str) -> DbResult<bool> {
        self.write(|queries| queries.dismiss_review_queue_item(id))
    }

    pub fn convert_review_queue_item(
        &self,
        id: &str,
        transaction: &NewTransaction,
    ) -> DbResult<bool> {
        self.write(|queries| queries.convert_review_queue_item(id, transaction))
    }

    pub fn list_review_queue(&self, limit: u32) -> DbResult<Vec<ReviewQueueItem>> {
        self.read(|queries| queries.list_review_queue(limit))
    }
}

fn current_epoch_ms() -> i64 {
    std::time::SystemTime::now()
        .duration_since(std::time::UNIX_EPOCH)
        .unwrap_or_default()
        .as_millis() as i64
}

#[cfg(test)]
mod tests {
    use super::Database;
    use centwise_domain::{Account, NewTransaction, ReviewQueueItem, TransactionType};

    #[test]
    fn review_conversion_persists_and_updates_balance() {
        let database = Database::open_in_memory().expect("open");
        database
            .write(|queries| {
                queries.insert_account(&Account {
                    id: "acct-1".into(),
                    name: "bKash".into(),
                    provider: "bkash".into(),
                    last_four: None,
                    balance_minor: 0,
                    archived: false,
                })?;
                queries.insert_review_queue_item(&ReviewQueueItem {
                    id: "review-1".into(),
                    sender: Some("bKash".into()),
                    raw_sms: "Payment of Tk 10.00 successful. Ref: TEST-1".into(),
                    received_at_epoch_ms: 1_700_000_000_000,
                    provider_id: Some("bkash".into()),
                    reason: "Needs confirmation".into(),
                    candidate_amount_minor: Some(1_000),
                    candidate_type: Some(TransactionType::Expense),
                    fee_minor: None,
                    balance_after_minor: None,
                    reference: Some("TEST-1".into()),
                    party: None,
                    merchant: None,
                    category_id: Some("other".into()),
                    account_last4: None,
                    account_hint: None,
                })?;
                Ok(())
            })
            .expect("setup");

        let converted = database
            .convert_review_queue_item(
                "review-1",
                &NewTransaction {
                    id: "tx-1".into(),
                    title: "Test payment".into(),
                    amount_minor: 1_000,
                    currency: "BDT".into(),
                    transaction_type: TransactionType::Expense,
                    category_id: "other".into(),
                    occurred_at_epoch_ms: 1_700_000_000_000,
                    account_id: "acct-1".into(),
                    reference: Some("TEST-1".into()),
                    balance_after_minor: None,
                    fee_minor: None,
                    notes: None,
                    raw_sms: Some("Payment of Tk 10.00 successful. Ref: TEST-1".into()),
                    is_auto_tracked: true,
                },
            )
            .expect("convert");

        assert!(converted);
        assert!(database.list_review_queue(10).expect("queue").is_empty());
        assert_eq!(database.account_balance("acct-1").expect("balance"), -1_000);
        assert_eq!(
            database.list_transactions(10).expect("transactions").len(),
            1
        );
    }
}

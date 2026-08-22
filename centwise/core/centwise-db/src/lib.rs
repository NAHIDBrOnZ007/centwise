//! Centwise shared SQLite database.
//!
//! One database, one migration runner, all writes through Rust — the single
//! source of truth for both Android and iOS (docs/decisions/0001).

pub mod error;
pub mod migrations;
pub mod notify;
pub mod queries;

use std::path::Path;
use std::sync::{Arc, Mutex, MutexGuard};

use notify::{DataObserver, ObserverRegistry};
use rusqlite::Connection;

use centwise_domain::{NewTransaction, Transaction};
pub use error::{DbError, DbResult};
pub use queries::Queries;

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
}

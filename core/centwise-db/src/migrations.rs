use rusqlite::Connection;

use crate::error::{DbError, DbResult};

/// One schema migration. `version` is the `user_version` value after running.
pub struct Migration {
    pub version: i64,
    pub name: &'static str,
    pub sql: &'static str,
}

/// All migrations in order. A database at `user_version = N` runs every
/// migration with `version > N` inside a transaction.
///
/// Rules (see docs/decisions/0001-single-rust-database.md):
/// - Append-only: never edit a shipped migration.
/// - Never destructive for released user data.
pub const MIGRATIONS: &[Migration] = &[
    Migration {
        version: 1,
        name: "initial schema",
        sql: r#"
CREATE TABLE accounts (
    id TEXT PRIMARY KEY NOT NULL,
    name TEXT NOT NULL,
    provider TEXT NOT NULL,
    last_four TEXT,
    balance_minor INTEGER NOT NULL DEFAULT 0,
    archived INTEGER NOT NULL DEFAULT 0,
    created_at_epoch_ms INTEGER NOT NULL
);

CREATE TABLE categories (
    id TEXT PRIMARY KEY NOT NULL,
    name TEXT NOT NULL,
    icon TEXT NOT NULL,
    color_hex TEXT NOT NULL,
    is_system INTEGER NOT NULL DEFAULT 0,
    sort_order INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE transactions (
    id TEXT PRIMARY KEY NOT NULL,
    title TEXT NOT NULL,
    amount_minor INTEGER NOT NULL,
    currency TEXT NOT NULL DEFAULT 'BDT',
    transaction_type TEXT NOT NULL,
    category_id TEXT NOT NULL,
    occurred_at_epoch_ms INTEGER NOT NULL,
    account_id TEXT NOT NULL,
    reference TEXT,
    balance_after_minor INTEGER,
    notes TEXT,
    is_auto_tracked INTEGER NOT NULL DEFAULT 0,
    created_at_epoch_ms INTEGER NOT NULL,
    FOREIGN KEY (category_id) REFERENCES categories(id),
    FOREIGN KEY (account_id) REFERENCES accounts(id)
);

CREATE INDEX idx_transactions_occurred ON transactions(occurred_at_epoch_ms DESC);
CREATE INDEX idx_transactions_account ON transactions(account_id);
CREATE INDEX idx_transactions_category ON transactions(category_id);

CREATE TABLE budgets (
    id TEXT PRIMARY KEY NOT NULL,
    category_id TEXT NOT NULL,
    limit_minor INTEGER NOT NULL,
    period TEXT NOT NULL DEFAULT 'monthly',
    start_epoch_ms INTEGER NOT NULL,
    end_epoch_ms INTEGER NOT NULL,
    FOREIGN KEY (category_id) REFERENCES categories(id)
);

CREATE TABLE subscriptions (
    id TEXT PRIMARY KEY NOT NULL,
    name TEXT NOT NULL,
    amount_minor INTEGER NOT NULL,
    billing_cycle TEXT NOT NULL DEFAULT 'monthly',
    next_due_epoch_ms INTEGER NOT NULL,
    account_id TEXT,
    is_active INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE rules (
    id TEXT PRIMARY KEY NOT NULL,
    name TEXT NOT NULL,
    keyword TEXT NOT NULL,
    match_type TEXT NOT NULL DEFAULT 'contains',
    category_id TEXT NOT NULL,
    transaction_type TEXT NOT NULL DEFAULT 'expense',
    is_enabled INTEGER NOT NULL DEFAULT 1,
    sort_order INTEGER NOT NULL DEFAULT 0
);
"#,
    },
    Migration {
        version: 2,
        name: "sms provenance and review queue",
        sql: r#"
ALTER TABLE transactions ADD COLUMN raw_sms TEXT;
ALTER TABLE transactions ADD COLUMN fee_minor INTEGER;

CREATE TABLE review_queue (
    id TEXT PRIMARY KEY NOT NULL,
    sender TEXT,
    raw_sms TEXT NOT NULL,
    received_at_epoch_ms INTEGER NOT NULL,
    provider_id TEXT,
    reason TEXT NOT NULL,
    candidate_amount_minor INTEGER,
    candidate_type TEXT,
    fee_minor INTEGER,
    balance_after_minor INTEGER,
    reference TEXT,
    party TEXT,
    merchant TEXT,
    category_id TEXT,
    account_last4 TEXT,
    account_hint TEXT,
    created_at_epoch_ms INTEGER NOT NULL,
    status TEXT NOT NULL DEFAULT 'pending'
);

CREATE INDEX idx_review_queue_pending
    ON review_queue(status, received_at_epoch_ms DESC);
CREATE INDEX idx_review_queue_reference
    ON review_queue(reference);
CREATE INDEX idx_transactions_reference
    ON transactions(reference);
"#,
    },
    Migration {
        version: 3,
        name: "default smart rules",
        sql: r#"-- Default rules are seeded after category migrations run."#,
    },
    Migration {
        version: 4,
        name: "sms category mappings and provenance",
        sql: r#"
ALTER TABLE transactions ADD COLUMN category_source TEXT;

CREATE TABLE merchant_category_mappings (
    normalized_merchant TEXT NOT NULL,
    transaction_type TEXT NOT NULL,
    category_id TEXT NOT NULL,
    created_at_epoch_ms INTEGER NOT NULL,
    updated_at_epoch_ms INTEGER NOT NULL,
    PRIMARY KEY (normalized_merchant, transaction_type),
    FOREIGN KEY (category_id) REFERENCES categories(id)
);

CREATE INDEX idx_merchant_category_mappings_category
    ON merchant_category_mappings(category_id);
"#,
    },
];

/// Latest schema version available in this build.
pub fn latest_version() -> i64 {
    MIGRATIONS
        .last()
        .map(|migration| migration.version)
        .unwrap_or(0)
}

/// Runs pending migrations and seeds system data on fresh installs.
pub fn run(connection: &Connection) -> DbResult<()> {
    let current: i64 = connection.query_row("PRAGMA user_version", [], |row| row.get(0))?;

    for migration in MIGRATIONS
        .iter()
        .filter(|migration| migration.version > current)
    {
        let transaction = connection.unchecked_transaction()?;
        transaction.execute_batch(migration.sql)?;
        transaction
            .pragma_update(None, "user_version", migration.version)
            .map_err(DbError::from)?;
        transaction.commit()?;
    }

    seed_system_data(connection)?;
    seed_default_rules(connection)?;

    Ok(())
}

fn seed_default_rules(connection: &Connection) -> DbResult<()> {
    let count: i64 = connection
        .query_row("SELECT COUNT(*) FROM rules", [], |row| row.get(0))
        .unwrap_or(0);
    if count > 0 {
        return Ok(());
    }

    let mut statement = connection.prepare(
        "INSERT OR IGNORE INTO rules
            (id, name, keyword, match_type, category_id, transaction_type,
             is_enabled, sort_order)
         VALUES (?1, ?2, ?3, 'contains', ?4, 'expense', 1, ?5)",
    )?;

    for (sort_order, rule) in centwise_domain::default_rules().into_iter().enumerate() {
        statement.execute(rusqlite::params![
            rule.id,
            rule.name,
            rule.keyword,
            rule.category_id,
            sort_order as i64
        ])?;
    }
    Ok(())
}

/// Inserts the default categories when the table is empty.
fn seed_system_data(connection: &Connection) -> DbResult<()> {
    let mut statement = connection.prepare(
        "INSERT OR IGNORE INTO categories (id, name, icon, color_hex, is_system, sort_order)
         VALUES (?1, ?2, ?3, ?4, 1, ?5)",
    )?;

    for (index, category) in centwise_domain::default_categories()
        .into_iter()
        .enumerate()
    {
        statement.execute(rusqlite::params![
            category.id,
            category.name,
            category.icon,
            category.color_hex,
            index as i64
        ])?;
    }

    Ok(())
}

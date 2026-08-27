-- Complete schema snapshot at migration version 2.
-- Existing databases reach this shape through migrations.rs; this file is
-- provided for inspection, tooling, and fresh-schema documentation.
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
    raw_sms TEXT,
    fee_minor INTEGER,
    is_auto_tracked INTEGER NOT NULL DEFAULT 0,
    created_at_epoch_ms INTEGER NOT NULL,
    FOREIGN KEY (category_id) REFERENCES categories(id),
    FOREIGN KEY (account_id) REFERENCES accounts(id)
);

CREATE INDEX idx_transactions_occurred ON transactions(occurred_at_epoch_ms DESC);
CREATE INDEX idx_transactions_account ON transactions(account_id);
CREATE INDEX idx_transactions_category ON transactions(category_id);
CREATE INDEX idx_transactions_reference ON transactions(reference);

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

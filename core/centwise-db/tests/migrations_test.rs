use std::sync::atomic::{AtomicUsize, Ordering};
use std::sync::Arc;

use centwise_db::notify::DataObserver;
use centwise_db::Database;
use centwise_domain::{Account, NewTransaction, TransactionType};

const EXPECTED_SYSTEM_CATEGORY_IDS: &[&str] = &[
    "food",
    "transport",
    "shopping",
    "bills",
    "recharge",
    "salary",
    "income",
    "refunds",
    "cashback",
    "interest-profit",
    "dividends",
    "fees",
    "cash-withdrawal",
    "housing",
    "travel",
    "transfer",
    "health",
    "entertainment",
    "education",
    "other",
];

struct CountingObserver {
    count: AtomicUsize,
}

impl DataObserver for CountingObserver {
    fn data_changed(&self) {
        self.count.fetch_add(1, Ordering::SeqCst);
    }
}

fn test_account() -> Account {
    Account {
        id: "acct-1".into(),
        name: "bKash".into(),
        provider: "bkash".into(),
        last_four: Some("5678".into()),
        balance_minor: 0,
        archived: false,
    }
}

fn test_transaction(
    id: &str,
    amount_minor: i64,
    transaction_type: TransactionType,
) -> NewTransaction {
    NewTransaction {
        id: id.into(),
        title: "Foodpanda order".into(),
        amount_minor,
        currency: "BDT".into(),
        transaction_type,
        category_id: "food".into(),
        occurred_at_epoch_ms: 1_700_000_000_000,
        account_id: "acct-1".into(),
        reference: None,
        balance_after_minor: None,
        fee_minor: None,
        notes: None,
        raw_sms: None,
        is_auto_tracked: true,
    }
}

#[test]
fn fresh_install_creates_latest_schema_and_seeds_categories() {
    let database = Database::open_in_memory().expect("open");

    // Tables exist and are queryable.
    database
        .read(|queries| queries.list_transactions(10).map(|_| ()))
        .expect("schema usable");

    let transactions = database.list_transactions(10).expect("list");
    assert!(transactions.is_empty());

    // System categories were seeded.
    let balance = database
        .read(|queries| queries.account_balance("missing"))
        .expect("accounts queryable");
    assert_eq!(balance, 0);

    let starter_rules = database.list_rules().expect("rules queryable");
    assert_eq!(starter_rules.len(), centwise_domain::default_rules().len());

    let category_ids: Vec<_> = database
        .list_categories()
        .expect("categories")
        .into_iter()
        .map(|category| category.id)
        .collect();
    assert_eq!(category_ids, EXPECTED_SYSTEM_CATEGORY_IDS);
}

#[test]
fn version_three_database_gains_internal_sms_intelligence_schema_without_data_loss() {
    let file = tempfile::NamedTempFile::new().expect("temp file");
    {
        let connection = rusqlite::Connection::open(file.path()).expect("open legacy database");
        connection
            .execute_batch(include_str!("../schemas/v3.sql"))
            .expect("install v3 schema");
        connection
            .pragma_update(None, "user_version", 3)
            .expect("mark v3");
        connection
            .execute(
                "INSERT INTO categories (id, name, icon, color_hex, is_system, sort_order)
                 VALUES ('custom', 'Custom', 'star', '#000000', 0, 0)",
                [],
            )
            .expect("custom category");
    }

    let database = Database::open(file.path()).expect("migrate");
    let categories = database.list_categories().expect("categories");
    assert!(categories.iter().any(|category| category.id == "custom"));
    for expected in EXPECTED_SYSTEM_CATEGORY_IDS {
        assert!(
            categories.iter().any(|category| category.id == *expected),
            "missing system category {expected}"
        );
    }
    drop(database);

    let connection = rusqlite::Connection::open(file.path()).expect("inspect migrated database");
    let mapping_table_count: i64 = connection
        .query_row(
            "SELECT COUNT(*) FROM sqlite_master
             WHERE type = 'table' AND name = 'merchant_category_mappings'",
            [],
            |row| row.get(0),
        )
        .expect("mapping table query");
    assert_eq!(mapping_table_count, 1);

    let has_category_source = connection
        .prepare("PRAGMA table_info(transactions)")
        .expect("transaction columns")
        .query_map([], |row| row.get::<_, String>(1))
        .expect("column rows")
        .filter_map(Result::ok)
        .any(|column| column == "category_source");
    assert!(has_category_source);
}

#[test]
fn migrations_run_once_and_are_idempotent_on_reopen() {
    let file = tempfile::NamedTempFile::new().expect("temp file");

    {
        let database = Database::open(file.path()).expect("first open");
        database
            .write(|queries| {
                queries.insert_account(&test_account())?;
                queries.insert_transaction(&test_transaction(
                    "tx-1",
                    50_000,
                    TransactionType::Expense,
                ))
            })
            .expect("insert");
    }

    // Reopen: migrations must not re-run or lose data.
    let reopened = Database::open(file.path()).expect("reopen");
    let transactions = reopened.list_transactions(10).expect("list");
    assert_eq!(transactions.len(), 1);
    assert_eq!(transactions[0].id, "tx-1");
    assert_eq!(transactions[0].amount_minor, 50_000);
}

#[test]
fn insert_updates_account_balance_atomically() {
    let database = Database::open_in_memory().expect("open");
    let account = test_account();

    database
        .write(|queries| {
            queries.insert_account(&account)?;
            queries.insert_transaction(&test_transaction(
                "tx-expense",
                25_000,
                TransactionType::Expense,
            ))?;
            queries.insert_transaction(&test_transaction(
                "tx-income",
                100_000,
                TransactionType::Income,
            ))
        })
        .expect("writes");

    let balance = database
        .read(|queries| queries.account_balance("acct-1"))
        .expect("balance");
    assert_eq!(balance, 75_000);
}

#[test]
fn category_provenance_is_stored_and_can_be_updated_internally() {
    let database = Database::open_in_memory().expect("open");
    database
        .write(|queries| {
            queries.insert_account(&test_account())?;
            queries.insert_transaction_with_category_source(
                &test_transaction("tx-source", 1_000, TransactionType::Expense),
                Some("system"),
            )?;
            assert_eq!(
                queries.transaction_category_source("tx-source")?,
                Some("system".into())
            );
            queries.set_transaction_category_source("tx-source", "user_correction")?;
            assert_eq!(
                queries.transaction_category_source("tx-source")?,
                Some("user_correction".into())
            );
            Ok(())
        })
        .expect("provenance");
}

#[test]
fn change_notification_fires_on_write() {
    let database = Database::open_in_memory().expect("open");
    let observer = Arc::new(CountingObserver {
        count: AtomicUsize::new(0),
    });
    database.add_observer(observer.clone());

    database
        .write(|queries| {
            queries.insert_account(&test_account())?;
            queries.insert_transaction(&test_transaction(
                "tx-notify",
                1_000,
                TransactionType::Expense,
            ))
        })
        .expect("writes");

    assert!(
        observer.count.load(Ordering::SeqCst) >= 1,
        "listener must fire"
    );
}

#[test]
fn delete_reverses_balance() {
    let database = Database::open_in_memory().expect("open");

    database
        .write(|queries| {
            queries.insert_account(&test_account())?;
            queries.insert_transaction(&test_transaction(
                "tx-del",
                40_000,
                TransactionType::Expense,
            ))
        })
        .expect("setup");

    let deleted = database.delete_transaction("tx-del").expect("delete");
    assert!(deleted);

    let balance = database
        .read(|queries| queries.account_balance("acct-1"))
        .expect("balance");
    assert_eq!(balance, 0);

    let deleted_again = database.delete_transaction("tx-del").expect("delete again");
    assert!(!deleted_again, "second delete must report false");
}

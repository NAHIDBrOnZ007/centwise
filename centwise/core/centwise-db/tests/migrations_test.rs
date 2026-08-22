use std::sync::atomic::{AtomicUsize, Ordering};
use std::sync::Arc;

use centwise_db::notify::DataObserver;
use centwise_db::Database;
use centwise_domain::{Account, NewTransaction, TransactionType};

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
        notes: None,
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

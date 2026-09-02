use centwise_db::Database;
use centwise_domain::{Account, NewTransaction, TransactionType};

fn seed_account(database: &Database) {
    database
        .write(|queries| {
            queries.insert_account(&Account {
                id: "acct-1".into(),
                name: "bKash".into(),
                provider: "bkash".into(),
                last_four: Some("5678".into()),
                balance_minor: 0,
                archived: false,
            })
        })
        .expect("account");
}

fn tx(id: &str, amount_minor: i64, kind: TransactionType, epoch_ms: i64) -> NewTransaction {
    NewTransaction {
        id: id.into(),
        title: format!("Merchant {id}"),
        amount_minor,
        currency: "BDT".into(),
        transaction_type: kind,
        category_id: "food".into(),
        occurred_at_epoch_ms: epoch_ms,
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
fn home_dashboard_sums_period_and_orders_recent() {
    let database = Database::open_in_memory().expect("open");
    seed_account(&database);

    let january_start: i64 = 1_700_000_000_000;
    let january_end: i64 = 1_700_000_000_000 + 30 * 24 * 60 * 60 * 1000;

    database
        .write(|queries| {
            queries.insert_transaction(&tx(
                "in-1",
                500_000,
                TransactionType::Income,
                january_start + 1_000,
            ))?;
            queries.insert_transaction(&tx(
                "out-1",
                150_000,
                TransactionType::Expense,
                january_start + 2_000,
            ))?;
            queries.insert_transaction(&tx(
                "out-2",
                50_000,
                TransactionType::Expense,
                january_start + 3_000,
            ))?;
            // Next period: must not count toward January.
            queries.insert_transaction(&tx(
                "out-3",
                999_999,
                TransactionType::Expense,
                january_end + 10_000,
            ))
        })
        .expect("seed");

    let dashboard = database
        .read(|queries| queries.home_dashboard(january_start, january_end, 5))
        .expect("dashboard");

    assert_eq!(dashboard.period_expense_minor, 200_000);
    assert_eq!(dashboard.period_income_minor, 500_000);

    // Transfers are neither expense nor income.
    database
        .write(|queries| {
            queries.insert_transaction(&tx(
                "move-1",
                70_000,
                TransactionType::Transfer,
                january_start + 4_000,
            ))
        })
        .expect("transfer");

    let dashboard = database
        .read(|queries| queries.home_dashboard(january_start, january_end, 10))
        .expect("dashboard");
    assert_eq!(dashboard.period_expense_minor, 200_000);
    assert_eq!(dashboard.period_income_minor, 500_000);

    // Recent list is newest-first and joins category data.
    let recent_ids: Vec<&str> = dashboard
        .recent_transactions
        .iter()
        .filter(|summary| summary.occurred_at_epoch_ms < january_end)
        .map(|summary| summary.id.as_str())
        .collect();
    assert_eq!(recent_ids, ["move-1", "out-2", "out-1", "in-1"]);
    assert_eq!(
        dashboard.recent_transactions[1].category_name,
        "Food & Dining"
    );
}

#[test]
fn list_respects_limit() {
    let database = Database::open_in_memory().expect("open");
    seed_account(&database);

    database
        .write(|queries| {
            for index in 0..10 {
                queries.insert_transaction(&tx(
                    &format!("tx-{index}"),
                    1_000,
                    TransactionType::Expense,
                    1_700_000_000_000 + index * 1_000,
                ))?;
            }
            Ok(())
        })
        .expect("seed");

    let limited = database.list_transactions(3).expect("list");
    assert_eq!(limited.len(), 3);
    assert_eq!(limited[0].id, "tx-9");
}

#[test]
fn negative_amount_is_rejected() {
    let database = Database::open_in_memory().expect("open");
    seed_account(&database);

    let result = database.insert_transaction(&tx("bad", -1, TransactionType::Expense, 0));
    assert!(result.is_err());
}

#[test]
fn refund_increases_balance_and_update_delete_reverse_the_effect() {
    let database = Database::open_in_memory().expect("open");
    seed_account(&database);

    let refund = tx("refund", 50_000, TransactionType::Refund, 100);
    database.insert_transaction(&refund).expect("insert refund");
    assert_eq!(database.account_balance("acct-1").expect("balance"), 50_000);

    let updated = tx("refund", 75_000, TransactionType::Refund, 100);
    database
        .update_transaction(&updated)
        .expect("update refund");
    assert_eq!(database.account_balance("acct-1").expect("balance"), 75_000);

    database
        .delete_transaction("refund")
        .expect("delete refund");
    assert_eq!(database.account_balance("acct-1").expect("balance"), 0);
}

#[test]
fn fees_affect_the_ledger_when_no_reported_balance_is_available() {
    let database = Database::open_in_memory().expect("open");
    seed_account(&database);
    let mut expense = tx("cash-out", 100_000, TransactionType::Expense, 100);
    expense.fee_minor = Some(1_850);

    database.insert_transaction(&expense).expect("cash out");

    assert_eq!(
        database.account_balance("acct-1").expect("balance"),
        -101_850
    );
}

#[test]
fn newest_reported_balance_is_anchor_for_out_of_order_imports() {
    let database = Database::open_in_memory().expect("open");
    seed_account(&database);

    let mut newest = tx("newest", 50_000, TransactionType::Income, 200);
    newest.balance_after_minor = Some(500_000);
    database.insert_transaction(&newest).expect("newest anchor");
    assert_eq!(
        database.account_balance("acct-1").expect("balance"),
        500_000
    );

    let mut older = tx("older", 100_000, TransactionType::Expense, 100);
    older.balance_after_minor = Some(900_000);
    database.insert_transaction(&older).expect("older import");
    assert_eq!(
        database.account_balance("acct-1").expect("balance"),
        500_000,
        "an older reported balance must not overwrite the newest account state"
    );

    let newer_expense = tx("after-anchor", 25_000, TransactionType::Expense, 300);
    database
        .insert_transaction(&newer_expense)
        .expect("newer expense");
    assert_eq!(
        database.account_balance("acct-1").expect("balance"),
        475_000
    );
}

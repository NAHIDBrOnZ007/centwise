use centwise_db::Database;
use centwise_domain::{Account, NewBudget, NewSubscription, NewTransaction, TransactionType};

fn account(id: &str, name: &str) -> Account {
    Account {
        id: id.into(),
        name: name.into(),
        provider: "bkash".into(),
        last_four: Some("1234".into()),
        balance_minor: 0,
        archived: false,
    }
}

fn transaction(account_id: &str) -> NewTransaction {
    NewTransaction {
        id: "tx-crud".into(),
        title: "Foodpanda".into(),
        amount_minor: 1_000,
        currency: "BDT".into(),
        transaction_type: TransactionType::Expense,
        category_id: "food".into(),
        occurred_at_epoch_ms: 1_700_000_000_000,
        account_id: account_id.into(),
        reference: Some("CRUD-1".into()),
        balance_after_minor: None,
        notes: None,
        raw_sms: None,
        fee_minor: None,
        is_auto_tracked: false,
    }
}

#[test]
fn crud_round_trips_for_accounts_transactions_budgets_and_subscriptions() {
    let database = Database::open_in_memory().expect("open database");

    database
        .write(|queries| {
            queries.insert_account(&account("acct-1", "Primary wallet"))?;
            queries.insert_account(&account("acct-2", "Backup wallet"))
        })
        .expect("insert accounts");

    database
        .insert_transaction(&transaction("acct-1"))
        .expect("insert transaction");
    assert_eq!(database.account_balance("acct-1").expect("balance"), -1_000);

    let mut updated_transaction = transaction("acct-2");
    updated_transaction.amount_minor = 2_500;
    updated_transaction.transaction_type = TransactionType::Income;
    updated_transaction.reference = Some("CRUD-2".into());
    assert!(database
        .update_transaction(&updated_transaction)
        .expect("update transaction"));
    assert_eq!(database.account_balance("acct-1").expect("old balance"), 0);
    assert_eq!(
        database.account_balance("acct-2").expect("new balance"),
        2_500
    );
    assert_eq!(
        database.list_transactions(10).expect("list transactions")[0],
        updated_transaction.clone().into_stored()
    );

    assert!(database
        .delete_transaction("tx-crud")
        .expect("delete transaction"));
    assert_eq!(
        database
            .account_balance("acct-2")
            .expect("balance after delete"),
        0
    );
    assert!(!database
        .delete_transaction("tx-crud")
        .expect("repeat transaction delete"));

    let mut updated_account = account("acct-2", "Archived wallet");
    updated_account.archived = true;
    assert!(database
        .update_account(&updated_account)
        .expect("update account"));
    let accounts = database.list_accounts().expect("list accounts");
    let archived_account = accounts
        .iter()
        .find(|item| item.id == "acct-2")
        .expect("updated account");
    assert_eq!(archived_account.name, "Archived wallet");
    assert!(archived_account.archived);

    database
        .insert_budget(&NewBudget {
            id: "budget-crud".into(),
            category_id: "food".into(),
            limit_minor: 10_000,
            period: "monthly".into(),
            start_epoch_ms: 1_700_000_000_000,
            end_epoch_ms: 1_800_000_000_000,
        })
        .expect("insert budget");
    let updated_budget = NewBudget {
        id: "budget-crud".into(),
        category_id: "transport".into(),
        limit_minor: 20_000,
        period: "weekly".into(),
        start_epoch_ms: 1_700_000_000_000,
        end_epoch_ms: 1_800_000_000_000,
    };
    assert!(database
        .update_budget(&updated_budget)
        .expect("update budget"));
    let budgets = database.list_budgets().expect("list budgets");
    assert_eq!(budgets.len(), 1);
    assert_eq!(budgets[0].category_id, "transport");
    assert_eq!(budgets[0].limit_minor, 20_000);
    assert_eq!(budgets[0].period, "weekly");
    assert!(database
        .delete_budget("budget-crud")
        .expect("delete budget"));
    assert!(!database
        .delete_budget("budget-crud")
        .expect("repeat budget delete"));

    database
        .insert_subscription(&NewSubscription {
            id: "subscription-crud".into(),
            name: "Streaming".into(),
            amount_minor: 500,
            billing_cycle: "monthly".into(),
            next_due_epoch_ms: 1_800_000_000_000,
            is_active: true,
        })
        .expect("insert subscription");
    let updated_subscription = NewSubscription {
        id: "subscription-crud".into(),
        name: "Streaming Plus".into(),
        amount_minor: 900,
        billing_cycle: "yearly".into(),
        next_due_epoch_ms: 1_900_000_000_000,
        is_active: false,
    };
    assert!(database
        .update_subscription(&updated_subscription)
        .expect("update subscription"));
    let subscriptions = database.list_subscriptions().expect("list subscriptions");
    assert_eq!(subscriptions.len(), 1);
    assert_eq!(subscriptions[0].name, "Streaming Plus");
    assert_eq!(subscriptions[0].amount_minor, 900);
    assert_eq!(subscriptions[0].billing_cycle, "yearly");
    assert!(!subscriptions[0].is_active);
    assert!(database
        .delete_subscription("subscription-crud")
        .expect("delete subscription"));
    assert!(!database
        .delete_subscription("subscription-crud")
        .expect("repeat subscription delete"));

    assert!(database.delete_account("acct-1").expect("delete account"));
    assert!(database.delete_account("acct-2").expect("delete account"));
    assert!(!database
        .delete_account("acct-2")
        .expect("repeat account delete"));
}

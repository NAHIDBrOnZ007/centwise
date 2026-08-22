use centwise_db::Database;
use centwise_domain::{Account, NewBudget, NewSubscription, NewTransaction, TransactionType};

fn month_start(year: i32, month: u32) -> i64 {
    // Fixed reference points keep this test independent of the real clock.
    // 2026-01-01 00:00 UTC = 1767225600 seconds.
    let base_month_seconds: i64 = 1_767_225_600;
    let months_since_2026_01 = (year as i64 - 2026) * 12 + (month as i64 - 1);
    (base_month_seconds + months_since_2026_01 * 31 * 24 * 60 * 60) * 1000
}

fn tx(
    id: &str,
    title: &str,
    amount_minor: i64,
    kind: TransactionType,
    category_id: &str,
    epoch_ms: i64,
) -> NewTransaction {
    NewTransaction {
        id: id.into(),
        title: title.into(),
        amount_minor,
        currency: "BDT".into(),
        transaction_type: kind,
        category_id: category_id.into(),
        occurred_at_epoch_ms: epoch_ms,
        account_id: "acct-1".into(),
        reference: None,
        balance_after_minor: None,
        notes: None,
        is_auto_tracked: true,
    }
}

fn seeded_database() -> Database {
    let database = Database::open_in_memory().expect("open");

    database
        .write(|queries| {
            queries.insert_account(&Account {
                id: "acct-1".into(),
                name: "bKash".into(),
                provider: "bkash".into(),
                last_four: Some("5678".into()),
                balance_minor: 0,
                archived: false,
            })?;

            let jan = month_start(2026, 1);
            let feb = month_start(2026, 2);

            // January: food 2 tx (300+700=1000), transport 1 tx (250)
            queries.insert_transaction(&tx(
                "f1",
                "Foodpanda",
                300,
                TransactionType::Expense,
                "food",
                jan + 1_000,
            ))?;
            queries.insert_transaction(&tx(
                "f2",
                "Foodpanda",
                700,
                TransactionType::Expense,
                "food",
                jan + 2_000,
            ))?;
            queries.insert_transaction(&tx(
                "t1",
                "Pathao",
                250,
                TransactionType::Expense,
                "transport",
                jan + 3_000,
            ))?;
            // January: income excluded from analytics
            queries.insert_transaction(&tx(
                "s1",
                "Salary",
                50_000,
                TransactionType::Income,
                "salary",
                jan + 4_000,
            ))?;
            // February: food again (belongs to the next period)
            queries.insert_transaction(&tx(
                "f3",
                "Foodpanda",
                999,
                TransactionType::Expense,
                "food",
                feb + 1_000,
            ))?;

            Ok(())
        })
        .expect("seed");

    database
}

#[test]
fn category_breakdown_groups_and_orders_expenses() {
    let database = seeded_database();
    let jan = month_start(2026, 1);
    let feb = month_start(2026, 2);

    let breakdown = database
        .read(|queries| queries.category_breakdown(jan, feb))
        .expect("breakdown");

    assert_eq!(breakdown.len(), 2, "only categories with spending appear");
    assert_eq!(breakdown[0].category_id, "food");
    assert_eq!(breakdown[0].total_minor, 1_000);
    assert_eq!(breakdown[0].transaction_count, 2);
    assert_eq!(breakdown[0].category_name, "Food & Dining");
    assert_eq!(breakdown[1].category_id, "transport");
    assert_eq!(breakdown[1].total_minor, 250);
}

#[test]
fn top_merchants_groups_by_title() {
    let database = seeded_database();
    let jan = month_start(2026, 1);
    let feb = month_start(2026, 2);

    let merchants = database
        .read(|queries| queries.top_merchants(jan, feb, 5))
        .expect("merchants");

    assert_eq!(merchants.len(), 2);
    assert_eq!(merchants[0].merchant, "Foodpanda");
    assert_eq!(merchants[0].total_minor, 1_000);
    assert_eq!(merchants[0].transaction_count, 2);
    assert_eq!(merchants[1].merchant, "Pathao");
}

#[test]
fn spending_by_month_buckets_expenses() {
    let database = seeded_database();

    // Anchor inside the seeded data so the test never depends on the clock.
    let months = database
        .read(|queries| queries.spending_by_month_anchored(6, month_start(2026, 2)))
        .expect("months");

    assert_eq!(months.len(), 2);
    assert_eq!(months[0].year, 2026);
    assert_eq!(months[0].month, 1);
    assert_eq!(months[0].total_expense_minor, 1_250);
    assert_eq!(months[1].month, 2);
    assert_eq!(months[1].total_expense_minor, 999);
}

#[test]
fn spending_by_month_window_excludes_old_data() {
    let database = seeded_database();

    // Anchor in February with a 1-month window: only February counts.
    let months = database
        .read(|queries| queries.spending_by_month_anchored(1, month_start(2026, 2)))
        .expect("months");

    assert_eq!(months.len(), 1);
    assert_eq!(months[0].month, 2);
    assert_eq!(months[0].total_expense_minor, 999);
}

#[test]
fn budget_progress_counts_only_period_expenses() {
    let database = seeded_database();
    let jan = month_start(2026, 1);
    let feb = month_start(2026, 2);

    database
        .write(|queries| {
            queries.insert_budget(&NewBudget {
                id: "budget-1".into(),
                category_id: "food".into(),
                limit_minor: 2_000,
                period: "monthly".into(),
                start_epoch_ms: jan,
                end_epoch_ms: feb,
            })
        })
        .expect("budget");

    let budgets = database
        .read(|queries| queries.list_budgets())
        .expect("budgets");

    assert_eq!(budgets.len(), 1);
    assert_eq!(budgets[0].spent_minor, 1_000, "February food excluded");
    assert_eq!(budgets[0].limit_minor, 2_000);
    assert_eq!(budgets[0].category_name, "Food & Dining");
}

#[test]
fn invalid_budget_limit_rejected() {
    let database = seeded_database();
    let result = database.write(|queries| {
        queries.insert_budget(&NewBudget {
            id: "bad".into(),
            category_id: "food".into(),
            limit_minor: 0,
            period: "monthly".into(),
            start_epoch_ms: 0,
            end_epoch_ms: 1,
        })
    });
    assert!(result.is_err());
}

#[test]
fn accounts_and_subscriptions_round_trip() {
    let database = seeded_database();

    database
        .write(|queries| {
            queries.insert_subscription(&NewSubscription {
                id: "sub-1".into(),
                name: "Netflix".into(),
                amount_minor: 65_000,
                billing_cycle: "monthly".into(),
                next_due_epoch_ms: month_start(2026, 9),
                is_active: true,
            })
        })
        .expect("subscription");

    let accounts = database
        .read(|queries| queries.list_accounts())
        .expect("accounts");
    assert_eq!(accounts.len(), 1);
    assert_eq!(accounts[0].name, "bKash");
    assert_eq!(accounts[0].last_four.as_deref(), Some("5678"));

    let subscriptions = database
        .read(|queries| queries.list_subscriptions())
        .expect("subscriptions");
    assert_eq!(subscriptions.len(), 1);
    assert_eq!(subscriptions[0].name, "Netflix");
    assert_eq!(subscriptions[0].amount_minor, 65_000);
    assert!(subscriptions[0].is_active);
}

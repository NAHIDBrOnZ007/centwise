use centwise_db::Database;

const DEMO_NOW: i64 = 1_756_656_000_000;

#[test]
fn demo_data_is_seeded_by_rust_and_reload_is_idempotent() {
    let database = Database::open_in_memory().expect("open");

    let first = database
        .replace_with_demo_data_at(DEMO_NOW)
        .expect("load demo data");
    assert!(first.accounts > 0);
    assert!(first.transactions > 0);
    assert!(first.budgets > 0);
    assert!(first.subscriptions > 0);

    let first_transactions = database.list_transactions(10_000).expect("transactions");
    assert_eq!(first_transactions.len(), first.transactions as usize);

    let second = database
        .replace_with_demo_data_at(DEMO_NOW)
        .expect("reload demo data");
    assert_eq!(first, second);
    assert_eq!(
        database
            .list_transactions(10_000)
            .expect("transactions")
            .len(),
        first.transactions as usize
    );
}

#[test]
fn reset_to_empty_keeps_system_categories_but_removes_demo_records() {
    let database = Database::open_in_memory().expect("open");
    database
        .replace_with_demo_data_at(DEMO_NOW)
        .expect("load demo data");

    database.reset_to_empty().expect("reset");

    assert!(database
        .list_transactions(10)
        .expect("transactions")
        .is_empty());
    assert!(database.list_accounts().expect("accounts").is_empty());
    assert!(database.list_budgets().expect("budgets").is_empty());
    assert!(database
        .list_subscriptions()
        .expect("subscriptions")
        .is_empty());
    assert_eq!(database.category_count().expect("categories"), 11);
}

#[test]
fn system_categories_are_read_from_rust_in_stable_order() {
    let database = Database::open_in_memory().expect("open");

    let categories = database.list_categories().expect("categories");

    assert_eq!(categories.len(), 11);
    assert!(categories.iter().all(|category| category.is_system));
    assert_eq!(categories[0].id, "food");
    assert_eq!(categories[0].name, "Food & Dining");
    assert!(categories
        .windows(2)
        .all(|pair| pair[0].sort_order < pair[1].sort_order));
}

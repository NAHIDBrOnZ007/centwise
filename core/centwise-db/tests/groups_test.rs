use centwise_db::{queries::groups::NewGroupExpenseItemInput, Database};

#[test]
fn test_group_crud_and_analytics() {
    let db = Database::open_in_memory().expect("open db");

    // Pre-seeded demo groups should exist on open
    let groups = db.list_groups().expect("list groups");
    assert!(!groups.is_empty(), "Should have pre-seeded demo groups");
    assert!(groups.iter().any(|g| g.id == "group_dhanmondi_4b"));

    // Create a new group
    let new_group = db
        .create_group(
            "test_group_1",
            "Friends Trip",
            "trip",
            "Cox's Bazar 2026",
            "TRIP-1234",
            "September 2026",
            100000,
            "Alice",
            "avatar_1",
        )
        .expect("create group");

    assert_eq!(new_group.name, "Friends Trip");
    assert_eq!(new_group.members.len(), 1);
    let creator = &new_group.members[0];
    assert_eq!(creator.name, "Alice");
    assert!(creator.is_admin);

    // Join group
    let updated = db
        .join_group("TRIP-1234", "Bob", "avatar_2")
        .expect("join group")
        .expect("group found");
    assert_eq!(updated.members.len(), 2);
    let bob = updated
        .members
        .iter()
        .find(|m| m.name == "Bob")
        .expect("bob");
    assert!(!bob.is_admin);

    // Add Bob deposit
    db.add_group_deposit(
        "test_group_1",
        &bob.id,
        100000,
        "bKash",
        "Advance",
        1_700_000_000_000,
        "13 Sep 2026",
    )
    .expect("add deposit");

    // Add Expense by Alice: 40000 BDT minor items
    let items = vec![
        NewGroupExpenseItemInput {
            name: "মিনিকেট চাল".into(),
            quantity: "5kg".into(),
            price_minor: 25000,
        },
        NewGroupExpenseItemInput {
            name: "রূপচাঁদা সয়াবিন তেল".into(),
            quantity: "1L".into(),
            price_minor: 15000,
        },
    ];
    let exp_id = db
        .add_group_expense(
            "test_group_1",
            &creator.id,
            &items,
            "Lunch grocery",
            1_700_000_100_000,
            "13 Sep 2026",
        )
        .expect("add expense");

    // Check group detail
    let group = db
        .get_group("test_group_1")
        .expect("get group")
        .expect("found");
    assert_eq!(group.daily_expenses.len(), 1);
    assert_eq!(group.daily_expenses[0].total_amount_minor, 40000);
    // Split for 2 members: 40000 / 2 = 20000
    assert_eq!(group.daily_expenses[0].split_per_member_minor, 20000);

    // Run Analytics
    let analytics = db
        .group_analytics("test_group_1", 0, i64::MAX, 6, "all")
        .expect("group analytics");
    assert_eq!(analytics.total_deposit_minor, 200000); // 100000 + 100000
    assert_eq!(analytics.total_expense_minor, 40000);
    assert_eq!(analytics.remaining_balance_minor, 160000);
    assert_eq!(analytics.transaction_count, 3); // 2 deposits + 1 expense
    assert!(!analytics.category_breakdown.is_empty());
    assert!(!analytics.top_spenders.is_empty());

    // Test debit filter
    let debit_analytics = db
        .group_analytics("test_group_1", 0, i64::MAX, 6, "debit")
        .expect("debit analytics");
    assert_eq!(debit_analytics.total_deposit_minor, 0);
    assert_eq!(debit_analytics.total_expense_minor, 40000);
    assert_eq!(debit_analytics.transaction_count, 1);

    // Test credit filter
    let credit_analytics = db
        .group_analytics("test_group_1", 0, i64::MAX, 6, "credit")
        .expect("credit analytics");
    assert_eq!(credit_analytics.total_deposit_minor, 200000);
    assert_eq!(credit_analytics.total_expense_minor, 0);
    assert_eq!(credit_analytics.transaction_count, 2);

    // Delete expense
    let deleted = db
        .delete_group_expense("test_group_1", &exp_id)
        .expect("delete");
    assert!(deleted);

    let group_after = db.get_group("test_group_1").expect("get").expect("found");
    assert_eq!(group_after.daily_expenses.len(), 0);
}

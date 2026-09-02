use centwise_db::{Database, DbError};
use centwise_domain::{NewCategory, NewSmartRule, RuleMatchType, TransactionType};

fn category(id: &str) -> NewCategory {
    NewCategory {
        id: id.into(),
        name: "Coffee Shops".into(),
        icon: "cup.and.saucer".into(),
        color_hex: "#A855F7".into(),
    }
}

#[test]
fn custom_categories_and_rules_persist_and_support_crud() {
    let database = Database::open_in_memory().expect("open database");
    database
        .insert_category(&category("coffee"))
        .expect("insert category");

    let mut updated = category("coffee");
    updated.name = "Coffee".into();
    assert!(database.update_category(&updated).expect("update category"));

    database
        .insert_rule(&NewSmartRule {
            id: "rule-coffee".into(),
            name: "Coffee merchants".into(),
            keyword: "Cafe".into(),
            match_type: RuleMatchType::Contains,
            category_id: "coffee".into(),
            transaction_type: TransactionType::Expense,
            is_enabled: true,
        })
        .expect("insert rule");

    let rules = database.list_rules().expect("list rules");
    assert_eq!(rules.len(), centwise_domain::default_rules().len() + 1);
    assert_eq!(
        rules
            .iter()
            .find(|rule| rule.id == "rule-coffee")
            .expect("custom rule")
            .category_id,
        "coffee"
    );
    assert_eq!(
        database
            .read(|queries| queries.matching_rule("Cafe Aroma", TransactionType::Expense))
            .expect("match rule")
            .expect("matching rule")
            .id,
        "rule-coffee"
    );

    assert!(database.delete_rule("rule-coffee").expect("delete rule"));
    assert!(database.delete_category("coffee").expect("delete category"));
    assert_eq!(
        database.list_rules().expect("list rules").len(),
        centwise_domain::default_rules().len()
    );
}

#[test]
fn system_categories_are_protected_and_reset_removes_user_categories_and_rules() {
    let database = Database::open_in_memory().expect("open database");
    database
        .insert_category(&category("coffee"))
        .expect("insert category");
    database
        .insert_rule(&NewSmartRule {
            id: "rule-coffee".into(),
            name: "Coffee merchants".into(),
            keyword: "Cafe".into(),
            match_type: RuleMatchType::Contains,
            category_id: "coffee".into(),
            transaction_type: TransactionType::Expense,
            is_enabled: true,
        })
        .expect("insert rule");

    assert!(matches!(
        database.delete_category("food"),
        Err(DbError::Invalid(message)) if message.contains("system categories")
    ));

    database.reset_to_empty().expect("reset");
    assert_eq!(database.list_categories().expect("categories").len(), 20);
    assert!(database.list_rules().expect("rules").is_empty());
    assert!(database
        .list_categories()
        .expect("categories")
        .iter()
        .all(|item| item.is_system));
}

#[test]
fn category_used_by_a_learned_mapping_cannot_be_deleted() {
    let database = Database::open_in_memory().expect("open database");
    database
        .insert_category(&category("coffee"))
        .expect("insert category");
    database
        .write(|queries| {
            queries.upsert_merchant_category_mapping(
                "Cafe Dhaka",
                TransactionType::Expense,
                "coffee",
            )
        })
        .expect("mapping");

    assert!(matches!(
        database.delete_category("coffee"),
        Err(DbError::Invalid(message)) if message.contains("still used")
    ));
}

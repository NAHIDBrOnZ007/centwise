use centwise_db::Database;
use centwise_domain::{Account, NewTransaction, ReviewQueueItem, TransactionType};

fn account() -> Account {
    Account {
        id: "acct-1".into(),
        name: "bKash".into(),
        provider: "bkash".into(),
        last_four: Some("5678".into()),
        balance_minor: 0,
        archived: false,
    }
}

fn review_item() -> ReviewQueueItem {
    ReviewQueueItem {
        id: "review-1".into(),
        sender: Some("bKash".into()),
        raw_sms: "Payment of Tk 650.00 to Foodpanda successful. Ref: FP8392.".into(),
        received_at_epoch_ms: 1_700_000_000_000,
        provider_id: Some("bkash".into()),
        reason: "Account needs confirmation".into(),
        candidate_amount_minor: Some(65_000),
        candidate_type: Some(TransactionType::Expense),
        fee_minor: Some(0),
        balance_after_minor: Some(1_425_000),
        reference: Some("FP8392".into()),
        party: Some("Foodpanda".into()),
        merchant: Some("Foodpanda".into()),
        category_id: Some("food".into()),
        account_last4: None,
        account_hint: None,
    }
}

fn transaction() -> NewTransaction {
    NewTransaction {
        id: "tx-1".into(),
        title: "Foodpanda".into(),
        amount_minor: 65_000,
        currency: "BDT".into(),
        transaction_type: TransactionType::Expense,
        category_id: "food".into(),
        occurred_at_epoch_ms: 1_700_000_000_000,
        account_id: "acct-1".into(),
        reference: Some("FP8392".into()),
        balance_after_minor: Some(1_425_000),
        fee_minor: Some(0),
        notes: None,
        raw_sms: Some("Payment of Tk 650.00 to Foodpanda successful. Ref: FP8392.".into()),
        is_auto_tracked: true,
    }
}

#[test]
fn review_queue_survives_reopen_and_can_be_dismissed() {
    let file = tempfile::NamedTempFile::new().expect("temp file");

    {
        let database = Database::open(file.path()).expect("open");
        let inserted = database
            .write(|queries| queries.insert_review_queue_item(&review_item()))
            .expect("insert review item");
        assert!(inserted);
    }

    let database = Database::open(file.path()).expect("reopen");
    let items = database
        .read(|queries| queries.list_review_queue(10))
        .expect("list review items");
    assert_eq!(items, vec![review_item()]);

    assert!(database
        .dismiss_review_queue_item("review-1")
        .expect("dismiss"));
    assert!(database
        .read(|queries| queries.list_review_queue(10))
        .expect("list after dismiss")
        .is_empty());
}

#[test]
fn converting_review_item_is_atomic_and_updates_balance() {
    let database = Database::open_in_memory().expect("open");
    database
        .write(|queries| {
            queries.insert_account(&account())?;
            queries.insert_review_queue_item(&review_item())?;
            Ok(())
        })
        .expect("setup");

    assert!(database
        .convert_review_queue_item("review-1", &transaction())
        .expect("convert"));
    assert!(database
        .read(|queries| queries.list_review_queue(10))
        .expect("list after convert")
        .is_empty());
    assert_eq!(
        database.account_balance("acct-1").expect("balance"),
        -65_000
    );
    let transactions = database.list_transactions(10).expect("transactions");
    assert_eq!(transactions, vec![transaction().into_stored()]);
}

#[test]
fn reference_is_deduplicated_across_transactions_and_review_queue() {
    let database = Database::open_in_memory().expect("open");
    database
        .write(|queries| {
            queries.insert_account(&account())?;
            assert!(queries.insert_review_queue_item(&review_item())?);
            assert!(!queries.insert_review_queue_item(&review_item())?);
            Ok(())
        })
        .expect("queue duplicate");

    database
        .convert_review_queue_item("review-1", &transaction())
        .expect("convert");

    let duplicate = database.insert_transaction(&transaction());
    assert!(duplicate.is_err(), "reference must not be inserted twice");
}

#[test]
fn user_can_convert_ambiguous_review_item_to_selected_account() {
    let database = Database::open_in_memory().expect("open");
    database
        .write(|queries| {
            for id in ["acct-1", "acct-2"] {
                queries.insert_account(&Account {
                    id: id.into(),
                    name: id.into(),
                    provider: "bkash".into(),
                    last_four: None,
                    balance_minor: 0,
                    archived: false,
                })?;
            }
            queries.insert_review_queue_item(&ReviewQueueItem {
                id: "review-ambiguous".into(),
                sender: Some("bKash".into()),
                raw_sms: "Payment of Tk 10.00 successful.".into(),
                received_at_epoch_ms: 1_700_000_000_000,
                provider_id: Some("bkash".into()),
                reason: "Multiple matching active accounts".into(),
                candidate_amount_minor: Some(1_000),
                candidate_type: Some(TransactionType::Expense),
                fee_minor: None,
                balance_after_minor: None,
                reference: None,
                party: None,
                merchant: None,
                category_id: Some("other".into()),
                account_last4: None,
                account_hint: None,
            })?;
            Ok(())
        })
        .expect("setup");

    // Non-existent account fails conversion
    let mut invalid_tx = transaction();
    invalid_tx.account_id = "non-existent-acct".into();
    assert!(!database
        .convert_review_queue_item("review-ambiguous", &invalid_tx)
        .expect("invalid account conversion"));

    // User-selected existing account succeeds
    assert!(database
        .convert_review_queue_item("review-ambiguous", &transaction())
        .expect("conversion result"));
    assert_eq!(database.list_review_queue(10).expect("queue").len(), 0);
    assert_eq!(
        database.list_transactions(10).expect("transactions").len(),
        1
    );
}

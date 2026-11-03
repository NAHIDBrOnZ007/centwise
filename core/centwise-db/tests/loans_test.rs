use centwise_db::{queries::loans::LoanPayment, Database};
use centwise_domain::Account;

#[test]
fn loan_repayments_change_the_selected_account_and_month_once() {
    let db = Database::open_in_memory().unwrap();
    db.write(|q| {
        for id in ["cash-test", "bank-test"] {
            q.insert_account(&Account {
                id: id.into(),
                name: id.into(),
                provider: "cash".into(),
                last_four: None,
                balance_minor: 600_000,
                archived: false,
            })?;
        }
        q.create_loan(
            "test-loan",
            "Test person",
            true,
            None,
            "",
            &LoanPayment {
                id: "test-principal".into(),
                account_id: "cash-test".into(),
                amount_minor: 500_000,
                occurred_at_epoch_ms: 1_793_491_200_000,
                existing_transaction_id: None,
            },
        )?;
        q.repay_loan(
            "test-loan",
            &LoanPayment {
                id: "test-repayment".into(),
                account_id: "bank-test".into(),
                amount_minor: 100_000,
                occurred_at_epoch_ms: 1_796_083_200_000,
                existing_transaction_id: None,
            },
        )
    })
    .unwrap();
    db.read(|q| {
        assert_eq!(q.account_balance("cash-test")?, 100_000);
        assert_eq!(q.account_balance("bank-test")?, 700_000);
        assert_eq!(q.list_loans()?[0].remaining_minor, 400_000);
        let months = q.loan_months()?;
        assert_eq!(months.len(), 2);
        assert_eq!(months[0].month, "2026-12");
        assert_eq!(
            (months[0].incoming_minor, months[0].outgoing_minor),
            (100_000, 0)
        );
        assert_eq!(months[1].month, "2026-11");
        assert_eq!(
            (months[1].incoming_minor, months[1].outgoing_minor),
            (0, 500_000)
        );
        let home = q.home_dashboard(0, i64::MAX, 5)?;
        assert_eq!(home.period_income_minor, 0);
        assert_eq!(home.period_expense_minor, 0);
        Ok(())
    })
    .unwrap();
}

#[test]
fn borrowed_money_can_link_existing_transactions_and_rejects_duplicate_or_excess_payments() {
    use centwise_domain::{NewTransaction, TransactionType};
    let db = Database::open_in_memory().unwrap();
    let principal = LoanPayment {
        id: "borrowed".into(),
        account_id: "wallet".into(),
        amount_minor: 500_000,
        occurred_at_epoch_ms: 1_700_000_000_000,
        existing_transaction_id: None,
    };
    db.write(|q| {
        q.insert_account(&Account {
            id: "wallet".into(),
            name: "Wallet".into(),
            provider: "cash".into(),
            last_four: None,
            balance_minor: 0,
            archived: false,
        })?;
        q.create_loan("borrow", "Test person", false, None, "", &principal)?;
        q.insert_transaction(&NewTransaction {
            id: "existing-sms".into(),
            account_id: "wallet".into(),
            title: "Bank payment".into(),
            amount_minor: 100_000,
            currency: "BDT".into(),
            transaction_type: TransactionType::Expense,
            category_id: "other".into(),
            occurred_at_epoch_ms: 1_700_000_000_001,
            reference: None,
            balance_after_minor: None,
            notes: None,
            raw_sms: None,
            fee_minor: None,
            is_auto_tracked: true,
        })
    })
    .unwrap();

    let linked = LoanPayment {
        id: "linked".into(),
        account_id: "wallet".into(),
        amount_minor: 100_000,
        occurred_at_epoch_ms: 1_700_000_000_001,
        existing_transaction_id: Some("existing-sms".into()),
    };
    db.write(|q| q.repay_loan("borrow", &linked)).unwrap();
    assert!(db.write(|q| q.repay_loan("borrow", &linked)).is_err());
    let mut last = LoanPayment {
        id: "final".into(),
        account_id: "wallet".into(),
        amount_minor: 400_001,
        occurred_at_epoch_ms: 1_700_000_000_002,
        existing_transaction_id: None,
    };
    assert!(db.write(|q| q.repay_loan("borrow", &last)).is_err());
    assert!(db.delete_transaction("existing-sms").is_err());
    db.read(|q| {
        assert_eq!(q.account_balance("wallet")?, 400_000);
        assert_eq!(q.get_loan("borrow")?.remaining_minor, 400_000);
        assert_eq!(q.loan_movements("borrow")?.len(), 2);
        assert_eq!(
            q.analytics_snapshot(0, i64::MAX, 12, "all")?
                .transaction_count,
            0
        );
        Ok(())
    })
    .unwrap();
    last.amount_minor = 400_000;
    db.write(|q| q.repay_loan("borrow", &last)).unwrap();
    db.read(|q| {
        assert_eq!(q.account_balance("wallet")?, 0);
        assert_eq!(q.get_loan("borrow")?.remaining_minor, 0);
        Ok(())
    })
    .unwrap();
}

#[test]
fn version_nine_upgrade_and_sqlite_backup_keep_loans_and_existing_balances() {
    use centwise_db::migrations::MIGRATIONS;
    let directory = tempfile::tempdir().unwrap();
    let original = directory.path().join("v9.db");
    let backup = directory.path().join("backup.db");
    {
        let connection = rusqlite::Connection::open(&original).unwrap();
        for migration in MIGRATIONS.iter().filter(|m| m.version <= 9) {
            connection.execute_batch(migration.sql).unwrap();
            connection
                .pragma_update(None, "user_version", migration.version)
                .unwrap();
        }
        connection.execute(
            "INSERT INTO accounts (id, name, provider, balance_minor, archived, created_at_epoch_ms)
             VALUES ('prior', 'Prior account', 'cash', 800000, 0, 1)", [],
        ).unwrap();
        connection
            .execute_batch(
                "INSERT INTO categories (id, name, icon, color_hex)
                 VALUES ('prior-category', 'Prior category', 'tag', '#000000');
                 INSERT INTO transactions (id, title, amount_minor, transaction_type,
                     category_id, occurred_at_epoch_ms, account_id, reference, created_at_epoch_ms)
                 VALUES ('prior-tx', 'Prior deposit', 800000, 'income', 'prior-category',
                     1600000000000, 'prior', 'SYN-PRIOR', 1600000000000);",
            )
            .unwrap();
    }
    let db = Database::open(&original).unwrap();
    db.write(|q| {
        assert_eq!(q.account_balance("prior")?, 800_000);
        q.create_loan(
            "saved-loan",
            "Test person",
            true,
            Some(1_710_000_000_000),
            "Example",
            &LoanPayment {
                id: "loan-tx".into(),
                account_id: "prior".into(),
                amount_minor: 500_000,
                occurred_at_epoch_ms: 1_700_000_000_000,
                existing_transaction_id: None,
            },
        )
    })
    .unwrap();
    rusqlite::Connection::open(&original)
        .unwrap()
        .execute("VACUUM INTO ?1", [backup.to_str().unwrap()])
        .unwrap();
    drop(db);
    let restored = Database::open(&backup).unwrap();
    restored
        .read(|q| {
            assert_eq!(q.account_balance("prior")?, 300_000);
            assert_eq!(q.get_loan("saved-loan")?.remaining_minor, 500_000);
            assert_eq!(q.loan_movements("saved-loan")?.len(), 1);
            let prior = q.get_transaction("prior-tx")?.unwrap();
            assert_eq!(prior.reference.as_deref(), Some("SYN-PRIOR"));
            assert_eq!(prior.amount_minor, 800_000);
            assert_eq!(
                q.home_dashboard(0, i64::MAX, 10)?.period_income_minor,
                800_000
            );
            Ok(())
        })
        .unwrap();
}

#[cfg(test)]
mod ingestion_tests {
    use crate::{
        AccountInput, CategoryInput, CentwiseCore, SmartRuleInput, SmsIngestStatus,
        TransactionInput, TransactionKind,
    };

    fn manual_transaction(
        id: &str,
        account_id: &str,
        account_provider: Option<&str>,
        account_last_four: Option<&str>,
    ) -> TransactionInput {
        TransactionInput {
            id: id.into(),
            title: "Manual transaction".into(),
            amount_minor: 1_000,
            currency: "BDT".into(),
            kind: TransactionKind::Expense,
            category_id: "other".into(),
            occurred_at_epoch_ms: 1_700_000_000_000,
            account_id: account_id.into(),
            account_provider: account_provider.map(str::to_string),
            account_name: None,
            account_last_four: account_last_four.map(str::to_string),
            reference: None,
            balance_after_minor: None,
            fee_minor: None,
            notes: None,
            raw_sms: None,
            is_auto_tracked: false,
        }
    }

    fn core_with_account() -> std::sync::Arc<CentwiseCore> {
        let core = CentwiseCore::open(":memory:".into()).expect("open core");
        core.insert_account(AccountInput {
            id: "acct-1".into(),
            name: "bKash".into(),
            provider: "bkash".into(),
            last_four: None,
            starting_balance_minor: 0,
            archived: false,
        })
        .expect("account");
        core
    }

    #[test]
    fn ingest_sms_inserts_when_one_account_matches() {
        let core = core_with_account();
        let result = core
            .ingest_sms(
                "Payment of Tk 650.00 to Foodpanda successful. Ref: FP8392.".into(),
                Some("bKash".into()),
                1_700_000_000_000,
            )
            .expect("ingest");

        assert_eq!(result.status, SmsIngestStatus::Inserted);
        assert_eq!(result.transaction_id.as_deref(), Some("sms-FP8392"));
        assert_eq!(
            core.account_balance("acct-1".into()).expect("balance"),
            -65_000
        );
    }

    #[test]
    fn ingest_sms_creates_and_reuses_provider_wallet_when_no_account_exists() {
        let core = CentwiseCore::open(":memory:".into()).expect("open core");

        for (reference, timestamp) in [("AUTO1", 1_700_000_000_000), ("AUTO2", 1_700_000_000_001)] {
            let result = core
                .ingest_sms(
                    format!("Payment of Tk 10.00 to Merchant successful. Ref: {reference}."),
                    Some("bKash".into()),
                    timestamp,
                )
                .expect("ingest");
            assert_eq!(result.status, SmsIngestStatus::Inserted);
        }

        let accounts = core.list_accounts().expect("accounts");
        assert_eq!(accounts.len(), 1);
        assert_eq!(accounts[0].id, "auto-bkash-wallet");
        assert_eq!(accounts[0].provider, "bkash");
        assert_eq!(core.list_transactions(10).expect("transactions").len(), 2);
    }

    #[test]
    fn manual_accountless_insert_creates_and_reuses_system_cash() {
        let core = CentwiseCore::open(":memory:".into()).expect("open core");

        core.insert_transaction(manual_transaction("manual-1", "", None, None))
            .expect("first manual insert");
        core.insert_transaction(manual_transaction("manual-2", "", None, None))
            .expect("second manual insert");

        let accounts = core.list_accounts().expect("accounts");
        assert_eq!(accounts.len(), 1);
        assert_eq!(accounts[0].id, "system-cash");
        assert_eq!(accounts[0].provider, "cash");
        assert!(core
            .list_transactions(10)
            .expect("transactions")
            .iter()
            .all(|transaction| transaction.account_id == "system-cash"));
    }

    #[test]
    fn accountless_insert_reactivates_an_archived_automatic_account() {
        let core = CentwiseCore::open(":memory:".into()).expect("open core");
        core.insert_transaction(manual_transaction(
            "manual-before-archive",
            "",
            Some("bkash"),
            None,
        ))
        .expect("create automatic account");
        core.update_account(AccountInput {
            id: "auto-bkash-wallet".into(),
            name: "bKash Wallet".into(),
            provider: "bkash".into(),
            last_four: None,
            starting_balance_minor: -1_000,
            archived: true,
        })
        .expect("archive automatic account");

        core.insert_transaction(manual_transaction(
            "manual-after-archive",
            "",
            Some("bkash"),
            None,
        ))
        .expect("reuse automatic account");

        let accounts = core.list_accounts().expect("accounts");
        assert_eq!(accounts.len(), 1);
        assert_eq!(accounts[0].id, "auto-bkash-wallet");
        assert!(!accounts[0].archived);
    }

    #[test]
    fn manual_provider_accounts_use_distinct_suffix_ids() {
        let core = CentwiseCore::open(":memory:".into()).expect("open core");

        core.insert_transaction(manual_transaction(
            "manual-1111",
            "",
            Some("city-bank"),
            Some("1111"),
        ))
        .expect("first provider insert");
        core.insert_transaction(manual_transaction(
            "manual-2222",
            "",
            Some("city-bank"),
            Some("2222"),
        ))
        .expect("second provider insert");

        let mut ids = core
            .list_accounts()
            .expect("accounts")
            .into_iter()
            .map(|account| account.id)
            .collect::<Vec<_>>();
        ids.sort();
        assert_eq!(ids, vec!["auto-city-bank-1111", "auto-city-bank-2222"]);
    }

    #[test]
    fn invalid_selected_account_does_not_fall_back_or_insert() {
        let core = CentwiseCore::open(":memory:".into()).expect("open core");

        let result = core.insert_transaction(manual_transaction(
            "manual-invalid",
            "missing-account",
            Some("bkash"),
            None,
        ));

        assert!(result.is_err());
        assert!(core.list_accounts().expect("accounts").is_empty());
        assert!(core.list_transactions(10).expect("transactions").is_empty());
    }

    #[test]
    fn review_conversion_can_create_cash_account_atomically() {
        let core = CentwiseCore::open(":memory:".into()).expect("open core");
        let queued = core
            .ingest_sms(
                "A financial transaction needs your attention.".into(),
                Some("Unknown Bank".into()),
                1_700_000_000_000,
            )
            .expect("queue ingest");
        let review_id = queued.review_id.expect("review id");

        let converted = core
            .convert_review_queue_item(
                review_id,
                manual_transaction("review-converted", "", None, None),
            )
            .expect("convert review");

        assert!(converted);
        assert_eq!(core.list_accounts().expect("accounts")[0].id, "system-cash");
        assert!(core.list_review_queue(10).expect("queue").is_empty());
        assert_eq!(core.list_transactions(10).expect("transactions").len(), 1);
    }

    #[test]
    fn ingest_sms_queues_when_account_is_ambiguous() {
        let core = CentwiseCore::open(":memory:".into()).expect("open core");
        for id in ["acct-1", "acct-2"] {
            core.insert_account(AccountInput {
                id: id.into(),
                name: format!("bKash {id}"),
                provider: "bkash".into(),
                last_four: None,
                starting_balance_minor: 0,
                archived: false,
            })
            .expect("account");
        }

        let result = core
            .ingest_sms(
                "Payment of Tk 650.00 to Foodpanda successful. Ref: FP8393.".into(),
                Some("bKash".into()),
                1_700_000_000_000,
            )
            .expect("ingest");

        assert_eq!(result.status, SmsIngestStatus::QueuedForReview);
        assert!(result.review_id.is_some());
        assert_eq!(core.list_review_queue(10).expect("review list").len(), 1);
    }

    #[test]
    fn ingest_sms_ignores_otp_and_duplicate_references() {
        let core = core_with_account();
        let otp = core
            .ingest_sms(
                "Your OTP is 123456. Do not share this verification code.".into(),
                Some("bKash".into()),
                1_700_000_000_000,
            )
            .expect("otp ingest");
        assert_eq!(otp.status, SmsIngestStatus::Ignored);

        let message = "Payment of Tk 650.00 to Foodpanda successful. Ref: FP8394.";
        core.ingest_sms(message.into(), Some("bKash".into()), 1_700_000_000_000)
            .expect("first ingest");
        let duplicate = core
            .ingest_sms(message.into(), Some("bKash".into()), 1_700_000_000_001)
            .expect("duplicate ingest");
        assert_eq!(duplicate.status, SmsIngestStatus::Duplicate);

        let no_reference = "Payment of Tk 12.00 to Coffee Shop successful.";
        core.ingest_sms(no_reference.into(), Some("bKash".into()), 1_700_000_000_010)
            .expect("no-reference ingest");
        let no_reference_duplicate = core
            .ingest_sms(no_reference.into(), Some("bKash".into()), 1_700_000_000_011)
            .expect("no-reference duplicate ingest");
        assert_eq!(no_reference_duplicate.status, SmsIngestStatus::Duplicate);
    }

    #[test]
    fn ingest_sms_applies_a_persisted_rust_rule() {
        let core = core_with_account();
        core.insert_category(CategoryInput {
            id: "coffee".into(),
            name: "Coffee".into(),
            icon: "cup".into(),
            color_hex: "#A855F7".into(),
        })
        .expect("category");
        core.insert_rule(SmartRuleInput {
            id: "rule-coffee".into(),
            name: "Coffee merchants".into(),
            keyword: "CoffeeHouse".into(),
            match_type: "contains".into(),
            category_id: "coffee".into(),
            kind: TransactionKind::Expense,
            is_enabled: true,
        })
        .expect("rule");

        let result = core
            .ingest_sms(
                "Payment of Tk 120.00 to CoffeeHouse successful. Ref: COF1.".into(),
                Some("bKash".into()),
                1_700_000_000_000,
            )
            .expect("ingest");

        assert_eq!(result.status, SmsIngestStatus::Inserted);
        let transaction = core
            .list_transactions(10)
            .expect("transactions")
            .into_iter()
            .find(|item| item.reference.as_deref() == Some("COF1"))
            .expect("stored transaction");
        assert_eq!(transaction.category_id, "coffee");
        assert_eq!(core.list_rules().expect("rules").len(), 6);
    }

    #[test]
    fn demo_data_is_owned_by_rust_and_can_be_reset() {
        let core = CentwiseCore::open(":memory:".into()).expect("open core");
        let summary = core.load_demo_data().expect("load demo");

        assert_eq!(summary.accounts, 4);
        assert!(summary.transactions > 400);
        assert_eq!(summary.budgets, 4);
        assert_eq!(summary.subscriptions, 3);
        assert_eq!(core.list_accounts().expect("accounts").len(), 4);
        assert!(core.list_transactions(10_000).expect("transactions").len() > 400);
        assert_eq!(core.list_budgets().expect("budgets").len(), 4);
        assert_eq!(core.list_subscriptions().expect("subscriptions").len(), 3);

        core.reset_to_empty_database().expect("reset demo");
        assert!(core.list_review_queue(10).expect("queue").is_empty());
    }

    #[test]
    fn system_categories_are_exposed_through_ffi() {
        let core = CentwiseCore::open(":memory:".into()).expect("open core");

        let categories = core.list_categories().expect("categories");

        assert_eq!(categories.len(), 11);
        assert_eq!(categories[0].id, "food");
        assert!(categories.iter().all(|category| category.is_system));
    }
}

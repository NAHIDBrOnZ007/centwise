#[cfg(test)]
mod ingestion_tests {
    use crate::{
        AccountInput, CategoryInput, CentwiseCore, SmartRuleInput, SmsBatchMessage,
        SmsIngestStatus, TransactionInput, TransactionKind,
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
    fn batch_card_receipts_do_not_guess_between_bank_accounts() {
        let core = CentwiseCore::open(":memory:".into()).unwrap();
        for suffix in ["1111", "2222"] {
            core.insert_account(AccountInput {
                id: format!("bank-{suffix}"),
                name: format!("Bank {suffix}"),
                provider: "ebl".into(),
                last_four: Some(suffix.into()),
                starting_balance_minor: 0,
                archived: false,
            })
            .unwrap();
        }
        let body = "Card ending 3333 was used at Demo Shop for BDT 250.00. TrxID SYNBATCH01";
        let results = core
            .ingest_sms_batch(vec![SmsBatchMessage {
                body: body.into(),
                sender_hint: Some("EBL".into()),
                occurred_at_epoch_ms: 1_700_000_000_000,
            }])
            .unwrap();
        assert_eq!(results[0].status, SmsIngestStatus::QueuedForReview);
        assert!(core.list_transactions(10).unwrap().is_empty());
        assert_eq!(core.list_review_queue(10).unwrap().len(), 1);
        assert!(core
            .list_accounts()
            .unwrap()
            .iter()
            .all(|a| a.balance_minor == 0));
    }

    #[test]
    fn public_biller_receipts_debit_the_wallet_without_creating_utility_accounts() {
        let core = core_with_account();
        for (index, body) in [
            "Bill payment is successful. Biller: BREBPrepaid A/C: 12340000 Contact: 017XXXXXXXX Amount: Tk 1200.00 Fee: TK 5.00 TrxID: SYNPUBLIC01",
            "Bill successfully paid. Biller: MetLife MMYY 102026/Contact: 017XXXXXXXX A/C: 56780000 Amount: TK 2500.00 Fee: Tk 0.00 TrxID: SYNPUBLIC02",
        ].into_iter().enumerate() {
            let result = core.ingest_sms(body.into(), Some("bKash".into()),
                1_700_000_000_000 + index as i64 * 1000).unwrap();
            assert_eq!(result.status, SmsIngestStatus::Inserted);
        }
        assert_eq!(core.account_balance("acct-1".into()).unwrap(), -370_500);
        assert_eq!(core.list_accounts().unwrap().len(), 1);
        assert_eq!(core.list_transactions(10).unwrap().len(), 2);
        assert!(core
            .list_transactions(10)
            .unwrap()
            .iter()
            .all(|tx| tx.account_id == "acct-1" && tx.balance_after_minor.is_none()));
    }

    #[test]
    fn confirmed_receipts_use_an_unknown_source_without_overwriting_a_bank_or_meter_balance() {
        let core = core_with_account();
        core.insert_account(AccountInput {
            id: "bank-1".into(),
            name: "City Bank".into(),
            provider: "city-bank".into(),
            last_four: Some("1234".into()),
            starting_balance_minor: 500_000,
            archived: false,
        })
        .unwrap();
        let result = core.ingest_sms(
            "Dear Customer, Tk. 100 (Details : Energy Cost:95.00,VAT:5.25:Rebate Amount-0.25) is successfully recharged for Meter no 12340000 and Customer no 56780000. Your meter balance is Tk. 80.00 as on 2026-10-22. Thanking You, DPDC.".into(),
            Some("Primary Account".into()), 1_700_000_000_000,
        ).unwrap();
        assert_eq!(result.status, SmsIngestStatus::Inserted);
        let messages = [
            "Your payment for BRTA Service Portal (BSP) Tran-No: SYNBRTA01 /BDT 125.50 is successful. Thank you.",
            "Your fee payment of tk 500.00 is successful.",
            "Dear valued customer, The fee for minute bundle you purchased is TK 9.00(including SD, VAT & SC).",
        ].into_iter().enumerate().map(|(i, body)| SmsBatchMessage {
            body: body.into(), sender_hint: Some("Primary Account".into()),
            occurred_at_epoch_ms: 1_700_000_001_000 + i as i64 * 1000,
        }).collect();
        assert!(core
            .ingest_sms_batch(messages)
            .unwrap()
            .iter()
            .all(|result| result.status == SmsIngestStatus::Inserted));
        let accounts = core.list_accounts().unwrap();
        let unknown = accounts
            .iter()
            .find(|account| account.provider == "banks-generic")
            .unwrap();
        assert_eq!(unknown.balance_minor, -73_450);
        assert_eq!(core.account_balance("bank-1".into()).unwrap(), 500_000);
        assert_eq!(core.account_balance("acct-1".into()).unwrap(), 0);
        let transactions = core.list_transactions(10).unwrap();
        assert_eq!(transactions.len(), 4);
        assert!(transactions
            .iter()
            .all(|tx| tx.balance_after_minor.is_none() && tx.fee_minor.is_none()));
        assert!(transactions.iter().all(|tx| tx.account_id == unknown.id));
        let invoice = core.ingest_sms(
            "Thanks for shopping from Ryans. Your purchase bill no is B-SYN10001, Tk 100. Click to see your invoice copy: https://example.invalid/receipt Thanks Ryans Computers Ltd.".into(),
            Some("Primary Account".into()), 1_700_000_005_000,
        ).unwrap();
        assert_eq!(invoice.status, SmsIngestStatus::Inserted);
        assert_eq!(core.list_transactions(10).unwrap().len(), 5);
        assert_eq!(core.account_balance(unknown.id.clone()).unwrap(), -83_450);
    }

    #[test]
    fn parser_corrections_preserve_the_ledger_and_do_not_drop_repeated_memos() {
        let core = CentwiseCore::open(":memory:".into()).expect("open core");
        let fee = core
            .ingest_sms(
                "SMS Alert Fee of BDT 230.00 has been debited from your A/C XXXX1234.".into(),
                Some("City Bank".into()),
                1_700_000_000_000,
            )
            .expect("fee");
        assert_eq!(fee.status, SmsIngestStatus::Inserted);
        assert_eq!(core.list_accounts().unwrap()[0].balance_minor, -23_000);

        let wallet = core_with_account();
        for (i, amount) in [100, 200].iter().enumerate() {
            let result = wallet
                .ingest_sms(
                    format!("Send Money Tk {amount}.00 to 017XXXXXXXX successful. Ref: rent."),
                    Some("bKash".into()),
                    1_700_000_000_000 + i as i64 * 86_400_000,
                )
                .unwrap();
            assert_eq!(result.status, SmsIngestStatus::Inserted);
        }
        assert_eq!(wallet.list_transactions(10).unwrap().len(), 2);
        assert_eq!(wallet.account_balance("acct-1".into()).unwrap(), -30_000);
    }

    #[test]
    fn batch_ingestion_ignores_offers_and_applies_posted_bank_direction() {
        let core = CentwiseCore::open(":memory:".into()).unwrap();
        let messages = [
            "Your A/C XXXX1234 will be debited by BDT 500.00 tomorrow.",
            "Your A/C XXXX1234 debited by BDT 500.00 via NPSB. Ref NP10001.",
            "Your A/C XXXX1234 credited by BDT 200.00 via NPSB. Ref NP10002.",
        ]
        .into_iter()
        .enumerate()
        .map(|(i, body)| SmsBatchMessage {
            body: body.into(),
            sender_hint: Some("EBL".into()),
            occurred_at_epoch_ms: 1_700_000_000_000 + i as i64 * 1000,
        })
        .collect();
        let results = core.ingest_sms_batch(messages).unwrap();
        assert_eq!(results[0].status, SmsIngestStatus::Ignored);
        assert_eq!(results[1].status, SmsIngestStatus::Inserted);
        assert_eq!(results[2].status, SmsIngestStatus::Inserted);
        assert_eq!(core.list_accounts().unwrap()[0].balance_minor, -30_000);
    }

    #[test]
    fn unicode_before_a_memo_does_not_shift_its_byte_offset() {
        let core = core_with_account();
        let body = format!("Payment Tk 10.00 successful. {} Ref: rent.", "İ".repeat(40));
        let result = core
            .ingest_sms(body, Some("bKash".into()), 1_700_000_000_000)
            .unwrap();
        assert_eq!(result.status, SmsIngestStatus::Inserted);
        assert_eq!(
            core.list_transactions(1).unwrap()[0].notes.as_deref(),
            Some("Memo: rent.")
        );
    }

    #[test]
    fn identical_payments_without_a_transaction_id_can_recur_on_another_day() {
        let core = core_with_account();
        let body = "Payment Tk 100.00 to Sample Shop successful. Ref: rent2026.";
        for timestamp in [1_700_000_000_000, 1_700_086_400_000] {
            assert_eq!(
                core.ingest_sms(body.into(), Some("bKash".into()), timestamp)
                    .unwrap()
                    .status,
                SmsIngestStatus::Inserted
            );
        }
        assert_eq!(core.list_transactions(10).unwrap().len(), 2);
    }

    #[test]
    fn distinct_receipts_without_authoritative_ids_do_not_collide_within_ten_minutes() {
        for (sender, bodies) in [
            (
                "EBL",
                [
                    "Your A/C XXXX1234 debited by BDT 500.00 via NPSB. Ref NP10001.",
                    "Your A/C XXXX1234 debited by BDT 500.00 via NPSB. Ref NP10002.",
                ],
            ),
            (
                "bKash",
                [
                    "Payment Tk 500.00 to Sample Shop successful. Ref: order1.",
                    "Payment Tk 500.00 to Sample Shop successful. Ref: order2.",
                ],
            ),
        ] {
            let core = CentwiseCore::open(":memory:".into()).unwrap();
            for (index, body) in bodies.into_iter().enumerate() {
                let result = core
                    .ingest_sms(
                        body.into(),
                        Some(sender.into()),
                        1_700_000_000_000 + index as i64 * 60_000,
                    )
                    .unwrap();
                assert_eq!(result.status, SmsIngestStatus::Inserted);
            }
            assert_eq!(core.list_transactions(10).unwrap().len(), 2);
        }
    }

    #[test]
    fn ingest_sms_inserts_when_one_account_matches() {
        let core = core_with_account();
        let result = core
            .ingest_sms(
                "Payment of Tk 650.00 to Foodpanda successful. TrxID: FP8392.".into(),
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
                    format!("Payment of Tk 10.00 to Merchant successful. TrxID: {reference}."),
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
    fn ingest_sms_links_card_and_account_sms_to_single_bank_account() {
        let core = CentwiseCore::open(":memory:".into()).expect("open core");

        // 1. Ingest account-based SMS from EBL (e.g. SkyBanking transfer with masked account 109***386)
        let res1 = core
            .ingest_sms(
                "AC 109***386 is debited with BDT 300 as EBL Skybanking MFS Transfer-bKash on 08-SEP-26 02:59:58 AM Balance is BDT 15318.5 Thanks. EBL Helpline 16230".into(),
                Some("EBL".into()),
                1_700_000_000_000,
            )
            .expect("ingest account sms");
        assert_eq!(res1.status, SmsIngestStatus::Inserted);

        // Verify Eastern Bank account was created
        let accounts = core.list_accounts().expect("accounts");
        assert_eq!(accounts.len(), 1);
        assert_eq!(accounts[0].name, "Eastern Bank");
        assert_eq!(accounts[0].provider, "ebl");
        let ebl_account_id = accounts[0].id.clone();

        // 2. Ingest card-based SMS from EBL (e.g. ATM withdrawal with Card 1549)
        let res2 = core
            .ingest_sms(
                "Cash WD BDT3000 from EBL DEMRA STAFF QUAR. Card 452017**1549 on 05-Sep-26 07:46:06 PM BST.Your A/C 109**0386 Balance BDT 16638.5. Thank You. EBL Helpline 16230".into(),
                Some("EBL".into()),
                1_700_000_060_000,
            )
            .expect("ingest card sms");
        assert_eq!(res2.status, SmsIngestStatus::Inserted);

        // 3. Verify STILL only 1 account exists, linked seamlessly!
        let accounts_after = core.list_accounts().expect("accounts");
        assert_eq!(accounts_after.len(), 1, "Should NOT create duplicate account for card");
        assert_eq!(accounts_after[0].id, ebl_account_id);

        // 4. Verify both transactions are attached to the same account and preserve card/ac instruments
        let txs = core.list_transactions(10).expect("transactions");
        assert_eq!(txs.len(), 2);
        assert_eq!(txs[0].account_id, ebl_account_id);
        assert_eq!(txs[1].account_id, ebl_account_id);

        let notes_0 = txs[0].notes.as_deref().unwrap_or_default();
        let notes_1 = txs[1].notes.as_deref().unwrap_or_default();
        assert!(notes_0.contains("Card") || notes_1.contains("Card"), "Card transaction notes must identify card");
    }

    #[test]
    fn ingest_sms_uses_official_names_for_banks_mfs_and_telcos() {
        let core = CentwiseCore::open(":memory:".into()).expect("open core");

        // Sonali Bank
        core.ingest_sms(
            "Tk. 5,000.00 has been credited to A/C XXXX1001. Avl. Bal: Tk. 12,000.00. Ref: SYN1001.".into(),
            Some("Sonali Bank".into()),
            1_700_000_000_000,
        ).expect("sonali");

        // City Bank
        core.ingest_sms(
            "15-Aug-2024 CITYTOUCH TXN Tk. 35,000 Withdrawal Tk. 29 Balance A/C: XX0001 NPSB Fee: Tk 10".into(),
            Some("City Bank".into()),
            1_700_000_000_001,
        ).expect("city");

        // Robi
        core.ingest_sms(
            "TK 8.0 has been added to your account. TK 8.0 + Service fee of TK 2.78 will be deducted from your next recharge. Your total outstanding is Tk 10.78. For Details, dial *8#".into(),
            Some("Robi".into()),
            1_700_000_000_002,
        ).expect("robi");

        let accounts = core.list_accounts().expect("accounts");
        let names: Vec<_> = accounts.into_iter().map(|a| a.name).collect();
        assert!(names.contains(&"Sonali Bank".to_string()));
        assert!(names.contains(&"City Bank".to_string()));
        assert!(names.contains(&"Robi".to_string()));
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
    fn financial_message_with_amount_but_uncertain_direction_is_reviewed() {
        let core = CentwiseCore::open(":memory:".into()).expect("open core");
        let result = core
            .ingest_sms(
                "Your bank account transaction of Tk 500.00 needs attention.".into(),
                Some("BANK".into()),
                1_700_000_000_000,
            )
            .expect("ingest");

        assert_eq!(result.status, SmsIngestStatus::QueuedForReview);
        assert_eq!(core.list_review_queue(10).expect("review").len(), 1);
        assert!(core.list_transactions(10).expect("transactions").is_empty());
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
                "Payment of Tk 650.00 to Foodpanda successful. TrxID: FP8393.".into(),
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

        let message = "Payment of Tk 650.00 to Foodpanda successful. TrxID: FP8394.";
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
    fn ingest_sms_bangla_gatekeeper_ignores_and_never_queues_bengali_sms() {
        let core = core_with_account();
        let bangla_sms_samples = [
            "আপনার বিকাশ একাউন্ট থেকে ৫০ টাকা রিচার্জ সফল হয়েছে। ট্রানজেকশন আইডি TX12345",
            "নগদ একাউন্টে ১,০০০ টাকা ক্যাশ ইন সম্পন্ন হয়েছে।",
            "সিটি ব্যাংক: আপনার একাউন্ট থেকে ৫০০ টাকা উত্তোলন করা হয়েছে।",
        ];

        for sample in bangla_sms_samples {
            let result = core
                .ingest_sms(sample.into(), Some("bKash".into()), 1_700_000_000_000)
                .expect("ingest bangla sms");
            assert_eq!(result.status, SmsIngestStatus::Ignored);
            assert!(result.transaction_id.is_none());
            assert!(result.review_id.is_none());
        }

        assert!(core.list_review_queue(10).expect("queue").is_empty());
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
                "Payment of Tk 120.00 to CoffeeHouse successful. TrxID: COF1.".into(),
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
        assert_eq!(
            core.list_rules().expect("rules").len(),
            centwise_domain::default_rules().len() + 1
        );
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

        assert_eq!(categories.len(), 20);
        assert_eq!(categories[0].id, "food");
        assert!(categories.iter().any(|category| category.id == "income"));
        assert!(categories.iter().any(|category| category.id == "refunds"));
        assert!(categories.iter().all(|category| category.is_system));
    }

    #[test]
    fn no_reference_dedup_normalizes_message_but_keeps_senders_distinct() {
        let core = CentwiseCore::open(":memory:".into()).expect("open core");
        let first = core
            .ingest_sms(
                "Payment of Tk 12.00 to Coffee Shop successful.".into(),
                Some("bKash".into()),
                1_700_000_000_000,
            )
            .expect("first");
        assert_eq!(first.status, SmsIngestStatus::Inserted);

        let duplicate = core
            .ingest_sms(
                "  payment  OF tk 12.00 TO coffee shop SUCCESSFUL. ".into(),
                Some("BKASH".into()),
                1_700_000_000_001,
            )
            .expect("normalized duplicate");
        assert_eq!(duplicate.status, SmsIngestStatus::Duplicate);

        let different_sender = core
            .ingest_sms(
                "Payment of Tk 12.00 to Coffee Shop successful.".into(),
                Some("Nagad".into()),
                1_700_000_000_002,
            )
            .expect("different sender");
        assert_eq!(different_sender.status, SmsIngestStatus::Inserted);
    }

    #[test]
    fn account_hints_create_distinct_automatic_accounts() {
        let core = CentwiseCore::open(":memory:".into()).expect("open core");
        for (hint, reference) in [("017901121****01", "HINT1"), ("017901121****02", "HINT2")] {
            let result = core
                .ingest_sms(
                    format!(
                        "Your account number {hint} has been debited with BDT 30.00. Ref No {reference}."
                    ),
                    Some("Janata Bank".into()),
                    1_700_000_000_000,
                )
                .expect("ingest");
            assert_eq!(result.status, SmsIngestStatus::Inserted);
        }

        let ids = core
            .list_accounts()
            .expect("accounts")
            .into_iter()
            .map(|account| account.id)
            .collect::<Vec<_>>();
        assert_eq!(ids.len(), 2);
        assert_ne!(ids[0], ids[1]);
    }

    #[test]
    fn batch_ingestion_preserves_order_and_per_message_results() {
        let core = core_with_account();
        let results = core
            .ingest_sms_batch(vec![
                SmsBatchMessage {
                    body: "Payment Tk 20.00 to Foodpanda successful. TrxID: BATCH1.".into(),
                    sender_hint: Some("bKash".into()),
                    occurred_at_epoch_ms: 1_700_000_000_000,
                },
                SmsBatchMessage {
                    body: "Your OTP is 123456. Do not share it.".into(),
                    sender_hint: Some("bKash".into()),
                    occurred_at_epoch_ms: 1_700_000_000_001,
                },
                SmsBatchMessage {
                    body: "Your A/C was credited with Tk 50.00. TrxID: BATCH2.".into(),
                    sender_hint: Some("bKash".into()),
                    occurred_at_epoch_ms: 1_700_000_000_002,
                },
            ])
            .expect("batch");

        assert_eq!(results[0].status, SmsIngestStatus::Inserted);
        assert_eq!(results[1].status, SmsIngestStatus::Ignored);
        assert_eq!(results[2].status, SmsIngestStatus::Inserted);
        assert_eq!(core.list_transactions(10).expect("transactions").len(), 2);
    }

    #[test]
    fn ordinary_credit_uses_income_and_only_salary_signals_use_salary() {
        let core = core_with_account();
        core.ingest_sms(
            "Your account was credited with Tk 5,000. TrxID: CREDIT1.".into(),
            Some("bKash".into()),
            1_700_000_000_000,
        )
        .expect("ordinary credit");
        core.ingest_sms(
            "Monthly salary of Tk 25,000 credited. TrxID: SALARY1.".into(),
            Some("bKash".into()),
            1_700_000_000_001,
        )
        .expect("salary credit");

        let transactions = core.list_transactions(10).expect("transactions");
        assert_eq!(
            transactions
                .iter()
                .find(|item| item.reference.as_deref() == Some("CREDIT1"))
                .expect("ordinary credit")
                .category_id,
            "income"
        );
        assert_eq!(
            transactions
                .iter()
                .find(|item| item.reference.as_deref() == Some("SALARY1"))
                .expect("salary")
                .category_id,
            "salary"
        );
    }

    #[test]
    fn correcting_auto_tracked_merchant_teaches_future_transactions_and_preserves_sms_metadata() {
        let core = core_with_account();
        core.ingest_sms(
            "Payment Tk 120.00 to Cafe Dhaka successful. Fee Tk 2.00. Balance Tk 878.00. TrxID: CAFE1.".into(),
            Some("bKash".into()),
            1_700_000_000_000,
        )
        .expect("first ingest");
        let stored = core
            .list_transactions(10)
            .expect("transactions")
            .into_iter()
            .next()
            .expect("first transaction");
        assert_eq!(stored.category_id, "other");

        let mut correction = manual_transaction(&stored.id, &stored.account_id, None, None);
        correction.title = stored.title.clone();
        correction.amount_minor = stored.amount_minor;
        correction.occurred_at_epoch_ms = stored.occurred_at_epoch_ms;
        correction.category_id = "food".into();
        core.update_transaction(correction)
            .expect("category correction");

        let corrected = core
            .get_transaction(stored.id)
            .expect("get")
            .expect("corrected");
        assert_eq!(corrected.reference.as_deref(), Some("CAFE1"));
        assert_eq!(corrected.fee_minor, Some(200));
        assert_eq!(corrected.balance_after_minor, Some(87_800));
        assert!(corrected.raw_sms.is_some());
        assert!(corrected.is_auto_tracked);

        core.ingest_sms(
            "Payment Tk 50.00 to Cafe Dhaka successful. TrxID: CAFE2.".into(),
            Some("bKash".into()),
            1_700_000_000_001,
        )
        .expect("second ingest");
        let learned = core
            .list_transactions(10)
            .expect("transactions")
            .into_iter()
            .find(|item| item.reference.as_deref() == Some("CAFE2"))
            .expect("learned transaction");
        assert_eq!(learned.category_id, "food");
    }

    #[test]
    fn update_transaction_updates_fields_and_resolves_account() {
        let core = CentwiseCore::open(":memory:".into()).expect("open core");
        let tx = manual_transaction("tx-1", "", Some("nagad"), None);
        core.insert_transaction(tx).expect("insert");

        let stored = core.list_transactions(10).expect("list");
        assert_eq!(stored.len(), 1);
        assert_eq!(stored[0].title, "Manual transaction");

        let mut update_input = manual_transaction("tx-1", &stored[0].account_id, None, None);
        update_input.title = "Pathao Rides".into();
        update_input.amount_minor = 76_000;
        update_input.category_id = "transport".into();

        let updated = core.update_transaction(update_input).expect("update");
        assert!(updated);

        let after_update = core.list_transactions(10).expect("list");
        assert_eq!(after_update[0].title, "Pathao Rides");
        assert_eq!(after_update[0].amount_minor, 76_000);
        assert_eq!(after_update[0].category_id, "transport");
    }
}

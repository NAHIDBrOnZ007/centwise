use centwise_domain::TransactionType;
use centwise_parser::parse_sms;

#[test]
fn synthetic_wallet_payments_cover_common_bangladesh_merchants() {
    // These vary an existing wallet format; they are not claimed merchant SMS templates.
    for (merchant, category) in [
        ("Foodpanda", "food"),
        ("Chaldal", "food"),
        ("Shwapno", "food"),
        ("Agora Superstore", "food"),
        ("Meena Bazar", "food"),
        ("Daraz", "shopping"),
        ("Aarong", "shopping"),
        ("Bata", "shopping"),
        ("Apex", "shopping"),
        ("Star Tech", "shopping"),
        ("Ryans", "shopping"),
        ("Pathao", "transport"),
        ("Uber", "transport"),
        ("DESCO", "bills"),
        ("DPDC", "bills"),
        ("Titas Gas", "bills"),
        ("Dhaka WASA", "bills"),
        ("BTCL", "bills"),
        ("Link3", "bills"),
    ] {
        let body = format!("Payment Tk 250.00 to {merchant} successful. Fee Tk 0.00. Balance Tk 750.00. TrxID SYNMERCHANT01");
        let outcome = parse_sms(&body, Some("bKash"));
        let tx = outcome
            .transaction()
            .unwrap_or_else(|| panic!("{merchant}: {outcome:?}"));
        assert_eq!(tx.provider_id, "bkash", "{merchant}");
        assert_eq!(tx.transaction_type, TransactionType::Expense, "{merchant}");
        assert_eq!(tx.amount_minor, 25_000, "{merchant}");
        assert_eq!(tx.category_id.as_deref(), Some(category), "{merchant}");
        assert!(tx.merchant.is_some(), "{merchant}");
    }
}

#[test]
fn biller_identifiers_never_become_the_payers_wallet_account() {
    for body in [
        "Bill payment is successful. Biller: BREBPrepaid A/C: 12340000 Contact: 017XXXXXXXX Amount: Tk 1200.00 Fee: TK 5.00 TrxID: SYNPUBLIC01",
        "Bill successfully paid. Biller: MetLife MMYY 102026/Contact: 017XXXXXXXX A/C: 56780000 Amount: TK 2500.00 Fee: Tk 0.00 TrxID: SYNPUBLIC02",
    ] {
        let outcome = parse_sms(body, Some("bKash"));
        let tx = outcome.transaction().expect("confirmed bill payment");
        assert_eq!(tx.account_hint, None);
        assert_eq!(tx.account_last4, None);
        assert_eq!(tx.balance_after_minor, None);
    }
}

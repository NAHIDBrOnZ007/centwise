use centwise_domain::TransactionType;
use centwise_parser::{is_likely_financial_review, parse_sms, ParseOutcome, ParsedTransaction};

fn receipt(body: &str) -> ParsedTransaction {
    match parse_sms(body, Some("Primary Account")) {
        ParseOutcome::Parsed(tx) => *tx,
        other => panic!("expected a completed receipt, got {other:?}"),
    }
}

#[test]
fn brta_completed_payment_is_an_expense_without_inventing_a_bank() {
    let tx = receipt("Your payment for BRTA Service Portal (BSP) Tran-No: SYNBRTA01 /BDT 125.50 is successful. Thank you.");
    assert_eq!(tx.provider_id, "banks-generic");
    assert_eq!(tx.transaction_type, TransactionType::Expense);
    assert_eq!(tx.amount_minor, 12_550);
    assert_eq!(tx.merchant.as_deref(), Some("BRTA"));
    assert_eq!(tx.category_id.as_deref(), Some("fees"));
    assert_eq!(tx.fee_minor, None);
    assert_eq!(tx.account_last4, None);
    assert_eq!(tx.balance_after_minor, None);
}

#[test]
fn dpdc_recharge_uses_the_paid_total_and_keeps_meter_balance_out_of_the_ledger() {
    let body = "Dear Customer, Tk. 100 (Details : Energy Cost:95.00,VAT:5.25:Rebate Amount-0.25) is successfully recharged for Meter no 12340000 and Customer no 56780000. Your meter balance is Tk. 80.00 as on 2026-10-22. Thanking You, DPDC.";
    for sender in [None, Some("01XXXXXXXXX"), Some("Primary Account")] {
        assert!(parse_sms(body, sender).is_transaction());
    }
    let tx = receipt(body);
    assert_eq!(tx.transaction_type, TransactionType::Expense);
    assert_eq!(tx.amount_minor, 10_000);
    assert_eq!(tx.merchant.as_deref(), Some("DPDC"));
    assert_eq!(tx.category_id.as_deref(), Some("bills"));
    assert_eq!(tx.fee_minor, None);
    assert_eq!(tx.balance_after_minor, None);
    assert_eq!(tx.account_last4, None);
    assert_eq!(tx.account_hint, None);
}

#[test]
fn a_bundle_already_purchased_is_spending_not_an_offer_or_an_extra_fee() {
    for (product, amount, expected) in [
        ("minute bundle", "9.00", 900),
        ("minute bundle", "16.00", 1_600),
        ("9999_AT_App_36_C", "36.00", 3_600),
    ] {
        let tx = receipt(&format!("Dear valued customer, The fee for {product} you purchased is TK {amount}(including SD, VAT & SC)."));
        assert_eq!(tx.provider_id, "banks-generic");
        assert_eq!(tx.transaction_type, TransactionType::Expense);
        assert_eq!(tx.amount_minor, expected);
        assert_eq!(tx.category_id.as_deref(), Some("recharge"));
        assert_eq!(tx.fee_minor, None);
        assert_eq!(tx.balance_after_minor, None);
    }
}

#[test]
fn a_successful_fee_payment_is_its_principal_not_an_additional_fee() {
    let tx = receipt("Your fee payment of tk 500.00 is successful. Reg. No: ,Ref: memo at 22/10/2026 05:58:49 AM");
    assert_eq!(tx.amount_minor, 50_000);
    assert_eq!(tx.transaction_type, TransactionType::Expense);
    assert_eq!(tx.category_id.as_deref(), Some("fees"));
    assert_eq!(tx.fee_minor, None);
}

#[test]
fn matching_institutions_do_not_make_offers_or_failed_payments_receipts() {
    for body in [
        "Your payment for BRTA Service Portal (BSP) Tran-No: SYNBRTA01 /BDT 125.50 is pending. Please wait.",
        "Your payment for BRTA Service Portal (BSP) Tran-No: SYNBRTA01 /BDT 125.50 was unsuccessful.",
        "Your payment for BRTA Service Portal (BSP) Tran-No: SYNBRTA01 /BDT 125.555 is successful.",
        "DPDC: Recharge Tk 100 to get a rebate. Offer valid for 7 days.",
        "The fee for minute bundle you purchase will be TK 9.00. Dial *121# to buy.",
        "Your fee payment of tk 500.00 will be processed tomorrow.",
        "Your fee payment of tk 500.00 is not successful.",
        "Your fee payment of USD 500.00 is successful.",
    ] {
        assert!(!parse_sms(body, Some("Primary Account")).is_transaction(), "{body}");
    }
}

#[test]
fn a_ryans_purchase_confirmation_tracks_spending_without_bank_details() {
    let body = "Thanks for shopping from Ryans. Your purchase bill no is B-SYN10001, Tk 100. Click to see your invoice copy: https://example.invalid/receipt Thanks Ryans Computers Ltd.";
    let tx = receipt(body);
    assert_eq!(tx.transaction_type, TransactionType::Expense);
    assert_eq!(tx.amount_minor, 10_000);
    assert_eq!(tx.merchant.as_deref(), Some("Ryans"));
    assert_eq!(tx.category_id.as_deref(), Some("shopping"));
    assert_eq!(tx.balance_after_minor, None);
    assert_eq!(tx.account_hint, None);
}

#[test]
fn quotations_cancelled_orders_and_unrecognized_invoices_are_not_purchases() {
    for suffix in ["Payment due: Tk 100.", "Payment has not been made."] {
        let body = format!("Thanks for shopping from Ryans. Your purchase bill no is B-SYN01, Tk 100. Click to see your invoice copy. Thanks Ryans Computers Ltd. {suffix}");
        assert!(
            !parse_sms(&body, Some("Primary Account")).is_transaction(),
            "{body}"
        );
    }
    for body in [
        "Ryans quotation: Your purchase bill no is Q-SYN01, Tk 100. Please confirm your order.",
        "Thanks for shopping from Ryans. Your purchase bill no is B-SYN01, Tk 100. Your order is cancelled.",
        "Ryans offer: Buy a keyboard for Tk 100. Click to shop now.",
        "Your invoice is Tk 100. Please pay by tomorrow.",
    ] {
        assert!(!parse_sms(body, Some("Primary Account")).is_transaction(), "{body}");
    }
}

#[test]
fn a_known_operator_bundle_charge_has_the_same_spending_category() {
    let body = "Dear valued customer, The fee for minute bundle you purchased is TK 9.00(including SD, VAT & SC).";
    let outcome = parse_sms(body, Some("GP"));
    let tx = outcome.transaction().expect("purchased bundle");
    assert_eq!(tx.provider_id, "grameenphone");
    assert_eq!(tx.amount_minor, 900);
    assert_eq!(tx.transaction_type, TransactionType::Expense);
    assert_eq!(tx.category_id.as_deref(), Some("recharge"));
}

#[test]
fn an_unrecognized_utility_receipt_remains_visible_even_without_a_known_sender() {
    let body = "DPDC: Meter credited with Tk 100.00. Payment receipt available in the portal.";
    assert!(!parse_sms(body, Some("01XXXXXXXXX")).is_transaction());
    assert!(is_likely_financial_review(body, Some("01XXXXXXXXX")));
}

use centwise_domain::TransactionType;
use centwise_parser::{is_likely_financial_review, parse_sms, ParseOutcome, ParsedTransaction};

fn transaction(body: &str, sender: &str) -> ParsedTransaction {
    match parse_sms(body, Some(sender)) {
        ParseOutcome::Parsed(tx) => *tx,
        other => panic!("expected transaction, got {other:?}"),
    }
}

#[test]
fn malformed_merchant_quotes_do_not_panic() {
    let tx = transaction(
        "Payment Tk 100.00 to ' successful. TrxID SYN10001.",
        "bKash",
    );
    assert_eq!(tx.party, None);
    assert_eq!(tx.amount_minor, 10_000);
}

#[test]
fn a_standalone_fee_is_not_an_additional_fee() {
    let tx = transaction(
        "SMS Alert Fee of BDT 230.00 has been debited from your A/C XXXX1234.",
        "City Bank",
    );
    assert_eq!(tx.amount_minor, 23_000);
    assert_eq!(tx.fee_minor, None);
}

#[test]
fn user_memos_are_not_unique_transaction_references() {
    for memo in ["rent", "rent2026", "123456"] {
        let body = format!(
            "Send Money Tk 100.00 to 017XXXXXXXX successful. Ref: {memo}. Balance Tk 900.00."
        );
        assert_eq!(transaction(&body, "bKash").reference, None);
    }
}

#[test]
fn future_actions_requests_and_offers_are_not_posted_transactions() {
    for body in [
        "Your A/C XXXX1234 will be debited by BDT 500.00 tomorrow.",
        "Scheduled cycle payment for bKash subscription with Demo Service. Payment of 67.85 will be debited on 2026-11-05.",
        "Your Pay Bill request of Tk 500.00 is being processed. Fee Tk 5.00. Org: DPDC.",
        "Your loan of Tk 5000.00 has been approved. It will be disbursed tomorrow.",
        "You are eligible for a loan of Tk 5000.00. Apply now.",
        "Your EMI of Tk 5000.00 is due tomorrow. Please keep sufficient balance.",
        "Your loan instalment of BDT 5000.00 is payable on 05/11/2026.",
        "Your loan of BDT 5000.00 has been approved at 9% interest. Open app to accept.",
        "You can get Tk 50 Emergency Balance. Dial *123*007#.",
        "Recharge Tk 99 to get 5GB. Offer valid for 7 days.",
        "Data pack 5GB at Tk 99 can be activated. Dial *121# to buy.",
    ] {
        assert!(!parse_sms(body, Some("bKash")).is_transaction(), "{body}");
        assert!(!is_likely_financial_review(body, Some("bKash")), "{body}");
    }
}

#[test]
fn otp_instructions_do_not_become_receipts() {
    for body in [
        "OTP for payment of BDT 500.00 is 123456. Enter this code for a successful payment.",
        "Use OTP 123456 for a successful payment of BDT 500.00.",
    ] {
        assert!(!parse_sms(body, Some("City Bank")).is_transaction());
        assert!(!is_likely_financial_review(body, Some("City Bank")));
    }
}

#[test]
fn security_advisories_do_not_capture_a_later_balance_as_an_otp() {
    let tx = transaction("Payment of Tk 500.00 to Demo Shop successful. Never share your OTP. Balance is 1500.00. TrxID SYN10001.", "bKash");
    assert_eq!(tx.amount_minor, 50_000);
    assert_eq!(tx.balance_after_minor, Some(150_000));
}

#[test]
fn negated_refund_completion_is_not_posted() {
    for body in [
        "Refund of BDT 500.00 is not successful. Please retry.",
        "Refund of BDT 500.00 is not completed.",
        "Refund of BDT 500.00 was not completed. Original payment of BDT 500.00 was successful.",
        "Refund of BDT 500.00 has not yet been credited. TrxID SYN10001.",
        "Refund of BDT 500.00 has not been successfully completed. TrxID SYN10001.",
    ] {
        assert!(!parse_sms(body, Some("City Bank")).is_transaction());
    }
}

#[test]
fn refund_amount_belongs_to_the_posted_refund_clause() {
    let tx = transaction("Payment of BDT 1000.00 failed. Refund of BDT 500.00 has been credited to your account. Balance BDT 1500.00.", "City Bank");
    assert_eq!(tx.transaction_type, TransactionType::Refund);
    assert_eq!(tx.amount_minor, 50_000);
}

#[test]
fn explicit_debits_and_merchant_names_do_not_become_income_or_refunds() {
    for body in [
        "Interest of BDT 100.00 has been debited from A/C XXXX1234. Balance BDT 900.00.",
        "Payment of Tk 500.00 to Refund Shop successful. TrxID SYN10002.",
        "Payment of Tk 500.00 to Interest Shop successful. TrxID SYN10003.",
        "Your Cashback Tk 10.00 is rolled back. Balance Tk 680.00. TrxID SYN10004.",
        "Payment Installment of Tk 500.00 has been received against A/C no. XXXX1234. Receipt No. SYN10005.",
    ] {
        assert_eq!(transaction(body, "City Bank").transaction_type, TransactionType::Expense, "{body}");
    }
}

#[test]
fn the_debited_account_wins_over_the_destination_card() {
    let tx = transaction(
        "Your A/C XXXX1234 debited by BDT 100.00 to Card ending 5678. Balance BDT 900.00.",
        "City Bank",
    );
    assert_eq!(tx.account_last4.as_deref(), Some("1234"));
    let tx = transaction(
        "Your account update. Card ending 5678 was used at Demo Store for BDT 100.00.",
        "City Bank",
    );
    assert_eq!(tx.account_last4.as_deref(), Some("5678"));
    let tx = transaction("Payment of Tk 500.00 to Biller Account 12345678 using Card ending 5678 successful. TrxID SYN10001.", "City Bank");
    assert_eq!(tx.account_last4.as_deref(), Some("5678"));
}

#[test]
fn reported_balances_keep_their_sign() {
    for balance in [
        "BDT -100.00 Balance",
        "Available Balance: BDT -100.00",
        "Balance -100.00",
    ] {
        let body = format!("Your A/C XXXX1234 debited by BDT 500.00. {balance}.");
        assert_eq!(
            transaction(&body, "City Bank").balance_after_minor,
            Some(-10_000)
        );
    }
    let tx = transaction(
        "BDT 500.00 credited to A/C XXXX1234. Available Balance: BDT +100.00.",
        "City Bank",
    );
    assert_eq!(tx.balance_after_minor, Some(10_000));
}

#[test]
fn unknown_merchant_receipts_are_reviewed_not_inserted() {
    let body = "Payment of Tk 500.00 to Sample Shop successful. Ref SYN10006.";
    assert!(!parse_sms(body, Some("SHOP")).is_transaction());
    assert!(is_likely_financial_review(body, Some("SHOP")));
}

#[test]
fn source_bank_is_not_the_destination_wallet() {
    let tx = transaction("Your EBL account debited by BDT 300.00 as EBL Skybanking MFS Transfer-bKash. Balance BDT 900.00.", "Unknown");
    assert_eq!(tx.provider_id, "ebl");
    let tx = transaction("You have received BDT 500.00 credited to your bKash account from City Bank. Balance BDT 900.00. TrxID SYN10001.", "Unknown");
    assert_eq!(tx.provider_id, "bkash");
}

#[test]
fn a_marketing_footer_does_not_hide_a_posted_receipt() {
    let tx = transaction("Payment of Tk 500.00 to Sample Shop successful. TrxID SYN10007. Balance Tk 900.00. Download now for special offers.", "bKash");
    assert_eq!(tx.amount_minor, 50_000);
}

#[test]
fn invalid_amount_precision_is_not_silently_truncated() {
    for amount in ["1.999", "1.2.3", "100.001"] {
        let body = format!(
            "Payment of Tk {amount} to Sample Shop successful. TrxID SYN10008. Balance Tk 900.00."
        );
        assert!(
            !parse_sms(&body, Some("bKash")).is_transaction(),
            "{amount}"
        );
    }
    assert!(!parse_sms(
        "Payment of BDT 1.999 debited from A/C XXXX1234. Fee BDT 5.00.",
        Some("City Bank")
    )
    .is_transaction());
}

#[test]
fn a_card_purchase_keeps_its_merchant_category() {
    let tx = transaction(
        "Card ending 1234 was used at Foodpanda for BDT 500.00. Balance BDT 900.00.",
        "SCB",
    );
    assert_eq!(tx.category_id.as_deref(), Some("food"));
    for merchant in ["Refund Shop", "Interest Shop", "Salary Shop"] {
        let body = format!("Payment Tk 500.00 to {merchant} successful. TrxID SYN30001.");
        assert_eq!(
            transaction(&body, "bKash").category_id.as_deref(),
            Some("other")
        );
    }
}

#[test]
fn non_cash_bundles_with_future_repayment_are_not_cash_income() {
    for body in [
        "400MB/3day(s) has been added to your account. Tk29.00 will be deducted from your next recharge. Your total outstanding is Tk 29. For Details dial *123*600#",
        "Dear Customer, 1GB, 12Hr @40Tk has been added to your account. Tk 40.0 + Service fee of TK 0.00 will be deducted from your next recharge. Your total outstanding is Tk 40.00. For Details, dial *8#",
        "Dear Customer, 25Min, 24Hr @25Tk has been added to your account. Tk 25.0 + Service fee of TK 0.00 will be deducted from your next recharge. Your total outstanding is Tk 25.00. For Details, dial *8#",
    ] {
        assert!(!parse_sms(body, Some("Robi")).is_transaction());
        assert!(!is_likely_financial_review(body, Some("Robi")));
    }
}

#[test]
fn future_loan_service_fees_are_not_charged_at_disbursement() {
    let tx = transaction("Tk15 has been added to your account. Tk15 + Service fee of Tk2.78 will be deducted from your next recharge. Your total outstanding is Tk 17.78. For Details dial *123*600#", "Robi");
    assert_eq!(tx.transaction_type, TransactionType::Income);
    assert_eq!(tx.amount_minor, 1_500);
    assert_eq!(tx.fee_minor, None);
}

#[test]
fn intermediary_recharge_notice_does_not_use_the_promotional_price() {
    let body = "Your bKash Mobile Recharge request of Tk 50.00 for 017XXXXXXXX was successful. To get 5 Tk Cashback, dial *247# and Mobile Recharge 105 Tk. TC Apply.";
    assert!(!parse_sms(body, Some("bKash")).is_transaction());
    assert!(!is_likely_financial_review(body, Some("bKash")));
}

#[test]
fn confirmed_loan_disbursement_repayment_and_packs_still_parse() {
    for (sender, body, kind, amount) in [
        ("bKash", "You have received Digital Loan Tk 500.00 from City Bank. Balance Tk 600.00. TrxID SYN20001.", TransactionType::Income, 50_000),
        ("bKash", "Your Digital Loan Repayment of Tk 500.00 is successful. Balance Tk 100.00. TrxID SYN20002.", TransactionType::Expense, 50_000),
        ("GP", "Tk 25.00 has been deducted from your recharge for previous Emergency Balance. Remaining balance Tk 25.00.", TransactionType::Expense, 2_500),
        ("GP", "You have successfully purchased 1.5GB Internet at Tk 43.00. Validity: 3 Days. Dial *121*1# to check balance.", TransactionType::Expense, 4_300),
    ] {
        let tx = transaction(body, sender);
        assert_eq!(tx.transaction_type, kind);
        assert_eq!(tx.amount_minor, amount);
    }
}

#[test]
fn bank_transfer_direction_follows_the_tracked_account() {
    for (body, expected) in [
        ("Your A/C XXXX1234 debited by BDT 500.00 via NPSB. Ref NP10001.", TransactionType::Expense),
        ("Fund transfer of BDT 500.00 from A/C XXXX1234 via NPSB. Ref NP10002.", TransactionType::Expense),
        ("Your A/C XXXX1234 credited by BDT 500.00 as EBL Account Transfer. Ref NP10003.", TransactionType::Income),
        ("Your A/C XXXX1234 credited by BDT 500.00 as EBL Skybanking MFS Transfer-bKash. Ref NP10004.", TransactionType::Income),
        ("Transfer of BDT 500.00 between your own accounts. Ref NP10005.", TransactionType::Transfer),
    ] {
        assert_eq!(transaction(body, "EBL").transaction_type, expected, "{body}");
    }
}

#[test]
fn recharge_requests_waiting_for_confirmation_are_not_spending() {
    let body = "Received Recharge request of Tk 20.00 for 017XXXXXXXX. Fee Tk 0.00. Balance Tk 100.00. Wait for confirmation.";
    assert!(!parse_sms(body, Some("bKash")).is_transaction());
    assert!(!is_likely_financial_review(body, Some("bKash")));
}

#[test]
fn recruitment_and_foreign_bonus_notices_are_not_financial_review() {
    for body in [
        "Sir/Madam, I have contacted you by phone. The daily salary for working in Demo Company is 500 Taka. If you are interested, please contact https://example.invalid/job",
        "ON: Welcome to My ON Your £5 bonus credit has been added to your balance. Terms apply. Go to your account: https://example.invalid/account",
    ] {
        assert!(!parse_sms(body, None).is_transaction());
        assert!(!is_likely_financial_review(body, None));
    }
}

#[test]
fn offer_instructions_are_not_transactions_for_any_provider() {
    for sender in [
        "GP",
        "Robi",
        "Banglalink",
        "Airtel",
        "Teletalk",
        "bKash",
        "City Bank",
    ] {
        for body in [
            "Recharge Tk 99.00 and get 10GB for 30 days.",
            "Buy 5GB at Tk 99.00 and enjoy extra minutes.",
            "Get a loan of BDT 5000.00 at 9% interest. Apply in the app.",
            "You have received an offer of a loan of Tk 5000.00. Open app to accept.",
        ] {
            assert!(
                !parse_sms(body, Some(sender)).is_transaction(),
                "{sender}: {body}"
            );
            assert!(
                !is_likely_financial_review(body, Some(sender)),
                "{sender}: {body}"
            );
        }
    }
}

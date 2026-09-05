use centwise_domain::TransactionType;
use centwise_parser::{parse_sms, ParseOutcome};

#[test]
fn rejects_unposted_refunds() {
    for body in [
        "Refund of Tk 500.00 was unsuccessful.",
        "Refund of Tk 500.00 has not been credited.",
        "Refund of Tk 500.00 will be credited tomorrow.",
        "Refund of Tk 500.00 is pending. Original payment was successful.",
    ] {
        assert!(!parse_sms(body, Some("bKash")).is_transaction(), "{body}");
    }
}

#[test]
fn rejects_pending_requests_and_reminders() {
    for body in [
        "Payment Tk 500.00 pending. Ref SYN10001.",
        "Transfer of Tk 500.00 requested. Ref SYN10002.",
        "Reminder: loan repayment Tk 500.00 due tomorrow.",
    ] {
        assert!(!parse_sms(body, Some("EBL")).is_transaction(), "{body}");
    }
}

#[test]
fn rejects_payment_otp() {
    assert!(!parse_sms(
        "Your OTP is 123456 for payment of BDT 500.00. Do not share this code.",
        Some("City Bank"),
    )
    .is_transaction());
}

#[test]
fn skips_leading_available_limit() {
    let ParseOutcome::Parsed(tx) = parse_sms(
        "Available Limit BDT 9,000.00. BDT 500.00 debited from Card *1001.",
        Some("City Bank"),
    ) else {
        panic!("expected transaction")
    };
    assert_eq!(tx.amount_minor, 50_000);
}

#[test]
fn preserves_completed_refunds_and_security_footers() {
    for body in [
        "Failed payment reversed. Refund of Tk 500.00 has been credited to A/C *1001.",
        "Refund of Tk 500.00 completed for cancelled payment.",
        "Tk 500.00 refunded for failed payment.",
    ] {
        let ParseOutcome::Parsed(tx) = parse_sms(body, Some("bKash")) else {
            panic!("expected posted refund: {body}")
        };
        assert_eq!(tx.transaction_type, TransactionType::Refund);
        assert_eq!(tx.amount_minor, 50_000);
    }
    assert!(parse_sms(
        "Payment of Tk 500.00 successful. Never share your OTP.",
        Some("bKash")
    )
    .is_transaction());
}

#[test]
fn amount_is_not_a_date_or_phone_after_a_verb() {
    for body in [
        "Payment 05/09/2026: Tk 500.00 successful. Ref SYN10001.",
        "Payment 01700000000: Tk 500.00 successful. Ref SYN10002.",
    ] {
        let ParseOutcome::Parsed(tx) = parse_sms(body, Some("bKash")) else {
            panic!("expected transaction: {body}")
        };
        assert_eq!(tx.amount_minor, 50_000, "{body}");
    }
}

#[test]
fn account_words_are_not_masked_accounts() {
    let ParseOutcome::Parsed(tx) = parse_sms(
        "Tk 500.00 credited to your account today. Ref SYN10001.",
        Some("bKash"),
    ) else {
        panic!("expected transaction")
    };
    assert_eq!(tx.account_hint, None);
    assert_eq!(tx.account_last4, None);
}

#[test]
fn parses_hyphenated_auto_debit() {
    let ParseOutcome::Parsed(tx) = parse_sms(
        "Auto-debit of BDT 500.00 completed from A/C *1001.",
        Some("UCB"),
    ) else {
        panic!("expected auto-debit")
    };
    assert_eq!(tx.transaction_type, TransactionType::Expense);
    assert_eq!(tx.amount_minor, 50_000);
}

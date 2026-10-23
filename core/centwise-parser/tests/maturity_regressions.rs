use centwise_domain::TransactionType;
use centwise_parser::{is_likely_financial_review, parse_sms, ParseOutcome};

#[test]
fn incoming_transfers_are_income_across_bank_and_wallet_senders() {
    for sender in ["bKash", "Nagad", "EBL", "City Bank"] {
        for body in [
            "You have received BDT 250.00 via fund transfer. Balance BDT 900.00.",
            "BDT 250.00 has been transferred to your account. Balance BDT 900.00.",
        ] {
            let ParseOutcome::Parsed(tx) = parse_sms(body, Some(sender)) else {
                panic!("expected incoming transfer for {sender}");
            };
            assert_eq!(tx.transaction_type, TransactionType::Income, "{sender}");
            assert_eq!(tx.amount_minor, 25_000);
        }
    }
}

#[test]
fn outgoing_transfers_with_recipient_credit_remain_expenses() {
    for body in [
        "BDT 250.00 debited from your account and transferred to another account.",
        "You have transferred BDT 250.00 to Demo Shop. Payment received by merchant.",
    ] {
        let ParseOutcome::Parsed(tx) = parse_sms(body, Some("EBL")) else {
            panic!("expected outgoing transfer");
        };
        assert_eq!(tx.transaction_type, TransactionType::Expense);
        assert_eq!(tx.amount_minor, 25_000);
    }
}

#[test]
fn security_code_aliases_never_become_successful_payments() {
    for label in [
        "one-time password",
        "verification PIN",
        "authentication code",
        "confirmation code",
        "temporary password",
        "security PIN",
    ] {
        let body =
            format!("Your {label} is 123456. Use it for a successful payment of BDT 250.00.");
        assert!(
            !parse_sms(&body, Some("City Bank")).is_transaction(),
            "{label}"
        );
        assert!(
            !is_likely_financial_review(&body, Some("City Bank")),
            "{label}"
        );
    }
}

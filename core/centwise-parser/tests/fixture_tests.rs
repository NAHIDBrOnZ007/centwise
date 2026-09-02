use centwise_domain::TransactionType;
use centwise_parser::{parse_sms, ParseOutcome};
use serde::Deserialize;

#[derive(Debug, Deserialize)]
struct FixtureFile {
    pub provider: String,
    pub messages: Vec<FixtureMessage>,
}

#[derive(Debug, Deserialize)]
struct FixtureMessage {
    pub kind: String,
    #[serde(rename = "senderHint")]
    pub sender_hint: Option<String>,
    pub body: String,
    pub expected: ExpectedResult,
}

#[derive(Debug, Deserialize)]
struct ExpectedResult {
    #[serde(rename = "isTransaction")]
    pub is_transaction: Option<bool>,
    #[serde(rename = "type")]
    pub transaction_type: Option<String>,
    #[serde(rename = "amountMinor")]
    pub amount_minor: Option<i64>,
    #[serde(rename = "feeMinor")]
    pub fee_minor: Option<i64>,
    #[serde(rename = "balanceAfterMinor")]
    pub balance_after_minor: Option<i64>,
    pub reference: Option<String>,
    pub party: Option<String>,
    pub merchant: Option<String>,
    pub category: Option<String>,
    #[serde(rename = "accountLast4")]
    pub account_last4: Option<String>,
    #[serde(rename = "accountHint")]
    pub account_hint: Option<String>,
    #[serde(rename = "date")]
    pub _date: Option<String>,
}

fn test_fixture_file(relative_path: &str) {
    let manifest_dir = env!("CARGO_MANIFEST_DIR");
    let full_path = std::path::Path::new(manifest_dir)
        .parent()
        .unwrap()
        .parent()
        .unwrap()
        .join(relative_path);

    let content = std::fs::read_to_string(&full_path)
        .unwrap_or_else(|e| panic!("Failed to read fixture file at {:?}: {}", full_path, e));

    let fixture: FixtureFile = serde_json::from_str(&content)
        .unwrap_or_else(|e| panic!("Failed to deserialize {:?}: {}", full_path, e));

    println!(
        "Testing fixture provider '{}' with {} messages",
        fixture.provider,
        fixture.messages.len()
    );

    for msg in &fixture.messages {
        let outcome = parse_sms(&msg.body, msg.sender_hint.as_deref());

        if msg.expected.is_transaction == Some(false) {
            assert!(
                !outcome.is_transaction(),
                "Expected message to be rejected, but got parsed transaction: {:?} for msg: '{}'",
                outcome,
                msg.body
            );
            continue;
        }

        match outcome {
            ParseOutcome::Parsed(tx) => {
                // Check transaction type
                if let Some(expected_type) = &msg.expected.transaction_type {
                    let expected_enum = match expected_type.as_str() {
                        "income" => TransactionType::Income,
                        "expense" => TransactionType::Expense,
                        "transfer" => TransactionType::Transfer,
                        "refund" => TransactionType::Refund,
                        _ => panic!("Unknown expected type: {}", expected_type),
                    };
                    assert_eq!(
                        tx.transaction_type, expected_enum,
                        "Transaction type mismatch in kind '{}': body: '{}'",
                        msg.kind, msg.body
                    );
                }

                // Check amount
                if let Some(expected_amt) = msg.expected.amount_minor {
                    assert_eq!(
                        tx.amount_minor, expected_amt,
                        "Amount mismatch in kind '{}': body: '{}'",
                        msg.kind, msg.body
                    );
                }

                // Check fee
                if let Some(expected_fee) = msg.expected.fee_minor {
                    assert_eq!(
                        tx.fee_minor,
                        Some(expected_fee),
                        "Fee mismatch in kind '{}': body: '{}'",
                        msg.kind,
                        msg.body
                    );
                }

                // Check balance after
                if let Some(expected_bal) = msg.expected.balance_after_minor {
                    assert_eq!(
                        tx.balance_after_minor,
                        Some(expected_bal),
                        "Balance mismatch in kind '{}': body: '{}'",
                        msg.kind,
                        msg.body
                    );
                }

                // Check reference
                if let Some(expected_ref) = &msg.expected.reference {
                    assert_eq!(
                        tx.reference.as_deref(),
                        Some(expected_ref.as_str()),
                        "Reference mismatch in kind '{}': body: '{}'",
                        msg.kind,
                        msg.body
                    );
                }

                // Check party
                if let Some(expected_party) = &msg.expected.party {
                    assert_eq!(
                        tx.party.as_deref(),
                        Some(expected_party.as_str()),
                        "Party mismatch in kind '{}': body: '{}'",
                        msg.kind,
                        msg.body
                    );
                }

                // Check merchant
                if let Some(expected_merchant) = &msg.expected.merchant {
                    assert_eq!(
                        tx.merchant.as_deref(),
                        Some(expected_merchant.as_str()),
                        "Merchant mismatch in kind '{}': body: '{}'",
                        msg.kind,
                        msg.body
                    );
                }

                // Check category
                if let Some(expected_cat) = &msg.expected.category {
                    assert_eq!(
                        tx.category_id.as_deref(),
                        Some(expected_cat.as_str()),
                        "Category mismatch in kind '{}': body: '{}'",
                        msg.kind,
                        msg.body
                    );
                }

                // Check account last 4
                if let Some(expected_last4) = &msg.expected.account_last4 {
                    assert_eq!(
                        tx.account_last4.as_deref(),
                        Some(expected_last4.as_str()),
                        "Account Last4 mismatch in kind '{}': body: '{}'",
                        msg.kind,
                        msg.body
                    );
                }

                // Check account hint
                if let Some(expected_hint) = &msg.expected.account_hint {
                    assert_eq!(
                        tx.account_hint.as_deref(),
                        Some(expected_hint.as_str()),
                        "Account Hint mismatch in kind '{}': body: '{}'",
                        msg.kind,
                        msg.body
                    );
                }
            }
            ParseOutcome::Rejected(reason) => {
                panic!(
                    "Message was unexpectedly rejected with reason {:?} in kind '{}': body: '{}'",
                    reason, msg.kind, msg.body
                );
            }
        }
    }
}

#[test]
fn test_bkash_fixtures() {
    test_fixture_file("fixtures/sms/bkash.json");
}

#[test]
fn test_nagad_fixtures() {
    test_fixture_file("fixtures/sms/nagad.json");
}

#[test]
fn test_rocket_fixtures() {
    test_fixture_file("fixtures/sms/rocket.json");
}

#[test]
fn test_banks_generic_fixtures() {
    test_fixture_file("fixtures/sms/banks-generic.json");
}

#[test]
fn test_bangla_fixtures() {
    test_fixture_file("fixtures/sms/bangla.json");
}

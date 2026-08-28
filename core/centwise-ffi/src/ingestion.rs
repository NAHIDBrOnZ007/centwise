use crate::types::TransactionKind;

/// Result of parsing an incoming SMS across the FFI boundary.
#[derive(uniffi::Record)]
pub struct ParsedSmsRecord {
    pub is_transaction: bool,
    pub provider_id: String,
    pub kind: Option<TransactionKind>,
    pub amount_minor: Option<i64>,
    pub fee_minor: Option<i64>,
    pub balance_after_minor: Option<i64>,
    pub reference: Option<String>,
    pub party: Option<String>,
    pub merchant: Option<String>,
    pub category_id: Option<String>,
    pub account_last4: Option<String>,
    pub account_hint: Option<String>,
    pub raw_date: Option<String>,
}

/// Standalone SMS parser function callable directly from Kotlin or Swift.
#[uniffi::export]
pub fn parse_sms_message(body: String, sender_hint: Option<String>) -> ParsedSmsRecord {
    match centwise_parser::parse_sms(&body, sender_hint.as_deref()) {
        centwise_parser::ParseOutcome::Parsed(tx) => {
            let tx = *tx;
            ParsedSmsRecord {
                is_transaction: true,
                provider_id: tx.provider_id,
                kind: Some(tx.transaction_type.into()),
                amount_minor: Some(tx.amount_minor),
                fee_minor: tx.fee_minor,
                balance_after_minor: tx.balance_after_minor,
                reference: tx.reference,
                party: tx.party,
                merchant: tx.merchant,
                category_id: tx.category_id,
                account_last4: tx.account_last4,
                account_hint: tx.account_hint,
                raw_date: tx.raw_date,
            }
        }
        centwise_parser::ParseOutcome::Rejected(_) => ParsedSmsRecord {
            is_transaction: false,
            provider_id: String::new(),
            kind: None,
            amount_minor: None,
            fee_minor: None,
            balance_after_minor: None,
            reference: None,
            party: None,
            merchant: None,
            category_id: None,
            account_last4: None,
            account_hint: None,
            raw_date: None,
        },
    }
}

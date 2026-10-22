//! Explicit generic fallback provider boundary and shared field pipeline.

use super::{NormalizedSms, ProviderParse};
use crate::types::{ParsedTransaction, RejectReason};

pub const PROVIDER_ID: &str = "banks-generic";

/// Runs the existing provider-independent field pipeline.
///
/// Provider modules call this only after their provider-specific checks. The
/// helper deliberately contains no provider names or sender matching.
pub fn parse_common(
    provider_id: &'static str,
    sms: NormalizedSms<'_>,
    account_override: Option<(Option<String>, Option<String>)>,
    party_override: Option<Option<String>>,
) -> ProviderParse {
    let receipt = (provider_id == PROVIDER_ID)
        .then(|| super::receipts::ConfirmedReceipt::recognize(sms.body))
        .flatten();
    if provider_id == PROVIDER_ID {
        let lower = sms.body.to_ascii_lowercase();
        let has_account = ["a/c", "account", "card", "acct", "ac "]
            .iter()
            .any(|marker| lower.contains(marker));
        let has_movement = [
            "debited",
            "credited",
            "transfer",
            "received",
            "cash-out",
            "cash-in",
            "withdraw",
            "deducted",
            "disbursed",
            "used at",
        ]
        .iter()
        .any(|marker| lower.contains(marker));
        if receipt.is_none() && (!has_account || !has_movement) {
            return ProviderParse::Rejected(RejectReason::UnsupportedProvider);
        }
    }
    let reference = crate::extract::extract_reference(sms.body);
    let event_body = crate::common::events::posted_refund_clause(sms.body)
        .and_then(|clause| sms.body.find(clause))
        .map(|start| &sms.body[start..])
        .unwrap_or(sms.body);
    let mut fee_minor = crate::extract::extract_fee(event_body);
    let balance_after_minor = crate::extract::extract_balance(sms.body);
    let amount_minor = match receipt
        .map(|receipt| receipt.amount_minor)
        .or_else(|| crate::extract::extract_main_amount(event_body, fee_minor, balance_after_minor))
    {
        Some(amount) => amount,
        None => return ProviderParse::Rejected(RejectReason::NoAmountFound),
    };
    let transaction_type = match crate::extract::detect_transaction_type(sms.body) {
        Some(transaction_type) => transaction_type,
        None => return ProviderParse::Rejected(RejectReason::NotATransaction),
    };
    let party = party_override.unwrap_or_else(|| crate::extract::extract_party(sms.body));
    let (account_last4, account_hint) =
        account_override.unwrap_or_else(|| crate::extract::extract_account_info(sms.body));
    let raw_date = crate::extract::extract_date_time(sms.body);
    let (merchant, category_id) =
        crate::extract::resolve_categorization(party.as_deref(), sms.body, transaction_type);
    if fee_minor == Some(amount_minor)
        && centwise_categorization::categorize_by_type_or_keywords(event_body, transaction_type)
            .as_deref()
            == Some("fees")
    {
        fee_minor = None;
    }

    let mut transaction = ParsedTransaction {
        provider_id: provider_id.to_string(),
        transaction_type,
        amount_minor,
        fee_minor,
        balance_after_minor,
        reference,
        party,
        merchant,
        category_id,
        account_last4,
        account_hint,
        raw_date,
    };
    if let Some(receipt) = receipt {
        receipt.apply(&mut transaction);
    }
    ProviderParse::Parsed(Box::new(transaction))
}

/// Applies a provider-owned category after the shared fields have been parsed.
/// The event predicate remains in the provider module; this helper only avoids
/// duplicating the common transaction construction code.
pub fn parse_with_category(
    provider_id: &'static str,
    sms: NormalizedSms<'_>,
    category_id: &'static str,
) -> ProviderParse {
    match parse_common(provider_id, sms, None, None) {
        ProviderParse::Parsed(mut transaction) => {
            transaction.category_id = Some(category_id.to_string());
            ProviderParse::Parsed(transaction)
        }
        other => other,
    }
}

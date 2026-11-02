//! Stage 3: Field Extraction
//!
//! Independently hunts for amount, fee, balance, reference, transaction type,
//! party/merchant, account identifiers, and dates.

pub mod account;
pub mod amount;
pub mod date;
pub mod party;
pub mod reference;
pub mod tx_type;

pub use account::extract_account_info;
pub use amount::{extract_balance, extract_fee, extract_main_amount};
pub use date::extract_date_time;
pub use party::extract_party;
pub use reference::extract_reference;
pub use tx_type::detect_transaction_type;

use centwise_categorization::{
    categorize_by_merchant, categorize_by_type_or_keywords, category_description,
};
use centwise_domain::TransactionType;

/// Resolves matched merchant name and category identifier.
pub fn resolve_categorization(
    party: Option<&str>,
    body: &str,
    transaction_type: TransactionType,
) -> (Option<String>, Option<String>) {
    let description = category_description(body);
    let body = description.as_str();
    let category = categorize_by_type_or_keywords(body, transaction_type);
    if transaction_type == TransactionType::Expense && category.as_deref() == Some("recharge") {
        if let Some((operator, _)) = categorize_mobile_recharge_by_prefix(party, body) {
            return (Some(operator.to_string()), category);
        }
    }
    if transaction_type != TransactionType::Expense || category.as_deref() != Some("other") {
        let merchant = (transaction_type == TransactionType::Expense)
            .then(|| categorize_by_merchant(body))
            .flatten()
            .filter(|matched| Some(matched.category_id.as_str()) == category.as_deref())
            .and_then(|matched| matched.matched_merchant);
        return (merchant, category);
    }
    // A recipient's identity does not explain the purpose of money sent to them.
    if [
        "send money",
        "money sent",
        "sent money",
        "transfer to",
        "fund transfer",
        "funds transfer",
        "mfs transfer",
        "account transfer",
        "trf to",
    ]
    .iter()
    .any(|marker| body.to_lowercase().contains(marker))
    {
        return (None, category);
    }
    // 1. If party matches a known merchant
    if let Some(p) = party.filter(|party| body.to_lowercase().contains(&party.to_lowercase())) {
        if let Some(cat_res) = categorize_by_merchant(p) {
            return (cat_res.matched_merchant, Some(cat_res.category_id));
        }
    }

    // 2. Scan entire body for known merchants
    if let Some(cat_res) = categorize_by_merchant(body) {
        return (cat_res.matched_merchant, Some(cat_res.category_id));
    }

    // 3. Fallback to keyword / transaction type based category
    (None, category)
}

fn categorize_mobile_recharge_by_prefix(
    party: Option<&str>,
    body: &str,
) -> Option<(&'static str, &'static str)> {
    let lower = body.to_lowercase();
    if !(lower.contains("recharge")
        || lower.contains("data pack")
        || lower.contains("internet at")
        || lower.contains("gb at"))
    {
        return None;
    }

    let phone = party?.trim();
    crate::common::phone::recharge_operator(phone).map(|operator| (operator, "recharge"))
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn transaction_meaning_takes_priority_over_merchants_and_offers() {
        for (body, kind, expected) in [
            (
                "Refund Tk 500 from Bata credited",
                TransactionType::Refund,
                "refunds",
            ),
            (
                "Tk 500 received from Foodpanda",
                TransactionType::Income,
                "income",
            ),
            (
                "Transfer between your own accounts at Bata",
                TransactionType::Transfer,
                "transfer",
            ),
            (
                "Payment Tk 500 successful. Get cashback at Foodpanda",
                TransactionType::Expense,
                "other",
            ),
            (
                "Send money Tk 500 to Airtel successful",
                TransactionType::Expense,
                "other",
            ),
            (
                "Cash Out Tk 500 at Foodpanda successful",
                TransactionType::Expense,
                "cash-withdrawal",
            ),
        ] {
            assert_eq!(
                resolve_categorization(None, body, kind).1.as_deref(),
                Some(expected),
                "{body}"
            );
        }
    }

    #[test]
    fn derives_mobile_operator_from_recharge_phone_prefix() {
        assert_eq!(
            resolve_categorization(
                Some("01712345678"),
                "Recharge of Tk 100.00 on 01712345678 successful",
                TransactionType::Expense,
            ),
            (
                Some("Grameenphone".to_string()),
                Some("recharge".to_string())
            )
        );
    }
}

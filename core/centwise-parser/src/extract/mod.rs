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

use centwise_categorization::{categorize_by_merchant, categorize_by_type_or_keywords};
use centwise_domain::TransactionType;

/// Resolves matched merchant name and category identifier.
pub fn resolve_categorization(
    party: Option<&str>,
    body: &str,
    transaction_type: TransactionType,
) -> (Option<String>, Option<String>) {
    // 1. If party matches a known merchant
    if let Some(p) = party {
        if let Some(cat_res) = categorize_by_merchant(p) {
            return (cat_res.matched_merchant, Some(cat_res.category_id));
        }
    }

    // 2. Scan entire body for known merchants
    if let Some(cat_res) = categorize_by_merchant(body) {
        return (cat_res.matched_merchant, Some(cat_res.category_id));
    }

    if transaction_type == TransactionType::Expense {
        if let Some((operator, category)) = categorize_mobile_recharge_by_prefix(party, body) {
            return (Some(operator.to_string()), Some(category.to_string()));
        }
    }

    // 3. Fallback to keyword / transaction type based category
    let category = categorize_by_type_or_keywords(body, transaction_type);
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
    if phone.starts_with("013") || phone.starts_with("017") {
        Some(("Grameenphone", "recharge"))
    } else if phone.starts_with("014") || phone.starts_with("019") {
        Some(("Banglalink", "recharge"))
    } else if phone.starts_with("015") {
        Some(("Teletalk", "recharge"))
    } else if phone.starts_with("016") {
        Some(("Airtel", "recharge"))
    } else if phone.starts_with("018") {
        Some(("Robi", "recharge"))
    } else {
        None
    }
}

#[cfg(test)]
mod tests {
    use super::*;

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

//! Merchant dictionary and rule-based categorization for Centwise.
//!
//! Maps extracted merchant strings and transaction types to stable category IDs
//! per `docs/architecture/parser-design.md`.

use centwise_domain::TransactionType;

/// Represents a detected category with its standard slug.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct CategorizationResult {
    pub category_id: String,
    pub matched_merchant: Option<String>,
}

/// Attempt to categorize by looking for known merchant keywords in text.
pub fn categorize_by_merchant(party_or_merchant: &str) -> Option<CategorizationResult> {
    let lower = party_or_merchant.to_lowercase();
    for rule in centwise_domain::default_merchant_categories() {
        for &keyword in rule.keywords {
            if lower.contains(keyword) {
                return Some(CategorizationResult {
                    category_id: rule.category_id.to_string(),
                    matched_merchant: Some(rule.name.to_string()),
                });
            }
        }
    }
    None
}

/// Fallback category inference from transaction type and keywords.
pub fn categorize_by_type_or_keywords(
    text: &str,
    transaction_type: TransactionType,
) -> Option<String> {
    let lower = text.to_lowercase();

    if transaction_type == TransactionType::Refund
        || contains_any(&lower, &["refund", "reversal", "reversed", "ফেরত"])
    {
        return Some("refunds".to_string());
    }
    if contains_any(&lower, &["cashback", "cash back", "ক্যাশব্যাক"]) {
        return Some("cashback".to_string());
    }
    if contains_any(
        &lower,
        &[
            "interest",
            "mudaraba profit",
            "profit credited",
            "মুনাফা",
            "সুদ",
        ],
    ) {
        return Some("interest-profit".to_string());
    }
    if contains_any(&lower, &["dividend", "লভ্যাংশ"]) {
        return Some("dividends".to_string());
    }
    if contains_any(&lower, &["salary", "payroll", "wages", "বেতন"]) {
        return Some("salary".to_string());
    }
    if lower.contains("recharge") {
        return Some("recharge".to_string());
    }
    if contains_any(
        &lower,
        &["atm", "cash withdrawal", "cash out", "নগদ উত্তোলন"],
    ) {
        return Some("cash-withdrawal".to_string());
    }
    if lower.contains("emi") || lower.contains("loan") || lower.contains("bill") {
        return Some("bills".to_string());
    }
    if is_fee_transaction(&lower) {
        return Some("fees".to_string());
    }

    Some(
        match transaction_type {
            TransactionType::Income => "income",
            TransactionType::Refund => "refunds",
            TransactionType::Transfer => "transfer",
            TransactionType::Expense => "other",
        }
        .to_string(),
    )
}

fn contains_any(text: &str, needles: &[&str]) -> bool {
    needles.iter().any(|needle| text.contains(needle))
}

fn is_fee_transaction(text: &str) -> bool {
    let has_fee_action = contains_any(
        text,
        &[
            "fee charged",
            "charge debited",
            "service charge",
            "annual fee",
            "annual card fee",
            "card annual fee",
            "sms alert fee",
            "card fee",
            "maintenance fee",
            "maintenance charge",
            "ledger fee",
            "excise duty",
            "আবগারি শুল্ক",
            "চার্জ কাটা",
            "সার্ভিস চার্জ",
        ],
    ) || (text.contains("fee") && text.contains("debited"));

    let has_primary_action = contains_any(
        text,
        &["payment", "purchase", "recharge", "cash out", "withdrawal"],
    );
    has_fee_action && !has_primary_action
}

#[cfg(test)]
mod tests {
    use super::*;
    use centwise_domain::TransactionType;

    #[test]
    fn categorizes_known_merchants() {
        let result = categorize_by_merchant("Foodpanda").unwrap();
        assert_eq!(result.category_id, "food");
        assert_eq!(result.matched_merchant, Some("Foodpanda".to_string()));

        let result = categorize_by_merchant("Payment to Pathao").unwrap();
        assert_eq!(result.category_id, "transport");
        assert_eq!(result.matched_merchant, Some("Pathao".to_string()));

        let result = categorize_by_merchant("Airtel 017XXXXXXXX").unwrap();
        assert_eq!(result.category_id, "recharge");
        assert_eq!(result.matched_merchant, Some("Airtel".to_string()));
    }

    #[test]
    fn fallback_by_keywords() {
        assert_eq!(
            categorize_by_type_or_keywords("Mobile Recharge Tk 100", TransactionType::Expense),
            Some("recharge".to_string())
        );
        assert_eq!(
            categorize_by_type_or_keywords("ATM Cash Withdrawal", TransactionType::Expense),
            Some("cash-withdrawal".to_string())
        );
        assert_eq!(
            categorize_by_type_or_keywords("EMI of Tk 8500", TransactionType::Expense),
            Some("bills".to_string())
        );
    }

    #[test]
    fn income_is_salary_only_with_a_strong_salary_signal() {
        assert_eq!(
            categorize_by_type_or_keywords(
                "Your account was credited with Tk 25,000",
                TransactionType::Income,
            ),
            Some("income".to_string())
        );
        assert_eq!(
            categorize_by_type_or_keywords(
                "Monthly salary credited to your account",
                TransactionType::Income,
            ),
            Some("salary".to_string())
        );
        assert_eq!(
            categorize_by_type_or_keywords("বেতন বাবদ ২৫,০০০ টাকা জমা হয়েছে", TransactionType::Income,),
            Some("salary".to_string())
        );
    }

    #[test]
    fn special_credits_remain_separate() {
        assert_eq!(
            categorize_by_type_or_keywords("Purchase refund credited", TransactionType::Refund),
            Some("refunds".to_string())
        );
        assert_eq!(
            categorize_by_type_or_keywords("Cashback credited", TransactionType::Income),
            Some("cashback".to_string())
        );
        assert_eq!(
            categorize_by_type_or_keywords("Mudaraba profit credited", TransactionType::Income),
            Some("interest-profit".to_string())
        );
        assert_eq!(
            categorize_by_type_or_keywords("Dividend credited", TransactionType::Income),
            Some("dividends".to_string())
        );
    }

    #[test]
    fn unknown_expense_uses_other_without_an_unknown_category() {
        assert_eq!(
            categorize_by_type_or_keywords("Card purchase", TransactionType::Expense),
            Some("other".to_string())
        );
    }
}

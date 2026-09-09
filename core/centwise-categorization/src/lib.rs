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
            let matched = if keyword.len() <= 4 && !keyword.contains(' ') {
                has_word(&lower, keyword)
            } else {
                lower.contains(keyword)
            };
            if matched {
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

    if transaction_type == TransactionType::Transfer
        || contains_any(
            &lower,
            &[
                "send money",
                "money sent",
                "sent money",
                "transfer to",
                "fund transfer",
                "mfs transfer",
                "account transfer",
                "trf to",
            ],
        )
    {
        return Some("transfer".to_string());
    }

    if transaction_type == TransactionType::Refund
        || contains_any(&lower, &["refund", "reversal", "reversed"])
    {
        return Some("refunds".to_string());
    }

    if transaction_type == TransactionType::Income {
        if contains_any(&lower, &["salary", "payroll", "wages"]) {
            return Some("salary".to_string());
        }
        if contains_any(&lower, &["interest", "mudaraba profit", "profit credited"]) {
            return Some("interest-profit".to_string());
        }
        if contains_any(&lower, &["dividend"]) {
            return Some("dividends".to_string());
        }
        if contains_any(&lower, &["cashback", "cash back"]) {
            return Some("cashback".to_string());
        }
        return Some("income".to_string());
    }

    if contains_any(&lower, &["atm", "cash withdrawal", "cash out", "cash wd"])
        || (lower.contains("citytouch txn") && lower.contains("withdrawal"))
    {
        return Some("cash-withdrawal".to_string());
    }
    if lower.contains("recharge")
        || ((lower.contains("data pack")
            || lower.contains("internet at")
            || lower.contains("gb at"))
            && (lower.contains("successfully purchased")
                || lower.contains("successfully activated")
                || lower.contains("successfully bought")
                || lower.contains("activated successfully")))
    {
        return Some("recharge".to_string());
    }
    if contains_any(&lower, &["cashback", "cash back"]) {
        return Some("cashback".to_string());
    }
    if contains_any(&lower, &["interest", "mudaraba profit", "profit credited"]) {
        return Some("interest-profit".to_string());
    }
    if contains_any(&lower, &["dividend"]) {
        return Some("dividends".to_string());
    }
    if contains_any(&lower, &["salary", "payroll", "wages"]) {
        return Some("salary".to_string());
    }
    let has_bill = lower.contains("bill")
        && !lower.contains("bill no")
        && !lower.contains("bill number")
        && !lower.contains("purchase bill");
    if has_word(&lower, "emi") || has_word(&lower, "loan") || has_bill {
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

fn has_word(text: &str, word: &str) -> bool {
    text.split(|c: char| !c.is_alphanumeric())
        .any(|token| token.eq_ignore_ascii_case(word))
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

        // Brand tests for newly added categories
        let bata = categorize_by_merchant("Bata Showroom").unwrap();
        assert_eq!(bata.category_id, "shopping");
        assert_eq!(bata.matched_merchant, Some("Bata".to_string()));

        let apex = categorize_by_merchant("Apex Footwear").unwrap();
        assert_eq!(apex.category_id, "shopping");
        assert_eq!(apex.matched_merchant, Some("Apex".to_string()));

        let foodi = categorize_by_merchant("Foodi Delivery").unwrap();
        assert_eq!(foodi.category_id, "food");
        assert_eq!(foodi.matched_merchant, Some("Foodi".to_string()));

        let uber = categorize_by_merchant("Uber Trip").unwrap();
        assert_eq!(uber.category_id, "transport");
        assert_eq!(uber.matched_merchant, Some("Uber".to_string()));

        let arogga = categorize_by_merchant("Arogga Pharmacy").unwrap();
        assert_eq!(arogga.category_id, "health");
        assert_eq!(arogga.matched_merchant, Some("Arogga".to_string()));

        let shikho = categorize_by_merchant("Shikho Learning").unwrap();
        assert_eq!(shikho.category_id, "education");
        assert_eq!(shikho.matched_merchant, Some("Shikho".to_string()));

        let bracu = categorize_by_merchant("BRAC University Tuition").unwrap();
        assert_eq!(bracu.category_id, "education");
        assert_eq!(bracu.matched_merchant, Some("BRAC University".to_string()));
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

    #[test]
    fn ryans_and_cashout_categorization() {
        // Ryans mapped to shopping
        let ryans = categorize_by_merchant("Ryans").unwrap();
        assert_eq!(ryans.category_id, "shopping");

        // Cash Out with promotional cashback footer must be categorized as cash-withdrawal, NOT cashback
        let cashout =
            "Cash Out Tk 4,000.00 to 01707376622 successful. Cashback 50 on 25,000 CashOut";
        assert_eq!(
            categorize_by_type_or_keywords(cashout, TransactionType::Expense),
            Some("cash-withdrawal".to_string())
        );

        // "purchase bill no" must not trigger "bills"
        let invoice = "Your purchase bill no is B-2242120, Tk 1,000";
        assert_eq!(
            categorize_by_type_or_keywords(invoice, TransactionType::Expense),
            Some("other".to_string())
        );
    }

    #[test]
    fn categorizes_citytouch_and_transfers_before_fee_fallback() {
        assert_eq!(
            categorize_by_type_or_keywords(
                "CITYTOUCH TXN Tk. 500 Withdrawal Tk. 20 Balance",
                TransactionType::Expense,
            ),
            Some("cash-withdrawal".to_string())
        );
        assert_eq!(
            categorize_by_type_or_keywords(
                "CellFin Transfer Tk 3,500 debited. Fee Tk 5.00.",
                TransactionType::Transfer,
            ),
            Some("transfer".to_string())
        );
    }

    #[test]
    fn categorizes_confirmed_telco_data_packs_as_recharge() {
        for text in [
            "You have successfully purchased 1.5GB Internet at Tk 43.00",
            "You have successfully activated 10GB Data Pack at Tk 199.00",
            "You have successfully bought 1GB at Tk 36.00",
            "Data pack 1GB at Tk 23.00 activated successfully",
        ] {
            assert_eq!(
                categorize_by_type_or_keywords(text, TransactionType::Expense),
                Some("recharge".to_string()),
                "{text}"
            );
        }
    }
}

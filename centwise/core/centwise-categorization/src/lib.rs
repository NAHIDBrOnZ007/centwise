//! Merchant dictionary and rule-based categorization for Centwise.
//!
//! Maps extracted merchant strings and transaction types to stable category IDs
//! per `docs/architecture/parser-design.md`.

/// Represents a detected category with its standard slug.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct CategorizationResult {
    pub category_id: String,
    pub matched_merchant: Option<String>,
}

/// Known merchants with their associated category slug.
const MERCHANT_RULES: &[(&[&str], &str)] = &[
    (
        &[
            "foodpanda", "hungerstation", "sultan's dine", "sultans dine",
            "kacchi bhai", "pizza hut", "kfc", "domino's", "dominos",
            "burger king", "chillox", "madchef", "takeout", "bfc",
            "herfy", "gloria jean", "secret recipe"
        ],
        "food"
    ),
    (
        &[
            "pathao", "uber", "obhai", "shohoz", "cng", "rail sheba",
            "jatri", "biman", "us-bangla", "novoair"
        ],
        "transport"
    ),
    (
        &[
            "daraz", "pickaboo", "unimart", "swapno", "shwapno", "meena bazar",
            "agora", "aarong", "apex", "bata", "chaldal", "sailor",
            "cats eye", "yellow", "artisan", "almas", "lotto"
        ],
        "shopping"
    ),
    (
        &[
            "airtel", "grameenphone", "gp", "robi", "banglalink", "teletalk", "skitto"
        ],
        "recharge"
    ),
    (
        &[
            "netflix", "spotify", "hoichoi", "chorki", "star cineplex",
            "blockbuster", "toffee", "sony liv"
        ],
        "entertainment"
    ),
    (
        &[
            "desco", "dpdc", "nesco", "wasa", "titas", "bakhrabad",
            "karnaphuli", "palli bidyut", "btcl", "link3", "amberit",
            "carnival", "dot internet", "sam online"
        ],
        "bills"
    ),
];

/// Attempt to categorize by looking for known merchant keywords in text.
pub fn categorize_by_merchant(party_or_merchant: &str) -> Option<CategorizationResult> {
    let lower = party_or_merchant.to_lowercase();
    for (keywords, category_id) in MERCHANT_RULES {
        for &keyword in *keywords {
            if lower.contains(keyword) {
                // Return matched keyword with appropriate capitalization from rule or original slice
                let matched_name = capitalize_merchant(keyword);
                return Some(CategorizationResult {
                    category_id: (*category_id).to_string(),
                    matched_merchant: Some(matched_name),
                });
            }
        }
    }
    None
}

/// Fallback category inference from transaction type and keywords.
pub fn categorize_by_type_or_keywords(text: &str, is_income: bool) -> Option<String> {
    let lower = text.to_lowercase();

    if lower.contains("recharge") {
        return Some("recharge".to_string());
    }
    if lower.contains("atm") || lower.contains("cash withdrawal") {
        return Some("cash-withdrawal".to_string());
    }
    if lower.contains("emi") || lower.contains("loan") || lower.contains("bill") {
        return Some("bills".to_string());
    }
    if is_income {
        if lower.contains("salary") {
            return Some("salary".to_string());
        }
        if lower.contains("interest") || lower.contains("cashback") {
            return Some("income".to_string());
        }
    }

    None
}

fn capitalize_merchant(keyword: &str) -> String {
    match keyword {
        "foodpanda" => "Foodpanda".to_string(),
        "pathao" => "Pathao".to_string(),
        "uber" => "Uber".to_string(),
        "daraz" => "Daraz".to_string(),
        "gp" | "grameenphone" => "GP".to_string(),
        "airtel" => "Airtel".to_string(),
        "robi" => "Robi".to_string(),
        "banglalink" => "Banglalink".to_string(),
        "teletalk" => "Teletalk".to_string(),
        "shohoz" => "Shohoz".to_string(),
        "unimart" => "Unimart".to_string(),
        "shwapno" | "swapno" => "Shwapno".to_string(),
        "aarong" => "Aarong".to_string(),
        _ => {
            let mut c = keyword.chars();
            match c.next() {
                None => String::new(),
                Some(f) => f.to_uppercase().collect::<String>() + c.as_str(),
            }
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

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
        assert_eq!(categorize_by_type_or_keywords("Mobile Recharge Tk 100", false), Some("recharge".to_string()));
        assert_eq!(categorize_by_type_or_keywords("ATM Cash Withdrawal", false), Some("cash-withdrawal".to_string()));
        assert_eq!(categorize_by_type_or_keywords("EMI of Tk 8500", false), Some("bills".to_string()));
    }
}

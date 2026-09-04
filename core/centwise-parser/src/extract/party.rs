//! Field extraction: Clean Merchant & Counterparty Names.
//!
//! Eliminates sentence fragments, verbs, and preposition trails:
//! - Eliminates "recharge 50 TAKA from 1847662920 is"
//! - Eliminates "your next recharge"
//! - Eliminates "purchase a data bundle and pay"
//! - Eliminates "get Jhotpot Loan"
//! - Cleans "'Software Shop Ltd' is" into "Software Shop Ltd"
//! - Extracts utility biller names from "Bill Payment ... to DESCO"
//! - Extracts transfer counterparty from "to bKash 017XXXXXXXX"

use regex::Regex;
use std::sync::LazyLock;

static AT_MERCHANT_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(r"(?i)\bat\s+([0-9A-Za-z\s'.-]+?)(?:\s+for|\s+on|\.|,)")
        .expect("valid at merchant regex")
});

static TO_SUCCESS_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(r"(?i)\bto\s+([0-9A-Za-z\s'.-]+?)\s+successful").expect("valid to success regex")
});

static FROM_SUCCESS_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(r"(?i)\bfrom\s+([0-9A-Za-z\s'.-]+?)(?:\s+successful|\s*[.,])")
        .expect("valid from success regex")
});

static TO_GENERAL_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(r"(?i)\bto\s+([0-9A-Za-z\s'.-]+?)(?:\s+on|\.|,|Fee|Balance|TrxID)")
        .expect("valid to general regex")
});

static RECHARGE_PHONE_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(r"(?i)\b(?:from|to|on)\s+(01[3-9][0-9X]{8}|1[3-9][0-9]{8})\b")
        .expect("valid recharge phone regex")
});

// Bill Payment specific: "Bill Payment of Tk 1,850.00 to DESCO successful"
static BILL_PAYMENT_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(r"(?i)\b(?:Bill\s+Pay(?:ment)?)\s+(?:of\s+)?(?:(?:Tk|৳|BDT)\s*)?[0-9][0-9,]*(?:\.[0-9]{1,2})?\s+to\s+([A-Za-z][A-Za-z\s.-]+?)(?:\s+successful|\s*[.,])")
        .expect("valid bill payment regex")
});

// CellFin/Upay transfer: "to bKash 017XXXXXXXX" or "to Bank 017XXXXXXXX"
static TRANSFER_TO_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(r"(?i)\bto\s+(?:bKash|Nagad|Rocket|Bank)\s+(01[3-9][0-9X]{8})\b")
        .expect("valid transfer to regex")
});

// Received from phone number: "received ... from 017XXXXXXXX"
static RECEIVED_FROM_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(r"(?i)\breceived\b.*?\bfrom\s+(01[3-9][0-9X]{8})\b")
        .expect("valid received from regex")
});

pub fn extract_party(text: &str) -> Option<String> {
    let lower = text.to_lowercase();

    // 1. Bill Payment biller extraction (highest priority for bill flows)
    if lower.contains("bill pay") {
        if let Some(cap) = BILL_PAYMENT_RE.captures(text) {
            if let Some(m) = cap.get(1) {
                if let Some(clean) = clean_party_candidate(m.as_str()) {
                    return Some(clean);
                }
            }
        }
    }

    // 2. Check for card merchant: "used at <Merchant> for ..."
    if let Some(cap) = AT_MERCHANT_RE.captures(text) {
        if let Some(m) = cap.get(1) {
            if let Some(clean) = clean_party_candidate(m.as_str()) {
                return Some(clean);
            }
        }
    }

    // 3. Telco recharge confirmation: "to recharge 50 TAKA from 1847662920 is successful" or "Recharge of Tk 100 on 017XXXXXXXX successful"
    if lower.contains("recharge") && lower.contains("successful") {
        if let Some(cap) = RECHARGE_PHONE_RE.captures(text) {
            if let Some(m) = cap.get(1) {
                let num = m.as_str().trim();
                let formatted = if num.starts_with('1') && num.len() == 10 {
                    format!("0{}", num)
                } else {
                    num.to_string()
                };
                return Some(formatted);
            }
        }
    }

    // 4. CellFin/Upay transfer: "to bKash 017XXXXXXXX"
    if let Some(cap) = TRANSFER_TO_RE.captures(text) {
        if let Some(m) = cap.get(1) {
            return Some(m.as_str().to_string());
        }
    }

    // 5. Received from phone: "received ... from 017XXXXXXXX"
    if lower.contains("received") {
        if let Some(cap) = RECEIVED_FROM_RE.captures(text) {
            if let Some(m) = cap.get(1) {
                return Some(m.as_str().to_string());
            }
        }
    }

    // 6. bKash / Nagad pattern: "to <party> successful"
    if let Some(cap) = TO_SUCCESS_RE.captures(text) {
        if let Some(m) = cap.get(1) {
            if let Some(clean) = clean_party_candidate(m.as_str()) {
                return Some(clean);
            }
        }
    }

    // 7. "from <party> successful" or "from <party>."
    if let Some(cap) = FROM_SUCCESS_RE.captures(text) {
        if let Some(m) = cap.get(1) {
            if let Some(clean) = clean_party_candidate(m.as_str()) {
                return Some(clean);
            }
        }
    }

    // 8. Fallback: "to <party> on" or "to <party>."
    if let Some(cap) = TO_GENERAL_RE.captures(text) {
        if let Some(m) = cap.get(1) {
            if let Some(clean) = clean_party_candidate(m.as_str()) {
                return Some(clean);
            }
        }
    }

    None
}

/// Cleans and validates candidate merchant strings, discarding verbs and sentence fragments.
fn clean_party_candidate(raw: &str) -> Option<String> {
    let mut candidate = raw.trim();

    // Strip trailing auxiliary words first (e.g. "'Software Shop Ltd' is" -> "'Software Shop Ltd'")
    let suffixes = [
        " is",
        " on",
        " was",
        " via",
        " with",
        " for",
        " at",
        " from",
        " successful",
        " completed",
    ];
    for suffix in suffixes {
        if candidate.to_lowercase().ends_with(suffix) {
            candidate = candidate[..candidate.len() - suffix.len()].trim();
        }
    }

    // Strip surrounding quotes if present (e.g. "'Software Shop Ltd'" -> "Software Shop Ltd")
    if (candidate.starts_with('\'') && candidate.ends_with('\''))
        || (candidate.starts_with('"') && candidate.ends_with('"'))
    {
        candidate = candidate[1..candidate.len() - 1].trim();
    }

    if candidate.is_empty() || candidate.len() < 2 || candidate.len() > 45 {
        return None;
    }

    let lower = candidate.to_lowercase();

    // Reject generic sentence fragments and account self-references
    if lower.starts_with("a/c")
        || lower.starts_with("your a/c")
        || lower.starts_with("your account")
        || lower.starts_with("your next recharge")
        || lower.starts_with("your card")
        || lower.starts_with("my account")
        || lower.starts_with("your balance")
        || lower.starts_with("biller a/c")
    {
        return None;
    }

    // Reject verb phrases captured erroneously by prepositions
    let invalid_leading_verbs = [
        "recharge",
        "purchase",
        "pay",
        "get",
        "settle",
        "take",
        "dial",
        "need",
        "deducted",
        "buy",
        "make",
        "enjoy",
        "subscribe",
        "activate",
        "check",
    ];
    for verb in invalid_leading_verbs {
        if lower.starts_with(verb)
            && (lower.len() == verb.len() || lower.chars().nth(verb.len()) == Some(' '))
        {
            return None;
        }
    }

    Some(candidate.to_string())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn cleans_merchant_with_trailing_is() {
        let text = "Payment to 'Software Shop Ltd' is successful. TrxID 123456";
        assert_eq!(extract_party(text), Some("Software Shop Ltd".to_string()));
    }

    #[test]
    fn extracts_card_merchant() {
        let text = "Your Card 1234 was used at SWAPNO for BDT 1,500.00 on 12/05/2026.";
        assert_eq!(extract_party(text), Some("SWAPNO".to_string()));
    }

    #[test]
    fn rejects_verb_phrases_from_next_recharge() {
        let text = "Tk 16 will be deducted from your next recharge. Dial *123*003#";
        assert_eq!(extract_party(text), None);
    }

    #[test]
    fn extracts_phone_number_from_recharge_confirmation() {
        let text = "Transaction number R250917.1531.34003b to recharge 50 TAKA from 1847662920 is successful";
        assert_eq!(extract_party(text), Some("01847662920".to_string()));
    }

    #[test]
    fn extracts_bill_payment_biller() {
        let text = "Bill Payment of Tk 1,850.00 to DESCO successful. Biller A/C 12345678. Fee Tk 0.00. Balance Tk 2,113.00.";
        assert_eq!(extract_party(text), Some("DESCO".to_string()));
    }

    #[test]
    fn extracts_transfer_to_bkash() {
        let text = "CellFin Transfer: Tk 2,500.00 debited from A/C *1234 to bKash 017XXXXXXXX. Fee: Tk 0.00.";
        assert_eq!(extract_party(text), Some("017XXXXXXXX".to_string()));
    }

    #[test]
    fn extracts_received_from_phone() {
        let text =
            "You have received Tk 1,500.00 from 017XXXXXXXX. Fee Tk 0.00. Balance Tk 10,408.00.";
        assert_eq!(extract_party(text), Some("017XXXXXXXX".to_string()));
    }

    #[test]
    fn cleans_trailing_successful() {
        let text = "Payment Tk 500 to Chaldal successful. TrxID AB12CD";
        assert_eq!(extract_party(text), Some("Chaldal".to_string()));
    }

    #[test]
    fn rejects_biller_account_as_party() {
        let text = "to Biller A/C 12345678 on 22/08/2026";
        assert_eq!(extract_party(text), None);
    }
}

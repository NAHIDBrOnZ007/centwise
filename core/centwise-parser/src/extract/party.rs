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
    Regex::new(r"(?i)\bto\s+([0-9A-Za-z\s'.-]+?)(?:\s+(?:Tk\.?|BDT)\b|\s+successful|\s*[.,])")
        .expect("valid to success regex")
});

static FROM_SUCCESS_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(r"(?i)\bfrom\s+([0-9A-Za-z\s'.-]+?)(?:\s+(?:Tk\.?|BDT)\b|\s+successful|\s*[.,])")
        .expect("valid from success regex")
});

static TO_GENERAL_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(r"(?i)\bto\s+([0-9A-Za-z\s'.-]+?)(?:\s+on|\.|,|Fee|Balance|TrxID|TxnId|TxnID|Date:)")
        .expect("valid to general regex")
});

static RECHARGE_PHONE_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(r"(?i)\b(?:from|to|on)\s+(01[3-9][0-9X]{8}|1[3-9][0-9]{8})\b")
        .expect("valid recharge phone regex")
});

// Bill Payment specific: "Bill Payment of Tk 1,850.00 to DESCO successful" or "Pay Bill Tk 1,420.00 to DESCO successful"
static BILL_PAYMENT_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(r"(?i)\b(?:Bill\s+Pay(?:ment)?|Pay\s+Bill)\s+(?:of\s+)?(?:(?:Tk|BDT)\s*)?[0-9][0-9,]*(?:\.[0-9]{1,2})?\s+to\s+([A-Za-z][A-Za-z\s.-]+?)(?:\s+successful|\s*[.,])")
        .expect("valid bill payment regex")
});

// Reservation payments: "Payment of Tk 420.00 is being reserved for Shohoj Limited-1-RM46212. Balance..."
static RESERVED_FOR_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(r"(?i)\b(?:is\s+being\s+)?reserved\s+for\s+([0-9A-Za-z\s'.-]+?)(?:\s+(?:Tk\.?|BDT)\b|\s+Balance|\s*[.,])")
        .expect("valid reserved for regex")
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

// EFT sender: "credited(EFT by: BIPOULHOSSAIN adn: ...)"
static EFT_BY_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(r"(?i)\bEFT\s+by:\s*([A-Za-z0-9\s.-]+?)(?:\s+adn:|\s+on|\)|,)")
        .expect("valid eft by regex")
});

// MFS explicit counterparty fields (Sender, Receiver, Uddokta, Mobile, From Bank, Biller/Org)
static MFS_SENDER_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(r"(?i)\bSender:\s*(01[3-9][0-9]{8}|[A-Za-z0-9\s.-]+?)(?:\s+Ref:|\s+TxnID:|\s+Fee:|\s+Balance:|\n|$)")
        .expect("valid mfs sender regex")
});

static MFS_RECEIVER_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(r"(?i)\bReceiver:\s*(01[3-9][0-9]{8}|[A-Za-z0-9\s.-]+?)(?:\s+Ref:|\s+TxnID:|\s+Fee:|\s+Balance:|\n|$)")
        .expect("valid mfs receiver regex")
});

static MFS_UDDOKTA_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(r"(?i)\bUddokta:\s*(01[3-9][0-9]{8}|[A-Za-z0-9\s.-]+?)(?:\s+TxnID:|\s+Fee:|\s+Balance:|\n|$)")
        .expect("valid mfs uddokta regex")
});

static MFS_MOBILE_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(r"(?i)\bMobile:\s*(01[3-9][0-9]{8})\b").expect("valid mfs mobile regex")
});

static MFS_FROM_BANK_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(r"(?i)\bFrom:\s*([A-Za-z0-9\s.-]+?)(?:\s+Amount:|\s+TxnID:|\n|$)")
        .expect("valid mfs from bank regex")
});

static MFS_BILLER_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(r"(?i)\b(?:Biller|Org):\s*([A-Za-z0-9\s.()-]+?)(?:\s+Amount:|\s+MMYYYY|\s+Fee:|\s+A/C:|\s+Ref:|\s+TrxID:|\n|$)")
        .expect("valid mfs biller regex")
});

static MFS_WALLET_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(r"(?i)\b(?:bKash\s+Wallet|Wallet|To):\s*([0-9A-Za-z]+)(?:\s+Amount:|\s+Trx|\n|$)")
        .expect("valid mfs wallet regex")
});

pub fn extract_party(text: &str) -> Option<String> {
    let lower = text.to_lowercase();

    // 0. Explicit MFS field headers (Sender, Receiver, Uddokta, Mobile, From, Biller/Org, Wallet/To)
    if let Some(cap) = MFS_WALLET_RE.captures(text) {
        if let Some(m) = cap.get(1) {
            let val = m.as_str().trim();
            if !val.is_empty() {
                if lower.contains("bkash wallet") && !val.to_lowercase().starts_with("bkash") {
                    return Some(format!("bKash {val}"));
                }
                if let Some(clean) = clean_party_candidate(val) {
                    return Some(clean);
                }
            }
        }
    }
    if let Some(cap) = MFS_BILLER_RE.captures(text) {
        if let Some(m) = cap.get(1) {
            if let Some(clean) = clean_party_candidate(m.as_str()) {
                return Some(clean);
            }
        }
    }
    if let Some(cap) = MFS_SENDER_RE.captures(text) {
        if let Some(m) = cap.get(1) {
            if let Some(clean) = clean_party_candidate(m.as_str()) {
                return Some(clean);
            }
        }
    }
    if let Some(cap) = MFS_RECEIVER_RE.captures(text) {
        if let Some(m) = cap.get(1) {
            if let Some(clean) = clean_party_candidate(m.as_str()) {
                return Some(clean);
            }
        }
    }
    if let Some(cap) = MFS_UDDOKTA_RE.captures(text) {
        if let Some(m) = cap.get(1) {
            if let Some(clean) = clean_party_candidate(m.as_str()) {
                return Some(clean);
            }
        }
    }
    if let Some(cap) = MFS_FROM_BANK_RE.captures(text) {
        if let Some(m) = cap.get(1) {
            if let Some(clean) = clean_party_candidate(m.as_str()) {
                return Some(clean);
            }
        }
    }
    if lower.contains("recharge") {
        if let Some(cap) = MFS_MOBILE_RE.captures(text) {
            if let Some(m) = cap.get(1) {
                if let Some(clean) = clean_party_candidate(m.as_str()) {
                    return Some(clean);
                }
            }
        }
    }

    // 0. EFT sender name (e.g. "EFT by: BIPOULHOSSAIN")
    if let Some(cap) = EFT_BY_RE.captures(text) {
        if let Some(m) = cap.get(1) {
            if let Some(clean) = clean_party_candidate(m.as_str()) {
                return Some(clean);
            }
        }
    }

    // 1. Bill Payment biller extraction (highest priority for bill flows)
    if lower.contains("bill pay") || lower.contains("pay bill") {
        if let Some(cap) = BILL_PAYMENT_RE.captures(text) {
            if let Some(m) = cap.get(1) {
                if let Some(clean) = clean_party_candidate(m.as_str()) {
                    return Some(clean);
                }
            }
        }
    }

    // 1b. Reservation payments: "Payment of Tk 420.00 is being reserved for Shohoj Limited-1-RM46212."
    if lower.contains("reserved for") {
        if let Some(cap) = RESERVED_FOR_RE.captures(text) {
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

    // Strip trailing periods (e.g. "Eastern Bank PLC." -> "Eastern Bank PLC")
    candidate = candidate.trim_end_matches('.').trim();

    // Strip trailing utility bill metadata (e.g. "DPDC Id 1011 Bill No 32205988" -> "DPDC")
    if let Some(pos) = candidate.find(" Id ") {
        candidate = candidate[..pos].trim();
    } else if let Some(pos) = candidate.find(" Bill No") {
        candidate = candidate[..pos].trim();
    }

    if candidate.is_empty() || candidate.len() < 2 || candidate.len() > 45 {
        return None;
    }

    let lower = candidate.to_lowercase();

    // Reject generic sentence fragments and account self-references
    if lower.starts_with("a/c")
        || lower.starts_with("your a/c")
        || lower.starts_with("your account")
        || lower.starts_with("your recharge")
        || lower.starts_with("your next recharge")
        || lower.starts_with("your card")
        || lower.starts_with("my account")
        || lower.starts_with("your balance")
        || lower.starts_with("biller a/c")
        || lower.starts_with("tk ")
        || lower.starts_with("tk.")
        || lower.starts_with("bdt ")
        || lower.starts_with("taka ")
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
        "receive",
        "open",
        "download",
        "contact",
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

    #[test]
    fn extracts_nagad_counterparties() {
        let money_received = "Money Received. Amount: Tk 20000.00 Sender: 01811552202 Ref: N/A TxnID: 75GFFXCN Balance: Tk 20005.47 01/06/2026 14:55";
        assert_eq!(
            extract_party(money_received),
            Some("01811552202".to_string())
        );

        let cash_in = "Cash In Received. Amount: Tk 1000.00 Uddokta: 01760526260 TxnID: 75AI91M3 Balance: 1017.16 02/05/2026 11:35";
        assert_eq!(extract_party(cash_in), Some("01760526260".to_string()));

        let send_money = "Send Money Successful. Amount: Tk 660.00 Receiver: 01321886176 Ref: tasfia TxnID: 73XL3F8B Fee: Tk 5.00 Balance: Tk 2400.00 16/05/2025 12:58";
        assert_eq!(extract_party(send_money), Some("01321886176".to_string()));

        let add_money = "Add Money from Bank is Successful. From: Eastern Bank PLC. Amount: Tk 2100.0 TxnID: 75NA7YH3 Balance: Tk 6100.62 09/07/2026 20:53";
        assert_eq!(
            extract_party(add_money),
            Some("Eastern Bank PLC".to_string())
        );

        let recharge = "Mobile Recharge Request Received. Amount: Tk 50.00 Mobile:01851096720 TxnID: 74E22XY4 Balance: Tk 4.16 23/09/2025 12:19";
        assert_eq!(extract_party(recharge), Some("01851096720".to_string()));
    }

    #[test]
    fn extracts_reservation_payment_party() {
        let text = "Payment of Tk 420.00 is being reserved for Shohoj Limited-1-RM46212. Balance Tk 184.94. TrxID DGP0P76EOO at 25/07/2026 11:24";
        assert_eq!(
            extract_party(text),
            Some("Shohoj Limited-1-RM46212".to_string())
        );
    }
}

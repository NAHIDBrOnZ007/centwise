//! Generic field-hunting parser engine for Centwise.
//!
//! Independently hunts for amount, fee, balance, transaction type, reference,
//! party/merchant, and dates without expecting a fixed template.

use centwise_categorization::{categorize_by_merchant, categorize_by_type_or_keywords};
use centwise_domain::TransactionType;
use centwise_normalization::{normalize_sms_text, parse_amount_minor};
use regex::Regex;
use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct ParsedTransaction {
    pub provider_id: String,
    pub transaction_type: TransactionType,
    pub amount_minor: i64,
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

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum RejectReason {
    NotATransaction,
    OtpOrSecurity,
    PromotionOrSpam,
    NoAmountFound,
    UnsupportedProvider,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum ParseOutcome {
    Parsed(Box<ParsedTransaction>),
    Rejected(RejectReason),
}

impl ParseOutcome {
    pub fn is_transaction(&self) -> bool {
        matches!(self, ParseOutcome::Parsed(_))
    }

    pub fn transaction(&self) -> Option<&ParsedTransaction> {
        match self {
            ParseOutcome::Parsed(t) => Some(t),
            _ => None,
        }
    }
}

/// Parses an SMS body with an optional sender hint into a structured transaction or rejection.
pub fn parse_sms(body: &str, sender_hint: Option<&str>) -> ParseOutcome {
    let normalized = normalize_sms_text(body);
    let trimmed = normalized.trim();

    // 1. Safety Filters: Check for OTP or Security alerts
    if is_otp_or_security_message(trimmed) {
        return ParseOutcome::Rejected(RejectReason::OtpOrSecurity);
    }

    // Check for obvious non-transaction or promotional spam without transaction markers
    if is_promotional_or_non_transaction(trimmed) {
        return ParseOutcome::Rejected(RejectReason::NotATransaction);
    }

    let provider = crate::providers::detect_provider(sender_hint, trimmed);

    // 2. Extract Reference / TrxID
    let reference = extract_reference(trimmed);

    // 3. Extract Fee
    let fee_minor = extract_fee(trimmed);

    // 4. Extract Balance After
    let balance_after_minor = extract_balance(trimmed);

    // 5. Extract Main Transaction Amount (Hunting standalone amount that is NOT fee and NOT balance)
    let amount_minor = match extract_main_amount(trimmed, fee_minor, balance_after_minor) {
        Some(amt) => amt,
        None => return ParseOutcome::Rejected(RejectReason::NoAmountFound),
    };

    // 6. Detect Transaction Type (Income vs. Expense)
    let transaction_type = match detect_transaction_type(trimmed) {
        Some(t) => t,
        None => return ParseOutcome::Rejected(RejectReason::NotATransaction),
    };

    // 7. Extract Party / Merchant
    let party = extract_party(trimmed);

    // 8. Extract Account last 4 or account hint
    let (account_last4, account_hint) = extract_account_info(trimmed);

    // 9. Extract Date / Time string
    let raw_date = extract_date_time(trimmed);

    // 10. Resolve Merchant and Category
    let (merchant, category_id) = resolve_categorization(
        party.as_deref(),
        trimmed,
        transaction_type == TransactionType::Income,
    );

    ParseOutcome::Parsed(Box::new(ParsedTransaction {
        provider_id: provider,
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
    }))
}

// ---------------------------------------------------------------------------
// Safety Filters
// ---------------------------------------------------------------------------

fn is_otp_or_security_message(text: &str) -> bool {
    let lower = text.to_lowercase();
    let has_otp_keyword = lower.contains("otp")
        || lower.contains("one time password")
        || lower.contains("verification code")
        || lower.contains("do not share")
        || lower.contains("security code");

    let has_transaction_action = lower.contains("successful")
        || lower.contains("credited")
        || lower.contains("debited")
        || lower.contains("cash out")
        || lower.contains("send money")
        || lower.contains("payment")
        || lower.contains("recharge");

    has_otp_keyword && !has_transaction_action
}

fn is_promotional_or_non_transaction(text: &str) -> bool {
    let lower = text.to_lowercase();

    if lower.contains("app update") || lower.contains("download now") {
        return true;
    }

    if lower.contains("trxid not applicable") && !lower.contains("successful") {
        return true;
    }

    false
}

// ---------------------------------------------------------------------------
// Field Hunting Functions
// ---------------------------------------------------------------------------

fn extract_reference(text: &str) -> Option<String> {
    let re = Regex::new(r"(?i)\b(?:TrxID|TxnID|Ref|Txn\s*ID)[:\s]+([A-Za-z0-9]+)").ok()?;
    for cap in re.captures_iter(text) {
        if let Some(m) = cap.get(1) {
            let val = m.as_str().trim();
            if val.eq_ignore_ascii_case("not") || val.eq_ignore_ascii_case("na") {
                continue;
            }
            return Some(val.to_string());
        }
    }
    None
}

fn extract_fee(text: &str) -> Option<i64> {
    let re = Regex::new(
        r"(?i)\b(?:Fee|Charge)(?:\s*(?:Tk|tk|TK|৳|BDT|[:]))?\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)",
    )
    .ok()?;
    if let Some(cap) = re.captures(text) {
        if let Some(amt_match) = cap.get(1) {
            return parse_amount_minor(amt_match.as_str());
        }
    }
    None
}

fn extract_balance(text: &str) -> Option<i64> {
    let re = Regex::new(
        r"(?i)\b(?:Available\s+Balance|Avail(?:\.|\s+)?Bal(?:ance)?|Balance)(?:\s*[:\-])?\s*(?:Tk|tk|TK|৳|BDT)?\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)",
    )
    .ok()?;
    if let Some(cap) = re.captures(text) {
        if let Some(amt_match) = cap.get(1) {
            return parse_amount_minor(amt_match.as_str());
        }
    }
    None
}

fn extract_main_amount(text: &str, fee: Option<i64>, balance: Option<i64>) -> Option<i64> {
    // 1. First priority: Look for amount directly attached to transaction verbs
    let verb_regex = Regex::new(
        r"(?i)(?:Cash\s+In|Cash\s+Out|Send\s+Money|Payment|Recharge|Withdrawal|credited\s+with|debited\s+with|EMI\s+of|Cashback(?:/Interest)?\s+of|received|credited\s+to|deposited|transferred)\s+(?:of\s+)?(?:Tk\s*)?([0-9][0-9,]*(?:\.[0-9]{1,2})?)"
    ).ok()?;

    if let Some(cap) = verb_regex.captures(text) {
        if let Some(amt_match) = cap.get(1) {
            if let Some(val) = parse_amount_minor(amt_match.as_str()) {
                if val > 0 {
                    return Some(val);
                }
            }
        }
    }

    // 2. Second priority: Look for amounts starting with Tk / ৳ / BDT
    let tk_regex =
        Regex::new(r"(?i)\b(?:Tk|tk|TK|৳|BDT)\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)").ok()?;
    for cap in tk_regex.captures_iter(text) {
        if let Some(m) = cap.get(1) {
            if let Some(val) = parse_amount_minor(m.as_str()) {
                // If this is the fee or balance, skip
                if fee == Some(val) && is_near_keyword(text, m.start(), "fee") {
                    continue;
                }
                if balance == Some(val) && is_near_keyword(text, m.start(), "balance") {
                    continue;
                }
                if val > 0 {
                    return Some(val);
                }
            }
        }
    }

    None
}

fn is_near_keyword(text: &str, pos: usize, keyword: &str) -> bool {
    let start = pos.saturating_sub(20);
    let slice = &text[start..pos];
    slice.to_lowercase().contains(keyword)
}

fn detect_transaction_type(text: &str) -> Option<TransactionType> {
    let lower = text.to_lowercase();

    // Income keywords
    if lower.contains("cash in")
        || lower.contains("received")
        || lower.contains("credited")
        || lower.contains("add money")
        || lower.contains("cashback")
        || lower.contains("interest")
        || lower.contains("salary")
        || lower.contains("deposit")
    {
        return Some(TransactionType::Income);
    }

    // Expense keywords
    if lower.contains("cash out")
        || lower.contains("send money")
        || lower.contains("payment")
        || lower.contains("debited")
        || lower.contains("withdrawal")
        || lower.contains("recharge")
        || lower.contains("emi")
        || lower.contains("deducted")
        || lower.contains("purchase")
        || lower.contains("bill pay")
    {
        return Some(TransactionType::Expense);
    }

    None
}

fn extract_party(text: &str) -> Option<String> {
    // bKash / Nagad pattern: "to <party> successful" or "from <party> successful"
    let re_to_success = Regex::new(r"(?i)\bto\s+([0-9A-Za-z\s'.-]+?)\s+successful").ok()?;
    if let Some(cap) = re_to_success.captures(text) {
        if let Some(m) = cap.get(1) {
            let s = m.as_str().trim();
            if !s.is_empty() && !s.eq_ignore_ascii_case("your a/c") {
                return Some(s.to_string());
            }
        }
    }

    let re_from_success =
        Regex::new(r"(?i)\bfrom\s+([0-9A-Za-z\s'.-]+?)(?:\s+successful|\s*[.,])").ok()?;
    if let Some(cap) = re_from_success.captures(text) {
        if let Some(m) = cap.get(1) {
            let s = m.as_str().trim();
            if !s.is_empty() && !s.to_lowercase().starts_with("a/c") {
                return Some(s.to_string());
            }
        }
    }

    // Fallback: "to <party>." or "to <party> on"
    let re_to_general =
        Regex::new(r"(?i)\bto\s+([0-9A-Za-z\s'.-]+?)(?:\s+on|\.|\,|Fee|Balance|TrxID)").ok()?;
    if let Some(cap) = re_to_general.captures(text) {
        if let Some(m) = cap.get(1) {
            let s = m.as_str().trim();
            if !s.is_empty()
                && !s.eq_ignore_ascii_case("your a/c")
                && !s.to_lowercase().starts_with("a/c")
            {
                return Some(s.to_string());
            }
        }
    }

    None
}

fn extract_account_info(text: &str) -> (Option<String>, Option<String>) {
    // Check for "A/C XXXX1234", "A/C: XXXX1234", or a masked wallet number.
    let re_bank_ac = Regex::new(r"(?i)\bA/C\s*(?:[:\s])?\s*([A-Za-z0-9]{4,15})\b").ok();
    if let Some(re) = re_bank_ac {
        if let Some(cap) = re.captures(text) {
            if let Some(m) = cap.get(1) {
                let account = m.as_str().trim();
                let last_four = account.strip_prefix("XXXX").unwrap_or(account);
                if last_four.len() == 4
                    && last_four
                        .chars()
                        .all(|character| character.is_ascii_digit())
                {
                    return (Some(last_four.to_string()), None);
                }
                // Could be a masked wallet number (e.g. Rocket 017XXXXXXXXX).
                return (None, Some(account.to_string()));
            }
        }
    }

    (None, None)
}

fn extract_date_time(text: &str) -> Option<String> {
    let re =
        Regex::new(r"([0-9]{2}/[0-9]{2}/[0-9]{4}(?:\s+[0-9]{2}:[0-9]{2}(?::[0-9]{2})?)?)").ok()?;
    if let Some(cap) = re.captures(text) {
        if let Some(m) = cap.get(1) {
            return Some(m.as_str().trim().to_string());
        }
    }
    None
}

fn resolve_categorization(
    party: Option<&str>,
    body: &str,
    is_income: bool,
) -> (Option<String>, Option<String>) {
    // 1. If party has a known merchant
    if let Some(p) = party {
        if let Some(cat_res) = categorize_by_merchant(p) {
            return (cat_res.matched_merchant, Some(cat_res.category_id));
        }
    }

    // 2. Scan entire body for known merchants
    if let Some(cat_res) = categorize_by_merchant(body) {
        return (cat_res.matched_merchant, Some(cat_res.category_id));
    }

    // 3. Fallback to keyword / type based categorization
    let category = categorize_by_type_or_keywords(body, is_income);
    (None, category)
}

#[cfg(test)]
mod tests {
    use super::{
        extract_account_info, extract_balance, extract_fee, extract_party, parse_sms, ParseOutcome,
    };
    use centwise_normalization::normalize_sms_text;

    #[test]
    fn extracts_zero_fee_with_and_without_currency_marker() {
        assert_eq!(extract_fee("Fee Tk 0.00"), Some(0));
        assert_eq!(extract_fee("Fee 0.00"), Some(0));
    }

    #[test]
    fn extracts_available_balance_with_currency_marker() {
        assert_eq!(
            extract_balance("Available Balance: Tk 25,450.00"),
            Some(2_545_000)
        );
    }

    #[test]
    fn parse_sms_keeps_zero_fee_and_available_balance() {
        let body = "Cash In Tk 3,000.00 successful. A/C 017XXXXXXXXX. Fee 0.00. Balance 8,500.00. TrxID 123456789012";
        let normalized = normalize_sms_text(body);
        assert_eq!(normalized, body);
        assert_eq!(extract_fee(&normalized), Some(0));
        assert_eq!(extract_balance(&normalized), Some(850_000));
        let outcome = parse_sms(body, Some("ROCKET"));

        let ParseOutcome::Parsed(transaction) = outcome else {
            panic!("expected a parsed transaction");
        };

        assert_eq!(transaction.fee_minor, Some(0));
        assert_eq!(transaction.balance_after_minor, Some(850_000));
    }

    #[test]
    fn extracts_party_when_phone_number_is_followed_by_punctuation() {
        assert_eq!(
            extract_party("You have received Tk 1,500.00 from 017XXXXXXXX. Fee Tk 0.00."),
            Some("017XXXXXXXX".to_string())
        );
    }

    #[test]
    fn extracts_masked_wallet_account_as_account_hint() {
        assert_eq!(
            extract_account_info("A/C 017XXXXXXXXX"),
            (None, Some("017XXXXXXXXX".to_string()))
        );
    }

    #[test]
    fn parses_received_amount_before_sentence_punctuation() {
        let outcome = parse_sms(
            "You have received Tk 2,500.00. Balance 7,477.00. TrxID 345678901234",
            Some("ROCKET"),
        );

        let ParseOutcome::Parsed(transaction) = outcome else {
            panic!("expected a parsed transaction");
        };

        assert_eq!(transaction.amount_minor, 250_000);
        assert_eq!(transaction.balance_after_minor, Some(747_700));
    }
}

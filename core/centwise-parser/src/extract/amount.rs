//! Field extraction: Amounts, Fees, and Balances.
//!
//! Supports diverse Bangladeshi banking formats:
//! - Standard currency markers: `Tk`, `TK`, `BDT`.
//! - Postfix currency markers: `taka`, `TAKA`.
//! - Bank accounts debited/credited without currency markers (e.g. "debited by 5,000.00").
//! - Card transactions (e.g. "Card 1234 used at SWAPNO for BDT 1,500.00").
//! - Telco recharge confirmations (e.g. "to recharge 50 TAKA").
//! - Safe rejection of future conditional repayment amounts ("will be deducted from your next recharge").
//! - Safe rejection of total-outstanding meta-amounts ("Your total outstanding is Tk 17.78").

use centwise_normalization::parse_amount_minor;
use regex::Regex;
use std::sync::LazyLock;

static FEE_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(r"(?i)\b(?:Fee|Charge|Service\s+fee)(?:\s*(?:of|is|[:]))?(?:\s*(?:Tk\.?|BDT))?\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)")
        .expect("valid fee regex")
});

static BALANCE_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(
        r"(?i)(?:Available\s+Balance|Avail(?:able)?\.?\s*Bal(?:ance)?|Avl\.?\s*Bal(?:ance)?|Closing\s+Balance|Ledger\s+Bal(?:ance)?|\bBal(?:ance)?\b|Current\s+Balance)(?:\s*[:\-])?\s*(?:Tk\.?|BDT)?\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)(?:\s*(?:Tk\.?|BDT))?",
    )
    .expect("valid balance regex")
});

// Bank verbs: debited with/by/for, credited with/by/for (even without currency symbol)
static BANK_DEBIT_CREDIT_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(
        r"(?i)\b(?:debited|credited)\s+(?:with|by|for)\s+(?:(?:Tk\.?|BDT)\s*)?([0-9][0-9,]*(?:\.[0-9]{1,2})?)",
    )
    .expect("valid bank debit credit regex")
});

// Card purchase: "used at ... for BDT 500" or "used for BDT 500"
static CARD_USAGE_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(
        r"(?i)\bused(?:\s+at\s+[^.]+?)?\s+for\s+(?:(?:Tk\.?|BDT)\s*)?([0-9][0-9,]*(?:\.[0-9]{1,2})?)",
    )
    .expect("valid card usage regex")
});

// Primary transaction verbs: Cash in, Cash out, Payment, Recharge, etc.
static VERB_AMOUNT_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(
        r"(?i)(?:Cash\s+In|Cash\s+Out|Cash\s+Deposit|Send\s+Money|Payment|Bill\s+Pay(?:ment)?|Recharge|Withdrawal|Withdrawn|received|deposited|transferred|Fund\s+Transfer|Remittance|Auto\s+Debit|Loan\s+Repayment|spent|charged|(?:DR|CR)\.?\s+transaction|added\s+to\s+your\s+account|EMI\s+of|Cashback(?:/Interest)?\s+of|Excise\s+Duty|Annual\s+(?:Card\s+)?Fee|SMS\s+Alert\s+Fee|Maintenance\s+Fee|Add\s+Money)\s+(?:of\s+)?(?:(?:Tk\.?|BDT)\s*)?([0-9][0-9,]*(?:\.[0-9]{1,2})?)(?:\s*(?:Tk\.?|BDT))?",
    )
    .expect("valid verb amount regex")
});

// Telco "recharge <amount> TAKA" pattern
static RECHARGE_TAKA_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(r"(?i)\brecharge\s+([0-9][0-9,]*(?:\.[0-9]{1,2})?)\s*(?:TAKA|Tk\.?|BDT)")
        .expect("valid recharge taka regex")
});

// Postfix currency: "5000 taka" (no prefix marker)
static TAKA_SUFFIX_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(r"(?i)([0-9][0-9,]*(?:\.[0-9]{1,2})?)\s*(?:taka|Tk\.?|BDT)")
        .expect("valid currency suffix regex")
});

// Generic currency amounts (Tk 500, BDT 500)
static CURRENCY_AMOUNT_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(r"(?i)(?:Tk\.?|BDT)\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)")
        .expect("valid currency amount regex")
});

static INFORMATIONAL_AMOUNT_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(r"(?i)\b(?:available\s+(?:credit\s+)?limit|(?:minimum\s+)?amount\s+due|due\s+amount|total\s+outstanding)\s*(?:is\s*)?[:\-]?\s*(?:Tk\.?|BDT)?\s*$")
        .expect("valid informational amount regex")
});

static DATE_OR_PHONE_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(r"^(?:01[0-9]{9}\b|[0-9]{1,4}[/\-][0-9])").expect("valid date or phone regex")
});

pub fn extract_fee(text: &str) -> Option<i64> {
    if let Some(cap) = FEE_RE.captures(text) {
        if let Some(m) = cap.get(1) {
            return parse_amount_minor(m.as_str());
        }
    }
    None
}

pub fn extract_balance(text: &str) -> Option<i64> {
    if let Some(cap) = BALANCE_RE.captures(text) {
        if let Some(m) = cap.get(1) {
            return parse_amount_minor(m.as_str());
        }
    }
    None
}

pub fn extract_main_amount(text: &str, fee: Option<i64>, balance: Option<i64>) -> Option<i64> {
    // 1. Bank debit/credit explicit phrasing (e.g. "debited by 5,000.00", "credited with BDT 2,500.00")
    if let Some(cap) = BANK_DEBIT_CREDIT_RE.captures(text) {
        if let Some(m) = cap.get(1) {
            if let Some(val) = parse_amount_minor(m.as_str()) {
                if val > 0 && !is_fee_or_balance(val, m.start(), text, fee, balance) {
                    return Some(val);
                }
            }
        }
    }

    // 2. Card usage phrasing (e.g. "used at SWAPNO for BDT 1,500.00")
    if let Some(cap) = CARD_USAGE_RE.captures(text) {
        if let Some(m) = cap.get(1) {
            if let Some(val) = parse_amount_minor(m.as_str()) {
                if val > 0 && !is_fee_or_balance(val, m.start(), text, fee, balance) {
                    return Some(val);
                }
            }
        }
    }

    // 3. Telco "recharge 50 TAKA"
    if let Some(cap) = RECHARGE_TAKA_RE.captures(text) {
        if let Some(m) = cap.get(1) {
            if let Some(val) = parse_amount_minor(m.as_str()) {
                if val > 0 && !is_fee_or_balance(val, m.start(), text, fee, balance) {
                    return Some(val);
                }
            }
        }
    }

    // 4. Primary transaction verbs (Cash in, Cash out, Send money, Payment, Bill pay, Fund Transfer, etc.)
    if let Some(cap) = VERB_AMOUNT_RE.captures(text) {
        if let Some(m) = cap.get(1) {
            if let Some(val) = parse_amount_minor(m.as_str()) {
                if val > 0 && !is_fee_or_balance(val, m.start(), text, fee, balance) {
                    return Some(val);
                }
            }
        }
    }

    // 5. Postfix TAKA pattern (e.g. "5000 taka")
    for cap in TAKA_SUFFIX_RE.captures_iter(text) {
        if let Some(m) = cap.get(1) {
            let start = m.start();
            if is_future_deduction_clause(text, start) || is_outstanding_clause(text, start) {
                continue;
            }
            if let Some(val) = parse_amount_minor(m.as_str()) {
                if is_fee_or_balance(val, start, text, fee, balance) {
                    continue;
                }
                if val > 0 {
                    return Some(val);
                }
            }
        }
    }

    // 6. Standalone currency amounts (Tk 500, BDT 500), skipping future conditional repayment promises
    for cap in CURRENCY_AMOUNT_RE.captures_iter(text) {
        if let Some(m) = cap.get(1) {
            let start = m.start();
            if is_future_deduction_clause(text, start) || is_outstanding_clause(text, start) {
                continue;
            }
            if let Some(val) = parse_amount_minor(m.as_str()) {
                if is_fee_or_balance(val, start, text, fee, balance) {
                    continue;
                }
                if val > 0 {
                    return Some(val);
                }
            }
        }
    }

    // 7. Fallback for fee-debit transactions (e.g. "SMS Alert Fee of BDT 230.00 has been debited from your A/C")
    if let Some(f) = fee {
        let lower = text.to_lowercase();
        if lower.contains("debited")
            || lower.contains("fee charged")
            || lower.contains("charge debited")
        {
            return Some(f);
        }
    }

    None
}

fn is_fee_or_balance(
    val: i64,
    pos: usize,
    text: &str,
    fee: Option<i64>,
    balance: Option<i64>,
) -> bool {
    if INFORMATIONAL_AMOUNT_RE.is_match(&text[..pos]) || DATE_OR_PHONE_RE.is_match(&text[pos..]) {
        return true;
    }
    if fee == Some(val) && is_near_keyword(text, pos, "fee") {
        return true;
    }
    if fee == Some(val) && is_near_keyword(text, pos, "charge") {
        return true;
    }
    if balance == Some(val)
        && (is_near_keyword(text, pos, "balance") || is_near_keyword(text, pos, "bal"))
    {
        return true;
    }
    false
}

fn is_near_keyword(text: &str, pos: usize, keyword: &str) -> bool {
    let mut start = pos.saturating_sub(25);
    while start < pos && !text.is_char_boundary(start) {
        start += 1;
    }
    let slice = &text[start..pos];
    slice.to_lowercase().contains(keyword)
}

/// Checks if an amount token is inside a future conditional clause
/// like "will be deducted from your next recharge" or "pay on your next recharge".
fn is_future_deduction_clause(text: &str, pos: usize) -> bool {
    // Only look within the same sentence (up to 50 chars or a period/semicolon).
    let mut end = (pos + 50).min(text.len());
    while end > pos && !text.is_char_boundary(end) {
        end -= 1;
    }
    let after_raw = &text[pos..end];
    // Stop at sentence boundaries
    let after = if let Some(dot_pos) = after_raw.find('.') {
        &after_raw[..dot_pos]
    } else {
        after_raw
    };
    let after_lower = after.to_lowercase();
    after_lower.contains("will be deducted")
        || after_lower.contains("pay on your next recharge")
        || after_lower.contains("will be charged")
        || after_lower.contains("to settle your")
}

/// Checks if an amount token is part of a "total outstanding" informational clause.
fn is_outstanding_clause(text: &str, pos: usize) -> bool {
    let mut start = pos.saturating_sub(40);
    while start < pos && !text.is_char_boundary(start) {
        start += 1;
    }
    let before = &text[start..pos].to_lowercase();
    before.contains("total outstanding")
        || before.contains("outstanding is")
        || before.contains("due amount")
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn parses_bank_debit_without_tk() {
        let text = "Dear Customer, your A/C XXXX1234 has been debited by 5,000.00 on 22/08/2026. Available Balance: 25,450.00.";
        let fee = extract_fee(text);
        let bal = extract_balance(text);
        let amount = extract_main_amount(text, fee, bal);
        assert_eq!(amount, Some(500_000));
        assert_eq!(bal, Some(2_545_000));
    }

    #[test]
    fn parses_bank_card_transaction() {
        let text = "Your Card 1234 was used at SWAPNO for BDT 1,500.00 on 12/05/2026.";
        let amount = extract_main_amount(text, None, None);
        assert_eq!(amount, Some(150_000));
    }

    #[test]
    fn parses_telco_recharge_taka_format() {
        let text = "Transaction number R250917.1531.34003b to recharge 50 TAKA from 1847662920 is successful";
        let amount = extract_main_amount(text, None, None);
        assert_eq!(amount, Some(5_000));
    }

    #[test]
    fn parses_added_emergency_loan_amount_ignoring_future_deduction() {
        let text = "Tk15 has been added to your account. Tk15 + Service fee of Tk2.78 will be deducted from your next recharge.";
        let fee = extract_fee(text);
        assert_eq!(fee, Some(278));
        let amount = extract_main_amount(text, fee, None);
        assert_eq!(amount, Some(1_500));
    }

    #[test]
    fn ignores_total_outstanding_amount() {
        let text = "Tk15 has been added to your account. Your total outstanding is Tk 17.78. For Details dial *123*600#";
        let amount = extract_main_amount(text, None, None);
        assert_eq!(amount, Some(1_500));
    }

    #[test]
    fn parses_sms_alert_fee_as_amount() {
        let text = "SMS Alert Fee of BDT 230.00 has been debited from your A/C XXXX1234 on 30/06/2026. Available Balance: BDT 18,200.00.";
        let fee = extract_fee(text);
        let bal = extract_balance(text);
        let amount = extract_main_amount(text, fee, bal);
        assert_eq!(amount, Some(23_000));
        assert_eq!(bal, Some(1_820_000));
    }

    #[test]
    fn parses_excise_duty_as_amount() {
        let text = "Excise Duty BDT 500.00 has been debited from your A/C XXXX5678. Balance: BDT 45,000.00";
        let fee = extract_fee(text);
        let bal = extract_balance(text);
        let amount = extract_main_amount(text, fee, bal);
        assert_eq!(amount, Some(50_000));
    }

    #[test]
    fn parses_current_balance_keyword() {
        let text = "Recharge of Tk 100.00 on 017XXXXXXXX successful on 22/08/2026 14:30. Current Balance: Tk 102.50.";
        let bal = extract_balance(text);
        assert_eq!(bal, Some(10_250));
    }
}

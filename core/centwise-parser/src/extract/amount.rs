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
    Regex::new(r"(?i)\b(?:Trx\s+Fee|Fee|Charge|Service\s+fee)(?:\s*(?:of|is|[:]))?(?:\s*(?:Tk\.?|BDT))?\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)")
        .expect("valid fee regex")
});

// Key-value structured amounts (e.g. "Amount: 1500.00", "AMOUNT: 4010.00", "Amount: Tk 1,488.00")
static KEY_VALUE_AMOUNT_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(r"(?i)\b(?:Amount|Total\s+Amount)\s*[:\-]\s*(?:(?:Tk\.?|BDT)\s*)?([0-9][0-9,]*(?:\.[0-9]{1,2})?)")
        .expect("valid key value amount regex")
});

static BALANCE_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(
        r"(?i)(?:Available\s+Balance|Avail(?:able)?\.?\s*Bal(?:ance)?|Avl\.?\s*Bal(?:ance)?|Closing\s+Balance|\bC/B\b|Ledger\s+Bal(?:ance)?|New\s+(?:main\s+)?Bal(?:ance)?|Main\s+Bal(?:ance)?|\bBal(?:ance)?\b|Current\s+Balance)(?:\s*(?:is\s*)?[:\-]|\s+is)?\s*(?:Tk\.?|BDT)?\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)(?:\s*(?:Tk\.?|BDT))?",
    )
    .expect("valid balance regex")
});

static BALANCE_POSTFIX_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(
        r"(?i)(?:(?:Tk\.?|BDT)\s*-?\s*)?([0-9][0-9,]*(?:\.[0-9]{1,2})?)\s*(?:Tk\.?|BDT)?\s*\b(?:Available\s+Balance|Balance|Bal)\b",
    )
    .expect("valid postfix balance regex")
});

// Postfix transaction verbs: "Tk. 500 Withdrawal", "BDT 1,000 Deposit", "Tk 500 Purchased"
static AMOUNT_POSTFIX_VERB_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(
        r"(?i)\b(?:(?:Tk\.?|BDT)[ \t]*-?[ \t]*)?([0-9][0-9,]*(?:\.[0-9]{1,2})?)[ \t]*(?:Tk\.?|BDT)?[ \t]+(?:Withdrawal|Withdrawn|Deposit|Deposited|Purchased)\b",
    )
    .expect("valid amount postfix verb regex")
});

// Bank verbs: debited with/by/for, credited with/by/for (even without currency symbol)
static BANK_DEBIT_CREDIT_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(
        r"(?i)\b(?:debited|credited)(?:\s*\([^)]*\))?\s+(?:with|by|for)\s+(?:(?:Tk\.?|BDT)\s*-?\s*)?([0-9][0-9,]*(?:\.[0-9]{1,2})?)",
    )
    .expect("valid bank debit credit regex")
});

// Card purchase: "used at ... for BDT 500" or "used for BDT 500"
static CARD_USAGE_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(
        r"(?i)\bused(?:\s+at\s+[^.]+?)?\s+for\s+(?:(?:Tk\.?|BDT)\s*-?\s*)?([0-9][0-9,]*(?:\.[0-9]{1,2})?)",
    )
    .expect("valid card usage regex")
});

// Primary transaction verbs: Cash in, Cash out, Payment, Recharge, etc.
static VERB_AMOUNT_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(
        r"(?i)(?:Cash\s+In|Cash\s+Out|Cash\s+Deposit|Deposit\s+Amount|Send\s+Money|sent|Payment|Bill\s+Pay(?:ment)?|Pay\s+Bill|Recharge|Withdrawal|Withdrawn|received|deposited|transferred|Transfer\s+Money|Fund\s+Transfer|Remittance|Auto\s+Debit|Loan\s+Repayment|spent|charged|(?:DR|CR)\.?\s+transaction|Txn|added\s+to\s+your\s+account|EMI\s+of|Cashback(?:/Interest)?\s+of|Excise\s+Duty|Annual\s+(?:Card\s+)?Fee|SMS\s+Alert\s+Fee|Maintenance\s+Fee|Add\s+Money|recovered\s+for\s+emergency\s+loan)[ \t]*[:\-]?[ \t]*(?:of[ \t]+)?(?:(?:Tk\.?|BDT)[ \t]*-?[ \t]*)?([0-9][0-9,]*(?:\.[0-9]{1,2})?)(?:[ \t]*(?:Tk\.?|BDT))?",
    )
    .expect("valid verb amount regex")
});

// Telco "recharge <amount> TAKA" pattern
static RECHARGE_TAKA_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(r"(?i)\brecharge\s+([0-9][0-9,]*(?:\.[0-9]{1,2})?)\s*(?:TAKA|Tk\.?|BDT)")
        .expect("valid recharge taka regex")
});

// Postfix currency: "5000 taka" (strictly taka/TAKA with word boundary, never prefix Tk/BDT)
static TAKA_SUFFIX_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(r"(?i)\b([0-9]+(?:,[0-9]+)*(?:\.[0-9]{1,2})?)\s*(?:taka|TAKA)\b")
        .expect("valid currency suffix regex")
});

// Generic currency amounts (Tk 500, BDT 500, Tk-500)
static CURRENCY_AMOUNT_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(r"(?i)\b(?:Tk\.?|BDT)\s*-?\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)")
        .expect("valid currency amount regex")
});

static INFORMATIONAL_AMOUNT_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(r"(?i)\b(?:available\s+(?:credit\s+)?limit|(?:minimum\s+)?amount\s+due|due\s+amount|total\s+outstanding|sum\s+insured)\s*(?:is\s*)?[:\-]?\s*(?:Tk\.?|BDT)?\s*$")
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
    if let Some(cap) = BALANCE_POSTFIX_RE.captures(text) {
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
                if val > 0 && !is_fee_or_balance(val, m.start(), m.end(), text, fee, balance) {
                    return Some(val);
                }
            }
        }
    }

    // 2. Card usage phrasing (e.g. "used at SWAPNO for BDT 1,500.00")
    if let Some(cap) = CARD_USAGE_RE.captures(text) {
        if let Some(m) = cap.get(1) {
            if let Some(val) = parse_amount_minor(m.as_str()) {
                if val > 0 && !is_fee_or_balance(val, m.start(), m.end(), text, fee, balance) {
                    return Some(val);
                }
            }
        }
    }

    // 3. Telco "recharge 50 TAKA"
    if let Some(cap) = RECHARGE_TAKA_RE.captures(text) {
        if let Some(m) = cap.get(1) {
            if let Some(val) = parse_amount_minor(m.as_str()) {
                if val > 0 && !is_fee_or_balance(val, m.start(), m.end(), text, fee, balance) {
                    return Some(val);
                }
            }
        }
    }

    // 4. Primary transaction verbs (Cash in, Cash out, Send money, Payment, Bill pay, Fund Transfer, etc.)
    if let Some(cap) = VERB_AMOUNT_RE.captures(text) {
        if let Some(m) = cap.get(1) {
            if let Some(val) = parse_amount_minor(m.as_str()) {
                if val > 0 && !is_fee_or_balance(val, m.start(), m.end(), text, fee, balance) {
                    return Some(val);
                }
            }
        }
    }

    // 4b. Postfix transaction verbs (e.g. "Tk. 690 Withdrawal", "Tk. 20,000 Deposit", "Tk 500 Purchased")
    for cap in AMOUNT_POSTFIX_VERB_RE.captures_iter(text) {
        if let Some(m) = cap.get(1) {
            if let Some(val) = parse_amount_minor(m.as_str()) {
                if val > 0 && !is_fee_or_balance(val, m.start(), m.end(), text, fee, balance) {
                    return Some(val);
                }
            }
        }
    }

    // 4c. Key-value structured amounts (e.g. "Amount: 1500.00", "AMOUNT: 4010.00", "Amount: Tk 1,488.00")
    if let Some(cap) = KEY_VALUE_AMOUNT_RE.captures(text) {
        if let Some(m) = cap.get(1) {
            if let Some(val) = parse_amount_minor(m.as_str()) {
                if val > 0 && !is_fee_or_balance(val, m.start(), m.end(), text, fee, balance) {
                    return Some(val);
                }
            }
        }
    }

    // 5. Postfix TAKA pattern (e.g. "5000 taka")
    for cap in TAKA_SUFFIX_RE.captures_iter(text) {
        if let Some(m) = cap.get(1) {
            let start = m.start();
            let end = m.end();
            if is_future_deduction_clause(text, start) || is_outstanding_clause(text, start) {
                continue;
            }
            if let Some(val) = parse_amount_minor(m.as_str()) {
                if is_fee_or_balance(val, start, end, text, fee, balance) {
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
            let end = m.end();
            if is_future_deduction_clause(text, start) || is_outstanding_clause(text, start) {
                continue;
            }
            if let Some(val) = parse_amount_minor(m.as_str()) {
                if is_fee_or_balance(val, start, end, text, fee, balance) {
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
    start_pos: usize,
    end_pos: usize,
    text: &str,
    fee: Option<i64>,
    balance: Option<i64>,
) -> bool {
    let after = &text[start_pos..];
    if after.starts_with(')')
        || after.starts_with(". ")
        || after.starts_with("P/")
        || after.starts_with("p/")
        || after.starts_with("GB")
        || after.starts_with("MB")
        || after.starts_with("Min")
        || after.starts_with("min")
    {
        return true;
    }

    if INFORMATIONAL_AMOUNT_RE.is_match(&text[..start_pos])
        || DATE_OR_PHONE_RE.is_match(&text[start_pos..])
        || is_account_or_identifier(text, start_pos)
    {
        return true;
    }
    if fee == Some(val) && is_near_keyword(text, start_pos, "fee") {
        return true;
    }
    if fee == Some(val) && is_near_keyword(text, start_pos, "charge") {
        return true;
    }
    if balance == Some(val) && is_adjacent_balance(text, start_pos, end_pos) {
        return true;
    }
    false
}

fn is_account_or_identifier(text: &str, pos: usize) -> bool {
    let mut start = pos.saturating_sub(25);
    while start < pos && !text.is_char_boundary(start) {
        start += 1;
    }
    let before = text[start..pos].to_lowercase();
    let trimmed = before.trim_end();
    trimmed.ends_with("acc:")
        || trimmed.ends_with("acc :")
        || trimmed.ends_with("acc.")
        || trimmed.ends_with("acc")
        || trimmed.ends_with("a/c:")
        || trimmed.ends_with("a/c :")
        || trimmed.ends_with("a/c#")
        || trimmed.ends_with("a/c")
        || trimmed.ends_with("acct:")
        || trimmed.ends_with("acct.")
        || trimmed.ends_with("acct")
        || trimmed.ends_with("account:")
        || trimmed.ends_with("account :")
        || trimmed.ends_with("account")
        || trimmed.ends_with("trxid:")
        || trimmed.ends_with("trxid :")
        || trimmed.ends_with("trx id:")
        || trimmed.ends_with("txnid:")
        || trimmed.ends_with("txnid :")
        || trimmed.ends_with("txn id:")
        || trimmed.ends_with("loan a/c:")
        || trimmed.ends_with("loan a/c :")
        || trimmed.ends_with("loan a/c")
        || trimmed.ends_with("ref:")
        || trimmed.ends_with("ref :")
}

fn is_adjacent_balance(text: &str, pos: usize, end_pos: usize) -> bool {
    // 1. Check if preceded by balance keyword: e.g. "Balance: Tk 1,172"
    let mut start = pos.saturating_sub(25);
    while start < pos && !text.is_char_boundary(start) {
        start += 1;
    }
    let before = text[start..pos].to_lowercase();
    if before.contains("balance") || before.contains("bal") || before.contains("c/b") {
        return true;
    }

    // 2. Check if immediately followed by balance keyword: e.g. "1,172 Balance" or "1,172 Tk Balance"
    let after_slice = &text[end_pos..];
    let after_words: Vec<&str> = after_slice.split_whitespace().take(2).collect();
    for word in after_words {
        let clean = word
            .trim_matches(|c: char| !c.is_alphabetic())
            .to_lowercase();
        if clean == "balance" || clean == "bal" {
            return true;
        }
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

    #[test]
    fn parses_city_bank_postfix_withdrawal_and_balance() {
        let text = "05-Feb-2024\nTk. 690 Withdrawal\nTk. 1,172 Balance\nA/C: 2303***7001";
        let bal = extract_balance(text);
        assert_eq!(bal, Some(117_200));
        let amount = extract_main_amount(text, None, bal);
        assert_eq!(amount, Some(69_000));
    }
}

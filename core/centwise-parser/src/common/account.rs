//! Provider-independent bank-account and card identifier extraction.

use regex::Regex;
use std::sync::LazyLock;

static BANK_ACCOUNT_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(r"(?i)\b(?:A/C|ACCT\.?|ACCOUNT|ACC\.?|AC)\s*(?:number|no\.?)?\s*(?:ending(?:\s+in)?\s*)?(?:[#:\s])?\s*([A-Za-z0-9*][A-Za-z0-9*.-]{3,24})\b")
        .expect("valid account regex")
});
static CARD_NUMBER_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(r"(?i)\bCard\s+(?:ending\s*(?:in\s*)?|no\.?\s*|[*])?([0-9*X]{4,16})\b")
        .expect("valid card regex")
});

pub fn extract_account_info(text: &str) -> (Option<String>, Option<String>) {
    if let Some(capture) = CARD_NUMBER_RE.captures(text) {
        if let Some(value) = capture.get(1) {
            let digits: String = value
                .as_str()
                .chars()
                .filter(|character| character.is_ascii_digit())
                .collect();
            if digits.len() >= 4 {
                let last4 = digits[digits.len() - 4..].to_string();
                return (Some(last4.clone()), Some(format!("Card *{last4}")));
            }
        }
    }

    for capture in BANK_ACCOUNT_RE.captures_iter(text) {
        if let Some(full_match) = capture.get(0) {
            if is_preceded_by_biller(text, full_match.start()) {
                continue;
            }
        }
        if let Some(value) = capture.get(1) {
            let account = value.as_str().trim();
            let digits: String = account
                .chars()
                .filter(|character| character.is_ascii_digit())
                .collect();
            if digits.is_empty() {
                continue;
            }
            if digits.len() == 4 {
                return (Some(digits), None);
            }
            return (None, Some(account.to_string()));
        }
    }

    (None, None)
}

fn is_preceded_by_biller(text: &str, match_start: usize) -> bool {
    let mut start = match_start.saturating_sub(10);
    while start < match_start && !text.is_char_boundary(start) {
        start += 1;
    }
    text[start..match_start]
        .trim_end()
        .to_ascii_lowercase()
        .ends_with("biller")
}

//! Field extraction: Bank Accounts, Card Numbers, and Wallet Hints.

use regex::Regex;
use std::sync::LazyLock;

static BANK_ACCOUNT_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(r"(?i)\b(?:A/C|ACCT\.?|ACCOUNT)\s*(?:ending(?:\s+in)?\s*)?(?:[:\s])?\s*([A-Za-z0-9*]{4,20})\b").expect("valid account regex")
});

static CARD_NUMBER_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(r"(?i)\bCard\s+(?:ending\s*(?:in\s*)?|no\.?\s*|[*])?([0-9*X]{4,16})\b")
        .expect("valid card regex")
});

pub fn extract_account_info(text: &str) -> (Option<String>, Option<String>) {
    // 1. Check for card pattern (e.g. "Card 1234", "Card ending 4321", "Card *5678", "Card no. 1234")
    if let Some(cap) = CARD_NUMBER_RE.captures(text) {
        if let Some(m) = cap.get(1) {
            let card_str = m.as_str().trim();
            let digits: String = card_str.chars().filter(|c| c.is_ascii_digit()).collect();
            if digits.len() >= 4 {
                let last4 = digits[digits.len() - 4..].to_string();
                return (Some(last4.clone()), Some(format!("Card *{}", last4)));
            }
        }
    }

    // 2. Check for bank account pattern (e.g. "A/C XXXX1234", "A/C *1234", "A/C 017XXXXXXXXX")
    // Guard: Skip "Biller A/C" matches — the biller's account is not the user's bank account.
    for cap in BANK_ACCOUNT_RE.captures_iter(text) {
        if let Some(full_match) = cap.get(0) {
            let match_start = full_match.start();
            if is_preceded_by_biller(text, match_start) {
                continue;
            }
        }
        if let Some(m) = cap.get(1) {
            let account = m.as_str().trim();
            let digits: String = account.chars().filter(|c| c.is_ascii_digit()).collect();
            if digits.is_empty() {
                continue;
            }
            if digits.len() == 4 {
                return (Some(digits), None);
            }
            // Could be a masked wallet or full account hint
            return (None, Some(account.to_string()));
        }
    }

    (None, None)
}

/// Checks if "Biller" appears immediately before the A/C match position.
fn is_preceded_by_biller(text: &str, match_start: usize) -> bool {
    // Look backward up to 10 chars for "biller" (case-insensitive)
    let mut start = match_start.saturating_sub(10);
    while start < match_start && !text.is_char_boundary(start) {
        start += 1;
    }
    let before = &text[start..match_start];
    before.trim_end().to_lowercase().ends_with("biller")
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn extracts_card_last4() {
        assert_eq!(
            extract_account_info("Your Card 1234 was used at SWAPNO"),
            (Some("1234".to_string()), Some("Card *1234".to_string()))
        );
        assert_eq!(
            extract_account_info("Card ending 4321 was debited"),
            (Some("4321".to_string()), Some("Card *4321".to_string()))
        );
    }

    #[test]
    fn extracts_card_star_prefix() {
        assert_eq!(
            extract_account_info("Card *5678 was used at Agora"),
            (Some("5678".to_string()), Some("Card *5678".to_string()))
        );
    }

    #[test]
    fn extracts_card_no_dot_format() {
        assert_eq!(
            extract_account_info("Card no. 4321 was used for purchase"),
            (Some("4321".to_string()), Some("Card *4321".to_string()))
        );
    }

    #[test]
    fn extracts_account_last4_and_hints() {
        assert_eq!(
            extract_account_info("Your A/C XXXX5678 has been debited"),
            (Some("5678".to_string()), None)
        );
        assert_eq!(
            extract_account_info("A/C *9012 credited"),
            (Some("9012".to_string()), None)
        );
        assert_eq!(
            extract_account_info("A/C 017XXXXXXXXX credited"),
            (None, Some("017XXXXXXXXX".to_string()))
        );
    }

    #[test]
    fn skips_biller_account() {
        // "Biller A/C 12345678" should NOT be captured as the user's bank account.
        let (last4, hint) =
            extract_account_info("Bill Payment to DESCO. Biller A/C 12345678. Fee Tk 0.00.");
        assert_eq!(last4, None);
        assert_eq!(hint, None);
    }

    #[test]
    fn extracts_user_account_when_biller_also_present() {
        // User's A/C should be found, but Biller A/C should be skipped.
        let text = "Bill Payment from A/C XXXX1234 to DESCO. Biller A/C 12345678.";
        let (last4, _hint) = extract_account_info(text);
        assert_eq!(last4, Some("1234".to_string()));
    }
}

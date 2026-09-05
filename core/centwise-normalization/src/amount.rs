//! Amount parsing to integer minor units (poisha) without floating point.
//!
//! Handles the shapes seen in Bangladeshi bank/MFS SMS:
//! - `5,000.00` (western grouping)
//! - `1,50,000.00` (lakh/crore grouping — commas stripped identically)
//! - `2000`, `0.50`, `.50`
//! - decorations: `Tk`, `tk`, `TK`, `BDT`, surrounding spaces
//!
//! Anything else (letters mixed into the number, multiple dots, more than
//! two decimals, negatives) is rejected with `None` — the caller decides
//! whether the message is a transaction; we never guess a value.

/// Currency decorations tolerated around a numeric amount.
const CURRENCY_PREFIXES: [&str; 4] = ["Tk", "tk", "TK", "BDT"];

/// Parses an amount string into minor units (taka × 100).
///
/// ```text
/// "Tk 5,000.00"   → 500_000
/// "2000"          → 200_000
/// "0.50"          → 50
/// "abc", "1.2.3", "1.234", "-500" → None
/// ```
pub fn parse_amount_minor(input: &str) -> Option<i64> {
    // Strip a leading currency marker (Tk/tk/TK/BDT) if present.
    let mut remainder = input.trim();
    for prefix in CURRENCY_PREFIXES {
        if let Some(stripped) = remainder.strip_prefix(prefix) {
            remainder = stripped.trim_start();
            break;
        }
    }

    // Keep only digits, commas, and at most one dot — everything else
    // (including any leftover letters) marks the input invalid.
    let mut saw_dot = false;
    let mut cleaned = String::with_capacity(remainder.len());
    for character in remainder.chars() {
        match character {
            '0'..='9' => cleaned.push(character),
            ',' => {}
            '.' if !saw_dot => {
                saw_dot = true;
                cleaned.push('.');
            }
            // Spaces / symbols / letters are not part of a valid amount token.
            _ => return None,
        }
    }

    let (whole_part, fractional_part) = match cleaned.split_once('.') {
        Some((whole, fractional)) => (whole, fractional),
        None => (cleaned.as_str(), ""),
    };

    if whole_part.is_empty() && fractional_part.is_empty() {
        return None;
    }
    // Money has at most two decimals in BDT.
    if fractional_part.len() > 2 {
        return None;
    }

    let whole_minor: i64 = if whole_part.is_empty() {
        0
    } else {
        whole_part.parse::<i64>().ok()?
    };
    let fraction_minor: i64 = match fractional_part.len() {
        0 => 0,
        1 => fractional_part.parse::<i64>().ok()? * 10,
        _ => fractional_part.parse::<i64>().ok()?,
    };

    whole_minor
        .checked_mul(100)
        .and_then(|total| total.checked_add(fraction_minor))
}

/// Finds all numeric amount candidates in a text: runs of digits with
/// optional commas and an optional two-decimal fraction.
///
/// This only locates candidates — phone prefixes (`017`), numeric Rocket
/// TrxIDs, and date/time components (`22`, `08`, `2026`, `14`, `25`) also
/// surface here. The parser layer disambiguates which candidate is the
/// amount vs fee, balance, or noise using the words around them.
pub fn find_amount_tokens(input: &str) -> Vec<String> {
    let mut tokens = Vec::new();
    let mut current = String::new();

    for character in input.chars() {
        let is_digit = character.is_ascii_digit();
        let continues_number = is_digit
            || (character == ','
                && !current.is_empty()
                && current.chars().all(|c| c.is_ascii_digit() || c == ','))
            || (character == '.'
                && !current.is_empty()
                && !current.contains('.')
                && current.chars().all(|c| c.is_ascii_digit() || c == ','));

        if continues_number {
            current.push(character);
        } else {
            if !current.is_empty() {
                tokens.push(std::mem::take(&mut current));
            }
        }
    }
    if !current.is_empty() {
        tokens.push(current);
    }

    // Drop pure-comma artifacts and tokens that are not parseable amounts.
    tokens
        .into_iter()
        .filter(|token| parse_amount_minor(token).is_some())
        .collect()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn parses_western_grouping_with_currency() {
        assert_eq!(parse_amount_minor("Tk 5,000.00"), Some(500_000));
        assert_eq!(parse_amount_minor("Tk 2,000.00"), Some(200_000));
    }

    #[test]
    fn parses_lakh_grouping() {
        assert_eq!(parse_amount_minor("1,50,000.00"), Some(15_000_000));
    }

    #[test]
    fn parses_plain_and_fraction_shapes() {
        assert_eq!(parse_amount_minor("2000"), Some(200_000));
        assert_eq!(parse_amount_minor("0.50"), Some(50));
        assert_eq!(parse_amount_minor(".50"), Some(50));
        assert_eq!(parse_amount_minor("19.5"), Some(1_950));
        assert_eq!(parse_amount_minor("BDT 850"), Some(85_000));
    }

    #[test]
    fn rejects_invalid_amounts() {
        assert_eq!(parse_amount_minor(""), None);
        assert_eq!(parse_amount_minor("abc"), None);
        assert_eq!(parse_amount_minor("1.2.3"), None);
        assert_eq!(parse_amount_minor("1.234"), None, "max two decimals");
        assert_eq!(parse_amount_minor("-500"), None, "negatives rejected");
        assert_eq!(parse_amount_minor("8H9J2K"), None, "mixed letters rejected");
        assert_eq!(parse_amount_minor(","), None);
    }

    #[test]
    fn finds_amount_candidates_in_real_sms() {
        let body = "Cash Out Tk 2,000.00 to 017XXXXXXXX successful. Fee Tk 18.00. Balance 7,482.00. TrxID 234567890123";
        let tokens = find_amount_tokens(body);
        // Phone prefix (017) and the numeric TrxID are also numeric
        // candidates; the parser layer picks by context.
        assert_eq!(
            tokens,
            vec![
                "2,000.00".to_string(),
                "017".to_string(),
                "18.00".to_string(),
                "7,482.00".to_string(),
                "234567890123".to_string()
            ]
        );
    }

    #[test]
    fn date_and_time_components_split_into_separate_tokens() {
        let tokens = find_amount_tokens("at 22/08/2026 14:25");
        assert_eq!(
            tokens,
            vec![
                "22".to_string(),
                "08".to_string(),
                "2026".to_string(),
                "14".to_string(),
                "25".to_string()
            ]
        );
    }
}

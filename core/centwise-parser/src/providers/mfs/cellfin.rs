//! CellFin-specific account extraction.

use super::super::{NormalizedSms, ProviderParse, ProviderParser};
use regex::Regex;
use std::sync::LazyLock;

pub const PROVIDER_ID: &str = "cellfin";

static FROM_ACCOUNT_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(r"(?i)\bFROM:\s*([0-9]{10,25})\b").expect("valid CellFin FROM account regex")
});

/// Extracts CellFin's unmasked `FROM: <account_digits>` account form.
pub fn extract_account_info(text: &str) -> Option<(String, String)> {
    let capture = FROM_ACCOUNT_RE.captures(text)?;
    let digits = capture.get(1)?.as_str().trim();
    let last4 = digits.get(digits.len().saturating_sub(4)..)?.to_string();
    Some((last4, digits.to_string()))
}

pub struct Parser;
pub static PARSER: Parser = Parser;

impl ProviderParser for Parser {
    fn id(&self) -> &'static str {
        PROVIDER_ID
    }

    fn can_handle(&self, sms: &NormalizedSms<'_>) -> bool {
        super::super::detect_provider(sms.sender, sms.body) == PROVIDER_ID
    }

    fn parse(&self, sms: &NormalizedSms<'_>) -> ProviderParse {
        let account = extract_account_info(sms.body).map(|(last4, hint)| (Some(last4), Some(hint)));
        super::super::generic::parse_common(PROVIDER_ID, *sms, account, None)
    }
}

pub fn parse(sms: NormalizedSms<'_>) -> ProviderParse {
    PARSER.parse(&sms)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn extracts_cellfin_from_account() {
        assert_eq!(
            extract_account_info("CellFin transfer FROM: 123456789012345"),
            Some(("2345".to_string(), "123456789012345".to_string()))
        );
    }

    #[test]
    fn ignores_non_cellfin_account_text_without_from_marker() {
        assert_eq!(extract_account_info("A/C 123456789012345"), None);
    }
}

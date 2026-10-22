//! Standard Chartered provider boundary.
pub const PROVIDER_ID: &str = "standard-chartered";

pub(crate) fn matches_body(body: &str) -> bool {
    body.to_ascii_lowercase().contains("standard chartered")
}

pub(crate) fn is_card_purchase(body: &str) -> bool {
    let lower = body.to_ascii_lowercase();
    lower.contains("card")
        && (lower.contains("used") || lower.contains("purchase") || lower.contains("spent"))
}

use super::super::{NormalizedSms, ProviderParse, ProviderParser};
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
        match super::super::generic::parse_common(PROVIDER_ID, *sms, None, None) {
            ProviderParse::Parsed(mut tx)
                if is_card_purchase(sms.body) && tx.category_id.as_deref() == Some("other") =>
            {
                tx.category_id = Some("shopping".into());
                ProviderParse::Parsed(tx)
            }
            other => other,
        }
    }
}
pub fn parse(sms: NormalizedSms<'_>) -> ProviderParse {
    ProviderParser::parse(&PARSER, &sms)
}

#[cfg(test)]
mod tests {
    use super::*;
    #[test]
    fn owns_standard_chartered_card_family() {
        assert!(is_card_purchase(
            "Standard Chartered card used at Demo Store"
        ));
    }
}

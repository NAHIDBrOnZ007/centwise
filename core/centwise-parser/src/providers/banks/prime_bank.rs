//! Prime Bank provider boundary.
pub const PROVIDER_ID: &str = "prime-bank";

pub(crate) fn matches_body(body: &str) -> bool {
    body.to_ascii_lowercase().contains("prime bank")
}

pub(crate) fn is_fee_alert(body: &str) -> bool {
    let lower = body.to_ascii_lowercase();
    lower.contains("excise duty")
        || lower.contains("annual fee")
        || lower.contains("service charge")
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
        if is_fee_alert(sms.body) {
            super::super::generic::parse_with_category(PROVIDER_ID, *sms, "fees")
        } else {
            super::super::generic::parse_common(PROVIDER_ID, *sms, None, None)
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
    fn owns_prime_fee_family() {
        assert!(is_fee_alert("Prime Bank excise duty BDT 500 debited"));
    }
}

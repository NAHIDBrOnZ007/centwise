//! Trust Bank provider boundary.
pub const PROVIDER_ID: &str = "trust-bank";

pub(crate) fn matches_body(body: &str) -> bool {
    body.to_ascii_lowercase().contains("trust bank")
}

pub(crate) fn is_atm_or_pos_alert(body: &str) -> bool {
    let lower = body.to_ascii_lowercase();
    lower.contains("atm") || lower.contains("pos")
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
        if is_atm_or_pos_alert(sms.body) && sms.body.to_ascii_lowercase().contains("withdraw") {
            super::super::generic::parse_with_category(PROVIDER_ID, *sms, "cash-withdrawal")
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
    fn owns_trust_bank_alert_family() {
        assert!(is_atm_or_pos_alert("Trust Bank ATM withdrawal alert"));
    }
}

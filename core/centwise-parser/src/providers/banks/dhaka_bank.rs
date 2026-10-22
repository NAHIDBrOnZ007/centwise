//! Dhaka Bank provider boundary.
pub const PROVIDER_ID: &str = "dhaka-bank";

pub(crate) fn matches_body(body: &str) -> bool {
    body.to_ascii_lowercase().contains("dhaka bank")
}

pub(crate) fn is_bill_payment(body: &str) -> bool {
    let lower = body.to_ascii_lowercase();
    lower.contains("bill payment")
        || lower.contains("bill paid")
        || lower.contains("mobile recharge")
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
        if is_bill_payment(sms.body) {
            super::super::generic::parse_with_category(PROVIDER_ID, *sms, "bills")
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
    fn owns_dhaka_bank_bill_family() {
        assert!(is_bill_payment("Dhaka Bank bill payment BDT 1000"));
    }
}

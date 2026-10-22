//! Pubali Bank provider boundary.
pub const PROVIDER_ID: &str = "pubali-bank";

pub(crate) fn matches_body(body: &str) -> bool {
    body.to_ascii_lowercase().contains("pubali bank")
}

pub(crate) fn is_clearing_cheque_return(body: &str) -> bool {
    let lower = body.to_ascii_lowercase();
    lower.contains("clearing") && lower.contains("cheque") && lower.contains("return")
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
        if is_clearing_cheque_return(sms.body) {
            ProviderParse::Rejected(crate::types::RejectReason::NotATransaction)
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
    fn rejects_pubali_clearing_return_notice() {
        assert!(is_clearing_cheque_return(
            "Pubali Bank clearing cheque return notice"
        ));
    }
}

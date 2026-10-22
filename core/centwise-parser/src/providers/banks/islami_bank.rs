//! Islami Bank Bangladesh provider boundary.
pub const PROVIDER_ID: &str = "islami-bank";

pub(crate) fn matches_body(body: &str) -> bool {
    let lower = body.to_ascii_lowercase();
    lower.contains("islami bank")
        || lower.contains("mudaraba")
        || lower.contains("ibbplc")
        || lower.contains("ibbl")
}

pub(crate) fn is_cash_withdrawal(body: &str) -> bool {
    let lower = body.to_ascii_lowercase();
    (lower.contains("atm") || lower.contains("cash")) && lower.contains("withdraw")
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
        if is_cash_withdrawal(sms.body) {
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
    fn owns_islami_cash_withdrawal_family() {
        assert!(is_cash_withdrawal(
            "Islami Bank ATM cash withdrawal BDT 1000"
        ));
    }
}

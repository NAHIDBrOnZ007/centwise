//! United Commercial Bank provider boundary.
pub const PROVIDER_ID: &str = "ucb";

pub(crate) fn matches_body(body: &str) -> bool {
    let lower = body.to_ascii_lowercase();
    lower.contains("ucb ") || lower.contains("united commercial")
}

pub(crate) fn is_loan_repayment(body: &str) -> bool {
    let lower = body.to_ascii_lowercase();
    lower.contains("auto debit") || lower.contains("auto-debit") || lower.contains("loan repayment")
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
        if is_loan_repayment(sms.body) {
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
    fn owns_ucb_loan_repayment_family() {
        assert!(is_loan_repayment(
            "UCB auto debit for loan repayment BDT 1000"
        ));
    }
}

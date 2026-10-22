//! BRAC Bank provider boundary.
pub const PROVIDER_ID: &str = "brac-bank";

pub(crate) fn matches_body(body: &str) -> bool {
    let lower = body.to_ascii_lowercase();
    lower.contains("brac bank") || lower.contains("astallion")
}

/// BRAC FD renewal notices describe a rollover, not a new cash-flow event.
pub(crate) fn is_fd_renewal_notice(body: &str) -> bool {
    let lower = body.to_ascii_lowercase();
    lower.contains("has been renewed at an interest rate")
        || (lower.contains("fd a/c") && lower.contains("has been renewed"))
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
        if is_fd_renewal_notice(sms.body) {
            return ProviderParse::Rejected(crate::types::RejectReason::NotATransaction);
        }
        super::super::generic::parse_common(PROVIDER_ID, *sms, None, None)
    }
}

pub fn parse(sms: NormalizedSms<'_>) -> ProviderParse {
    ProviderParser::parse(&PARSER, &sms)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn rejects_fd_rollover_as_non_transaction() {
        assert!(is_fd_renewal_notice(
            "Your FD A/C 30570***001 for TK 50,000.00 with interest TK 225.00 has been renewed at an interest rate of 3.00% for 60 Days."
        ));
    }

    #[test]
    fn keeps_brac_card_purchase_as_transaction() {
        let sms = NormalizedSms {
            body: "Dear Customer, Card ending 4321 was used at SWAPNO for BDT 1,200.00 on 22/08/2026. Available Balance: BDT 15,000.00.",
            sender: Some("BRAC-BANK"),
        };
        assert!(matches!(parse(sms), ProviderParse::Parsed(_)));
    }
}

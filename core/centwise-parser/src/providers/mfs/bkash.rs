//! bKash-specific safety and field rules.

use regex::Regex;
use std::sync::LazyLock;

use super::super::{NormalizedSms, ProviderParse, ProviderParser};

pub const PROVIDER_ID: &str = super::super::PROVIDER_BKASH;
pub struct Parser;
pub static PARSER: Parser = Parser;

static IBANKING_DEPOSIT_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(r"(?i)\bdeposit\s+from\s+iBanking\s+of\s+(?:(?:Tk\.?|BDT)\s*)?[0-9][0-9,]*(?:\.[0-9]{1,2})?\s+from\s+([0-9A-Za-z\s'.-]+?)(?:\s+Fee|\s*[.,])")
        .expect("valid bKash iBanking deposit regex")
});

impl ProviderParser for Parser {
    fn id(&self) -> &'static str {
        PROVIDER_ID
    }

    fn can_handle(&self, sms: &NormalizedSms<'_>) -> bool {
        super::super::detect_provider(sms.sender, sms.body) == PROVIDER_ID
    }

    fn parse(&self, sms: &NormalizedSms<'_>) -> ProviderParse {
        let party_override = extract_ibanking_deposit_party(sms.body).map(Some);
        super::super::generic::parse_common(PROVIDER_ID, *sms, None, party_override)
    }
}

pub fn parse(sms: NormalizedSms<'_>) -> ProviderParse {
    ProviderParser::parse(&PARSER, &sms)
}

pub(crate) fn extract_ibanking_deposit_party(text: &str) -> Option<String> {
    if !text.to_ascii_lowercase().contains("deposit from ibanking") {
        return None;
    }
    let capture = IBANKING_DEPOSIT_RE.captures(text)?;
    super::super::super::extract::party::clean_party_candidate(capture.get(1)?.as_str())
}

/// Identifies the non-authoritative recharge request notice that precedes the
/// actual bKash confirmation receipt.
pub fn is_intermediary_recharge_request(text: &str) -> bool {
    let lower = text.to_ascii_lowercase();
    lower.contains("your bkash mobile recharge request") && lower.contains("use bkash app")
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn identifies_intermediary_recharge_notice() {
        assert!(is_intermediary_recharge_request(
            "Your bKash Mobile Recharge request of Tk 40.00 was successful. Use bKash App."
        ));
    }

    #[test]
    fn does_not_reject_authoritative_receipt_without_app_notice() {
        assert!(!is_intermediary_recharge_request(
            "Recharge of Tk 40.00 successful. TrxID ABC12345. Balance Tk 100.00."
        ));
    }

    #[test]
    fn owns_ibanking_deposit_party_extraction() {
        let sms = NormalizedSms {
            body: "You have received deposit from iBanking of Tk 2,500.00 from City Bank. Fee Tk 0.00. Balance Tk 2,609.13. TrxID DID4FTCC10 at 13/09/2026 06:14",
            sender: Some("bKash"),
        };
        let ProviderParse::Parsed(transaction) = parse(sms) else {
            panic!("expected bKash transaction");
        };
        assert_eq!(transaction.party.as_deref(), Some("City Bank"));
    }
}

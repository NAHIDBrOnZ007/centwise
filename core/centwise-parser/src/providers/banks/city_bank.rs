//! City Bank provider boundary.
pub const PROVIDER_ID: &str = "city-bank";

pub(crate) fn matches_body(body: &str) -> bool {
    let lower = body.to_ascii_lowercase();
    lower.contains("city touch") || lower.contains("city bank") || lower.contains("amex")
}

/// CityTouch ATM alerts use a multiline withdrawal format with a separate
/// balance line. Keep the recognition rule at the City Bank boundary.
pub(crate) fn is_atm_withdrawal(body: &str) -> bool {
    let lower = body.to_ascii_lowercase();
    lower.contains("atm txn") && lower.contains("withdrawal")
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
        let result = super::super::generic::parse_common(PROVIDER_ID, *sms, None, None);
        match result {
            ProviderParse::Parsed(mut transaction) if is_atm_withdrawal(sms.body) => {
                transaction.category_id = Some("cash-withdrawal".to_string());
                ProviderParse::Parsed(transaction)
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
    fn identifies_citytouch_atm_withdrawal() {
        assert!(is_atm_withdrawal(
            "15-Aug-2024\nATM TXN\nTk. 16,000 Withdrawal\nTk. 35,039 Balance"
        ));
        assert!(!is_atm_withdrawal("CITYTOUCH TXN\nTk. 1,000 Payment"));
    }

    #[test]
    fn parses_citytouch_atm_withdrawal() {
        let sms = NormalizedSms {
            body:
                "15-Aug-2024\nATM TXN\nTk. 16,000 Withdrawal\nTk. 35,039 Balance\nA/C: 2303***7001",
            sender: Some("City Bank"),
        };
        let ProviderParse::Parsed(transaction) = parse(sms) else {
            panic!("expected City Bank ATM transaction");
        };
        assert_eq!(transaction.amount_minor, 1_600_000);
        assert_eq!(transaction.balance_after_minor, Some(3_503_900));
    }
}

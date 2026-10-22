//! Eastern Bank provider boundary.
pub const PROVIDER_ID: &str = "ebl";

pub(crate) fn matches_body(body: &str) -> bool {
    let lower = body.to_ascii_lowercase();
    lower.contains("eastern bank") || lower.contains("ebl connect") || lower.contains("ebl ")
}

use super::super::{NormalizedSms, ProviderParse, ProviderParser};
use centwise_domain::TransactionType;

pub struct Parser;
pub static PARSER: Parser = Parser;

/// EBL labels a wallet funding movement as an MFS transfer. Keep this rule at
/// the EBL boundary so other banks cannot inherit the wording accidentally.
pub(crate) fn is_skybanking_mfs_transfer(body: &str) -> bool {
    let lower = body.to_ascii_lowercase();
    lower.contains("ebl skybanking mfs transfer-")
        && (lower.contains("debited") || lower.contains("credited"))
}

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
            ProviderParse::Parsed(mut transaction) if is_skybanking_mfs_transfer(sms.body) => {
                transaction.transaction_type = TransactionType::Transfer;
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
    fn identifies_ebl_skybanking_wallet_transfer() {
        assert!(is_skybanking_mfs_transfer(
            "AC 109***386 is debited with BDT 300 as EBL Skybanking MFS Transfer-bKash"
        ));
        assert!(!is_skybanking_mfs_transfer(
            "AC 109***386 is debited with BDT 300 as cash withdrawal"
        ));
    }

    #[test]
    fn parses_ebl_wallet_transfer_as_transfer() {
        let sms = NormalizedSms {
            body: "AC 109***386 is debited with BDT 300 as EBL Skybanking MFS Transfer-bKash on 08-SEP-26 02:59:58 AM Balance is BDT 15318.5",
            sender: Some("EBL"),
        };
        let ProviderParse::Parsed(transaction) = parse(sms) else {
            panic!("expected EBL transaction");
        };
        assert_eq!(transaction.transaction_type, TransactionType::Transfer);
        assert_eq!(transaction.amount_minor, 30_000);
    }
}

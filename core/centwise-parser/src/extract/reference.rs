//! Field extraction: Transaction Reference Numbers & TrxIDs.

use regex::Regex;
use std::sync::LazyLock;

static REFERENCE_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(
        r"(?i)\b(?:TrxID|TxnID|Ref\s*ID|Ref\s*No\.?|Ref|Txn\s*ID|Transaction\s*(?:ID|number|no\.?|num)|NPSB\s*Ref|BEFTN\s*Ref|Trx\s*No\.?|Reference)[\s:#]+([A-Za-z0-9.]+)",
    )
    .expect("valid reference regex")
});

pub fn extract_reference(text: &str) -> Option<String> {
    for cap in REFERENCE_RE.captures_iter(text) {
        if let Some(m) = cap.get(1) {
            let val = m.as_str().trim().trim_end_matches('.');
            if val.eq_ignore_ascii_case("not")
                || val.eq_ignore_ascii_case("na")
                || val.eq_ignore_ascii_case("none")
                || val.eq_ignore_ascii_case("applicable")
            {
                continue;
            }
            if val.len() >= 4 {
                return Some(val.to_string());
            }
        }
    }
    None
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn extracts_standard_trxid() {
        assert_eq!(
            extract_reference("Payment successful. TrxID: 9J8K7L6M"),
            Some("9J8K7L6M".to_string())
        );
        assert_eq!(
            extract_reference("TxnID ABC12345678 on 12/05/2026"),
            Some("ABC12345678".to_string())
        );
    }

    #[test]
    fn extracts_robi_transaction_number() {
        let text = "Transaction number R250917.1531.34003b to recharge 50 TAKA from 1847662920 is successful";
        assert_eq!(
            extract_reference(text),
            Some("R250917.1531.34003b".to_string())
        );
    }

    #[test]
    fn extracts_ref_no_format() {
        assert_eq!(
            extract_reference("Transfer via NPSB. Ref No. NP987654321. Fee: BDT 10.00"),
            Some("NP987654321".to_string())
        );
    }

    #[test]
    fn extracts_npsb_beftn_ref() {
        assert_eq!(
            extract_reference("Fund transfer via BEFTN. BEFTN Ref: BF12345678"),
            Some("BF12345678".to_string())
        );
        assert_eq!(
            extract_reference("NPSB Ref#NP98765432 on 05/09/2026"),
            Some("NP98765432".to_string())
        );
    }

    #[test]
    fn extracts_trx_no_format() {
        assert_eq!(
            extract_reference("Trx No: TX12345678 completed on 22/08/2026"),
            Some("TX12345678".to_string())
        );
    }

    #[test]
    fn skips_not_applicable_references() {
        assert_eq!(extract_reference("TrxID not applicable"), None);
        assert_eq!(extract_reference("Ref: NA"), None);
    }
}

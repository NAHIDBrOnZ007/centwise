//! Compatibility facade for the shared reference primitive.

pub use crate::common::reference::extract_reference;

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn preserves_standard_reference_cases() {
        assert_eq!(
            extract_reference("Payment successful. TrxID: 9J8K7L6M"),
            Some("9J8K7L6M".to_string())
        );
        assert_eq!(
            extract_reference("Transfer via NPSB. Ref No. NP987654321."),
            Some("NP987654321".to_string())
        );
        assert_eq!(extract_reference("TrxID not applicable"), None);
    }

    #[test]
    fn extracts_robi_transaction_number() {
        assert_eq!(
            extract_reference(
                "Transaction number R250917.1531.34003b to recharge 50 TAKA from 1847662920"
            ),
            Some("R250917.1531.34003b".to_string())
        );
    }

    #[test]
    fn extracts_npsb_beftn_reference() {
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
    fn extracts_ref_no_format() {
        assert_eq!(
            extract_reference("Transfer via NPSB. Ref No. NP987654321. Fee: BDT 10.00"),
            Some("NP987654321".to_string())
        );
    }

    #[test]
    fn skips_not_applicable_references() {
        assert_eq!(extract_reference("TrxID not applicable"), None);
        assert_eq!(extract_reference("Ref: NA"), None);
    }
}

//! Compatibility facade for the shared account/card primitive.

pub use crate::common::account::extract_account_info;

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn extracts_card_last4() {
        assert_eq!(
            extract_account_info("Your Card 1234 was used at SWAPNO"),
            (Some("1234".to_string()), Some("Card *1234".to_string()))
        );
        assert_eq!(
            extract_account_info("Card ending 4321 was debited"),
            (Some("4321".to_string()), Some("Card *4321".to_string()))
        );
    }

    #[test]
    fn extracts_card_star_prefix() {
        assert_eq!(
            extract_account_info("Card *5678 was used at Agora"),
            (Some("5678".to_string()), Some("Card *5678".to_string()))
        );
    }

    #[test]
    fn extracts_card_no_dot_format() {
        assert_eq!(
            extract_account_info("Card no. 4321 was used for purchase"),
            (Some("4321".to_string()), Some("Card *4321".to_string()))
        );
    }

    #[test]
    fn extracts_account_last4_and_hints() {
        assert_eq!(
            extract_account_info("Your A/C XXXX5678 has been debited"),
            (Some("5678".to_string()), None)
        );
        assert_eq!(
            extract_account_info("A/C *9012 credited"),
            (Some("9012".to_string()), None)
        );
        assert_eq!(
            extract_account_info("A/C 017XXXXXXXXX credited"),
            (None, Some("017XXXXXXXXX".to_string()))
        );
    }

    #[test]
    fn extracts_account_number_and_bare_ac_formats() {
        assert_eq!(
            extract_account_info("Your account number 017901121****01 was debited"),
            (None, Some("017901121****01".to_string()))
        );
        assert_eq!(
            extract_account_info("AC 160***160 is credited"),
            (None, Some("160***160".to_string()))
        );
        assert_eq!(
            extract_account_info("A/C# XX0001 credited"),
            (Some("0001".to_string()), None)
        );
        assert_eq!(
            extract_account_info("A/C no. DSQ-H2712 credited"),
            (Some("2712".to_string()), None)
        );
    }

    #[test]
    fn skips_biller_account() {
        let result =
            extract_account_info("Bill Payment to DESCO. Biller A/C 12345678. Fee Tk 0.00.");
        assert_eq!(result, (None, None));
    }

    #[test]
    fn extracts_user_account_when_biller_also_present() {
        let result =
            extract_account_info("Bill Payment from A/C XXXX1234 to DESCO. Biller A/C 12345678.");
        assert_eq!(result.0, Some("1234".to_string()));
    }
}

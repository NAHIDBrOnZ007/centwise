//! Fast safety filter for OTP, 2FA, PIN, and security alerts.

/// Checks if an SMS is an OTP or security alert that must never become a transaction.
pub fn is_otp_or_security_message(text: &str) -> bool {
    let lower = text.to_lowercase();
    let has_otp_keyword = lower.contains("otp")
        || lower.contains("one time password")
        || lower.contains("verification code")
        || lower.contains("do not share")
        || lower.contains("security code")
        || lower.contains("pin code")
        || lower.contains("login code")
        || lower.contains("two factor")
        || lower.contains("2fa")
        || lower.contains("authentication code");

    // Guard: Some transaction notifications mention "do not share your PIN/OTP" as a footer.
    // Only classify as pure OTP if it lacks completed financial action keywords.
    let has_completed_action = lower.contains("successful")
        || lower.contains("credited")
        || lower.contains("debited")
        || lower.contains("cash out")
        || lower.contains("send money")
        || lower.contains("payment")
        || lower.contains("recharge")
        || lower.contains("bill pay")
        || lower.contains("fund transfer")
        || lower.contains("add money");

    has_otp_keyword && !has_completed_action
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn detects_standard_otp_messages() {
        assert!(is_otp_or_security_message(
            "Your OTP is 123456. Do not share with anyone."
        ));
        assert!(is_otp_or_security_message(
            "bKash verification code is 9876. Valid for 2 mins."
        ));
    }

    #[test]
    fn detects_2fa_and_authentication_codes() {
        assert!(is_otp_or_security_message(
            "Your authentication code is 456789. Valid for 5 minutes."
        ));
        assert!(is_otp_or_security_message(
            "2FA code: 123456. Do not share this code."
        ));
    }

    #[test]
    fn does_not_block_transactions_with_security_warning_footers() {
        assert!(!is_otp_or_security_message(
            "Payment of Tk 500 to Merchant is successful. Do not share your PIN."
        ));
        assert!(!is_otp_or_security_message(
            "Fund transfer of BDT 5,000 via NPSB. Do not share your OTP."
        ));
    }
}

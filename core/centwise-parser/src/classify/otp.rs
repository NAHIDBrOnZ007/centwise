//! Fast safety filter for OTP, 2FA, PIN, and security alerts.

use regex::Regex;
use std::sync::LazyLock;

static SECURITY_CODE_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(r"(?i)\b(?:otp|one[ -]time password|onetime password|verification code|verify code|verification pin|verify pin|security code|security pin|login code|login pin|login otp|access code|activation code|confirmation code|two[ -]factor|2fa|authentication code|secret code|temporary password|temp password|passcode)\b.*?\b[0-9]{4,8}\b|\b[0-9]{4,8}\b.*?\b(?:otp|one[ -]time password|onetime password|verification code|verify code|verification pin|verify pin|security code|security pin|login code|login pin|login otp|access code|activation code|confirmation code|two[ -]factor|2fa|authentication code|secret code|temporary password|temp password|passcode)\b")
        .expect("valid security code regex")
});

static OTP_KEYWORD_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(r"(?i)\b(?:otp|one[ -]time password|onetime password|verification code|verify code|verification pin|verify pin|security code|security pin|login code|login pin|login otp|access code|activation code|confirmation code|two[ -]factor|2fa|authentication code|secret code|temporary password|temp password|passcode)\b")
        .expect("valid otp keyword regex")
});

/// Checks if an SMS is an OTP or security alert that must never become a transaction.
pub fn is_otp_or_security_message(text: &str) -> bool {
    let lower = text.to_lowercase();
    let has_otp_keyword = OTP_KEYWORD_RE.is_match(text)
        || lower.contains("do not share")
        || lower.contains("never share")
        || lower.contains("don't share")
        || lower.contains("do not disclose")
        || lower.contains("never disclose");

    if !has_otp_keyword && !SECURITY_CODE_RE.is_match(text) {
        return false;
    }

    // Guard: Confirmed transaction receipts may have security advisory footers:
    // e.g. "Payment of Tk 500 to Merchant is successful. Do not share your PIN/OTP."
    let is_confirmed_receipt = lower.contains("is successful")
        || lower.contains("was successful")
        || (lower.contains("successful") && !lower.contains("unsuccessful"))
        || lower.contains("has been debited")
        || lower.contains("was debited")
        || lower.contains("debited from")
        || lower.contains("has been credited")
        || lower.contains("was credited")
        || lower.contains("credited to")
        || lower.contains("payment received")
        || lower.contains("recharge successful")
        || lower.contains("spent")
        || lower.contains("was charged")
        || lower.contains("withdrawn")
        || lower.contains("cash deposit");

    if is_confirmed_receipt {
        return false;
    }

    true
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn detects_all_bangladesh_provider_otp_formats() {
        // bKash
        assert!(is_otp_or_security_message(
            "Your OTP is 123456. Do not share with anyone."
        ));
        assert!(is_otp_or_security_message(
            "bKash verification code is 9876. Valid for 2 mins."
        ));
        assert!(is_otp_or_security_message(
            "Your OTP for bKash payment of Tk 500 is 123456. Do not share with anyone."
        ));
        assert!(is_otp_or_security_message(
            "654321 is your OTP for bKash login."
        ));

        // Nagad
        assert!(is_otp_or_security_message(
            "Your Nagad OTP is 456789. Never share this code or your PIN."
        ));
        assert!(is_otp_or_security_message(
            "Your OTP for Nagad transaction is 123456. It is valid for 3 minutes."
        ));
        assert!(is_otp_or_security_message(
            "123456 is your Nagad registration OTP. Valid for 2 mins."
        ));

        // Rocket & Upay
        assert!(is_otp_or_security_message(
            "Rocket: Your OTP is 123456. Do not share your OTP and PIN."
        ));
        assert!(is_otp_or_security_message(
            "Rocket security code is 123456. Never share this."
        ));
        assert!(is_otp_or_security_message(
            "Your upay OTP is 123456. Valid for 3 mins. Never share your OTP or PIN."
        ));

        // Banks (City Bank, BRAC, EBL, IBBL / CellFin)
        assert!(is_otp_or_security_message(
            "OTP for your transaction on Card ... is 123456. Do not share this OTP with anyone."
        ));
        assert!(is_otp_or_security_message(
            "Your OTP for Citytouch login is 123456."
        ));
        assert!(is_otp_or_security_message(
            "Your one-time password (OTP) for Card 1234 is 123456. Valid for 2 mins."
        ));
        assert!(is_otp_or_security_message(
            "Your EBL OTP is 123456 for Internet Banking purchase of BDT 2,000."
        ));
        assert!(is_otp_or_security_message(
            "Your CellFin registration OTP is 123456. Never share this."
        ));
        assert!(is_otp_or_security_message(
            "Use OTP 987654 to complete fund transfer of BDT 10,000."
        ));

        // Telco login / app codes
        assert!(is_otp_or_security_message(
            "Your MyGP login OTP is 123456. Valid for 5 mins."
        ));
        assert!(is_otp_or_security_message(
            "Your MyBL verification code is 1234."
        ));
        assert!(is_otp_or_security_message(
            "Robi: Your security code is 123456."
        ));
        assert!(is_otp_or_security_message(
            "Your Airtel Thanks verification PIN is 1234."
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
        assert!(is_otp_or_security_message(
            "Your temporary password is 889900."
        ));
        assert!(is_otp_or_security_message(
            "Confirmation code: 334455. Do not disclose to anyone."
        ));
    }

    #[test]
    fn does_not_block_transactions_with_security_warning_footers() {
        assert!(!is_otp_or_security_message(
            "Payment of Tk 500 to Merchant is successful. Do not share your PIN."
        ));
        assert!(!is_otp_or_security_message(
            "Fund transfer of BDT 5,000 via NPSB is successful. Do not share your OTP."
        ));
        assert!(!is_otp_or_security_message(
            "Payment of Tk 1,200.00 to Merchant successful. Fee Tk 0.00. Balance Tk 6,475.50. TrxID 8W9X0Y1Z2A. Never share OTP."
        ));
        assert!(!is_otp_or_security_message(
            "Your A/C XX1234 has been debited by BDT 1,000.00. Never share your OTP with anyone."
        ));
    }

    #[test]
    fn does_not_block_jhotpot_emergency_loan_notifications() {
        assert!(!is_otp_or_security_message(
            "You have received Tk 20.00 Jhotpot balance. Outstanding loan Tk 20.00. Dial *222*16# to check."
        ));
    }
}

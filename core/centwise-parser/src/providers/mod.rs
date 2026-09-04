//! Stage 2: Provider Identification
//!
//! Resolves canonical provider identifiers for Bangladeshi Banks, MFS, and Telcos
//! from sender hints and body signatures.

pub mod catalog;

pub use catalog::*;

/// Detects the canonical provider identifier from sender hint and message body.
pub fn detect_provider(sender_hint: Option<&str>, body: &str) -> String {
    if let Some(sender) = sender_hint {
        let s = normalize_sender(sender);
        if let Some(provider) = catalog::lookup_sender(&s) {
            // DBBL sends both core bank and Rocket SMS
            if provider == PROVIDER_DBBL && body.to_lowercase().contains("rocket") {
                return PROVIDER_ROCKET.to_string();
            }
            return provider.to_string();
        }
    }

    // Secondary scan from body keywords and distinctive patterns
    let body_lower = body.to_lowercase();

    // MFS providers (most distinctive)
    if body_lower.contains("bkash") {
        return PROVIDER_BKASH.to_string();
    }
    if body_lower.contains("nagad") {
        return PROVIDER_NAGAD.to_string();
    }
    if body_lower.contains("rocket") || body_lower.contains("16216") {
        return PROVIDER_ROCKET.to_string();
    }
    if body_lower.contains("cellfin") {
        return PROVIDER_CELLFIN.to_string();
    }
    if body_lower.contains("upay") || body_lower.contains("ucash") {
        return PROVIDER_UPAY.to_string();
    }
    if body_lower.contains("nexuspay") {
        return PROVIDER_NEXUSPAY.to_string();
    }

    // Telco recharge confirmations and body signatures
    if body_lower.contains("robi")
        || body_lower.contains("transaction number r")
        || body_lower.contains("*123*")
        || body_lower.contains("dial *0#")
    {
        return PROVIDER_ROBI.to_string();
    }
    if body_lower.contains("grameenphone") || body_lower.contains("*121*") {
        return PROVIDER_GRAMEENPHONE.to_string();
    }
    if body_lower.contains("banglalink")
        || body_lower.contains("*212*")
        || body_lower.contains("ghechang")
    {
        return PROVIDER_BANGLALINK.to_string();
    }
    if body_lower.contains("airtel") {
        return PROVIDER_AIRTEL.to_string();
    }
    if body_lower.contains("teletalk") {
        return PROVIDER_TELETALK.to_string();
    }

    // Specific bank signatures in body
    if body_lower.contains("city touch")
        || body_lower.contains("city bank")
        || body_lower.contains("amex")
    {
        return PROVIDER_CITY_BANK.to_string();
    }
    if body_lower.contains("brac bank") || body_lower.contains("astallion") {
        return PROVIDER_BRAC_BANK.to_string();
    }
    if body_lower.contains("eastern bank") || body_lower.contains("ebl connect") {
        return PROVIDER_EBL.to_string();
    }
    if body_lower.contains("dutch-bangla") || body_lower.contains("dutch bangla") {
        return PROVIDER_DBBL.to_string();
    }
    if body_lower.contains("islami bank") || body_lower.contains("mudaraba") {
        return PROVIDER_ISLAMI_BANK.to_string();
    }
    if body_lower.contains("mutual trust bank") || body_lower.contains("mtb ") {
        return PROVIDER_MUTUAL_TRUST_BANK.to_string();
    }
    if body_lower.contains("standard chartered") {
        return PROVIDER_STANDARD_CHARTERED.to_string();
    }
    if body_lower.contains("bank asia") {
        return PROVIDER_BANK_ASIA.to_string();
    }
    if body_lower.contains("dhaka bank") {
        return PROVIDER_DHAKA_BANK.to_string();
    }
    if body_lower.contains("trust bank") {
        return PROVIDER_TRUST_BANK.to_string();
    }
    if body_lower.contains("sonali bank") {
        return PROVIDER_SONALI_BANK.to_string();
    }
    if body_lower.contains("pubali bank") {
        return PROVIDER_PUBALI_BANK.to_string();
    }
    if body_lower.contains("janata bank") {
        return PROVIDER_JANATA_BANK.to_string();
    }
    if body_lower.contains("rupali bank") {
        return PROVIDER_RUPALI_BANK.to_string();
    }
    if body_lower.contains("prime bank") {
        return PROVIDER_PRIME_BANK.to_string();
    }
    if body_lower.contains("agrani bank") {
        return PROVIDER_AGRANI_BANK.to_string();
    }
    if body_lower.contains("ucb ") || body_lower.contains("united commercial") {
        return PROVIDER_UCB.to_string();
    }

    PROVIDER_BANKS_GENERIC.to_string()
}

fn normalize_sender(sender: &str) -> String {
    sender
        .trim()
        .to_lowercase()
        .chars()
        .filter(|character| character.is_alphanumeric())
        .collect()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn detects_telco_providers() {
        assert_eq!(detect_provider(Some("ROBI"), ""), PROVIDER_ROBI);
        assert_eq!(detect_provider(Some("123"), ""), PROVIDER_ROBI);
        assert_eq!(detect_provider(Some("GP"), ""), PROVIDER_GRAMEENPHONE);
        assert_eq!(detect_provider(Some("Banglalink"), ""), PROVIDER_BANGLALINK);
    }

    #[test]
    fn detects_robi_recharge_from_body() {
        let text = "Transaction number R250917.1531.34003b to recharge 50 TAKA from 1847662920 is successful";
        assert_eq!(detect_provider(None, text), PROVIDER_ROBI);
    }

    #[test]
    fn detects_banks() {
        assert_eq!(detect_provider(Some("BRAC-BANK"), ""), PROVIDER_BRAC_BANK);
        assert_eq!(detect_provider(Some("City Bank"), ""), PROVIDER_CITY_BANK);
        assert_eq!(detect_provider(Some("EBL"), ""), PROVIDER_EBL);
        assert_eq!(detect_provider(Some("NexusPay"), ""), PROVIDER_NEXUSPAY);
    }

    #[test]
    fn detects_mfs_from_sender_shortcodes() {
        assert_eq!(detect_provider(Some("16247"), ""), PROVIDER_BKASH);
        assert_eq!(detect_provider(Some("16167"), ""), PROVIDER_NAGAD);
        assert_eq!(detect_provider(Some("16216"), ""), PROVIDER_ROCKET);
    }

    #[test]
    fn detects_banks_from_body_signatures() {
        assert_eq!(
            detect_provider(None, "Your Mutual Trust Bank A/C debited"),
            PROVIDER_MUTUAL_TRUST_BANK
        );
        assert_eq!(
            detect_provider(None, "Standard Chartered alert: Card used"),
            PROVIDER_STANDARD_CHARTERED
        );
        assert_eq!(
            detect_provider(None, "Bank Asia: Your A/C credited"),
            PROVIDER_BANK_ASIA
        );
        assert_eq!(
            detect_provider(None, "Dhaka Bank: Your A/C debited"),
            PROVIDER_DHAKA_BANK
        );
    }

    #[test]
    fn nexuspay_is_separate_from_dbbl() {
        assert_eq!(
            detect_provider(None, "NexusPay payment Tk 500 successful"),
            PROVIDER_NEXUSPAY
        );
        assert_eq!(
            detect_provider(None, "Dutch-Bangla Bank: Your A/C debited"),
            PROVIDER_DBBL
        );
    }

    #[test]
    fn detects_upay_from_body() {
        assert_eq!(
            detect_provider(None, "Upay Cash In Tk 1,000 successful"),
            PROVIDER_UPAY
        );
    }
}

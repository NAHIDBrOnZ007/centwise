//! Stage 2: Provider Identification
//!
//! Resolves canonical provider identifiers for Bangladeshi Banks, MFS, and Telcos
//! from sender hints and body signatures.

pub mod banks;
pub mod contract;
pub mod generic;
pub mod mfs;
mod receipts;
pub mod telco;

pub mod catalog;

pub use catalog::*;
pub use contract::{NormalizedSms, ProviderParse, ProviderParser};

static TRACKED_WALLET_RE: std::sync::LazyLock<regex::Regex> = std::sync::LazyLock::new(|| {
    regex::Regex::new(
        r"(?i)\byour\s+(bkash|nagad|rocket|upay|cellfin|nexuspay)\s+(?:account|wallet|a/c)\b",
    )
    .expect("valid tracked wallet regex")
});

macro_rules! define_common_provider_parser {
    ($provider_id:expr) => {
        pub struct Parser;
        pub static PARSER: Parser = Parser;

        impl $crate::providers::ProviderParser for Parser {
            fn id(&self) -> &'static str {
                $provider_id
            }

            fn can_handle(&self, sms: &$crate::providers::NormalizedSms<'_>) -> bool {
                $crate::providers::detect_provider(sms.sender, sms.body) == $provider_id
            }

            fn parse(
                &self,
                sms: &$crate::providers::NormalizedSms<'_>,
            ) -> $crate::providers::ProviderParse {
                $crate::providers::generic::parse_common($provider_id, *sms, None, None)
            }
        }

        pub fn parse(
            sms: $crate::providers::NormalizedSms<'_>,
        ) -> $crate::providers::ProviderParse {
            $crate::providers::ProviderParser::parse(&PARSER, &sms)
        }
    };
}

pub(crate) use define_common_provider_parser;

/// Detects the canonical provider identifier from sender hint and message body.
pub fn detect_provider(sender_hint: Option<&str>, body: &str) -> String {
    if let Some(sender) = sender_hint {
        let s = normalize_sender(sender);
        if s == "16216" && body.to_lowercase().contains("nexuspay") {
            return PROVIDER_NEXUSPAY.to_string();
        }
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

    if let Some(wallet) = TRACKED_WALLET_RE.captures(&body_lower) {
        return wallet[1].to_string();
    }

    // A bank debit naming its destination wallet still belongs to the bank.
    if (body_lower.contains("debited") || body_lower.contains("credited"))
        && (body_lower.contains("account")
            || body_lower.contains("a/c")
            || body_lower.starts_with("ac "))
    {
        if let Some(bank) = detect_bank_from_body(body) {
            return bank.to_string();
        }
    }

    // MFS providers (most distinctive)
    if body_lower.contains("bkash") || body_lower.contains("bka.sh") {
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
        || body_lower.contains("*8#")
        || body_lower.contains("emergency loan advances")
        || body_lower.contains("emergency loan")
        || body_lower.contains("jhotpot")
    {
        return PROVIDER_ROBI.to_string();
    }
    if body_lower.contains("grameenphone") || body_lower.contains("*121*") {
        return PROVIDER_GRAMEENPHONE.to_string();
    }
    if body_lower.contains("banglalink")
        || body_lower.contains("*212*")
        || body_lower.contains("ghechang")
        || body_lower.contains("toffee")
    {
        return PROVIDER_BANGLALINK.to_string();
    }
    if body_lower.contains("airtel") || body_lower.contains("*141*") {
        return PROVIDER_AIRTEL.to_string();
    }
    if body_lower.contains("teletalk")
        || body_lower.contains("telecharge")
        || body_lower.contains("has recharged your account with")
    {
        return PROVIDER_TELETALK.to_string();
    }

    if let Some(bank) = detect_bank_from_body(body) {
        return bank.to_string();
    }

    PROVIDER_BANKS_GENERIC.to_string()
}

fn detect_bank_from_body(body: &str) -> Option<&'static str> {
    // Bank-specific body signatures live in the bank modules so adding a
    // provider cannot silently expand a global catch-all detector.
    if banks::city_bank::matches_body(body) {
        return Some(PROVIDER_CITY_BANK);
    }
    if banks::brac_bank::matches_body(body) {
        return Some(PROVIDER_BRAC_BANK);
    }
    if banks::ebl::matches_body(body) {
        return Some(PROVIDER_EBL);
    }
    if banks::dbbl::matches_body(body) {
        return Some(PROVIDER_DBBL);
    }
    if banks::islami_bank::matches_body(body) {
        return Some(PROVIDER_ISLAMI_BANK);
    }
    if banks::mutual_trust_bank::matches_body(body) {
        return Some(PROVIDER_MUTUAL_TRUST_BANK);
    }
    if banks::standard_chartered::matches_body(body) {
        return Some(PROVIDER_STANDARD_CHARTERED);
    }
    if banks::bank_asia::matches_body(body) {
        return Some(PROVIDER_BANK_ASIA);
    }
    if banks::dhaka_bank::matches_body(body) {
        return Some(PROVIDER_DHAKA_BANK);
    }
    if banks::trust_bank::matches_body(body) {
        return Some(PROVIDER_TRUST_BANK);
    }
    if banks::sonali_bank::matches_body(body) {
        return Some(PROVIDER_SONALI_BANK);
    }
    if banks::pubali_bank::matches_body(body) {
        return Some(PROVIDER_PUBALI_BANK);
    }
    if banks::janata_bank::matches_body(body) {
        return Some(PROVIDER_JANATA_BANK);
    }
    if banks::rupali_bank::matches_body(body) {
        return Some(PROVIDER_RUPALI_BANK);
    }
    if banks::prime_bank::matches_body(body) {
        return Some(PROVIDER_PRIME_BANK);
    }
    if banks::agrani_bank::matches_body(body) {
        return Some(PROVIDER_AGRANI_BANK);
    }
    if banks::ucb::matches_body(body) {
        return Some(PROVIDER_UCB);
    }

    None
}

/// Dispatches a normalized message to its provider module.
pub fn parse_provider(provider_id: &str, sms: NormalizedSms<'_>) -> ProviderParse {
    match provider_id {
        PROVIDER_BKASH => mfs::bkash::parse(sms),
        PROVIDER_NAGAD => mfs::nagad::parse(sms),
        PROVIDER_ROCKET => mfs::rocket::parse(sms),
        PROVIDER_UPAY => mfs::upay::parse(sms),
        PROVIDER_CELLFIN => mfs::cellfin::parse(sms),
        PROVIDER_NEXUSPAY => mfs::nexuspay::parse(sms),
        PROVIDER_ROBI => telco::robi::parse(sms),
        PROVIDER_GRAMEENPHONE => telco::grameenphone::parse(sms),
        PROVIDER_BANGLALINK => telco::banglalink::parse(sms),
        PROVIDER_AIRTEL => telco::airtel::parse(sms),
        PROVIDER_TELETALK => telco::teletalk::parse(sms),
        PROVIDER_CITY_BANK => banks::city_bank::parse(sms),
        PROVIDER_BRAC_BANK => banks::brac_bank::parse(sms),
        PROVIDER_EBL => banks::ebl::parse(sms),
        PROVIDER_DBBL => banks::dbbl::parse(sms),
        PROVIDER_ISLAMI_BANK => banks::islami_bank::parse(sms),
        PROVIDER_SONALI_BANK => banks::sonali_bank::parse(sms),
        PROVIDER_PUBALI_BANK => banks::pubali_bank::parse(sms),
        PROVIDER_UCB => banks::ucb::parse(sms),
        PROVIDER_PRIME_BANK => banks::prime_bank::parse(sms),
        PROVIDER_AGRANI_BANK => banks::agrani_bank::parse(sms),
        PROVIDER_MUTUAL_TRUST_BANK => banks::mutual_trust_bank::parse(sms),
        PROVIDER_STANDARD_CHARTERED => banks::standard_chartered::parse(sms),
        PROVIDER_BANK_ASIA => banks::bank_asia::parse(sms),
        PROVIDER_DHAKA_BANK => banks::dhaka_bank::parse(sms),
        PROVIDER_TRUST_BANK => banks::trust_bank::parse(sms),
        PROVIDER_JANATA_BANK => banks::janata_bank::parse(sms),
        PROVIDER_RUPALI_BANK => banks::rupali_bank::parse(sms),
        _ => generic::parse_common(PROVIDER_BANKS_GENERIC, sms, None, None),
    }
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
    fn detects_nexuspay_from_ambiguous_16216_sender() {
        assert_eq!(
            detect_provider(Some("16216"), "NexusPay transaction successful"),
            PROVIDER_NEXUSPAY
        );
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

    #[test]
    fn every_catalog_provider_dispatches_to_a_dedicated_parser() {
        let sms = NormalizedSms {
            body: "Payment of Tk 100 successful",
            sender: None,
        };
        let provider_ids = [
            PROVIDER_BKASH,
            PROVIDER_NAGAD,
            PROVIDER_ROCKET,
            PROVIDER_UPAY,
            PROVIDER_CELLFIN,
            PROVIDER_NEXUSPAY,
            PROVIDER_ROBI,
            PROVIDER_GRAMEENPHONE,
            PROVIDER_BANGLALINK,
            PROVIDER_AIRTEL,
            PROVIDER_TELETALK,
            PROVIDER_CITY_BANK,
            PROVIDER_BRAC_BANK,
            PROVIDER_EBL,
            PROVIDER_DBBL,
            PROVIDER_ISLAMI_BANK,
            PROVIDER_SONALI_BANK,
            PROVIDER_PUBALI_BANK,
            PROVIDER_UCB,
            PROVIDER_PRIME_BANK,
            PROVIDER_AGRANI_BANK,
            PROVIDER_MUTUAL_TRUST_BANK,
            PROVIDER_STANDARD_CHARTERED,
            PROVIDER_BANK_ASIA,
            PROVIDER_DHAKA_BANK,
            PROVIDER_TRUST_BANK,
            PROVIDER_JANATA_BANK,
            PROVIDER_RUPALI_BANK,
        ];

        for provider_id in provider_ids {
            let result = parse_provider(provider_id, sms);
            assert!(
                !matches!(result, ProviderParse::NotThisFormat),
                "provider {provider_id} was not dispatched"
            );
        }
    }

    #[test]
    fn provider_parser_contract_reports_its_canonical_id() {
        let sms = NormalizedSms {
            body: "",
            sender: Some("BRAC-BANK"),
        };
        assert_eq!(banks::brac_bank::PARSER.id(), PROVIDER_BRAC_BANK);
        assert!(banks::brac_bank::PARSER.can_handle(&sms));
    }
}

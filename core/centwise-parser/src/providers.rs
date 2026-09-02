//! Provider detection and quirks handling for Bangladeshi Banks & MFS.

/// Canonical provider identifiers.
pub const PROVIDER_BKASH: &str = "bkash";
pub const PROVIDER_NAGAD: &str = "nagad";
pub const PROVIDER_ROCKET: &str = "rocket";
pub const PROVIDER_BANKS_GENERIC: &str = "banks-generic";

/// Detects the canonical provider identifier from sender hint and message body.
pub fn detect_provider(sender_hint: Option<&str>, body: &str) -> String {
    if let Some(sender) = sender_hint {
        let s = normalize_sender(sender);
        if let Some(provider) = provider_for_sender(&s) {
            // If sender is DBBL but body mentions Rocket / 16216, it's rocket
            if provider == "dbbl" && body.to_lowercase().contains("rocket") {
                return PROVIDER_ROCKET.to_string();
            }
            return provider.to_string();
        }
    }

    // Secondary scan from body keywords or trailing brackets
    let body_lower = body.to_lowercase();
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
        return "cellfin".to_string();
    }
    if body_lower.contains("upay") {
        return "upay".to_string();
    }
    if body.contains("[Bank Name]") || body_lower.contains("a/c xxxx") {
        return PROVIDER_BANKS_GENERIC.to_string();
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

fn provider_for_sender(sender: &str) -> Option<&'static str> {
    match sender {
        "bkash" | "bkashbd" => Some(PROVIDER_BKASH),
        "nagad" | "nagadbd" => Some(PROVIDER_NAGAD),
        "rocket" | "dbblrocket" | "16216" => Some(PROVIDER_ROCKET),
        "dbbl" | "dutchbanglabank" | "dutchbanglabankplc" => Some("dbbl"),
        "citybank" | "thecitybank" | "citybankplc" => Some("city-bank"),
        "bracbank" | "bracbankplc" => Some("brac-bank"),
        "ebl" | "easternbank" | "easternbankplc" => Some("ebl"),
        "sonalibank" => Some("sonali-bank"),
        "ibbl" | "islamibank" | "islamibankbangladesh" => Some("islami-bank"),
        "pubalibank" => Some("pubali-bank"),
        "ucb" | "ucbbank" => Some("ucb"),
        "primebank" | "primebankplc" => Some("prime-bank"),
        "agranibank" => Some("agrani-bank"),
        _ => None,
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn broad_commercial_sender_words_do_not_impersonate_banks() {
        assert_eq!(
            detect_provider(Some("City Mart"), "Payment Tk 100"),
            PROVIDER_BANKS_GENERIC
        );
        assert_eq!(
            detect_provider(Some("Prime Deals"), "Payment Tk 100"),
            PROVIDER_BANKS_GENERIC
        );
    }

    #[test]
    fn normalized_known_sender_aliases_resolve_exactly() {
        assert_eq!(detect_provider(Some("CITY BANK"), ""), "city-bank");
        assert_eq!(detect_provider(Some("BRAC-BANK"), ""), "brac-bank");
        assert_eq!(detect_provider(Some("DBBL"), ""), "dbbl");
        assert_eq!(detect_provider(Some("bKash"), ""), PROVIDER_BKASH);
    }

    #[test]
    fn generic_transaction_id_without_provider_evidence_stays_generic() {
        assert_eq!(
            detect_provider(None, "TxnID ABC123. Payment Tk 100."),
            PROVIDER_BANKS_GENERIC
        );
    }
}

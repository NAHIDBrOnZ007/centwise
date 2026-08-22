//! Provider detection and quirks handling for Bangladeshi Banks & MFS.

/// Canonical provider identifiers.
pub const PROVIDER_BKASH: &str = "bkash";
pub const PROVIDER_NAGAD: &str = "nagad";
pub const PROVIDER_ROCKET: &str = "rocket";
pub const PROVIDER_BANKS_GENERIC: &str = "banks-generic";

/// Detects the canonical provider identifier from sender hint and message body.
pub fn detect_provider(sender_hint: Option<&str>, body: &str) -> String {
    if let Some(sender) = sender_hint {
        let s = sender.trim().to_lowercase();
        if s.contains("bkash") {
            return PROVIDER_BKASH.to_string();
        }
        if s.contains("nagad") {
            return PROVIDER_NAGAD.to_string();
        }
        if s.contains("rocket") || s.contains("16216") {
            return PROVIDER_ROCKET.to_string();
        }
        if s.contains("dbbl") {
            // If sender is DBBL but body mentions Rocket / 16216, it's rocket
            if body.to_lowercase().contains("rocket") {
                return PROVIDER_ROCKET.to_string();
            }
            return "dbbl".to_string();
        }
        if s.contains("city") {
            return "city-bank".to_string();
        }
        if s.contains("brac") {
            return "brac-bank".to_string();
        }
        if s.contains("ebl") || s.contains("eastern") {
            return "ebl".to_string();
        }
        if s.contains("sonali") {
            return "sonali-bank".to_string();
        }
        if s.contains("islami") || s.contains("ibbl") {
            return "islami-bank".to_string();
        }
        if s.contains("pubali") {
            return "pubali-bank".to_string();
        }
        if s.contains("ucb") {
            return "ucb".to_string();
        }
        if s.contains("prime") {
            return "prime-bank".to_string();
        }
        if s.contains("agrani") {
            return "agrani-bank".to_string();
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
    if body_lower.contains("rocket") {
        return PROVIDER_ROCKET.to_string();
    }
    if body.contains("[Bank Name]") || body_lower.contains("a/c xxxx") {
        return PROVIDER_BANKS_GENERIC.to_string();
    }

    PROVIDER_BANKS_GENERIC.to_string()
}

//! Bangladesh mobile-number helpers shared by provider and category rules.

/// Returns the canonical operator label used for recharge categorization.
pub fn recharge_operator(phone: &str) -> Option<&'static str> {
    let phone = phone.trim();
    if phone.starts_with("013") || phone.starts_with("017") {
        Some("Grameenphone")
    } else if phone.starts_with("014") || phone.starts_with("019") {
        Some("Banglalink")
    } else if phone.starts_with("015") {
        Some("Teletalk")
    } else if phone.starts_with("016") {
        Some("Airtel")
    } else if phone.starts_with("018") {
        Some("Robi")
    } else {
        None
    }
}

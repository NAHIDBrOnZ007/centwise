//! Field extraction: Date and Time stamps.

use regex::Regex;
use std::sync::LazyLock;

// DD/MM/YY, DD/MM/YYYY, DD-MM-YY, DD-MM-YYYY (with optional time HH:MM or HH:MM:SS)
static DATE_DMY_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(r"([0-9]{2}[/-][0-9]{2}[/-][0-9]{2,4}(?:\s+[0-9]{2}:[0-9]{2}(?::[0-9]{2})?)?)")
        .expect("valid date dmy regex")
});

// YYYY-MM-DD (ISO format, with optional time)
static DATE_ISO_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(r"([0-9]{4}-[0-9]{2}-[0-9]{2}(?:\s+[0-9]{2}:[0-9]{2}(?::[0-9]{2})?)?)")
        .expect("valid date iso regex")
});

// DD MMM YYYY or DD-MMM-YY (e.g. "05 Sep 2026", "05-Sep-26")
static DATE_NAMED_MONTH_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(r"(?i)([0-9]{2}[\s-](?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[\s-][0-9]{2,4}(?:\s+[0-9]{2}:[0-9]{2}(?::[0-9]{2})?)?)")
        .expect("valid date named month regex")
});

pub fn extract_date_time(text: &str) -> Option<String> {
    // 1. Try ISO YYYY-MM-DD first (must be checked before DMY to prevent partial match)
    if let Some(cap) = DATE_ISO_RE.captures(text) {
        if let Some(m) = cap.get(1) {
            return Some(m.as_str().trim().to_string());
        }
    }

    // 2. Try standard DD/MM/YYYY or DD-MM-YYYY (most common in BD SMS)
    if let Some(cap) = DATE_DMY_RE.captures(text) {
        if let Some(m) = cap.get(1) {
            return Some(m.as_str().trim().to_string());
        }
    }

    // 3. Try named month DD MMM YYYY (used by some international banks like SCB)
    if let Some(cap) = DATE_NAMED_MONTH_RE.captures(text) {
        if let Some(m) = cap.get(1) {
            return Some(m.as_str().trim().to_string());
        }
    }

    None
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn extracts_dmy_date_formats() {
        assert_eq!(
            extract_date_time("debited on 22/08/2026. Available"),
            Some("22/08/2026".to_string())
        );
        assert_eq!(
            extract_date_time("on 12/05/26 14:30:15 Bal"),
            Some("12/05/26 14:30:15".to_string())
        );
    }

    #[test]
    fn extracts_iso_date_format() {
        assert_eq!(
            extract_date_time("Transaction on 2026-09-05 at 14:30"),
            Some("2026-09-05".to_string())
        );
        assert_eq!(
            extract_date_time("Credited on 2026-09-05 14:30:00."),
            Some("2026-09-05 14:30:00".to_string())
        );
    }

    #[test]
    fn extracts_named_month_date_format() {
        assert_eq!(
            extract_date_time("Transaction on 05 Sep 2026 at merchant"),
            Some("05 Sep 2026".to_string())
        );
        assert_eq!(
            extract_date_time("Debited on 05-Sep-26 14:30"),
            Some("05-Sep-26 14:30".to_string())
        );
    }
}

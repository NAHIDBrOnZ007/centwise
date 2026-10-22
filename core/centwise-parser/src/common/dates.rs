//! Shared date and time extraction.

use regex::Regex;
use std::sync::LazyLock;

static DATE_DMY_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(r"([0-9]{2}[/-][0-9]{2}[/-][0-9]{2,4}(?:\s+[0-9]{1,2}:[0-9]{2}(?::[0-9]{2})?(?:\s+[APap][Mm])?)?)")
        .expect("valid date dmy regex")
});
static DATE_ISO_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(
        r"([0-9]{4}-[0-9]{2}-[0-9]{2}(?:\s+[0-9]{1,2}:[0-9]{2}(?::[0-9]{2})?(?:\s+[APap][Mm])?)?)",
    )
    .expect("valid date iso regex")
});
static DATE_NAMED_MONTH_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(r"(?i)([0-9]{2}[\s-](?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[\s-][0-9]{2,4}(?:\s+[0-9]{1,2}:[0-9]{2}(?::[0-9]{2})?(?:\s+[APap][Mm])?)?)")
        .expect("valid date named month regex")
});

pub fn extract_date_time(text: &str) -> Option<String> {
    for regex in [&*DATE_ISO_RE, &*DATE_DMY_RE, &*DATE_NAMED_MONTH_RE] {
        if let Some(capture) = regex.captures(text) {
            if let Some(value) = capture.get(1) {
                return Some(value.as_str().trim().to_string());
            }
        }
    }
    None
}

//! Shared transaction-reference extraction.

use regex::Regex;
use std::sync::LazyLock;

static TXN_ID_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(r"(?i)\b(?:Trx\s*ID|Txn\s*ID|Transaction\s*(?:ID|number|no\.?|num)|NPSB\s*Ref|BEFTN\s*Ref|Trx\s*No\.?|Txn\s*No\.?|Ref\s*No\.?|Ref\s*ID|RRN|STAN|Auth(?:orization)?\s+Code)[\s:#]+([A-Za-z0-9.-]+)")
        .expect("valid txn id regex")
});
static GENERIC_REF_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(r"(?i)\b(?:Ref|Reference)[\s:#]+([A-Za-z0-9.-]+)").expect("valid generic ref regex")
});

pub fn extract_reference(text: &str) -> Option<String> {
    for regex in [&*TXN_ID_RE, &*GENERIC_REF_RE] {
        for capture in regex.captures_iter(text) {
            if let Some(value) = capture.get(1) {
                let value = value.as_str().trim().trim_end_matches('.');
                if is_valid(value) {
                    return Some(value.to_string());
                }
            }
        }
    }
    None
}

fn is_valid(value: &str) -> bool {
    !matches!(
        value.to_ascii_lowercase().as_str(),
        "not" | "na" | "none" | "applicable"
    ) && value.len() >= 4
}

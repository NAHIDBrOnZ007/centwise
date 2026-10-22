//! Posted event evidence shared by safety checks and field extraction.

use regex::Regex;
use std::sync::LazyLock;

static REFUND_ACTION_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(r"(?i)\b(?:refund(?:ed)?|reversal|reversed)\b(?:\s+(?:of|Tk|BDT|has|was|is|credited|completed|successful|for)\b|[.,]|$)")
        .expect("valid refund action regex")
});
static POSTED_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(r"(?i)\b(?:(?:has|have)\s+been|was|is)\s+(?:debited|credited|deducted|recovered|added|paid|disbursed)\b|\b(?:payment|recharge|transfer|cash\s+(?:in|out)|send\s+money|refund)[^\r\n]*\b(?:successful|completed)\b|\byou\s+(?:have\s+)?(?:received|purchased|bought|paid)\b|\b(?:credited|debited)\s+(?:with|by)\b")
        .expect("valid posted event regex")
});
static UNPOSTED_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(r"(?i)\b(?:will|would|can|could|pending|requested|reminder|unsuccessful)\b")
        .expect("valid unposted event regex")
});

static NEGATED_COMPLETION_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(r"(?i)\bnot(?:\s+[a-z]+){0,4}\s+(?:credited|debited|refunded|successful|completed|reversed|processed)\b")
        .expect("valid negated completion regex")
});

pub fn has_negated_completion(text: &str) -> bool {
    NEGATED_COMPLETION_RE.is_match(text)
}

pub fn has_posted_receipt(text: &str) -> bool {
    text.split(". ").any(|clause| {
        POSTED_RE.is_match(clause)
            && !UNPOSTED_RE.is_match(clause)
            && !has_negated_completion(clause)
    })
}

pub fn posted_refund_clause(text: &str) -> Option<&str> {
    text.split(". ").find(|clause| {
        REFUND_ACTION_RE.is_match(clause)
            && clause.chars().any(|c| c.is_ascii_digit())
            && !UNPOSTED_RE.is_match(clause)
            && !has_negated_completion(clause)
            && [
                "credited",
                "refunded",
                "completed",
                "successful",
                "reversed",
            ]
            .iter()
            .any(|marker| clause.to_ascii_lowercase().contains(marker))
    })
}

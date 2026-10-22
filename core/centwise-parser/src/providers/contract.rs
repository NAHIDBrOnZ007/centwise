//! Rust-native provider parser contract.

use crate::types::{ParsedTransaction, RejectReason};

/// Input shared by provider parsers after normalization and global safety checks.
#[derive(Debug, Clone, Copy)]
pub struct NormalizedSms<'a> {
    pub body: &'a str,
    pub sender: Option<&'a str>,
}

/// Result of a provider parser attempt.
#[derive(Debug, PartialEq, Eq)]
pub enum ProviderParse {
    Parsed(Box<ParsedTransaction>),
    Rejected(RejectReason),
    NotThisFormat,
}

/// Provider-specific parser boundary.
pub trait ProviderParser {
    fn id(&self) -> &'static str;
    fn can_handle(&self, sms: &NormalizedSms<'_>) -> bool;
    fn parse(&self, sms: &NormalizedSms<'_>) -> ProviderParse;
}

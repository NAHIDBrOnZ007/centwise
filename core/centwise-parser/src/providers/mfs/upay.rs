//! Upay provider boundary.
pub const PROVIDER_ID: &str = "upay";
use super::super::define_common_provider_parser;
define_common_provider_parser!(PROVIDER_ID);

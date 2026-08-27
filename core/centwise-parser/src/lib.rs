//! Core SMS Parser Engine for Centwise.
//!
//! Follows the "Never match templates. Hunt fields." architecture from
//! `docs/architecture/parser-design.md`.

pub mod engine;
pub mod providers;

pub use engine::{parse_sms, ParseOutcome, ParsedTransaction, RejectReason};
pub use providers::detect_provider;

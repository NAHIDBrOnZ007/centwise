//! Text and amount normalization for parsing Bangladeshi bank and MFS SMS.
//!
//! The base layer under `centwise-parser` (see
//! `docs/architecture/parser-design.md`): deterministic, dependency-free,
//! and covered by unit tests derived from real (anonymized) message shapes.

pub mod amount;
pub mod digits;
pub mod text;

pub use amount::{find_amount_tokens, parse_amount_minor};
pub use digits::{bangla_to_english_digits, english_to_bangla_digits};
pub use text::normalize_sms_text;

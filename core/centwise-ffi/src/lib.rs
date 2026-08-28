//! UniFFI surface for the Centwise core.
//!
//! The FFI contract is split by responsibility so the generated Kotlin and
//! Swift bindings remain stable while the Rust bridge stays easy to maintain.

mod conversions;
mod core;
mod error;
mod ingestion;
mod types;

uniffi::setup_scaffolding!();

pub use core::CentwiseCore;
pub use error::{CentwiseError, ChangeListener};
pub use ingestion::{parse_sms_message, ParsedSmsRecord};
pub use types::*;

#[cfg(test)]
mod tests;

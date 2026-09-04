//! Core SMS Parser Engine for Centwise.
//!
//! Follows the 3-Stage Pipeline Architecture:
//! 1. Stage 1: Classify (Fast safety filters: OTPs, telco MB packs, loan ads, low balance notices)
//! 2. Stage 2: Provider Identification (Bangladeshi Banks, MFS, Telcos)
//! 3. Stage 3: Field Extraction (Amounts, types, merchants, references, accounts, dates)

pub mod classify;
pub mod engine;
pub mod extract;
pub mod providers;
pub mod types;

pub use classify::is_likely_financial_review;
pub use engine::parse_sms;
pub use providers::*;
pub use types::*;

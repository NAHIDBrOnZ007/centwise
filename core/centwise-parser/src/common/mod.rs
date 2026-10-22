//! Provider-independent parser primitives.
//!
//! These helpers deliberately contain no provider names or transaction
//! policy. Provider-specific rules belong under `providers/`.

pub mod account;
pub mod amount;
pub mod dates;
pub mod events;
pub mod phone;
pub mod reference;

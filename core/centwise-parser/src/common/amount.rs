//! Shared BDT amount normalization.

/// Parses a normalized BDT amount into integer minor units.
pub fn parse_minor_units(value: &str) -> Option<i64> {
    centwise_normalization::parse_amount_minor(value)
}

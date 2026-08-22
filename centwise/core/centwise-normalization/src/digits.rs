//! Digit conversion between Bengali and ASCII numerals.
//!
//! Bengali digits occupy U+09E6..U+09EF (`০`..`৯`). Conversion is a flat
//! code-point offset, applied character-by-character without touching any
//! other text.

const BENGALI_ZERO: char = '\u{09E6}'; // ০
const BENGALI_NINE: char = '\u{09EF}'; // ৯

/// Replaces every Bengali digit with its ASCII equivalent (০ → 0).
/// All other characters pass through unchanged.
pub fn bangla_to_english_digits(input: &str) -> String {
    input
        .chars()
        .map(|character| {
            if (BENGALI_ZERO..=BENGALI_NINE).contains(&character) {
                char::from(b'0' + (character as u32 - BENGALI_ZERO as u32) as u8)
            } else {
                character
            }
        })
        .collect()
}

/// Replaces every ASCII digit with its Bengali equivalent (0 → ০).
/// All other characters pass through unchanged.
pub fn english_to_bangla_digits(input: &str) -> String {
    input
        .chars()
        .map(|character| {
            if character.is_ascii_digit() {
                char::from_u32(BENGALI_ZERO as u32 + (character as u32 - '0' as u32))
                    .expect("Bengali digit range is contiguous")
            } else {
                character
            }
        })
        .collect()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn converts_bengali_digits_to_ascii() {
        assert_eq!(bangla_to_english_digits("৫,০০০.০০"), "5,000.00");
        assert_eq!(bangla_to_english_digits("৳১২,৫০০.৫০"), "৳12,500.50");
        assert_eq!(bangla_to_english_digits("০১২৩৪৫৬৭৮৯"), "0123456789");
    }

    #[test]
    fn converts_ascii_digits_to_bengali() {
        assert_eq!(english_to_bangla_digits("5,000.00"), "৫,০০০.০০");
        assert_eq!(english_to_bangla_digits("Tk 1250"), "Tk ১২৫০");
    }

    #[test]
    fn leaves_non_digits_untouched() {
        assert_eq!(bangla_to_english_digits("Cash In"), "Cash In");
        assert_eq!(
            bangla_to_english_digits("TrxID 8H9J2K3L4M"),
            "TrxID 8H9J2K3L4M"
        );
    }

    #[test]
    fn round_trips_through_both_directions() {
        let original = "Balance ১২,৪৫০.০০";
        let round_trip = english_to_bangla_digits(&bangla_to_english_digits(original));
        assert_eq!(round_trip, original);
    }

    #[test]
    fn mixed_digits_are_all_converted() {
        assert_eq!(bangla_to_english_digits("৫00"), "500");
    }
}

//! Whole-message normalization applied before any parsing.

/// Normalizes an SMS body for parsing:
/// - converts Bengali digits to ASCII digits
/// - collapses runs of whitespace into single spaces
/// - trims leading/trailing whitespace
///
/// Case, punctuation, and Bengali letters are preserved — merchant names and
/// provider wording must stay intact.
pub fn normalize_sms_text(input: &str) -> String {
    let converted = crate::digits::bangla_to_english_digits(input);
    let mut result = String::with_capacity(converted.len());
    let mut last_was_space = true; // also trims leading spaces

    for character in converted.chars() {
        if character.is_whitespace() {
            if !last_was_space {
                result.push(' ');
                last_was_space = true;
            }
        } else {
            result.push(character);
            last_was_space = false;
        }
    }

    if result.ends_with(' ') {
        result.pop();
    }
    result
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn converts_digits_and_collapses_whitespace() {
        assert_eq!(
            normalize_sms_text("  Cash In ৫,০০০.০০ from   017XXXXXXXX\nsuccessful.  "),
            "Cash In 5,000.00 from 017XXXXXXXX successful."
        );
    }

    #[test]
    fn preserves_case_punctuation_and_bengali_letters() {
        assert_eq!(
            normalize_sms_text("টাকা পাঠিয়েছেন TrxID AB12CD"),
            "টাকা পাঠিয়েছেন TrxID AB12CD"
        );
    }

    #[test]
    fn empty_and_whitespace_only_inputs() {
        assert_eq!(normalize_sms_text(""), "");
        assert_eq!(normalize_sms_text("   \n\t "), "");
    }
}

//! Whole-message normalization applied before any parsing.

/// Normalizes an SMS body for parsing:
/// - collapses runs of whitespace into single spaces
/// - trims leading/trailing whitespace
pub fn normalize_sms_text(input: &str) -> String {
    let mut result = String::with_capacity(input.len());
    let mut last_was_space = true; // also trims leading spaces

    for character in input.chars() {
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
    fn collapses_whitespace() {
        assert_eq!(
            normalize_sms_text("  Cash In 5,000.00 from   017XXXXXXXX\nsuccessful.  "),
            "Cash In 5,000.00 from 017XXXXXXXX successful."
        );
    }

    #[test]
    fn empty_and_whitespace_only_inputs() {
        assert_eq!(normalize_sms_text(""), "");
        assert_eq!(normalize_sms_text("   \n\t "), "");
    }
}

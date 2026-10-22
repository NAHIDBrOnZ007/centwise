//! Compatibility facade for the shared date primitive.

pub use crate::common::dates::extract_date_time;

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn extracts_dmy_date_formats() {
        assert_eq!(
            extract_date_time("debited on 22/08/2026. Available"),
            Some("22/08/2026".to_string())
        );
        assert_eq!(
            extract_date_time("on 12/05/26 14:30:15 Bal"),
            Some("12/05/26 14:30:15".to_string())
        );
    }

    #[test]
    fn extracts_iso_date_format() {
        assert_eq!(
            extract_date_time("Transaction on 2026-09-05 at 14:30"),
            Some("2026-09-05".to_string())
        );
        assert_eq!(
            extract_date_time("Credited on 2026-09-05 14:30:00."),
            Some("2026-09-05 14:30:00".to_string())
        );
    }

    #[test]
    fn extracts_named_month_date_format() {
        assert_eq!(
            extract_date_time("Transaction on 05 Sep 2026 at merchant"),
            Some("05 Sep 2026".to_string())
        );
        assert_eq!(
            extract_date_time("Debited on 05-Sep-26 14:30"),
            Some("05-Sep-26 14:30".to_string())
        );
    }
}

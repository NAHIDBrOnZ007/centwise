use centwise_parser::{parse_sms, ParseOutcome};
use std::fs::File;
use std::io::{BufRead, BufReader};
use std::path::{Path, PathBuf};

fn audit_fixture(folder: &str, filename: &str) -> PathBuf {
    Path::new(env!("CARGO_MANIFEST_DIR"))
        .join("../..")
        .join("fixtures/audits")
        .join(folder)
        .join(filename)
}

fn parse_csv_rows(path: &Path) -> Vec<Vec<String>> {
    let file = File::open(path).expect("audit fixture must exist");
    let reader = BufReader::new(file);
    let mut rows = Vec::new();
    let mut record = Vec::new();

    for line in reader.lines() {
        record.push(line.expect("audit fixture must be readable"));
        let quotes: usize = record.iter().map(|line| line.matches('"').count()).sum();
        if !quotes.is_multiple_of(2) {
            continue;
        }

        let full = record.join("\n");
        record.clear();
        let mut fields = Vec::new();
        let mut field = String::new();
        let mut quoted = false;
        let mut chars = full.chars().peekable();
        while let Some(character) = chars.next() {
            if character == '"' {
                if quoted && chars.peek() == Some(&'"') {
                    field.push('"');
                    chars.next();
                } else {
                    quoted = !quoted;
                }
            } else if character == ',' && !quoted {
                fields.push(field.trim().to_string());
                field.clear();
            } else {
                field.push(character);
            }
        }
        fields.push(field.trim().to_string());
        rows.push(fields);
    }
    rows
}

fn audit_file(
    folder: &str,
    filename: &str,
    sender_column: usize,
    body_column: usize,
    expected_parsed: usize,
    expected_rejected: usize,
) {
    let rows = parse_csv_rows(&audit_fixture(folder, filename));
    assert!(rows.len() > 1, "{} must contain data rows", filename);

    let mut parsed = 0;
    let mut rejected = 0;
    for row in rows.iter().skip(1) {
        if row.len() <= body_column || row[sender_column].is_empty() || row[body_column].is_empty()
        {
            continue;
        }
        match parse_sms(&row[body_column], Some(&row[sender_column])) {
            ParseOutcome::Parsed(_) => parsed += 1,
            ParseOutcome::Rejected(_) => rejected += 1,
        }
    }

    println!("{filename}: parsed={parsed}, rejected={rejected}");
    assert!(
        parsed + rejected > 0,
        "{} must produce audit outcomes",
        filename
    );
    assert_eq!(
        parsed, expected_parsed,
        "{} parsed baseline changed",
        filename
    );
    assert_eq!(
        rejected, expected_rejected,
        "{} rejection baseline changed",
        filename
    );
}

#[test]
fn all_sixteen_audit_reports_are_loaded_and_parsed() {
    for filename in [
        "transaction-report-01.csv",
        "transaction-report-02.csv",
        "transaction-report-03.csv",
        "transaction-report-04.csv",
        "transaction-report-05.csv",
        "transaction-report-06.csv",
        "transaction-report-07.csv",
        "transaction-report-08.csv",
        "transaction-report-09.csv",
        "transaction-report-10.csv",
        "transaction-report-11.csv",
    ] {
        let expected = match filename {
            "transaction-report-01.csv" => (647, 29),
            "transaction-report-02.csv" => (647, 18),
            "transaction-report-03.csv" => (557, 8),
            "transaction-report-04.csv" => (109, 11),
            "transaction-report-05.csv" => (102, 9),
            "transaction-report-06.csv" => (98, 32),
            "transaction-report-07.csv" => (128, 4),
            "transaction-report-08.csv" => (143, 9),
            "transaction-report-09.csv" => (196, 10),
            "transaction-report-10.csv" => (143, 4),
            "transaction-report-11.csv" => (565, 5),
            _ => unreachable!(),
        };
        audit_file(
            "transaction-reports",
            filename,
            5,
            8,
            expected.0,
            expected.1,
        );
    }
    for filename in [
        "review-queue-report-01.csv",
        "review-queue-report-02.csv",
        "review-queue-report-03.csv",
        "review-queue-report-04.csv",
        "review-queue-report-05.csv",
    ] {
        let expected = match filename {
            "review-queue-report-01.csv" => (384, 30),
            "review-queue-report-02.csv" => (8, 7),
            "review-queue-report-03.csv" => (295, 151),
            "review-queue-report-04.csv" => (6, 4),
            "review-queue-report-05.csv" => (311, 44),
            _ => unreachable!(),
        };
        audit_file(
            "review-queue-reports",
            filename,
            1,
            7,
            expected.0,
            expected.1,
        );
    }
}

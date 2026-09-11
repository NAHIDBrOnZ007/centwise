use centwise_parser::{parse_sms, ParseOutcome};
use std::fs::File;
use std::io::{BufRead, BufReader};
use std::path::{Path, PathBuf};

fn audit_fixture(report_type: &str, filename: &str) -> PathBuf {
    Path::new(env!("CARGO_MANIFEST_DIR"))
        .join("../..")
        .join("fixtures/audits")
        .join(report_type)
        .join(filename)
}

fn parse_csv_rows(path: &Path) -> Vec<Vec<String>> {
    let file = match File::open(path) {
        Ok(f) => f,
        Err(_) => return Vec::new(),
    };
    let reader = BufReader::new(file);
    let mut rows = Vec::new();
    let mut current_record = Vec::new();

    for line_res in reader.lines() {
        let line = line_res.unwrap();
        current_record.push(line);

        let quote_count: usize = current_record.iter().map(|s| s.matches('"').count()).sum();
        if !quote_count.is_multiple_of(2) {
            continue;
        }

        let full_row = current_record.join("\n");
        current_record.clear();

        let mut fields = Vec::new();
        let mut cur_field = String::new();
        let mut inside = false;
        let mut chars = full_row.chars().peekable();
        while let Some(c) = chars.next() {
            if c == '"' {
                if inside && chars.peek() == Some(&'"') {
                    cur_field.push('"');
                    chars.next();
                } else {
                    inside = !inside;
                }
            } else if c == ',' && !inside {
                fields.push(cur_field.trim().to_string());
                cur_field.clear();
            } else {
                cur_field.push(c);
            }
        }
        fields.push(cur_field.trim().to_string());
        rows.push(fields);
    }
    rows
}

#[test]
fn test_audit_transaction_report_08() {
    let rows = parse_csv_rows(&audit_fixture(
        "transaction-reports",
        "transaction-report-08.csv",
    ));
    assert!(!rows.is_empty(), "transaction report 08 fixture must exist");
    println!("Transaction report 08 rows parsed from CSV: {}", rows.len());
    let mut total = 0;
    let mut parsed = 0;
    let mut rejected = 0;

    for (idx, record) in rows.iter().enumerate().skip(1) {
        if record.len() < 9 {
            continue;
        }
        total += 1;
        let expected_amount = &record[2];
        let expected_type = &record[3];
        let raw_sms = &record[8];
        let sender = &record[5];

        match parse_sms(raw_sms, Some(sender)) {
            ParseOutcome::Parsed(p) => {
                parsed += 1;
                let amt_str = format!("{:.2}", p.amount_minor as f64 / 100.0);
                if amt_str != *expected_amount {
                    println!(
                        "Row {} AMT MISMATCH: expected {}, got {} | SMS: {}",
                        idx + 1,
                        expected_amount,
                        amt_str,
                        raw_sms
                    );
                }
            }
            ParseOutcome::Rejected(reason) => {
                rejected += 1;
                println!(
                    "Row {} REJECTED: {:?} | Expected: {} {} | SMS: {}",
                    idx + 1,
                    reason,
                    expected_amount,
                    expected_type,
                    raw_sms
                );
            }
        }
    }
    println!(
        "\nTRANSACTION REPORT 08 AUDIT: Total: {}, Parsed: {}, Rejected: {}",
        total, parsed, rejected
    );
}

#[test]
fn test_audit_review_queue_report_03() {
    let rows = parse_csv_rows(&audit_fixture(
        "review-queue-reports",
        "review-queue-report-03.csv",
    ));
    assert!(
        !rows.is_empty(),
        "review queue report 03 fixture must exist"
    );
    println!(
        "Review queue report 03 rows parsed from CSV: {}",
        rows.len()
    );
    let mut total = 0;
    let mut parsed = 0;
    let mut rejected = 0;

    for (idx, record) in rows.iter().enumerate().skip(1) {
        if record.len() < 8 {
            continue;
        }
        total += 1;
        let sender = &record[1];
        let _reason = &record[2];
        let raw_sms = &record[7];

        match parse_sms(raw_sms, Some(sender)) {
            ParseOutcome::Parsed(p) => {
                parsed += 1;
                println!(
                    "[R8 PARSED]: Row {} | Provider: {} | Amt: {:.2} | Type: {:?} | SMS: {}",
                    idx + 1,
                    p.provider_id,
                    p.amount_minor as f64 / 100.0,
                    p.transaction_type,
                    raw_sms.replace('\n', " ")
                );
            }
            ParseOutcome::Rejected(r) => {
                rejected += 1;
                let queues =
                    matches!(
                        r,
                        centwise_parser::RejectReason::NoAmountFound
                            | centwise_parser::RejectReason::NotATransaction
                            | centwise_parser::RejectReason::UnsupportedProvider
                    ) && centwise_parser::is_likely_financial_review(raw_sms, Some(sender));
                println!(
                    "[R8 REJECTED]: Row {} | Sender: {} | Reason: {:?} | Queued: {} | SMS: {}",
                    idx + 1,
                    sender,
                    r,
                    queues,
                    raw_sms.replace('\n', " ")
                );
            }
        }
    }
    println!(
        "\nREVIEW QUEUE REPORT 03 AUDIT: Total: {}, Parsed: {}, Rejected: {}",
        total, parsed, rejected
    );
}

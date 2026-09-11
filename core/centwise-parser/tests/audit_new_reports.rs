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
        for c in full_row.chars() {
            if c == '"' {
                inside = !inside;
            } else if c == ',' && !inside {
                fields.push(cur_field.trim().to_string());
                cur_field = String::new();
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
fn test_audit_transaction_reports_09_and_10() {
    for filename in &["transaction-report-09.csv", "transaction-report-10.csv"] {
        let path = audit_fixture("transaction-reports", filename);
        let rows = parse_csv_rows(&path);
        assert!(!rows.is_empty(), "Failed to read {}", filename);

        println!("\n=======================================================");
        println!("AUDITING DETECTED REPORT: {}", filename);
        println!("=======================================================");

        let mut total = 0;
        let mut parsed_count = 0;
        let mut rejected_count = 0;
        let mut transfer_count = 0;
        let mut shopping_count = 0;
        let mut recharge_count = 0;

        for (idx, row) in rows.iter().enumerate().skip(1) {
            if row.len() < 9 {
                continue;
            }
            total += 1;
            let csv_method = &row[5];
            let raw_sms = &row[8];

            let outcome = parse_sms(raw_sms, Some(csv_method.as_str()));
            match outcome {
                ParseOutcome::Parsed(tx) => {
                    parsed_count += 1;
                    if tx.transaction_type == centwise_domain::TransactionType::Transfer {
                        transfer_count += 1;
                    }
                    if tx.category_id.as_deref() == Some("shopping") {
                        shopping_count += 1;
                    }
                    if tx.category_id.as_deref() == Some("recharge") {
                        recharge_count += 1;
                    }
                }
                ParseOutcome::Rejected(reason) => {
                    rejected_count += 1;
                    println!(
                        "[REJECTED IN DETECTED REPORT]: Row {} | Reason: {:?} | SMS: {}",
                        idx + 1,
                        reason,
                        raw_sms.replace('\n', " ")
                    );
                }
            }
        }

        println!("RESULT {}: Total: {}, Parsed: {}, Rejected: {}, Transfers: {}, Shopping: {}, Recharge: {}",
            filename, total, parsed_count, rejected_count, transfer_count, shopping_count, recharge_count);

        if filename == &"transaction-report-09.csv" {
            // Row 174 (KHADIJA absent notice) should now be rejected as NotATransaction!
            assert_eq!(
                rejected_count, 1,
                "Expected exactly 1 rejected row in transaction report 09 (the absent notice)"
            );
            assert_eq!(
                parsed_count, 205,
                "Expected 205 legitimate transactions parsed in transaction report 09"
            );
        } else if filename == &"transaction-report-10.csv" {
            assert_eq!(rejected_count, 0, "All rows in report 3 should parse");
            assert_eq!(parsed_count, 147, "Expected 147 parsed in transaction report 10");
            // MFS transfers should now be correctly recognized as Transfers!
            assert!(
                transfer_count >= 30,
                "MFS transfers should now be categorized as Transfers"
            );
        }
    }
}

#[test]
fn test_audit_review_queue_reports_01_and_02() {
    let q1_path = audit_fixture("review-queue-reports", "review-queue-report-01.csv");
    let q2_path = audit_fixture("review-queue-reports", "review-queue-report-02.csv");

    let rows_q1 = parse_csv_rows(&q1_path);
    let rows_q2 = parse_csv_rows(&q2_path);

    println!("\n=======================================================");
    println!("AUDITING NEW QUEUE REPORTS AFTER FIXES");
    println!("=======================================================");

    let mut q1_parsed = 0;
    let mut q1_rejected = 0;
    let mut q1_teletalk_count = 0;
    let mut q1_telecharge_sender_count = 0;

    for (idx, row) in rows_q1.iter().enumerate().skip(1) {
        if row.len() < 8 {
            continue;
        }
        let sender = &row[1];
        let raw_sms = &row[7];

        if sender.eq_ignore_ascii_case("telecharge") {
            q1_telecharge_sender_count += 1;
        }

        let outcome = parse_sms(raw_sms, Some(sender.as_str()));
        match outcome {
            ParseOutcome::Parsed(tx) => {
                q1_parsed += 1;
                if tx.provider_id == "teletalk" {
                    q1_teletalk_count += 1;
                }
            }
            ParseOutcome::Rejected(reason) => {
                q1_rejected += 1;
                println!(
                    "[QUEUE 1 REJECTED]: Row {} | Sender: {} | Reason: {:?} | SMS: {}",
                    idx + 1,
                    sender,
                    reason,
                    raw_sms.replace('\n', " ")
                );
            }
        }
    }

    println!("\nREPORT 1 RESULTS: Total: {}, Parsed: {}, Rejected: {}, Telecharge Senders: {}, Teletalk Recognized: {}",
        rows_q1.len().saturating_sub(1), q1_parsed, q1_rejected, q1_telecharge_sender_count, q1_teletalk_count);

    let mut q2_parsed = 0;
    let mut q2_rejected = 0;

    for (idx, row) in rows_q2.iter().enumerate().skip(1) {
        if row.len() < 8 {
            continue;
        }
        let sender = &row[1];
        let raw_sms = &row[7];

        let outcome = parse_sms(raw_sms, Some(sender.as_str()));
        match outcome {
            ParseOutcome::Parsed(tx) => {
                q2_parsed += 1;
                println!("[QUEUE 2 PARSED]: Row {} | Provider: {} | Amt: {} | Type: {:?} | Cat: {:?} | Party: {:?}",
                    idx + 1, tx.provider_id, (tx.amount_minor as f64) / 100.0, tx.transaction_type, tx.category_id, tx.party);
            }
            ParseOutcome::Rejected(reason) => {
                q2_rejected += 1;
                println!(
                    "[QUEUE 2 REJECTED]: Row {} | Sender: {} | Reason: {:?} | SMS: {}",
                    idx + 1,
                    sender,
                    reason,
                    raw_sms.replace('\n', " ")
                );
            }
        }
    }

    println!(
        "\nREPORT 2 RESULTS: Total: {}, Parsed: {}, Rejected: {}",
        rows_q2.len().saturating_sub(1),
        q2_parsed,
        q2_rejected
    );

    // In Report 2: Exactly 8 ATM Cash WD should be parsed, and exactly 7 non-financial notices rejected!
    assert_eq!(
        q2_parsed, 8,
        "Expected exactly 8 parsed Cash WD transactions in report 2"
    );
    assert_eq!(
        q2_rejected, 7,
        "Expected exactly 7 rejected non-financial notices in report 2"
    );

    // In Report 1: 390 real transactions parsed, and exactly 24 non-financial notices rejected!
    assert_eq!(
        q1_parsed, 390,
        "Expected 390 parsed transactions in queue report 1"
    );
    assert_eq!(
        q1_rejected, 24,
        "Expected 24 non-financial notices rejected in queue report 1"
    );
    assert_eq!(
        q1_teletalk_count, q1_telecharge_sender_count,
        "All Telecharge SMS should resolve to teletalk!"
    );
    assert_eq!(
        q1_teletalk_count, 236,
        "Exact 236 Telecharge SMS in queue report 1"
    );
}

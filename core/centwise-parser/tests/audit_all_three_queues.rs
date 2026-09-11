use centwise_parser::{is_likely_financial_review, parse_sms, ParseOutcome, RejectReason};
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

#[derive(Default)]
struct QueueAuditStats {
    total: usize,
    parsed_real_tx: usize,
    rejected_non_tx: usize,
    unwanted_ignored: usize,
    reasons: std::collections::BTreeMap<String, usize>,
    unparsed_queued_for_review: Vec<(usize, String, String)>,
}

fn audit_file(path: &Path, sender_col: usize, body_col: usize) -> QueueAuditStats {
    let rows = parse_csv_rows(path);
    let mut stats = QueueAuditStats::default();

    for (idx, r) in rows.iter().enumerate().skip(1) {
        if r.len() <= sender_col || r.len() <= body_col {
            continue;
        }
        stats.total += 1;
        let sender = &r[sender_col];
        let body = &r[body_col];

        match parse_sms(body, Some(sender)) {
            ParseOutcome::Parsed(_) => {
                stats.parsed_real_tx += 1;
            }
            ParseOutcome::Rejected(reason) => {
                stats.rejected_non_tx += 1;
                let reason_str = format!("{:?}", reason);
                *stats.reasons.entry(reason_str).or_insert(0) += 1;
                let queues = matches!(
                    reason,
                    RejectReason::NoAmountFound
                        | RejectReason::NotATransaction
                        | RejectReason::UnsupportedProvider
                ) && is_likely_financial_review(body, Some(sender));

                if queues {
                    stats.unparsed_queued_for_review.push((
                        idx + 1,
                        sender.to_string(),
                        body.replace('\n', " "),
                    ));
                } else {
                    stats.unwanted_ignored += 1;
                }
            }
        }
    }
    stats
}

#[test]
fn test_all_three_review_queues_deep_audit() {
    println!("\n=======================================================");
    println!("AUDITING REVIEW QUEUE REPORT: review-queue-report-01.csv");
    println!("=======================================================");
    let q1 = audit_file(
        &audit_fixture("review-queue-reports", "review-queue-report-01.csv"),
        1,
        7,
    );
    println!("Total Rows: {}", q1.total);
    println!("Parsed Real Transactions: {}", q1.parsed_real_tx);
    println!("Rejected Non-Transactions: {}", q1.rejected_non_tx);
    println!("Rejection Breakdown: {:?}", q1.reasons);
    println!(
        "Unwanted Safely Ignored (Never Queued): {}",
        q1.unwanted_ignored
    );
    println!(
        "Unparsed Items Queued for Review: {}",
        q1.unparsed_queued_for_review.len()
    );
    for (row, sender, sms) in &q1.unparsed_queued_for_review {
        println!("  -> Queued Row {} [{}]: {}", row, sender, sms);
    }

    println!("\n=======================================================");
    println!("AUDITING REVIEW QUEUE REPORT: review-queue-report-02.csv");
    println!("=======================================================");
    let q2 = audit_file(
        &audit_fixture("review-queue-reports", "review-queue-report-02.csv"),
        1,
        7,
    );
    println!("Total Rows: {}", q2.total);
    println!("Parsed Real Transactions: {}", q2.parsed_real_tx);
    println!("Rejected Non-Transactions: {}", q2.rejected_non_tx);
    println!("Rejection Breakdown: {:?}", q2.reasons);
    println!(
        "Unwanted Safely Ignored (Never Queued): {}",
        q2.unwanted_ignored
    );
    println!(
        "Unparsed Items Queued for Review: {}",
        q2.unparsed_queued_for_review.len()
    );
    for (row, sender, sms) in &q2.unparsed_queued_for_review {
        println!("  -> Queued Row {} [{}]: {}", row, sender, sms);
    }

    println!("\n=======================================================");
    println!("AUDITING REVIEW QUEUE REPORT: review-queue-report-03.csv");
    println!("=======================================================");
    let q3 = audit_file(
        &audit_fixture("review-queue-reports", "review-queue-report-03.csv"),
        1,
        7,
    );
    println!("Total Rows: {}", q3.total);
    println!("Parsed Real Transactions: {}", q3.parsed_real_tx);
    println!("Rejected Non-Transactions: {}", q3.rejected_non_tx);
    println!("Rejection Breakdown: {:?}", q3.reasons);
    println!(
        "Unwanted Safely Ignored (Never Queued): {}",
        q3.unwanted_ignored
    );
    println!(
        "Unparsed Items Queued for Review: {}",
        q3.unparsed_queued_for_review.len()
    );
    for (row, sender, sms) in &q3.unparsed_queued_for_review {
        println!("  -> Queued Row {} [{}]: {}", row, sender, sms);
    }

    // Assertions
    // All 390 real transactions in Queue 1 must parse
    assert_eq!(q1.parsed_real_tx, 390);
    // All 24 unwanted items in Queue 1 must be ignored (0 queued)
    assert_eq!(q1.unparsed_queued_for_review.len(), 0);

    // All 8 real transactions in Queue 2 must parse
    assert_eq!(q2.parsed_real_tx, 8);
    // All 7 unwanted items in Queue 2 must be ignored (0 queued)
    assert_eq!(q2.unparsed_queued_for_review.len(), 0);

    // All 385 real transactions in Queue 3 must parse
    assert_eq!(q3.parsed_real_tx, 385);
    // All 61 unwanted noise items in Queue 3 must be ignored (0 queued)
    assert_eq!(q3.unparsed_queued_for_review.len(), 0);
}

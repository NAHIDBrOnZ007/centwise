use centwise_domain::TransactionType;
use centwise_parser::{is_likely_financial_review, parse_sms, ParseOutcome, RejectReason};
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
fn test_audit_review_queue_report_04() {
    let path = audit_fixture("review-queue-reports", "review-queue-report-04.csv");
    let rows = parse_csv_rows(&path);
    assert!(
        !rows.is_empty(),
        "review-queue-report-04.csv fixture must exist"
    );

    println!("\n=======================================================");
    println!(
        "AUDITING REVIEW QUEUE REPORT 04 ({} rows)",
        rows.len().saturating_sub(1)
    );
    println!("=======================================================");

    let mut parsed_tx_count = 0;
    let mut rejected_count = 0;
    let mut queued_for_review = Vec::new();

    for (idx, r) in rows.iter().enumerate().skip(1) {
        if r.len() < 8 {
            continue;
        }
        let sender = &r[1];
        let raw_sms = &r[7];

        let outcome = parse_sms(raw_sms, Some(sender));
        match outcome {
            ParseOutcome::Parsed(tx) => {
                parsed_tx_count += 1;
                println!(
                    "[PARSED TX] Row {}: provider={}, amount={:.2}, type={:?}, party={:?}, merchant={:?}, cat={:?}, ref={:?}, bal={:?}",
                    idx + 1,
                    tx.provider_id,
                    tx.amount_minor as f64 / 100.0,
                    tx.transaction_type,
                    tx.party,
                    tx.merchant,
                    tx.category_id,
                    tx.reference,
                    tx.balance_after_minor.map(|b| b as f64 / 100.0)
                );

                if idx + 1 == 4 {
                    // Row 4: Shohoj reservation payment
                    assert_eq!(tx.provider_id, "bkash");
                    assert_eq!(tx.amount_minor, 42_000);
                    assert_eq!(tx.transaction_type, TransactionType::Expense);
                    assert_eq!(tx.party.as_deref(), Some("Shohoj Limited-1-RM46212"));
                    assert_eq!(tx.merchant.as_deref(), Some("Shohoz"));
                    assert_eq!(tx.category_id.as_deref(), Some("transport"));
                    assert_eq!(tx.reference.as_deref(), Some("DGP0P76EOO"));
                    assert_eq!(tx.balance_after_minor, Some(18_494));
                }
            }
            ParseOutcome::Rejected(reason) => {
                rejected_count += 1;
                let queues = matches!(
                    reason,
                    RejectReason::NoAmountFound
                        | RejectReason::NotATransaction
                        | RejectReason::UnsupportedProvider
                ) && is_likely_financial_review(raw_sms, Some(sender));

                if queues {
                    queued_for_review.push((
                        idx + 1,
                        sender.to_string(),
                        raw_sms.replace('\n', " "),
                    ));
                }

                println!(
                    "[REJECTED NON-TX] Row {}: reason={:?}, queued_for_review={}, sms={}",
                    idx + 1,
                    reason,
                    queues,
                    raw_sms.replace('\n', " ")
                );
            }
        }
    }

    println!("\nAUDIT SUMMARY FOR REPORT 04:");
    println!("Total Rows: {}", rows.len().saturating_sub(1));
    println!("Parsed Legitimate Transactions: {}", parsed_tx_count);
    println!(
        "Safely Rejected Non-Transactions / Spam: {}",
        rejected_count
    );
    println!("Items Queued for Human Review: {}", queued_for_review.len());

    for (row, sender, sms) in &queued_for_review {
        println!("  -> Unexpectedly queued Row {} [{}]: {}", row, sender, sms);
    }

    // All 6 legitimate transactions in Report 04 must parse cleanly
    assert_eq!(
        parsed_tx_count, 6,
        "Expected exactly 6 parsed transactions in report 04"
    );

    // All 4 non-transaction / promotional notices must be rejected
    assert_eq!(
        rejected_count, 4,
        "Expected exactly 4 rejected noise rows in report 04"
    );

    // ZERO noise items should enter the review queue
    assert_eq!(
        queued_for_review.len(),
        0,
        "No non-transaction or promotional spam should enter the review queue"
    );
}

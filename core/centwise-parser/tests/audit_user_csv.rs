use centwise_parser::{parse_sms, ParseOutcome};
use std::fs::File;
use std::io::{BufRead, BufReader};

fn audit_csv_path(path: &str) -> Option<(usize, usize, usize, usize)> {
    let file = File::open(path).ok()?;
    let reader = BufReader::new(file);

    let mut lines = reader.lines();
    let _header = lines.next()?.ok()?;

    let mut count = 0;
    let mut parsed_ok = 0;
    let mut rejected = 0;
    let mut amount_diff_count = 0;

    let mut current_record = Vec::new();

    let mut total_income_minor: i64 = 0;
    let mut total_expense_minor: i64 = 0;
    let mut total_transfer_minor: i64 = 0;
    let mut total_refund_minor: i64 = 0;

    for line_res in lines {
        let line = line_res.unwrap();
        // Handle multiline CSV rows
        current_record.push(line.clone());

        let quote_count: usize = current_record.iter().map(|s| s.matches('"').count()).sum();
        if !quote_count.is_multiple_of(2) {
            // still inside multiline quote
            continue;
        }

        let full_row = current_record.join("\n");
        current_record.clear();

        let mut fields = Vec::new();
        let mut cur_field = String::new();
        let mut inside = false;
        let chars = full_row.chars();
        for c in chars {
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

        if fields.len() < 9 {
            continue;
        }

        count += 1;
        let csv_amount_str = &fields[2];
        let csv_method = &fields[5];
        let raw_sms = &fields[8];

        let csv_amount_minor =
            (csv_amount_str.parse::<f64>().unwrap_or(0.0) * 100.0).round() as i64;

        let outcome = parse_sms(raw_sms, Some(csv_method.as_str()));
        match outcome {
            ParseOutcome::Parsed(tx) => {
                parsed_ok += 1;
                match tx.transaction_type {
                    centwise_domain::TransactionType::Income => {
                        total_income_minor += tx.amount_minor;
                    }
                    centwise_domain::TransactionType::Expense => {
                        total_expense_minor += tx.amount_minor;
                    }
                    centwise_domain::TransactionType::Transfer => {
                        total_transfer_minor += tx.amount_minor;
                    }
                    centwise_domain::TransactionType::Refund => {
                        total_refund_minor += tx.amount_minor;
                    }
                }
                if tx.amount_minor != csv_amount_minor {
                    amount_diff_count += 1;
                    println!(
                        "[AMOUNT DIFF #{}]: CSV: {} ({} minor) | Rust: {} minor ({:.2}) | Raw: {}",
                        amount_diff_count,
                        csv_amount_str,
                        csv_amount_minor,
                        tx.amount_minor,
                        (tx.amount_minor as f64) / 100.0,
                        raw_sms.replace('\n', " ")
                    );
                }
            }
            ParseOutcome::Rejected(reason) => {
                rejected += 1;
                println!(
                    "[REJECTED]: CSV had {} | Reason: {:?} | Raw: {}",
                    csv_amount_str,
                    reason,
                    raw_sms.replace('\n', " ")
                );
            }
        }
    }

    let net_minor = total_income_minor - total_expense_minor;
    println!(
        "\nTOTAL ROWS: {} | Parsed: {} | Amount Diffs: {} | Rejected: {}",
        count, parsed_ok, amount_diff_count, rejected
    );
    println!(
        "RUST TOTALS:\n  Income:   {:.2} BDT\n  Expense:  {:.2} BDT\n  Transfer: {:.2} BDT\n  Refund:   {:.2} BDT\n  NET BAL:  {:+.2} BDT",
        (total_income_minor as f64) / 100.0,
        (total_expense_minor as f64) / 100.0,
        (total_transfer_minor as f64) / 100.0,
        (total_refund_minor as f64) / 100.0,
        (net_minor as f64) / 100.0,
    );
    Some((count, parsed_ok, amount_diff_count, rejected))
}

#[test]
fn test_audit_report_4_csv() {
    let candidates = [
        "/Users/faysal/Documents/centwise/csv report/repoet 4.csv",
        "csv report/repoet 4.csv",
        "../csv report/repoet 4.csv",
    ];
    if let Some((count, parsed, diffs, rejected)) =
        candidates.iter().find_map(|p| audit_csv_path(p))
    {
        assert_eq!(count, 120);
        assert_eq!(diffs, 0, "No amount differences allowed");
        // Two rejected: 1 WhatsApp job scam and 1 educational admission receipt
        assert_eq!(
            rejected, 2,
            "Expected 2 rejected spam/non-transaction messages"
        );
        assert_eq!(parsed, 118, "Expected 118 parsed transactions");
    }
}

#[test]
fn test_audit_report_3_csv() {
    let candidates = [
        "/Users/faysal/Documents/centwise/csv report/report 3.csv",
        "csv report/report 3.csv",
        "../csv report/report 3.csv",
    ];
    if let Some((count, _parsed, diffs, _rejected)) =
        candidates.iter().find_map(|p| audit_csv_path(p))
    {
        assert_eq!(count, 565);
        assert_eq!(diffs, 0, "No amount differences allowed in report 3");
    }
}

#[test]
fn test_audit_report_6_csv() {
    let candidates = [
        "/Users/faysal/Documents/centwise/csv report/report 6.csv",
        "csv report/report 6.csv",
        "../csv report/report 6.csv",
    ];
    if let Some((count, parsed, diffs, rejected)) =
        candidates.iter().find_map(|p| audit_csv_path(p))
    {
        assert_eq!(count, 130);
        assert_eq!(
            diffs, 1,
            "Only 1 difference expected: row 20 account number 20 quadrillion fix"
        );
        assert_eq!(
            rejected, 29,
            "29 rejected: 26 loan reminders, 1 FD renewal, 2 promo ads"
        );
        assert_eq!(parsed, 101, "101 legitimate transactions parsed");
    }
}

#[test]
fn test_audit_report_7_csv() {
    let candidates = [
        "/Users/faysal/Documents/centwise/csv report/report 7.csv",
        "csv report/report 7.csv",
        "../csv report/report 7.csv",
    ];
    if let Some((count, parsed, diffs, rejected)) =
        candidates.iter().find_map(|p| audit_csv_path(p))
    {
        assert_eq!(count, 132);
        assert_eq!(diffs, 0, "No amount differences allowed");
        assert_eq!(
            rejected, 1,
            "Expected 1 rejected non-transaction (insurance expiry notice)"
        );
        assert_eq!(parsed, 131, "Expected 131 legitimate transactions parsed");
    }
}

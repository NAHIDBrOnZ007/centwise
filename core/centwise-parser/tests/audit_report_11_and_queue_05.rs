use centwise_parser::{is_likely_financial_review, parse_sms, ParseOutcome, RejectReason};
use std::collections::BTreeMap;
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
        Err(e) => {
            eprintln!("Failed to open {}: {}", path.display(), e);
            return Vec::new();
        }
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
fn test_audit_new_csvs() {
    let tx_path = audit_fixture("transaction-reports", "transaction-report-011.csv");
    let queue_path = audit_fixture("review-queue-reports", "review-qeue-report-05.csv");

    println!("=======================================================");
    println!("AUDITING TRANSACTION REPORT 011: {}", tx_path.display());
    println!("=======================================================");

    let tx_rows = parse_csv_rows(&tx_path);
    println!("Total rows in transaction-report-011: {}", tx_rows.len());

    let mut tx_parsed = 0;
    let mut tx_rejected = 0;
    let mut tx_rejected_reasons: BTreeMap<String, Vec<(usize, String, String)>> = BTreeMap::new();
    let mut tx_parsed_discrepancies: Vec<String> = Vec::new();

    for (idx, r) in tx_rows.iter().enumerate().skip(1) {
        if r.len() < 9 {
            continue;
        }
        // Date,Title,Amount,Type,Category,Payment Method,Reference,Note,Raw SMS
        let expected_amount = &r[2];
        let expected_type = &r[3];
        let sender = &r[5];
        let expected_ref = &r[6];
        let raw_sms = &r[8];

        if raw_sms.trim().is_empty() {
            continue;
        }

        let outcome = parse_sms(raw_sms, Some(sender));
        match outcome {
            ParseOutcome::Parsed(tx) => {
                tx_parsed += 1;
                // Check discrepancies against expected
                if !expected_amount.is_empty() {
                    let parsed_amt = format!("{:.2}", tx.amount_minor as f64 / 100.0);
                    if let Ok(exp_f) = expected_amount.parse::<f64>() {
                        let exp_amt = format!("{:.2}", exp_f);
                        if parsed_amt != exp_amt {
                            tx_parsed_discrepancies.push(format!(
                                "Row {}: Amount mismatch - parsed {}, expected {}. SMS: {}",
                                idx + 1,
                                parsed_amt,
                                exp_amt,
                                raw_sms.replace('\n', " ")
                            ));
                        }
                    }
                }
                if !expected_type.is_empty() {
                    let parsed_type = format!("{:?}", tx.transaction_type);
                    let exp_type = expected_type.as_str();
                    if parsed_type != exp_type && !(parsed_type == "Income" && exp_type == "Credit")
                    {
                        tx_parsed_discrepancies.push(format!(
                            "Row {}: Type mismatch - parsed {}, expected {}. SMS: {}",
                            idx + 1,
                            parsed_type,
                            exp_type,
                            raw_sms.replace('\n', " ")
                        ));
                    }
                }
                if !expected_ref.is_empty()
                    && tx.reference.as_deref() != Some(expected_ref.as_str())
                {
                    tx_parsed_discrepancies.push(format!(
                        "Row {}: Reference mismatch - parsed {:?}, expected {:?}. SMS: {}",
                        idx + 1,
                        tx.reference,
                        expected_ref,
                        raw_sms.replace('\n', " ")
                    ));
                }
            }
            ParseOutcome::Rejected(reason) => {
                tx_rejected += 1;
                tx_rejected_reasons
                    .entry(format!("{:?}", reason))
                    .or_default()
                    .push((idx + 1, sender.clone(), raw_sms.clone()));
            }
        }
    }

    println!("Transaction Report 011 Parsed: {}", tx_parsed);
    println!("Transaction Report 011 Rejected: {}", tx_rejected);
    println!(
        "Transaction Report 011 Discrepancies: {}",
        tx_parsed_discrepancies.len()
    );
    for d in tx_parsed_discrepancies.iter().take(20) {
        println!("  {}", d);
    }
    for (reason, items) in &tx_rejected_reasons {
        println!("  Rejected reason {:?}: {} items", reason, items.len());
        for (row, sender, sms) in items.iter().take(5) {
            println!("    Row {} [{}]: {}", row, sender, sms.replace('\n', " "));
        }
    }

    println!("\n=======================================================");
    println!("AUDITING REVIEW QUEUE REPORT 05: {}", queue_path.display());
    println!("=======================================================");

    let q_rows = parse_csv_rows(&queue_path);
    println!("Total rows in review-qeue-report-05: {}", q_rows.len());

    let mut q_parsed = 0;
    let mut q_rejected = 0;
    let mut q_queued_for_review = Vec::new();
    let mut q_rejected_reasons: BTreeMap<String, Vec<(usize, String, String)>> = BTreeMap::new();

    let mut q_parsed_discrepancies = Vec::new();
    let mut party_quality_issues = Vec::new();

    for (idx, r) in q_rows.iter().enumerate().skip(1) {
        if r.len() < 8 {
            continue;
        }
        // Date,Sender,Reason,Candidate Amount,Candidate Type,Candidate Party,Reference,Raw SMS
        let sender = &r[1];
        let original_reason = &r[2];
        let exp_amount = &r[3];
        let _exp_type = &r[4];
        let exp_party = &r[5];
        let exp_ref = &r[6];
        let raw_sms = &r[7];

        if raw_sms.trim().is_empty() {
            continue;
        }

        let outcome = parse_sms(raw_sms, Some(sender));
        match outcome {
            ParseOutcome::Parsed(tx) => {
                q_parsed += 1;
                let parsed_amt = format!("{:.2}", tx.amount_minor as f64 / 100.0);
                if !exp_amount.is_empty() {
                    if let Ok(exp_f) = exp_amount.parse::<f64>() {
                        let exp_str = format!("{:.2}", exp_f);
                        if parsed_amt != exp_str {
                            q_parsed_discrepancies.push(format!(
                                "Row {}: Amount mismatch - parsed {}, exp {}. SMS: {}",
                                idx + 1,
                                parsed_amt,
                                exp_str,
                                raw_sms.replace('\n', " ")
                            ));
                        }
                    }
                }
                if !exp_ref.is_empty() && tx.reference.as_deref() != Some(exp_ref.as_str()) {
                    q_parsed_discrepancies.push(format!(
                        "Row {}: Reference mismatch - parsed {:?}, exp {:?}. SMS: {}",
                        idx + 1,
                        tx.reference,
                        exp_ref,
                        raw_sms.replace('\n', " ")
                    ));
                }
                if let Some(party) = &tx.party {
                    let p_lower = party.to_lowercase();
                    if p_lower.starts_with("of ")
                        || p_lower.ends_with(" of")
                        || p_lower == "ibanking of"
                        || p_lower.starts_with("from ")
                        || p_lower.starts_with("to ")
                        || p_lower.ends_with(" is")
                        || p_lower.ends_with(" at")
                        || p_lower.ends_with('.')
                        || p_lower.ends_with(',')
                    {
                        party_quality_issues.push(format!(
                            "Row {}: Bad party '{}' (candidate was '{}'). SMS: {}",
                            idx + 1,
                            party,
                            exp_party,
                            raw_sms.replace('\n', " ")
                        ));
                    }
                }
            }
            ParseOutcome::Rejected(reason) => {
                q_rejected += 1;
                let queues = matches!(
                    reason,
                    RejectReason::NoAmountFound
                        | RejectReason::NotATransaction
                        | RejectReason::UnsupportedProvider
                ) && is_likely_financial_review(raw_sms, Some(sender));

                if queues {
                    q_queued_for_review.push((
                        idx + 1,
                        sender.clone(),
                        original_reason.clone(),
                        reason.clone(),
                        raw_sms.clone(),
                    ));
                }

                q_rejected_reasons
                    .entry(format!("{:?}", reason))
                    .or_default()
                    .push((idx + 1, sender.clone(), raw_sms.clone()));
            }
        }
    }

    println!("Review Queue Report 05 Parsed: {}", q_parsed);
    println!("Review Queue Report 05 Rejected: {}", q_rejected);
    println!(
        "Review Queue Report 05 Discrepancies: {}",
        q_parsed_discrepancies.len()
    );
    for d in q_parsed_discrepancies.iter().take(20) {
        println!("  {}", d);
    }
    println!(
        "Review Queue Report 05 Party Quality Issues: {}",
        party_quality_issues.len()
    );
    for p in party_quality_issues.iter().take(20) {
        println!("  {}", p);
    }
    println!(
        "Review Queue Report 05 Unexpectedly Queued: {}",
        q_queued_for_review.len()
    );
    for (row, sender, orig_reason, reason, sms) in &q_queued_for_review {
        println!(
            "  Row {} [{}] (orig reason: {}): {:?} -> {}",
            row,
            sender,
            orig_reason,
            reason,
            sms.replace('\n', " ")
        );
    }
    for (reason, items) in &q_rejected_reasons {
        println!("  Rejected reason {:?}: {} items", reason, items.len());
        for (row, sender, sms) in items.iter().take(5) {
            println!("    Row {} [{}]: {}", row, sender, sms.replace('\n', " "));
        }
    }
}

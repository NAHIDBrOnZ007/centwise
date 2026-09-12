use centwise_parser::{is_likely_financial_review, parse_sms, ParseOutcome, RejectReason};
use regex::Regex;
use std::collections::BTreeMap;
use std::fs::File;
use std::io::{BufRead, BufReader};
use std::path::Path;
use std::sync::LazyLock;

static URL_RE: LazyLock<Regex> = LazyLock::new(|| Regex::new(r"https?://\S+").unwrap());
static DATE_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(r"\b(?:[0-9]{1,2}[/-][0-9]{1,2}[/-][0-9]{2,4}|[0-9]{1,2}-[A-Za-z]{3}-[0-9]{2,4})\b")
        .unwrap()
});
static TIME_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(r"\b[0-9]{1,2}:[0-9]{2}(?::[0-9]{2})?(?:\s*(?:AM|PM|am|pm))?\b").unwrap()
});
static MASKED_ACC_RE: LazyLock<Regex> =
    LazyLock::new(|| Regex::new(r"[\*xX]{2,}[0-9]{3,4}").unwrap());
static PHONE_RE: LazyLock<Regex> = LazyLock::new(|| Regex::new(r"\b01[3-9][0-9]{8}\b").unwrap());
static TRXID_RE: LazyLock<Regex> =
    LazyLock::new(|| Regex::new(r"(?i)\b(?:TrxID|TxnID|TxnId|Ref)[:\s]+([A-Za-z0-9]+)").unwrap());
static AMT_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(r"(?i)(?:Tk\.?|BDT)\s*-?\s*(?:\.[0-9]{1,2}|[0-9][0-9,]*(?:\.[0-9]{1,2})?)").unwrap()
});
static NUM_RE: LazyLock<Regex> = LazyLock::new(|| Regex::new(r"\b[0-9]+(?:\.[0-9]+)?\b").unwrap());
static WS_RE: LazyLock<Regex> = LazyLock::new(|| Regex::new(r"\s+").unwrap());

fn normalize_to_pattern(text: &str) -> String {
    let mut s = text.to_string();
    s = URL_RE.replace_all(&s, "<URL>").into_owned();
    s = DATE_RE.replace_all(&s, "<DATE>").into_owned();
    s = TIME_RE.replace_all(&s, "<TIME>").into_owned();
    s = TRXID_RE.replace_all(&s, "TrxID <ID>").into_owned();
    s = MASKED_ACC_RE.replace_all(&s, "<ACC>").into_owned();
    s = PHONE_RE.replace_all(&s, "<PHONE>").into_owned();
    s = AMT_RE.replace_all(&s, "<AMT>").into_owned();
    s = NUM_RE.replace_all(&s, "<N>").into_owned();
    s = WS_RE.replace_all(&s, " ").into_owned();
    s.trim().to_lowercase()
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
        let line = match line_res {
            Ok(l) => l,
            Err(_) => continue,
        };
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

#[derive(Debug)]
#[allow(dead_code)]
struct PatternCluster {
    sender: String,
    pattern: String,
    sample_sms: String,
    count: usize,
    sources: Vec<String>,
}

#[test]
fn test_audit_all_csv_reports_clustered() {
    let audit_dir = Path::new(env!("CARGO_MANIFEST_DIR"))
        .join("../..")
        .join("fixtures/audits");

    let tx_reports = [
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
    ];

    let review_reports = [
        "review-queue-report-01.csv",
        "review-queue-report-02.csv",
        "review-queue-report-03.csv",
        "review-queue-report-04.csv",
    ];

    let mut clusters: BTreeMap<(String, String), PatternCluster> = BTreeMap::new();
    let mut total_messages = 0;

    // 1. Ingest Transaction Reports (col 5 is payment method, col 8 is raw sms)
    for filename in &tx_reports {
        let path = audit_dir.join("transaction-reports").join(filename);
        let rows = parse_csv_rows(&path);
        let mut file_count = 0;
        for r in rows.iter().skip(1) {
            if r.len() < 9 {
                continue;
            }
            let sender = r[5].trim().to_string();
            let raw_sms = r[8].trim().to_string();
            if raw_sms.is_empty() {
                continue;
            }
            total_messages += 1;
            file_count += 1;
            let pattern = normalize_to_pattern(&raw_sms);
            let entry = clusters
                .entry((sender.clone(), pattern.clone()))
                .or_insert_with(|| PatternCluster {
                    sender,
                    pattern,
                    sample_sms: raw_sms,
                    count: 0,
                    sources: Vec::new(),
                });
            entry.count += 1;
            if !entry.sources.contains(&filename.to_string()) {
                entry.sources.push(filename.to_string());
            }
        }
        println!("Loaded {}: {} rows", filename, file_count);
    }

    // 2. Ingest Review Queue Reports (col 1 is sender, col 7 is raw sms)
    for filename in &review_reports {
        let path = audit_dir.join("review-queue-reports").join(filename);
        let rows = parse_csv_rows(&path);
        let mut file_count = 0;
        for r in rows.iter().skip(1) {
            if r.len() < 8 {
                continue;
            }
            let sender = r[1].trim().to_string();
            let raw_sms = r[7].trim().to_string();
            if raw_sms.is_empty() {
                continue;
            }
            total_messages += 1;
            file_count += 1;
            let pattern = normalize_to_pattern(&raw_sms);
            let entry = clusters
                .entry((sender.clone(), pattern.clone()))
                .or_insert_with(|| PatternCluster {
                    sender,
                    pattern,
                    sample_sms: raw_sms,
                    count: 0,
                    sources: Vec::new(),
                });
            entry.count += 1;
            if !entry.sources.contains(&filename.to_string()) {
                entry.sources.push(filename.to_string());
            }
        }
        println!("Loaded {}: {} rows", filename, file_count);
    }

    println!("\n=======================================================");
    println!("TOTAL SCANNED SMS: {}", total_messages);
    println!("UNIQUE PATTERN CLUSTERS: {}", clusters.len());
    println!("=======================================================\n");

    let mut parsed_patterns = 0;
    let mut parsed_instances = 0;

    let mut rejected_safe_patterns = 0;
    let mut rejected_safe_instances = 0;

    let mut rejected_queued_patterns = 0;
    let mut rejected_queued_instances = 0;

    let mut queued_clusters = Vec::new();
    let mut missing_reference_clusters = Vec::new();
    let mut missing_party_clusters = Vec::new();
    let mut rejected_breakdown: BTreeMap<String, (usize, usize, Vec<String>)> = BTreeMap::new();

    for cluster in clusters.values() {
        let outcome = parse_sms(&cluster.sample_sms, Some(&cluster.sender));
        match outcome {
            ParseOutcome::Parsed(tx) => {
                parsed_patterns += 1;
                parsed_instances += cluster.count;

                let lower = cluster.sample_sms.to_lowercase();

                // Check if reference was missed despite being present in SMS
                let has_trx_marker = lower.contains("trxid")
                    || lower.contains("txnid")
                    || lower.contains("txn id")
                    || lower.contains("trx id");
                if has_trx_marker
                    && tx.reference.is_none()
                    && !lower.contains("trxid not applicable")
                    && !lower.contains("trxid na")
                {
                    missing_reference_clusters.push((cluster, tx.clone()));
                }

                // Check if party might have been missed
                let has_party_marker = (lower.contains("from 01")
                    || lower.contains("to 01")
                    || lower.contains("sender: 01")
                    || lower.contains("receiver: 01")
                    || lower.contains("used at "))
                    && !lower.contains("cash out")
                    && !lower.contains("atm ");
                if has_party_marker && tx.party.is_none() && tx.merchant.is_none() {
                    missing_party_clusters.push((cluster, tx.clone()));
                }
            }
            ParseOutcome::Rejected(reason) => {
                let reason_str = format!("{:?}", reason);
                let entry = rejected_breakdown
                    .entry(reason_str)
                    .or_insert((0, 0, Vec::new()));
                entry.0 += 1;
                entry.1 += cluster.count;
                if entry.2.len() < 3 {
                    entry.2.push(format!(
                        "[{}] {}",
                        cluster.sender,
                        cluster.sample_sms.replace('\n', " ")
                    ));
                }

                let queues =
                    matches!(
                        reason,
                        RejectReason::NoAmountFound
                            | RejectReason::NotATransaction
                            | RejectReason::UnsupportedProvider
                    ) && is_likely_financial_review(&cluster.sample_sms, Some(&cluster.sender));

                if queues {
                    rejected_queued_patterns += 1;
                    rejected_queued_instances += cluster.count;
                    queued_clusters.push((cluster, reason));
                } else {
                    rejected_safe_patterns += 1;
                    rejected_safe_instances += cluster.count;
                }
            }
        }
    }

    println!("AUDIT RESULTS SUMMARY:");
    println!("-------------------------------------------------------");
    println!(
        "Parsed Transactions:        {} patterns (covering {} SMS instances)",
        parsed_patterns, parsed_instances
    );
    println!(
        "Safely Rejected (Spam/OTP): {} patterns (covering {} SMS instances)",
        rejected_safe_patterns, rejected_safe_instances
    );
    println!(
        "Queued for Review:          {} patterns (covering {} SMS instances)",
        rejected_queued_patterns, rejected_queued_instances
    );
    println!("-------------------------------------------------------\n");

    println!("REJECTION REASON BREAKDOWN:");
    for (reason, (p_count, i_count, samples)) in &rejected_breakdown {
        println!(
            "\nReason: {} -> {} patterns ({} instances)",
            reason, p_count, i_count
        );
        for s in samples {
            println!("   Sample: {}", s);
        }
    }

    println!("\n-------------------------------------------------------");
    println!("FIELD EXTRACTION QUALITY CHECKS:");
    println!(
        "Missing Reference when TrxID in SMS: {} patterns",
        missing_reference_clusters.len()
    );
    for (c, _tx) in &missing_reference_clusters {
        println!(
            "   [MISSING REF] (count: {}) [{}]: {}",
            c.count,
            c.sender,
            c.sample_sms.replace('\n', " ")
        );
    }

    println!(
        "\nMissing Party when Party marker in SMS: {} patterns",
        missing_party_clusters.len()
    );
    for (c, _tx) in &missing_party_clusters {
        println!(
            "   [MISSING PARTY] (count: {}) [{}]: {}",
            c.count,
            c.sender,
            c.sample_sms.replace('\n', " ")
        );
    }
}

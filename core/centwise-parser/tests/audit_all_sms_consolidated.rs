use centwise_parser::{detect_provider, is_likely_financial_review, parse_sms, ParseOutcome};
use regex::Regex;
use std::path::Path;
use std::sync::LazyLock;

static REFERENCE_MARKER_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(r"(?i)\b(?:trxid|txnid|txn id|rrn|stan)\b").expect("valid reference regex")
});
static BALANCE_MARKER_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(r"(?i)\b(?:balance|c/b|avail(?:able)? bal|ledger bal)\b")
        .expect("valid balance regex")
});
static FEE_AMOUNT_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(r"(?i)\bfee\b\s*(?:[:=-]|is)?\s*(?:tk|bdt|rs|৳)\s*[0-9]").expect("valid fee regex")
});
static ACCOUNT_MARKER_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(
        r"(?i)(?:\b(?:a/c|account)\b[^,;:]{0,24}(?:xx\d{4}|[x*]{2,}\d{2,}|\d{4,})|\bcard\b[^,;:]{0,24}(?:\d{4}|[x*]{2,}\d{2,}))",
    )
    .expect("valid account regex")
});
static PARTY_MARKER_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(r"(?i)(?:\bfrom\s+01|\bto\s+01|\bsender:\s*01|\breceiver:\s*01|\bused at\s+)")
        .expect("valid party regex")
});

fn parse_csv_rows(path: &Path) -> Vec<Vec<String>> {
    let content = std::fs::read_to_string(path).expect("consolidated CSV should be readable");
    let mut rows = Vec::new();
    let mut record = String::new();
    let mut inside_quotes = false;

    for line in content.lines() {
        if !record.is_empty() {
            record.push('\n');
        }
        record.push_str(line);

        let mut escaped = false;
        for character in line.chars() {
            if character == '"' {
                if escaped {
                    escaped = false;
                } else {
                    inside_quotes = !inside_quotes;
                }
            } else {
                escaped = false;
            }
        }

        if inside_quotes {
            continue;
        }

        rows.push(parse_csv_record(&record));
        record.clear();
    }

    assert!(
        record.is_empty(),
        "consolidated CSV ended inside a quoted field"
    );
    rows
}

fn parse_csv_record(record: &str) -> Vec<String> {
    let mut fields = Vec::new();
    let mut field = String::new();
    let mut inside_quotes = false;
    let mut characters = record.chars().peekable();

    while let Some(character) = characters.next() {
        match character {
            '"' if inside_quotes && characters.peek() == Some(&'"') => {
                field.push('"');
                characters.next();
            }
            '"' => inside_quotes = !inside_quotes,
            ',' if !inside_quotes => {
                fields.push(field);
                field = String::new();
            }
            _ => field.push(character),
        }
    }
    fields.push(field);
    fields
}

#[test]
fn every_consolidated_sms_is_accounted_for_without_unsafe_review_leaks() {
    let path = Path::new(env!("CARGO_MANIFEST_DIR"))
        .join("../..")
        .join("fixtures/audits/all-sms-test-input.csv");
    let rows = parse_csv_rows(&path);
    assert!(!rows.is_empty(), "consolidated SMS input is empty");
    let header = rows[0]
        .iter()
        .map(|field| field.trim_start_matches('\u{feff}').to_string())
        .collect::<Vec<_>>();
    assert_eq!(
        header,
        [
            "source_kind",
            "source_file",
            "source_row",
            "sender",
            "message_text"
        ]
    );

    let mut parsed = 0usize;
    let mut safely_rejected = 0usize;
    let mut queued_for_review = Vec::new();
    let mut malformed_rows = Vec::new();
    let mut source_transaction_rejections = 0usize;
    let mut review_queue_rows_now_supported = 0usize;

    for (index, row) in rows.iter().enumerate().skip(1) {
        if row.len() != 5 || row[3].trim().is_empty() || row[4].trim().is_empty() {
            malformed_rows.push(index + 1);
            continue;
        }

        let source_kind = row[0].as_str();
        let sender = Some(row[3].as_str());
        let body = row[4].as_str();
        assert!(
            !body.contains(['\r', '\n']),
            "message_text must be one direct-text line at consolidated row {}",
            index + 1
        );
        let detected_provider = detect_provider(sender, body);

        match parse_sms(body, sender) {
            ParseOutcome::Parsed(transaction) => {
                parsed += 1;
                assert!(
                    transaction.amount_minor > 0,
                    "non-positive amount at consolidated row {}: {:?}",
                    index + 1,
                    transaction
                );
                assert_eq!(
                    transaction.provider_id,
                    detected_provider,
                    "provider mismatch at consolidated row {}",
                    index + 1
                );
                if source_kind == "review-queue-report" {
                    review_queue_rows_now_supported += 1;
                }
                if REFERENCE_MARKER_RE.is_match(body)
                    && !body.to_ascii_lowercase().contains("not applicable")
                    && transaction.reference.is_none()
                {
                    panic!(
                        "reference marker was not extracted at consolidated row {}",
                        index + 1
                    );
                }
            }
            ParseOutcome::Rejected(reason) => {
                let is_review_candidate = is_likely_financial_review(body, sender);
                if is_review_candidate {
                    queued_for_review.push((index + 1, reason, body.to_string()));
                } else {
                    safely_rejected += 1;
                }
                if source_kind == "transaction-report" {
                    source_transaction_rejections += 1;
                }
            }
        }
    }

    println!(
        "CONSOLIDATED SMS: total={} parsed={} safely_rejected={} queued={} malformed={} source_transaction_rejections={} review_rows_now_supported={}",
        rows.len() - 1,
        parsed,
        safely_rejected,
        queued_for_review.len(),
        malformed_rows.len(),
        source_transaction_rejections,
        review_queue_rows_now_supported
    );

    assert!(
        malformed_rows.is_empty(),
        "malformed consolidated rows: {malformed_rows:?}"
    );
    assert!(
        queued_for_review.is_empty(),
        "review row count changed; check the per-row regression snapshot"
    );
    assert_eq!(
        parsed + safely_rejected + queued_for_review.len(),
        rows.len() - 1
    );
    assert_eq!(rows.len() - 1, 4_714);
    assert_eq!(parsed, 4_339);
    assert_eq!(safely_rejected, 375);
}

#[test]
fn every_sms_matches_its_reviewed_regression_snapshot() {
    let root = Path::new(env!("CARGO_MANIFEST_DIR")).join("../../fixtures/audits");
    let rows = parse_csv_rows(&root.join("all-sms-test-input.csv"));
    let expected = std::fs::read_to_string(root.join("all-sms-expected.csv")).unwrap();
    let expected: Vec<_> = expected.lines().skip(1).collect();
    assert_eq!(expected.len(), rows.len() - 1);
    for (index, (row, expected)) in rows.iter().skip(1).zip(expected).enumerate() {
        let outcome = parse_sms(&row[4], Some(&row[3]));
        let actual = match outcome {
            ParseOutcome::Parsed(tx) => format!(
                "{},parsed,{},{:?},{},{},{}",
                index + 2,
                tx.provider_id,
                tx.transaction_type,
                tx.amount_minor,
                tx.fee_minor.map(|v| v.to_string()).unwrap_or_default(),
                tx.balance_after_minor.is_some()
            ),
            ParseOutcome::Rejected(_) => {
                let disposition = if is_likely_financial_review(&row[4], Some(&row[3])) {
                    "review"
                } else {
                    "ignored"
                };
                format!("{},{disposition},,,,,", index + 2)
            }
        };
        assert_eq!(actual, expected, "SMS row {} changed", index + 2);
    }
}

#[test]
fn direct_text_preserves_main_transaction_fields() {
    let path = Path::new(env!("CARGO_MANIFEST_DIR"))
        .join("../..")
        .join("fixtures/audits/all-sms-test-input.csv");
    let rows = parse_csv_rows(&path);
    let mut parsed_rows = 0usize;
    let mut balance_expected = 0usize;
    let mut fee_expected = 0usize;
    let mut account_expected = 0usize;
    let mut party_expected = 0usize;
    let mut missing_balance = Vec::new();
    let mut missing_fee = Vec::new();
    let mut missing_account = Vec::new();
    let mut missing_party = Vec::new();

    for (index, row) in rows.iter().enumerate().skip(1) {
        let body = &row[4];
        let outcome = parse_sms(body, Some(&row[3]));
        let Some(transaction) = outcome.transaction() else {
            continue;
        };
        parsed_rows += 1;
        let lower = body.to_ascii_lowercase();

        let balance_expected_for_row = BALANCE_MARKER_RE.is_match(body)
            && !lower.contains("emergency balance")
            && !lower.contains("meter balance")
            && !lower.contains("balance advances");
        if balance_expected_for_row {
            balance_expected += 1;
            if transaction.balance_after_minor.is_none() {
                missing_balance.push(index + 1);
            }
        }
        if FEE_AMOUNT_RE.is_match(body) {
            fee_expected += 1;
            if transaction.fee_minor.is_none() {
                missing_fee.push(index + 1);
            }
        }
        if ACCOUNT_MARKER_RE.is_match(body) {
            account_expected += 1;
            if transaction.account_last4.is_none() {
                missing_account.push(index + 1);
            }
        }
        let party_expected_for_row = PARTY_MARKER_RE.is_match(body)
            && !lower.contains("cash out")
            && !lower.contains("atm ");
        if party_expected_for_row {
            party_expected += 1;
            if transaction.party.is_none() && transaction.merchant.is_none() {
                missing_party.push(index + 1);
            }
        }
    }

    println!(
        "DIRECT TEXT FIELDS: parsed={} balance={}/{} fee={}/{} account={}/{} party={}/{}, missing rows: balance={:?} fee={:?} account={:?} party={:?}",
        parsed_rows,
        balance_expected - missing_balance.len(),
        balance_expected,
        fee_expected - missing_fee.len(),
        fee_expected,
        account_expected - missing_account.len(),
        account_expected,
        party_expected - missing_party.len(),
        party_expected,
        missing_balance,
        missing_fee,
        missing_account,
        missing_party
    );
    assert!(
        missing_balance.is_empty(),
        "balance extraction gaps: {missing_balance:?}"
    );
    assert!(
        missing_fee.is_empty(),
        "fee extraction gaps: {missing_fee:?}"
    );
    assert!(
        missing_account.is_empty(),
        "account extraction gaps: {missing_account:?}"
    );
    assert!(
        missing_party.is_empty(),
        "party extraction gaps: {missing_party:?}"
    );
}

#[test]
fn consolidated_rejections_are_explicitly_safe() {
    let path = Path::new(env!("CARGO_MANIFEST_DIR"))
        .join("../..")
        .join("fixtures/audits/all-sms-test-input.csv");
    let rows = parse_csv_rows(&path);
    let mut reasons = std::collections::BTreeMap::<String, usize>::new();

    for row in rows.iter().skip(1) {
        if let ParseOutcome::Rejected(reason) = parse_sms(&row[4], Some(&row[3])) {
            *reasons.entry(format!("{reason:?}")).or_default() += 1;
        }
    }

    println!("SAFE REJECTION BREAKDOWN: {reasons:?}");
    assert!(
        !reasons.is_empty(),
        "the consolidated test must exercise rejection paths"
    );
}

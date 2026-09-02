use centwise_db::queries::Queries;
use centwise_domain::{ReviewQueueItem, SmartRule};

use crate::conversions::sms_transaction_id;
use crate::core::default_account_name;
use crate::types::{SmsIngestResult, SmsIngestStatus, TransactionKind};

/// Result of parsing an incoming SMS across the FFI boundary.
#[derive(uniffi::Record)]
pub struct ParsedSmsRecord {
    pub is_transaction: bool,
    pub provider_id: String,
    pub kind: Option<TransactionKind>,
    pub amount_minor: Option<i64>,
    pub fee_minor: Option<i64>,
    pub balance_after_minor: Option<i64>,
    pub reference: Option<String>,
    pub party: Option<String>,
    pub merchant: Option<String>,
    pub category_id: Option<String>,
    pub account_last4: Option<String>,
    pub account_hint: Option<String>,
    pub raw_date: Option<String>,
}

/// Standalone SMS parser function callable directly from Kotlin or Swift.
#[uniffi::export]
pub fn parse_sms_message(body: String, sender_hint: Option<String>) -> ParsedSmsRecord {
    match centwise_parser::parse_sms(&body, sender_hint.as_deref()) {
        centwise_parser::ParseOutcome::Parsed(tx) => {
            let tx = *tx;
            ParsedSmsRecord {
                is_transaction: true,
                provider_id: tx.provider_id,
                kind: Some(tx.transaction_type.into()),
                amount_minor: Some(tx.amount_minor),
                fee_minor: tx.fee_minor,
                balance_after_minor: tx.balance_after_minor,
                reference: tx.reference,
                party: tx.party,
                merchant: tx.merchant,
                category_id: tx.category_id,
                account_last4: tx.account_last4,
                account_hint: tx.account_hint,
                raw_date: tx.raw_date,
            }
        }
        centwise_parser::ParseOutcome::Rejected(_) => ParsedSmsRecord {
            is_transaction: false,
            provider_id: String::new(),
            kind: None,
            amount_minor: None,
            fee_minor: None,
            balance_after_minor: None,
            reference: None,
            party: None,
            merchant: None,
            category_id: None,
            account_last4: None,
            account_hint: None,
            raw_date: None,
        },
    }
}

pub(crate) fn ingest_sms_in_transaction(
    queries: &Queries<'_>,
    body: String,
    sender_hint: Option<String>,
    occurred_at_epoch_ms: i64,
    outcome: centwise_parser::ParseOutcome,
    rules: &[SmartRule],
) -> centwise_db::DbResult<SmsIngestResult> {
    match outcome {
        centwise_parser::ParseOutcome::Parsed(parsed) => {
            let matches = queries
                .find_matching_accounts(&parsed.provider_id, parsed.account_last4.as_deref())?;
            let reference = parsed.reference.clone();
            let merchant_or_party = parsed
                .merchant
                .as_deref()
                .or(parsed.party.as_deref())
                .unwrap_or_default();
            let mapped_category =
                queries.matching_merchant_category(merchant_or_party, parsed.transaction_type)?;
            let rule_category = rules
                .iter()
                .find(|rule| {
                    rule.is_enabled
                        && rule.transaction_type == parsed.transaction_type
                        && rule.match_type.matches(merchant_or_party, &rule.keyword)
                })
                .map(|rule| rule.category_id.clone());
            let (category_id, category_source) = mapped_category
                .map(|category| (category, "learned_mapping"))
                .or_else(|| rule_category.map(|category| (category, "smart_rule")))
                .or_else(|| {
                    parsed
                        .category_id
                        .clone()
                        .map(|category| (category, "system"))
                })
                .unwrap_or_else(|| {
                    if parsed.transaction_type == centwise_domain::TransactionType::Income {
                        ("income".into(), "fallback")
                    } else {
                        ("other".into(), "fallback")
                    }
                });

            let target_account_id = match matches.as_slice() {
                [account] => Some(account.id.clone()),
                [] => Some(queries.resolve_or_create_account(
                    &parsed.provider_id,
                    parsed.account_last4.as_deref(),
                    default_account_name(&parsed.provider_id),
                )?),
                _ => None,
            };

            if let Some(account_id) = target_account_id {
                let transaction_id =
                    sms_transaction_id(reference.as_deref(), &body, sender_hint.as_deref());
                let transaction = centwise_domain::NewTransaction {
                    id: transaction_id.clone(),
                    title: parsed
                        .merchant
                        .clone()
                        .or_else(|| parsed.party.clone())
                        .unwrap_or_else(|| format!("{} transaction", parsed.provider_id)),
                    amount_minor: parsed.amount_minor,
                    currency: "BDT".into(),
                    transaction_type: parsed.transaction_type,
                    category_id,
                    occurred_at_epoch_ms,
                    account_id,
                    reference: parsed.reference.clone(),
                    balance_after_minor: parsed.balance_after_minor,
                    fee_minor: parsed.fee_minor,
                    notes: None,
                    raw_sms: Some(body),
                    is_auto_tracked: true,
                };
                return match queries
                    .insert_transaction_with_category_source(&transaction, Some(category_source))
                {
                    Ok(()) => Ok(SmsIngestResult {
                        status: SmsIngestStatus::Inserted,
                        transaction_id: Some(transaction_id),
                        review_id: None,
                        reference,
                    }),
                    Err(centwise_db::DbError::DuplicateReference(_))
                    | Err(centwise_db::DbError::DuplicateTransaction(_)) => Ok(SmsIngestResult {
                        status: SmsIngestStatus::Duplicate,
                        transaction_id: None,
                        review_id: None,
                        reference,
                    }),
                    Err(error) => Err(error),
                };
            }

            let review_id = format!(
                "review-{}",
                sms_transaction_id(reference.as_deref(), &body, sender_hint.as_deref())
            );
            let item = ReviewQueueItem {
                id: review_id.clone(),
                sender: sender_hint,
                raw_sms: body,
                received_at_epoch_ms: occurred_at_epoch_ms,
                provider_id: Some(parsed.provider_id),
                reason: "Multiple matching accounts".into(),
                candidate_amount_minor: Some(parsed.amount_minor),
                candidate_type: Some(parsed.transaction_type),
                fee_minor: parsed.fee_minor,
                balance_after_minor: parsed.balance_after_minor,
                reference: parsed.reference,
                party: parsed.party,
                merchant: parsed.merchant,
                category_id: Some(category_id),
                account_last4: parsed.account_last4,
                account_hint: parsed.account_hint,
            };
            let inserted = queries.insert_review_queue_item(&item)?;
            Ok(SmsIngestResult {
                status: if inserted {
                    SmsIngestStatus::QueuedForReview
                } else {
                    SmsIngestStatus::Duplicate
                },
                transaction_id: None,
                review_id: inserted.then_some(review_id),
                reference,
            })
        }
        centwise_parser::ParseOutcome::Rejected(reason) => {
            if !matches!(
                reason,
                centwise_parser::RejectReason::NoAmountFound
                    | centwise_parser::RejectReason::NotATransaction
                    | centwise_parser::RejectReason::UnsupportedProvider
            ) || !centwise_parser::is_likely_financial_review(&body, sender_hint.as_deref())
            {
                return Ok(SmsIngestResult {
                    status: SmsIngestStatus::Ignored,
                    transaction_id: None,
                    review_id: None,
                    reference: None,
                });
            }
            let review_id = format!(
                "review-{}",
                sms_transaction_id(None, &body, sender_hint.as_deref())
            );
            let item = ReviewQueueItem {
                id: review_id.clone(),
                sender: sender_hint.clone(),
                raw_sms: body.clone(),
                received_at_epoch_ms: occurred_at_epoch_ms,
                provider_id: Some(centwise_parser::detect_provider(
                    sender_hint.as_deref(),
                    &body,
                )),
                reason: format!("SMS could not be parsed: {reason:?}"),
                candidate_amount_minor: None,
                candidate_type: None,
                fee_minor: None,
                balance_after_minor: None,
                reference: None,
                party: None,
                merchant: None,
                category_id: None,
                account_last4: None,
                account_hint: None,
            };
            let inserted = queries.insert_review_queue_item(&item)?;
            Ok(SmsIngestResult {
                status: if inserted {
                    SmsIngestStatus::QueuedForReview
                } else {
                    SmsIngestStatus::Duplicate
                },
                transaction_id: None,
                review_id: inserted.then_some(review_id),
                reference: None,
            })
        }
    }
}

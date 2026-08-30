use std::sync::Arc;

use centwise_db::Database;
use centwise_domain as domain;

use crate::conversions::*;
use crate::error::{CentwiseError, ChangeListener, ForeignObserver};
use crate::types::*;

/// The Centwise core handle shared with both platforms.
#[derive(uniffi::Object)]
pub struct CentwiseCore {
    database: Database,
}

#[uniffi::export]
impl CentwiseCore {
    /// Opens (or creates) the shared database at `path` and runs migrations.
    /// The platform passes an app-container path; Rust never decides locations.
    #[uniffi::constructor]
    pub fn open(path: String) -> Result<Arc<CentwiseCore>, CentwiseError> {
        let database = Database::open(path).map_err(CentwiseError::from)?;
        Ok(Arc::new(CentwiseCore { database }))
    }

    /// Registers a change listener. Keep the object alive on the native side
    /// or notifications stop.
    pub fn add_listener(&self, listener: Box<dyn ChangeListener>) {
        self.database
            .add_observer(Arc::new(ForeignObserver(listener)));
    }

    pub fn insert_account(&self, account: AccountInput) -> Result<(), CentwiseError> {
        self.database
            .write(|queries| {
                let account = account_record_to_domain(account);
                if queries.update_account(&account)? {
                    Ok(())
                } else {
                    queries.insert_account(&account)
                }
            })
            .map_err(CentwiseError::from)
    }

    pub fn update_account(&self, account: AccountInput) -> Result<bool, CentwiseError> {
        self.database
            .update_account(&account_record_to_domain(account))
            .map_err(CentwiseError::from)
    }

    pub fn delete_account(&self, id: String) -> Result<bool, CentwiseError> {
        self.database
            .delete_account(&id)
            .map_err(CentwiseError::from)
    }

    /// Explicitly replaces user records with Rust's deterministic demo set.
    /// The native UI must ask for confirmation before calling this method.
    pub fn load_demo_data(&self) -> Result<DemoDataSummaryRecord, CentwiseError> {
        self.database
            .replace_with_demo_data()
            .map(|summary| DemoDataSummaryRecord {
                accounts: summary.accounts,
                transactions: summary.transactions,
                budgets: summary.budgets,
                subscriptions: summary.subscriptions,
            })
            .map_err(CentwiseError::from)
    }

    /// Clears user records and preserves Rust's system categories.
    pub fn reset_to_empty_database(&self) -> Result<(), CentwiseError> {
        self.database.reset_to_empty().map_err(CentwiseError::from)
    }

    pub fn insert_transaction(&self, input: TransactionInput) -> Result<(), CentwiseError> {
        let provider_hint = input
            .account_provider
            .clone()
            .filter(|value| !value.trim().is_empty());
        let account_name_hint = input
            .account_name
            .clone()
            .filter(|value| !value.trim().is_empty());
        let account_last_four = input
            .account_last_four
            .clone()
            .filter(|value| !value.trim().is_empty());
        let mut transaction = transaction_input_to_domain(input);

        self.database
            .write(|queries| {
                if transaction.account_id.trim().is_empty() {
                    let provider = provider_hint.as_deref().unwrap_or("cash");
                    let account_name = account_name_hint
                        .as_deref()
                        .unwrap_or_else(|| default_account_name(provider));
                    transaction.account_id = queries.resolve_or_create_account(
                        provider,
                        account_last_four.as_deref(),
                        account_name,
                    )?;
                } else if !queries.account_exists(&transaction.account_id)? {
                    return Err(centwise_db::DbError::Invalid(format!(
                        "account does not exist: {}",
                        transaction.account_id
                    )));
                }

                queries.insert_transaction(&transaction)
            })
            .map_err(CentwiseError::from)
    }

    pub fn update_transaction(&self, input: TransactionInput) -> Result<bool, CentwiseError> {
        let provider_hint = input
            .account_provider
            .clone()
            .filter(|value| !value.trim().is_empty());
        let account_name_hint = input
            .account_name
            .clone()
            .filter(|value| !value.trim().is_empty());
        let account_last_four = input
            .account_last_four
            .clone()
            .filter(|value| !value.trim().is_empty());
        let mut transaction = transaction_input_to_domain(input);

        self.database
            .write(|queries| {
                if transaction.account_id.trim().is_empty()
                    || !queries.account_exists(&transaction.account_id)?
                {
                    let provider = provider_hint.as_deref().unwrap_or("cash");
                    let account_name = account_name_hint
                        .as_deref()
                        .unwrap_or_else(|| default_account_name(provider));
                    transaction.account_id = queries.resolve_or_create_account(
                        provider,
                        account_last_four.as_deref(),
                        account_name,
                    )?;
                }
                queries.update_transaction(&transaction)
            })
            .map_err(CentwiseError::from)
    }

    pub fn list_transactions(&self, limit: u32) -> Result<Vec<TransactionRecord>, CentwiseError> {
        self.database
            .list_transactions(limit)
            .map(|items| items.into_iter().map(transaction_record).collect())
            .map_err(CentwiseError::from)
    }

    pub fn get_transaction(&self, id: String) -> Result<Option<TransactionRecord>, CentwiseError> {
        self.database
            .get_transaction(&id)
            .map(|item| item.map(transaction_record))
            .map_err(CentwiseError::from)
    }

    pub fn list_accounts(&self) -> Result<Vec<AccountRecord>, CentwiseError> {
        self.database
            .list_accounts()
            .map(|items| items.into_iter().map(account_record).collect())
            .map_err(CentwiseError::from)
    }

    pub fn list_budgets(&self) -> Result<Vec<BudgetRecord>, CentwiseError> {
        self.database
            .list_budgets()
            .map(|items| items.into_iter().map(budget_record).collect())
            .map_err(CentwiseError::from)
    }

    pub fn list_subscriptions(&self) -> Result<Vec<SubscriptionRecord>, CentwiseError> {
        self.database
            .list_subscriptions()
            .map(|items| items.into_iter().map(subscription_record).collect())
            .map_err(CentwiseError::from)
    }

    pub fn insert_budget(&self, input: BudgetInput) -> Result<(), CentwiseError> {
        self.database
            .insert_budget(&budget_input_to_domain(input))
            .map_err(CentwiseError::from)
    }

    pub fn update_budget(&self, input: BudgetInput) -> Result<bool, CentwiseError> {
        self.database
            .update_budget(&budget_input_to_domain(input))
            .map_err(CentwiseError::from)
    }

    pub fn delete_budget(&self, id: String) -> Result<bool, CentwiseError> {
        self.database
            .delete_budget(&id)
            .map_err(CentwiseError::from)
    }

    pub fn insert_subscription(&self, input: SubscriptionInput) -> Result<(), CentwiseError> {
        self.database
            .insert_subscription(&subscription_input_to_domain(input))
            .map_err(CentwiseError::from)
    }

    pub fn update_subscription(&self, input: SubscriptionInput) -> Result<bool, CentwiseError> {
        self.database
            .update_subscription(&subscription_input_to_domain(input))
            .map_err(CentwiseError::from)
    }

    pub fn delete_subscription(&self, id: String) -> Result<bool, CentwiseError> {
        self.database
            .delete_subscription(&id)
            .map_err(CentwiseError::from)
    }

    pub fn list_categories(&self) -> Result<Vec<CategoryRecord>, CentwiseError> {
        self.database
            .list_categories()
            .map(|items| items.into_iter().map(category_record).collect())
            .map_err(CentwiseError::from)
    }

    pub fn insert_category(&self, input: CategoryInput) -> Result<(), CentwiseError> {
        self.database
            .insert_category(&domain::NewCategory {
                id: input.id,
                name: input.name,
                icon: input.icon,
                color_hex: input.color_hex,
            })
            .map_err(CentwiseError::from)
    }

    pub fn update_category(&self, input: CategoryInput) -> Result<bool, CentwiseError> {
        self.database
            .update_category(&domain::NewCategory {
                id: input.id,
                name: input.name,
                icon: input.icon,
                color_hex: input.color_hex,
            })
            .map_err(CentwiseError::from)
    }

    pub fn delete_category(&self, id: String) -> Result<bool, CentwiseError> {
        self.database
            .delete_category(&id)
            .map_err(CentwiseError::from)
    }

    pub fn list_rules(&self) -> Result<Vec<SmartRuleRecord>, CentwiseError> {
        let categories = self
            .database
            .list_categories()
            .map_err(CentwiseError::from)?;
        self.database
            .list_rules()
            .map(|items| {
                items
                    .into_iter()
                    .map(|item| smart_rule_record(item, &categories))
                    .collect()
            })
            .map_err(CentwiseError::from)
    }

    pub fn insert_rule(&self, input: SmartRuleInput) -> Result<(), CentwiseError> {
        self.database
            .insert_rule(&smart_rule_input_to_domain(input)?)
            .map_err(CentwiseError::from)
    }

    pub fn update_rule(&self, input: SmartRuleInput) -> Result<bool, CentwiseError> {
        self.database
            .update_rule(&smart_rule_input_to_domain(input)?)
            .map_err(CentwiseError::from)
    }

    pub fn delete_rule(&self, id: String) -> Result<bool, CentwiseError> {
        self.database.delete_rule(&id).map_err(CentwiseError::from)
    }

    /// Parses, resolves, deduplicates, and stores an SMS in one Rust-owned
    /// operation. Native platforms only provide the message and timestamp.
    pub fn ingest_sms(
        &self,
        body: String,
        sender_hint: Option<String>,
        occurred_at_epoch_ms: i64,
    ) -> Result<SmsIngestResult, CentwiseError> {
        let outcome = centwise_parser::parse_sms(&body, sender_hint.as_deref());

        self.database
            .write(|queries| match outcome {
                centwise_parser::ParseOutcome::Parsed(parsed) => {
                    let matches = queries.find_matching_accounts(
                        &parsed.provider_id,
                        parsed.account_last4.as_deref(),
                    )?;
                    let reference = parsed.reference.clone();
                    let merchant_or_party = parsed
                        .merchant
                        .as_deref()
                        .or(parsed.party.as_deref())
                        .unwrap_or_default();
                    let rule_category_id = queries
                        .matching_rule(merchant_or_party, parsed.transaction_type)?
                        .map(|rule| rule.category_id);
                    let category_id = rule_category_id
                        .clone()
                        .or_else(|| parsed.category_id.clone())
                        .unwrap_or_else(|| {
                            if parsed.transaction_type == domain::TransactionType::Income {
                                "salary".into()
                            } else {
                                "other".into()
                            }
                        });

                    let target_account_id = if matches.len() == 1 {
                        Some(matches[0].id.clone())
                    } else if matches.is_empty() {
                        Some(queries.resolve_or_create_account(
                            &parsed.provider_id,
                            parsed.account_last4.as_deref(),
                            default_account_name(&parsed.provider_id),
                        )?)
                    } else {
                        None
                    };

                    if let Some(account_id) = target_account_id {
                        let transaction_id = sms_transaction_id(reference.as_deref(), &body);
                        let transaction = domain::NewTransaction {
                            id: transaction_id.clone(),
                            title: parsed
                                .merchant
                                .clone()
                                .or_else(|| parsed.party.clone())
                                .unwrap_or_else(|| format!("{} transaction", parsed.provider_id)),
                            amount_minor: parsed.amount_minor,
                            currency: "BDT".into(),
                            transaction_type: parsed.transaction_type,
                            category_id: category_id.clone(),
                            occurred_at_epoch_ms,
                            account_id,
                            reference: parsed.reference.clone(),
                            balance_after_minor: parsed.balance_after_minor,
                            fee_minor: parsed.fee_minor,
                            notes: None,
                            raw_sms: Some(body.clone()),
                            is_auto_tracked: true,
                        };

                        match queries.insert_transaction(&transaction) {
                            Ok(()) => Ok(SmsIngestResult {
                                status: SmsIngestStatus::Inserted,
                                transaction_id: Some(transaction_id),
                                review_id: None,
                                reference,
                            }),
                            Err(centwise_db::DbError::DuplicateReference(_))
                            | Err(centwise_db::DbError::DuplicateTransaction(_)) => {
                                Ok(SmsIngestResult {
                                    status: SmsIngestStatus::Duplicate,
                                    transaction_id: None,
                                    review_id: None,
                                    reference,
                                })
                            }
                            Err(error) => Err(error),
                        }
                    } else {
                        let review_id =
                            format!("review-{}", sms_transaction_id(reference.as_deref(), &body));
                        let item = domain::ReviewQueueItem {
                            id: review_id.clone(),
                            sender: sender_hint.clone(),
                            raw_sms: body.clone(),
                            received_at_epoch_ms: occurred_at_epoch_ms,
                            provider_id: Some(parsed.provider_id.clone()),
                            reason: if matches.is_empty() {
                                "No matching active account".into()
                            } else {
                                "Multiple matching accounts".into()
                            },
                            candidate_amount_minor: Some(parsed.amount_minor),
                            candidate_type: Some(parsed.transaction_type),
                            fee_minor: parsed.fee_minor,
                            balance_after_minor: parsed.balance_after_minor,
                            reference: parsed.reference.clone(),
                            party: parsed.party.clone(),
                            merchant: parsed.merchant.clone(),
                            category_id: Some(category_id),
                            account_last4: parsed.account_last4.clone(),
                            account_hint: parsed.account_hint.clone(),
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
                }
                centwise_parser::ParseOutcome::Rejected(reason) => {
                    if !matches!(reason, centwise_parser::RejectReason::NoAmountFound)
                        || !centwise_parser::is_likely_financial_review(
                            &body,
                            sender_hint.as_deref(),
                        )
                    {
                        return Ok(SmsIngestResult {
                            status: SmsIngestStatus::Ignored,
                            transaction_id: None,
                            review_id: None,
                            reference: None,
                        });
                    }

                    let review_id = format!("review-{}", sms_transaction_id(None, &body));
                    let item = domain::ReviewQueueItem {
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
            })
            .map_err(CentwiseError::from)
    }

    /// Ingests multiple platform SMS messages through the same Rust parser.
    /// The native bridge crosses once for the whole batch; each message keeps
    /// the existing Rust deduplication and review-queue behavior.
    pub fn ingest_sms_batch(
        &self,
        messages: Vec<SmsBatchMessage>,
    ) -> Result<Vec<SmsIngestResult>, CentwiseError> {
        messages
            .into_iter()
            .map(|message| {
                self.ingest_sms(
                    message.body,
                    message.sender_hint,
                    message.occurred_at_epoch_ms,
                )
            })
            .collect()
    }

    pub fn list_review_queue(&self, limit: u32) -> Result<Vec<ReviewQueueRecord>, CentwiseError> {
        self.database
            // Read a wider window before filtering legacy non-financial rows that
            // were queued by older parser versions, then apply the caller limit.
            .list_review_queue(10_000)
            .map(|items| {
                items
                    .into_iter()
                    .filter(|item| {
                        item.candidate_amount_minor.is_some()
                            || centwise_parser::is_likely_financial_review(
                                &item.raw_sms,
                                item.sender.as_deref(),
                            )
                    })
                    .take(limit as usize)
                    .map(review_queue_record)
                    .collect()
            })
            .map_err(CentwiseError::from)
    }

    pub fn dismiss_review_queue_item(&self, id: String) -> Result<bool, CentwiseError> {
        self.database
            .dismiss_review_queue_item(&id)
            .map_err(CentwiseError::from)
    }

    pub fn convert_review_queue_item(
        &self,
        id: String,
        input: TransactionInput,
    ) -> Result<bool, CentwiseError> {
        let provider_hint = input
            .account_provider
            .clone()
            .filter(|value| !value.trim().is_empty());
        let account_name_hint = input
            .account_name
            .clone()
            .filter(|value| !value.trim().is_empty());
        let account_last_four = input
            .account_last_four
            .clone()
            .filter(|value| !value.trim().is_empty());
        let mut transaction = transaction_input_to_domain(input);

        self.database
            .write(|queries| {
                if transaction.account_id.trim().is_empty() {
                    let provider = provider_hint.as_deref().unwrap_or("cash");
                    let account_name = account_name_hint
                        .as_deref()
                        .unwrap_or_else(|| default_account_name(provider));
                    transaction.account_id = queries.resolve_or_create_account(
                        provider,
                        account_last_four.as_deref(),
                        account_name,
                    )?;
                }
                queries.convert_review_queue_item(&id, &transaction)
            })
            .map_err(CentwiseError::from)
    }

    pub fn delete_transaction(&self, id: String) -> Result<bool, CentwiseError> {
        self.database
            .delete_transaction(&id)
            .map_err(CentwiseError::from)
    }

    pub fn account_balance(&self, account_id: String) -> Result<i64, CentwiseError> {
        self.database
            .read(|queries| queries.account_balance(&account_id))
            .map_err(CentwiseError::from)
    }

    /// The single query powering the Home screen.
    pub fn home_dashboard(
        &self,
        start_epoch_ms: i64,
        end_epoch_ms: i64,
        recent_limit: u32,
    ) -> Result<HomeDashboardRecord, CentwiseError> {
        self.database
            .read(|queries| queries.home_dashboard(start_epoch_ms, end_epoch_ms, recent_limit))
            .map(|dashboard| HomeDashboardRecord {
                period_expense_minor: dashboard.period_expense_minor,
                period_income_minor: dashboard.period_income_minor,
                recent_transactions: dashboard
                    .recent_transactions
                    .into_iter()
                    .map(|summary| TransactionSummaryRecord {
                        id: summary.id,
                        title: summary.title,
                        amount_minor: summary.amount_minor,
                        kind: summary.transaction_type.into(),
                        category_name: summary.category_name,
                        category_icon: summary.category_icon,
                        category_color_hex: summary.category_color_hex,
                        occurred_at_epoch_ms: summary.occurred_at_epoch_ms,
                        account_name: summary.account_name,
                    })
                    .collect(),
            })
            .map_err(CentwiseError::from)
    }
}

fn default_account_name(provider: &str) -> &str {
    match provider {
        "bkash" => "bKash",
        "nagad" => "Nagad",
        "rocket" => "Rocket",
        "upay" => "Upay",
        "cellfin" => "CellFin",
        "dbbl" => "Dutch-Bangla Bank",
        "city-bank" => "City Bank",
        "brac-bank" => "BRAC Bank",
        "ebl" => "Eastern Bank",
        "cash" => "Cash / Unassigned",
        _ => "Primary Account",
    }
}

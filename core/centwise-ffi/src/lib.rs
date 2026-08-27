//! UniFFI surface for the Centwise core.
//!
//! Exposes the shared database (open, write, query) plus the change
//! notification callback that native reactive layers wrap into StateFlow
//! (Android) and Combine (iOS).

uniffi::setup_scaffolding!();

use std::hash::{Hash, Hasher};
use std::sync::Arc;

use centwise_db::notify::DataObserver;
use centwise_db::Database;
use centwise_domain as domain;

/// Transaction kind used across the FFI boundary.
#[derive(uniffi::Enum)]
pub enum TransactionKind {
    Expense,
    Income,
    Transfer,
    Refund,
}

impl From<TransactionKind> for domain::TransactionType {
    fn from(kind: TransactionKind) -> Self {
        match kind {
            TransactionKind::Expense => domain::TransactionType::Expense,
            TransactionKind::Income => domain::TransactionType::Income,
            TransactionKind::Transfer => domain::TransactionType::Transfer,
            TransactionKind::Refund => domain::TransactionType::Refund,
        }
    }
}

impl From<domain::TransactionType> for TransactionKind {
    fn from(kind: domain::TransactionType) -> Self {
        match kind {
            domain::TransactionType::Expense => TransactionKind::Expense,
            domain::TransactionType::Income => TransactionKind::Income,
            domain::TransactionType::Transfer => TransactionKind::Transfer,
            domain::TransactionType::Refund => TransactionKind::Refund,
        }
    }
}

/// A transaction to insert.
#[derive(uniffi::Record)]
pub struct TransactionInput {
    pub id: String,
    pub title: String,
    pub amount_minor: i64,
    pub currency: String,
    pub kind: TransactionKind,
    pub category_id: String,
    pub occurred_at_epoch_ms: i64,
    pub account_id: String,
    pub reference: Option<String>,
    pub balance_after_minor: Option<i64>,
    pub fee_minor: Option<i64>,
    pub notes: Option<String>,
    pub raw_sms: Option<String>,
    pub is_auto_tracked: bool,
}

/// An account to insert.
#[derive(uniffi::Record)]
pub struct AccountInput {
    pub id: String,
    pub name: String,
    pub provider: String,
    pub last_four: Option<String>,
    pub starting_balance_minor: i64,
    pub archived: bool,
}

/// A user-created category to insert or update.
#[derive(uniffi::Record)]
pub struct CategoryInput {
    pub id: String,
    pub name: String,
    pub icon: String,
    pub color_hex: String,
}

/// A Smart Rule to insert or update.
#[derive(uniffi::Record)]
pub struct SmartRuleInput {
    pub id: String,
    pub name: String,
    pub keyword: String,
    pub match_type: String,
    pub category_id: String,
    pub kind: TransactionKind,
    pub is_enabled: bool,
}

/// A budget to insert or update.
#[derive(uniffi::Record)]
pub struct BudgetInput {
    pub id: String,
    pub category_id: String,
    pub limit_minor: i64,
    pub period: String,
    pub start_epoch_ms: i64,
    pub end_epoch_ms: i64,
}

/// A subscription to insert or update.
#[derive(uniffi::Record)]
pub struct SubscriptionInput {
    pub id: String,
    pub name: String,
    pub amount_minor: i64,
    pub billing_cycle: String,
    pub next_due_epoch_ms: i64,
    pub is_active: bool,
}

/// A short transaction for lists.
#[derive(uniffi::Record)]
pub struct TransactionSummaryRecord {
    pub id: String,
    pub title: String,
    pub amount_minor: i64,
    pub kind: TransactionKind,
    pub category_name: String,
    pub category_icon: String,
    pub category_color_hex: String,
    pub occurred_at_epoch_ms: i64,
    pub account_name: String,
}

#[derive(uniffi::Record)]
pub struct TransactionRecord {
    pub id: String,
    pub title: String,
    pub amount_minor: i64,
    pub currency: String,
    pub kind: TransactionKind,
    pub category_id: String,
    pub occurred_at_epoch_ms: i64,
    pub account_id: String,
    pub reference: Option<String>,
    pub balance_after_minor: Option<i64>,
    pub fee_minor: Option<i64>,
    pub notes: Option<String>,
    pub raw_sms: Option<String>,
    pub is_auto_tracked: bool,
}

#[derive(uniffi::Record)]
pub struct AccountRecord {
    pub id: String,
    pub name: String,
    pub provider: String,
    pub last_four: Option<String>,
    pub balance_minor: i64,
    pub archived: bool,
}

#[derive(uniffi::Record)]
pub struct BudgetRecord {
    pub id: String,
    pub category_id: String,
    pub category_name: String,
    pub limit_minor: i64,
    pub period: String,
    pub start_epoch_ms: i64,
    pub end_epoch_ms: i64,
    pub spent_minor: i64,
}

#[derive(uniffi::Record)]
pub struct SubscriptionRecord {
    pub id: String,
    pub name: String,
    pub amount_minor: i64,
    pub billing_cycle: String,
    pub next_due_epoch_ms: i64,
    pub is_active: bool,
}

/// A category read from Rust's canonical category table.
#[derive(uniffi::Record)]
pub struct CategoryRecord {
    pub id: String,
    pub name: String,
    pub icon: String,
    pub color_hex: String,
    pub is_system: bool,
    pub sort_order: i32,
}

/// A persisted Smart Rule shaped for native settings screens.
#[derive(uniffi::Record)]
pub struct SmartRuleRecord {
    pub id: String,
    pub name: String,
    pub keyword: String,
    pub match_type: String,
    pub category_id: String,
    pub category_name: String,
    pub kind: TransactionKind,
    pub is_enabled: bool,
    pub sort_order: i32,
}

/// Everything the Home screen needs in one call.
#[derive(uniffi::Record)]
pub struct HomeDashboardRecord {
    pub period_expense_minor: i64,
    pub period_income_minor: i64,
    pub recent_transactions: Vec<TransactionSummaryRecord>,
}

/// Outcome of the Rust-owned SMS ingestion pipeline.
#[derive(Debug, PartialEq, Eq, uniffi::Enum)]
pub enum SmsIngestStatus {
    Inserted,
    QueuedForReview,
    Ignored,
    Duplicate,
}

#[derive(uniffi::Record)]
pub struct SmsIngestResult {
    pub status: SmsIngestStatus,
    pub transaction_id: Option<String>,
    pub review_id: Option<String>,
    pub reference: Option<String>,
}

/// A review queue row shaped for native review screens.
#[derive(uniffi::Record)]
pub struct ReviewQueueRecord {
    pub id: String,
    pub sender: Option<String>,
    pub raw_sms: String,
    pub received_at_epoch_ms: i64,
    pub provider_id: Option<String>,
    pub reason: String,
    pub candidate_amount_minor: Option<i64>,
    pub candidate_kind: Option<TransactionKind>,
    pub fee_minor: Option<i64>,
    pub balance_after_minor: Option<i64>,
    pub reference: Option<String>,
    pub party: Option<String>,
    pub merchant: Option<String>,
    pub category_id: Option<String>,
    pub account_last4: Option<String>,
    pub account_hint: Option<String>,
}

/// Counts returned after replacing the local database with Rust-owned demo
/// records.
#[derive(uniffi::Record)]
pub struct DemoDataSummaryRecord {
    pub accounts: u32,
    pub transactions: u32,
    pub budgets: u32,
    pub subscriptions: u32,
}

#[cfg(test)]
mod ingestion_tests {
    use super::{
        AccountInput, CategoryInput, CentwiseCore, SmartRuleInput, SmsIngestStatus, TransactionKind,
    };

    fn core_with_account() -> std::sync::Arc<CentwiseCore> {
        let core = CentwiseCore::open(":memory:".into()).expect("open core");
        core.insert_account(AccountInput {
            id: "acct-1".into(),
            name: "bKash".into(),
            provider: "bkash".into(),
            last_four: None,
            starting_balance_minor: 0,
            archived: false,
        })
        .expect("account");
        core
    }

    #[test]
    fn ingest_sms_inserts_when_one_account_matches() {
        let core = core_with_account();
        let result = core
            .ingest_sms(
                "Payment of Tk 650.00 to Foodpanda successful. Ref: FP8392.".into(),
                Some("bKash".into()),
                1_700_000_000_000,
            )
            .expect("ingest");

        assert_eq!(result.status, SmsIngestStatus::Inserted);
        assert_eq!(result.transaction_id.as_deref(), Some("sms-FP8392"));
        assert_eq!(
            core.account_balance("acct-1".into()).expect("balance"),
            -65_000
        );
    }

    #[test]
    fn ingest_sms_queues_when_account_is_ambiguous() {
        let core = CentwiseCore::open(":memory:".into()).expect("open core");
        for id in ["acct-1", "acct-2"] {
            core.insert_account(AccountInput {
                id: id.into(),
                name: format!("bKash {id}"),
                provider: "bkash".into(),
                last_four: None,
                starting_balance_minor: 0,
                archived: false,
            })
            .expect("account");
        }

        let result = core
            .ingest_sms(
                "Payment of Tk 650.00 to Foodpanda successful. Ref: FP8393.".into(),
                Some("bKash".into()),
                1_700_000_000_000,
            )
            .expect("ingest");

        assert_eq!(result.status, SmsIngestStatus::QueuedForReview);
        assert!(result.review_id.is_some());
        assert_eq!(core.list_review_queue(10).expect("review list").len(), 1);
    }

    #[test]
    fn ingest_sms_ignores_otp_and_duplicate_references() {
        let core = core_with_account();
        let otp = core
            .ingest_sms(
                "Your OTP is 123456. Do not share this verification code.".into(),
                Some("bKash".into()),
                1_700_000_000_000,
            )
            .expect("otp ingest");
        assert_eq!(otp.status, SmsIngestStatus::Ignored);

        let message = "Payment of Tk 650.00 to Foodpanda successful. Ref: FP8394.";
        core.ingest_sms(message.into(), Some("bKash".into()), 1_700_000_000_000)
            .expect("first ingest");
        let duplicate = core
            .ingest_sms(message.into(), Some("bKash".into()), 1_700_000_000_001)
            .expect("duplicate ingest");
        assert_eq!(duplicate.status, SmsIngestStatus::Duplicate);

        let no_reference = "Payment of Tk 12.00 to Coffee Shop successful.";
        core.ingest_sms(no_reference.into(), Some("bKash".into()), 1_700_000_000_010)
            .expect("no-reference ingest");
        let no_reference_duplicate = core
            .ingest_sms(no_reference.into(), Some("bKash".into()), 1_700_000_000_011)
            .expect("no-reference duplicate ingest");
        assert_eq!(no_reference_duplicate.status, SmsIngestStatus::Duplicate);
    }

    #[test]
    fn ingest_sms_applies_a_persisted_rust_rule() {
        let core = core_with_account();
        core.insert_category(CategoryInput {
            id: "coffee".into(),
            name: "Coffee".into(),
            icon: "cup".into(),
            color_hex: "#A855F7".into(),
        })
        .expect("category");
        core.insert_rule(SmartRuleInput {
            id: "rule-coffee".into(),
            name: "Coffee merchants".into(),
            keyword: "CoffeeHouse".into(),
            match_type: "contains".into(),
            category_id: "coffee".into(),
            kind: TransactionKind::Expense,
            is_enabled: true,
        })
        .expect("rule");

        let result = core
            .ingest_sms(
                "Payment of Tk 120.00 to CoffeeHouse successful. Ref: COF1.".into(),
                Some("bKash".into()),
                1_700_000_000_000,
            )
            .expect("ingest");

        assert_eq!(result.status, SmsIngestStatus::Inserted);
        let transaction = core
            .list_transactions(10)
            .expect("transactions")
            .into_iter()
            .find(|item| item.reference.as_deref() == Some("COF1"))
            .expect("stored transaction");
        assert_eq!(transaction.category_id, "coffee");
        assert_eq!(core.list_rules().expect("rules").len(), 6);
    }

    #[test]
    fn demo_data_is_owned_by_rust_and_can_be_reset() {
        let core = CentwiseCore::open(":memory:".into()).expect("open core");
        let summary = core.load_demo_data().expect("load demo");

        assert_eq!(summary.accounts, 4);
        assert!(summary.transactions > 400);
        assert_eq!(summary.budgets, 4);
        assert_eq!(summary.subscriptions, 3);
        assert_eq!(core.list_accounts().expect("accounts").len(), 4);
        assert!(core.list_transactions(10_000).expect("transactions").len() > 400);
        assert_eq!(core.list_budgets().expect("budgets").len(), 4);
        assert_eq!(core.list_subscriptions().expect("subscriptions").len(), 3);

        core.reset_to_empty_database().expect("reset demo");
        assert!(core.list_review_queue(10).expect("queue").is_empty());
    }

    #[test]
    fn system_categories_are_exposed_through_ffi() {
        let core = CentwiseCore::open(":memory:".into()).expect("open core");

        let categories = core.list_categories().expect("categories");

        assert_eq!(categories.len(), 11);
        assert_eq!(categories[0].id, "food");
        assert!(categories.iter().all(|category| category.is_system));
    }
}

/// Errors surfaced to Kotlin/Swift.
#[derive(Debug, uniffi::Error)]
pub enum CentwiseError {
    Db { reason: String },
    Invalid { reason: String },
}

impl std::fmt::Display for CentwiseError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            CentwiseError::Db { reason } => write!(f, "database error: {reason}"),
            CentwiseError::Invalid { reason } => write!(f, "invalid input: {reason}"),
        }
    }
}

impl From<centwise_db::DbError> for CentwiseError {
    fn from(error: centwise_db::DbError) -> Self {
        match error {
            centwise_db::DbError::Invalid(reason) => CentwiseError::Invalid { reason },
            other => CentwiseError::Db {
                reason: other.to_string(),
            },
        }
    }
}

/// Implemented in Kotlin/Swift. Fires after every data write.
#[uniffi::export(callback_interface)]
pub trait ChangeListener: Send + Sync {
    fn on_data_changed(&self);
}

/// Adapter that lets a foreign listener plug into the database registry.
struct ForeignObserver(Box<dyn ChangeListener>);

impl DataObserver for ForeignObserver {
    fn data_changed(&self) {
        self.0.on_data_changed();
    }
}

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
        self.database
            .insert_transaction(&domain::NewTransaction {
                id: input.id,
                title: input.title,
                amount_minor: input.amount_minor,
                currency: input.currency,
                transaction_type: input.kind.into(),
                category_id: input.category_id,
                occurred_at_epoch_ms: input.occurred_at_epoch_ms,
                account_id: input.account_id,
                reference: input.reference,
                balance_after_minor: input.balance_after_minor,
                fee_minor: input.fee_minor,
                notes: input.notes,
                raw_sms: input.raw_sms,
                is_auto_tracked: input.is_auto_tracked,
            })
            .map_err(CentwiseError::from)
    }

    pub fn update_transaction(&self, input: TransactionInput) -> Result<bool, CentwiseError> {
        self.database
            .update_transaction(&transaction_input_to_domain(input))
            .map_err(CentwiseError::from)
    }

    pub fn list_transactions(&self, limit: u32) -> Result<Vec<TransactionRecord>, CentwiseError> {
        self.database
            .list_transactions(limit)
            .map(|items| items.into_iter().map(transaction_record).collect())
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

                    if matches.len() == 1 {
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
                            account_id: matches[0].id.clone(),
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
                    if !matches!(reason, centwise_parser::RejectReason::NoAmountFound) {
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

    pub fn list_review_queue(&self, limit: u32) -> Result<Vec<ReviewQueueRecord>, CentwiseError> {
        self.database
            .list_review_queue(limit)
            .map(|items| items.into_iter().map(review_queue_record).collect())
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
        self.database
            .convert_review_queue_item(
                &id,
                &domain::NewTransaction {
                    id: input.id,
                    title: input.title,
                    amount_minor: input.amount_minor,
                    currency: input.currency,
                    transaction_type: input.kind.into(),
                    category_id: input.category_id,
                    occurred_at_epoch_ms: input.occurred_at_epoch_ms,
                    account_id: input.account_id,
                    reference: input.reference,
                    balance_after_minor: input.balance_after_minor,
                    fee_minor: input.fee_minor,
                    notes: input.notes,
                    raw_sms: input.raw_sms,
                    is_auto_tracked: input.is_auto_tracked,
                },
            )
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

fn review_queue_record(item: domain::ReviewQueueItem) -> ReviewQueueRecord {
    ReviewQueueRecord {
        id: item.id,
        sender: item.sender,
        raw_sms: item.raw_sms,
        received_at_epoch_ms: item.received_at_epoch_ms,
        provider_id: item.provider_id,
        reason: item.reason,
        candidate_amount_minor: item.candidate_amount_minor,
        candidate_kind: item.candidate_type.map(Into::into),
        fee_minor: item.fee_minor,
        balance_after_minor: item.balance_after_minor,
        reference: item.reference,
        party: item.party,
        merchant: item.merchant,
        category_id: item.category_id,
        account_last4: item.account_last4,
        account_hint: item.account_hint,
    }
}

fn transaction_record(item: domain::Transaction) -> TransactionRecord {
    TransactionRecord {
        id: item.id,
        title: item.title,
        amount_minor: item.amount_minor,
        currency: item.currency,
        kind: item.transaction_type.into(),
        category_id: item.category_id,
        occurred_at_epoch_ms: item.occurred_at_epoch_ms,
        account_id: item.account_id,
        reference: item.reference,
        balance_after_minor: item.balance_after_minor,
        fee_minor: item.fee_minor,
        notes: item.notes,
        raw_sms: item.raw_sms,
        is_auto_tracked: item.is_auto_tracked,
    }
}

fn account_record(item: domain::AccountSummary) -> AccountRecord {
    AccountRecord {
        id: item.id,
        name: item.name,
        provider: item.provider,
        last_four: item.last_four,
        balance_minor: item.balance_minor,
        archived: item.archived,
    }
}

fn budget_record(item: domain::BudgetWithProgress) -> BudgetRecord {
    BudgetRecord {
        id: item.id,
        category_id: item.category_id,
        category_name: item.category_name,
        limit_minor: item.limit_minor,
        period: item.period,
        start_epoch_ms: item.start_epoch_ms,
        end_epoch_ms: item.end_epoch_ms,
        spent_minor: item.spent_minor,
    }
}

fn subscription_record(item: domain::SubscriptionSummary) -> SubscriptionRecord {
    SubscriptionRecord {
        id: item.id,
        name: item.name,
        amount_minor: item.amount_minor,
        billing_cycle: item.billing_cycle,
        next_due_epoch_ms: item.next_due_epoch_ms,
        is_active: item.is_active,
    }
}

fn category_record(item: domain::CategorySummary) -> CategoryRecord {
    CategoryRecord {
        id: item.id,
        name: item.name,
        icon: item.icon,
        color_hex: item.color_hex,
        is_system: item.is_system,
        sort_order: item.sort_order,
    }
}

fn account_record_to_domain(input: AccountInput) -> domain::Account {
    domain::Account {
        id: input.id,
        name: input.name,
        provider: input.provider,
        last_four: input.last_four,
        balance_minor: input.starting_balance_minor,
        archived: input.archived,
    }
}

fn transaction_input_to_domain(input: TransactionInput) -> domain::NewTransaction {
    domain::NewTransaction {
        id: input.id,
        title: input.title,
        amount_minor: input.amount_minor,
        currency: input.currency,
        transaction_type: input.kind.into(),
        category_id: input.category_id,
        occurred_at_epoch_ms: input.occurred_at_epoch_ms,
        account_id: input.account_id,
        reference: input.reference,
        balance_after_minor: input.balance_after_minor,
        fee_minor: input.fee_minor,
        notes: input.notes,
        raw_sms: input.raw_sms,
        is_auto_tracked: input.is_auto_tracked,
    }
}

fn budget_input_to_domain(input: BudgetInput) -> domain::NewBudget {
    domain::NewBudget {
        id: input.id,
        category_id: input.category_id,
        limit_minor: input.limit_minor,
        period: input.period,
        start_epoch_ms: input.start_epoch_ms,
        end_epoch_ms: input.end_epoch_ms,
    }
}

fn subscription_input_to_domain(input: SubscriptionInput) -> domain::NewSubscription {
    domain::NewSubscription {
        id: input.id,
        name: input.name,
        amount_minor: input.amount_minor,
        billing_cycle: input.billing_cycle,
        next_due_epoch_ms: input.next_due_epoch_ms,
        is_active: input.is_active,
    }
}

fn smart_rule_input_to_domain(
    input: SmartRuleInput,
) -> Result<domain::NewSmartRule, CentwiseError> {
    let match_type = domain::RuleMatchType::from_str_value(&input.match_type).ok_or_else(|| {
        CentwiseError::Invalid {
            reason: format!("unknown rule match type: {}", input.match_type),
        }
    })?;
    Ok(domain::NewSmartRule {
        id: input.id,
        name: input.name,
        keyword: input.keyword,
        match_type,
        category_id: input.category_id,
        transaction_type: input.kind.into(),
        is_enabled: input.is_enabled,
    })
}

fn smart_rule_record(
    item: domain::SmartRule,
    categories: &[domain::CategorySummary],
) -> SmartRuleRecord {
    let category_name = categories
        .iter()
        .find(|category| category.id == item.category_id)
        .map(|category| category.name.clone())
        .unwrap_or_else(|| item.category_id.clone());
    SmartRuleRecord {
        id: item.id,
        name: item.name,
        keyword: item.keyword,
        match_type: item.match_type.as_str().into(),
        category_id: item.category_id,
        category_name,
        kind: item.transaction_type.into(),
        is_enabled: item.is_enabled,
        sort_order: item.sort_order,
    }
}

fn sms_transaction_id(reference: Option<&str>, body: &str) -> String {
    if let Some(reference) = reference.filter(|value| !value.trim().is_empty()) {
        return format!("sms-{reference}");
    }

    let mut hasher = std::collections::hash_map::DefaultHasher::new();
    body.hash(&mut hasher);
    format!("sms-{:016x}", hasher.finish())
}

//! UniFFI surface for the Centwise core.
//!
//! Exposes the shared database (open, write, query) plus the change
//! notification callback that native reactive layers wrap into StateFlow
//! (Android) and Combine (iOS).

uniffi::setup_scaffolding!();

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
    pub notes: Option<String>,
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

/// Everything the Home screen needs in one call.
#[derive(uniffi::Record)]
pub struct HomeDashboardRecord {
    pub period_expense_minor: i64,
    pub period_income_minor: i64,
    pub recent_transactions: Vec<TransactionSummaryRecord>,
}

/// Errors surfaced to Kotlin/Swift.
#[derive(Debug, uniffi::Error)]
pub enum CentwiseError {
    Db { message: String },
    Invalid { message: String },
}

impl std::fmt::Display for CentwiseError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            CentwiseError::Db { message } => write!(f, "database error: {message}"),
            CentwiseError::Invalid { message } => write!(f, "invalid input: {message}"),
        }
    }
}

impl From<centwise_db::DbError> for CentwiseError {
    fn from(error: centwise_db::DbError) -> Self {
        match error {
            centwise_db::DbError::Invalid(message) => CentwiseError::Invalid { message },
            other => CentwiseError::Db {
                message: other.to_string(),
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
                queries.insert_account(&domain::Account {
                    id: account.id,
                    name: account.name,
                    provider: account.provider,
                    last_four: account.last_four,
                    balance_minor: account.starting_balance_minor,
                    archived: false,
                })
            })
            .map_err(CentwiseError::from)
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
                notes: input.notes,
                is_auto_tracked: input.is_auto_tracked,
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

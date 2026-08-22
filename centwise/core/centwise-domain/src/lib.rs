//! Shared Centwise domain models.
//!
//! Conventions:
//! - Money is stored as signed 64-bit minor units (poisha for BDT, 1 taka = 100 poisha).
//! - Dates are epoch milliseconds.
//! - Ids are caller-generated stable strings.

/// Transaction kind as parsed from SMS or entered manually.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum TransactionType {
    Expense,
    Income,
    Transfer,
    Refund,
}

impl TransactionType {
    pub fn as_str(&self) -> &'static str {
        match self {
            TransactionType::Expense => "expense",
            TransactionType::Income => "income",
            TransactionType::Transfer => "transfer",
            TransactionType::Refund => "refund",
        }
    }

    pub fn from_str_value(value: &str) -> Option<Self> {
        match value {
            "expense" => Some(TransactionType::Expense),
            "income" => Some(TransactionType::Income),
            "transfer" => Some(TransactionType::Transfer),
            "refund" => Some(TransactionType::Refund),
            _ => None,
        }
    }
}

/// A spending/income category.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Category {
    pub id: String,
    pub name: String,
    pub icon: String,
    pub color_hex: String,
}

/// A bank account, MFS wallet, card, or cash wallet.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Account {
    pub id: String,
    pub name: String,
    pub provider: String,
    pub last_four: Option<String>,
    pub balance_minor: i64,
    pub archived: bool,
}

/// A transaction as stored in the database.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Transaction {
    pub id: String,
    pub title: String,
    pub amount_minor: i64,
    pub currency: String,
    pub transaction_type: TransactionType,
    pub category_id: String,
    pub occurred_at_epoch_ms: i64,
    pub account_id: String,
    pub reference: Option<String>,
    pub balance_after_minor: Option<i64>,
    pub notes: Option<String>,
    pub is_auto_tracked: bool,
}

/// A transaction to insert.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct NewTransaction {
    pub id: String,
    pub title: String,
    pub amount_minor: i64,
    pub currency: String,
    pub transaction_type: TransactionType,
    pub category_id: String,
    pub occurred_at_epoch_ms: i64,
    pub account_id: String,
    pub reference: Option<String>,
    pub balance_after_minor: Option<i64>,
    pub notes: Option<String>,
    pub is_auto_tracked: bool,
}

impl NewTransaction {
    pub fn into_stored(self) -> Transaction {
        Transaction {
            id: self.id,
            title: self.title,
            amount_minor: self.amount_minor,
            currency: self.currency,
            transaction_type: self.transaction_type,
            category_id: self.category_id,
            occurred_at_epoch_ms: self.occurred_at_epoch_ms,
            account_id: self.account_id,
            reference: self.reference,
            balance_after_minor: self.balance_after_minor,
            notes: self.notes,
            is_auto_tracked: self.is_auto_tracked,
        }
    }
}

/// A short transaction representation for lists.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct TransactionSummary {
    pub id: String,
    pub title: String,
    pub amount_minor: i64,
    pub transaction_type: TransactionType,
    pub category_name: String,
    pub category_icon: String,
    pub category_color_hex: String,
    pub occurred_at_epoch_ms: i64,
    pub account_name: String,
}

/// Everything the Home screen needs, computed once.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct HomeDashboard {
    pub period_expense_minor: i64,
    pub period_income_minor: i64,
    pub recent_transactions: Vec<TransactionSummary>,
}

/// Default category seed shared by fresh installs.
pub fn default_categories() -> Vec<Category> {
    vec![
        Category {
            id: "food".into(),
            name: "Food & Dining".into(),
            icon: "fork.knife".into(),
            color_hex: "#F97316".into(),
        },
        Category {
            id: "transport".into(),
            name: "Transport".into(),
            icon: "car".into(),
            color_hex: "#06B6D4".into(),
        },
        Category {
            id: "shopping".into(),
            name: "Shopping".into(),
            icon: "bag".into(),
            color_hex: "#EC4899".into(),
        },
        Category {
            id: "bills".into(),
            name: "Bills & Utilities".into(),
            icon: "bolt".into(),
            color_hex: "#EAB308".into(),
        },
        Category {
            id: "recharge".into(),
            name: "Mobile Recharge".into(),
            icon: "antenna.radiowaves.left.and.right".into(),
            color_hex: "#8B5CF6".into(),
        },
        Category {
            id: "salary".into(),
            name: "Salary".into(),
            icon: "banknote".into(),
            color_hex: "#10B981".into(),
        },
        Category {
            id: "transfer".into(),
            name: "Transfers".into(),
            icon: "arrow.left.arrow.right".into(),
            color_hex: "#3B82F6".into(),
        },
        Category {
            id: "health".into(),
            name: "Healthcare".into(),
            icon: "cross.case".into(),
            color_hex: "#EF4444".into(),
        },
        Category {
            id: "entertainment".into(),
            name: "Entertainment".into(),
            icon: "play.tv".into(),
            color_hex: "#6366F1".into(),
        },
        Category {
            id: "education".into(),
            name: "Education".into(),
            icon: "book".into(),
            color_hex: "#14B8A6".into(),
        },
        Category {
            id: "other".into(),
            name: "Other".into(),
            icon: "square.grid.2x2".into(),
            color_hex: "#64748B".into(),
        },
    ]
}

//! Shared Centwise domain models.
//!
//! Conventions:
//! - Money is stored as signed 64-bit minor units (poisha for BDT, 1 taka = 100 poisha).
//! - Dates are epoch milliseconds.
//! - Ids are caller-generated stable strings.

/// Transaction kind as parsed from SMS or entered manually.
#[derive(Debug, Clone, Copy, PartialEq, Eq, serde::Serialize, serde::Deserialize)]
pub enum TransactionType {
    Expense,
    Income,
    Transfer,
    Refund,
}

#[cfg(test)]
mod tests {
    use super::TransactionType;

    #[test]
    fn transaction_type_round_trips_through_json() {
        let variants = [
            TransactionType::Expense,
            TransactionType::Income,
            TransactionType::Transfer,
            TransactionType::Refund,
        ];

        for variant in variants {
            let encoded = serde_json::to_string(&variant).expect("transaction type serializes");
            let decoded: TransactionType =
                serde_json::from_str(&encoded).expect("transaction type deserializes");
            assert_eq!(decoded, variant);
        }
    }
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

/// A category row read from the Rust-owned database.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct CategorySummary {
    pub id: String,
    pub name: String,
    pub icon: String,
    pub color_hex: String,
    pub is_system: bool,
    pub sort_order: i32,
}

/// A user-created category to persist in the shared database.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct NewCategory {
    pub id: String,
    pub name: String,
    pub icon: String,
    pub color_hex: String,
}

/// Match operation used by a persisted Smart Rule.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum RuleMatchType {
    Contains,
    StartsWith,
    ExactlyMatches,
}

impl RuleMatchType {
    pub fn as_str(&self) -> &'static str {
        match self {
            RuleMatchType::Contains => "contains",
            RuleMatchType::StartsWith => "starts_with",
            RuleMatchType::ExactlyMatches => "exactly_matches",
        }
    }

    pub fn from_str_value(value: &str) -> Option<Self> {
        match value {
            "contains" => Some(RuleMatchType::Contains),
            "starts_with" => Some(RuleMatchType::StartsWith),
            "exactly_matches" => Some(RuleMatchType::ExactlyMatches),
            _ => None,
        }
    }

    pub fn matches(&self, value: &str, keyword: &str) -> bool {
        let value = value.to_lowercase();
        let keyword = keyword.trim().to_lowercase();
        if keyword.is_empty() {
            return false;
        }

        match self {
            RuleMatchType::Contains => value.contains(&keyword),
            RuleMatchType::StartsWith => value.starts_with(&keyword),
            RuleMatchType::ExactlyMatches => value == keyword,
        }
    }
}

/// A Smart Rule stored in Rust SQLite and applied during SMS ingestion.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct SmartRule {
    pub id: String,
    pub name: String,
    pub keyword: String,
    pub match_type: RuleMatchType,
    pub category_id: String,
    pub transaction_type: TransactionType,
    pub is_enabled: bool,
    pub sort_order: i32,
}

/// A Smart Rule to insert or update.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct NewSmartRule {
    pub id: String,
    pub name: String,
    pub keyword: String,
    pub match_type: RuleMatchType,
    pub category_id: String,
    pub transaction_type: TransactionType,
    pub is_enabled: bool,
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
    pub fee_minor: Option<i64>,
    pub notes: Option<String>,
    pub raw_sms: Option<String>,
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
    pub fee_minor: Option<i64>,
    pub notes: Option<String>,
    pub raw_sms: Option<String>,
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
            fee_minor: self.fee_minor,
            notes: self.notes,
            raw_sms: self.raw_sms,
            is_auto_tracked: self.is_auto_tracked,
        }
    }
}

/// An ambiguous or otherwise reviewable financial SMS persisted until the
/// user resolves it or dismisses it.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ReviewQueueItem {
    pub id: String,
    pub sender: Option<String>,
    pub raw_sms: String,
    pub received_at_epoch_ms: i64,
    pub provider_id: Option<String>,
    pub reason: String,
    pub candidate_amount_minor: Option<i64>,
    pub candidate_type: Option<TransactionType>,
    pub fee_minor: Option<i64>,
    pub balance_after_minor: Option<i64>,
    pub reference: Option<String>,
    pub party: Option<String>,
    pub merchant: Option<String>,
    pub category_id: Option<String>,
    pub account_last4: Option<String>,
    pub account_hint: Option<String>,
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

/// Analytics: spending grouped by category for a period (expenses only).
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct CategorySpendSummary {
    pub category_id: String,
    pub category_name: String,
    pub category_icon: String,
    pub category_color_hex: String,
    pub total_minor: i64,
    pub transaction_count: i64,
}

/// Analytics: spending grouped by transaction title (merchant/counterparty).
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct MerchantSpendSummary {
    pub merchant: String,
    pub total_minor: i64,
    pub transaction_count: i64,
}

/// Analytics: total expenses for one calendar month.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct MonthlySpend {
    pub year: i32,
    pub month: u32, // 1..=12
    pub total_expense_minor: i64,
}

/// Everything needed to render Analytics for one period and type filter.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct AnalyticsSnapshot {
    pub total_income_minor: i64,
    pub total_expense_minor: i64,
    pub transaction_count: i64,
    pub category_breakdown: Vec<CategorySpendSummary>,
    pub top_merchants: Vec<MerchantSpendSummary>,
    pub monthly_trends: Vec<MonthlySpend>,
}

/// Account row for lists.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct AccountSummary {
    pub id: String,
    pub name: String,
    pub provider: String,
    pub last_four: Option<String>,
    pub balance_minor: i64,
    pub archived: bool,
}

/// Budget with live spending progress within its period.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct BudgetWithProgress {
    pub id: String,
    pub category_id: String,
    pub category_name: String,
    pub category_icon: String,
    pub category_color_hex: String,
    pub limit_minor: i64,
    pub period: String,
    pub start_epoch_ms: i64,
    pub end_epoch_ms: i64,
    pub spent_minor: i64,
}

/// Subscription row for lists.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct SubscriptionSummary {
    pub id: String,
    pub name: String,
    pub amount_minor: i64,
    pub billing_cycle: String,
    pub next_due_epoch_ms: i64,
    pub is_active: bool,
}

/// Budget period for inserts.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct NewBudget {
    pub id: String,
    pub category_id: String,
    pub limit_minor: i64,
    pub period: String,
    pub start_epoch_ms: i64,
    pub end_epoch_ms: i64,
}

/// Subscription to insert.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct NewSubscription {
    pub id: String,
    pub name: String,
    pub amount_minor: i64,
    pub billing_cycle: String,
    pub next_due_epoch_ms: i64,
    pub is_active: bool,
}

pub mod default_categories;
pub mod default_rules;

pub use default_categories::default_categories;
pub use default_rules::{default_merchant_categories, default_rules, DefaultMerchantCategory};

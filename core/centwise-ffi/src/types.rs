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
    pub account_provider: Option<String>,
    pub account_name: Option<String>,
    pub account_last_four: Option<String>,
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

#[derive(uniffi::Record)]
pub struct AnalyticsCategoryRecord {
    pub category_id: String,
    pub category_name: String,
    pub category_icon: String,
    pub category_color_hex: String,
    pub total_minor: i64,
    pub transaction_count: i64,
}

#[derive(uniffi::Record)]
pub struct AnalyticsMerchantRecord {
    pub merchant: String,
    pub total_minor: i64,
    pub transaction_count: i64,
}

#[derive(uniffi::Record)]
pub struct AnalyticsMonthlyRecord {
    pub year: i32,
    pub month: u32,
    pub total_expense_minor: i64,
}

#[derive(uniffi::Record)]
pub struct AnalyticsSnapshotRecord {
    pub total_income_minor: i64,
    pub total_expense_minor: i64,
    pub transaction_count: i64,
    pub category_breakdown: Vec<AnalyticsCategoryRecord>,
    pub top_merchants: Vec<AnalyticsMerchantRecord>,
    pub monthly_trends: Vec<AnalyticsMonthlyRecord>,
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

/// An SMS supplied by a native platform to the shared ingestion pipeline.
#[derive(uniffi::Record)]
pub struct SmsBatchMessage {
    pub body: String,
    pub sender_hint: Option<String>,
    pub occurred_at_epoch_ms: i64,
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

#[derive(uniffi::Record)]
pub struct GroupMemberRecord {
    pub id: String,
    pub group_id: String,
    pub name: String,
    pub avatar: String,
    pub phone: String,
    pub is_admin: bool,
    pub deposit_paid_minor: i64,
    pub total_spent_share_minor: i64,
    pub net_balance_minor: i64,
}

#[derive(uniffi::Record)]
pub struct GroupExpenseItemRecord {
    pub id: String,
    pub expense_id: String,
    pub group_id: String,
    pub name: String,
    pub quantity: String,
    pub price_minor: i64,
}

#[derive(uniffi::Record)]
pub struct GroupExpenseRecord {
    pub id: String,
    pub group_id: String,
    pub spender_id: String,
    pub spender_name: String,
    pub spender_avatar: String,
    pub occurred_at_epoch_ms: i64,
    pub date_formatted: String,
    pub total_amount_minor: i64,
    pub split_per_member_minor: i64,
    pub notes: String,
    pub items: Vec<GroupExpenseItemRecord>,
}

#[derive(uniffi::Record)]
pub struct GroupDepositRecord {
    pub id: String,
    pub group_id: String,
    pub member_id: String,
    pub member_name: String,
    pub member_avatar: String,
    pub amount_minor: i64,
    pub occurred_at_epoch_ms: i64,
    pub date_formatted: String,
    pub payment_method: String,
    pub note: String,
}

#[derive(uniffi::Record)]
pub struct GroupRecord {
    pub id: String,
    pub name: String,
    pub group_type: String,
    pub description: String,
    pub join_code: String,
    pub month_name: String,
    pub monthly_target_minor: i64,
    pub members: Vec<GroupMemberRecord>,
    pub daily_expenses: Vec<GroupExpenseRecord>,
    pub deposits: Vec<GroupDepositRecord>,
    pub has_new_activity: bool,
    pub created_at_epoch_ms: i64,
}

#[derive(uniffi::Record)]
pub struct GroupExpenseItemInput {
    pub name: String,
    pub quantity: String,
    pub price_minor: i64,
}

#[derive(uniffi::Record)]
pub struct GroupMemberShareRecord {
    pub member_id: String,
    pub member_name: String,
    pub member_avatar: String,
    pub deposit_paid_minor: i64,
    pub total_spent_share_minor: i64,
    pub net_balance_minor: i64,
}

#[derive(uniffi::Record)]
pub struct GroupItemCategorySpendRecord {
    pub category_name: String,
    pub total_minor: i64,
    pub percentage: f64,
    pub item_count: i64,
}

#[derive(uniffi::Record)]
pub struct GroupSettlementRecord {
    pub from_member_id: String,
    pub from_member_name: String,
    pub to_member_id: String,
    pub to_member_name: String,
    pub amount_minor: i64,
}

#[derive(uniffi::Record)]
pub struct GroupAnalyticsRecord {
    pub group_id: String,
    pub group_name: String,
    pub total_deposit_minor: i64,
    pub total_expense_minor: i64,
    pub remaining_balance_minor: i64,
    pub transaction_count: i64,
    pub expense_count: i64,
    pub deposit_count: i64,
    pub member_shares: Vec<GroupMemberShareRecord>,
    pub category_breakdown: Vec<GroupItemCategorySpendRecord>,
    pub top_spenders: Vec<AnalyticsMerchantRecord>,
    pub monthly_trends: Vec<AnalyticsMonthlyRecord>,
    pub settlements: Vec<GroupSettlementRecord>,
}

//! Shared Centwise models for Shared Groups (Mess & Family budget tracking).

use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum GroupType {
    BachelorMess,
    Family,
    Flatmates,
    Trip,
    Other,
}

impl GroupType {
    pub fn as_str(&self) -> &'static str {
        match self {
            GroupType::BachelorMess => "mess",
            GroupType::Family => "family",
            GroupType::Flatmates => "flatmates",
            GroupType::Trip => "trip",
            GroupType::Other => "other",
        }
    }

    pub fn from_str_value(value: &str) -> Self {
        match value.to_lowercase().as_str() {
            "mess" | "bachelormess" => GroupType::BachelorMess,
            "family" => GroupType::Family,
            "flatmates" => GroupType::Flatmates,
            "trip" => GroupType::Trip,
            _ => GroupType::Other,
        }
    }
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct GroupMember {
    pub id: String,
    pub group_id: String,
    pub name: String,
    pub avatar: String,
    pub phone: String,
    pub is_admin: bool,
    pub deposit_paid_minor: i64,
    pub total_spent_share_minor: i64,
}

impl GroupMember {
    pub fn net_balance_minor(&self) -> i64 {
        self.deposit_paid_minor - self.total_spent_share_minor
    }
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct GroupExpenseItem {
    pub id: String,
    pub expense_id: String,
    pub group_id: String,
    pub name: String,
    pub quantity: String,
    pub price_minor: i64,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct GroupExpenseEntry {
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
    pub items: Vec<GroupExpenseItem>,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct GroupDeposit {
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

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct SharedGroup {
    pub id: String,
    pub name: String,
    pub group_type: GroupType,
    pub description: String,
    pub join_code: String,
    pub month_name: String,
    pub monthly_target_minor: i64,
    pub members: Vec<GroupMember>,
    pub daily_expenses: Vec<GroupExpenseEntry>,
    pub deposits: Vec<GroupDeposit>,
    pub has_new_activity: bool,
    pub created_at_epoch_ms: i64,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct GroupMemberShareSummary {
    pub member_id: String,
    pub member_name: String,
    pub member_avatar: String,
    pub deposit_paid_minor: i64,
    pub total_spent_share_minor: i64,
    pub net_balance_minor: i64,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct GroupSettlementSummary {
    pub from_member_id: String,
    pub from_member_name: String,
    pub to_member_id: String,
    pub to_member_name: String,
    pub amount_minor: i64,
}

#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct GroupItemCategorySpend {
    pub category_name: String,
    pub total_minor: i64,
    pub percentage: f64,
    pub item_count: i64,
}

use crate::{MerchantSpendSummary, MonthlySpend};

#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct GroupAnalyticsSnapshot {
    pub group_id: String,
    pub group_name: String,
    pub total_deposit_minor: i64,
    pub total_expense_minor: i64,
    pub remaining_balance_minor: i64,
    pub transaction_count: i64,
    pub expense_count: i64,
    pub deposit_count: i64,
    pub member_shares: Vec<GroupMemberShareSummary>,
    pub category_breakdown: Vec<GroupItemCategorySpend>,
    pub top_spenders: Vec<MerchantSpendSummary>,
    pub monthly_trends: Vec<MonthlySpend>,
    pub settlements: Vec<GroupSettlementSummary>,
}

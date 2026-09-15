use std::hash::{Hash, Hasher};

use centwise_domain as domain;

use crate::error::CentwiseError;
use crate::types::*;

pub(crate) fn review_queue_record(item: domain::ReviewQueueItem) -> ReviewQueueRecord {
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

pub(crate) fn transaction_record(item: domain::Transaction) -> TransactionRecord {
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

pub(crate) fn account_record(item: domain::AccountSummary) -> AccountRecord {
    AccountRecord {
        id: item.id,
        name: item.name,
        provider: item.provider,
        last_four: item.last_four,
        balance_minor: item.balance_minor,
        archived: item.archived,
    }
}

pub(crate) fn budget_record(item: domain::BudgetWithProgress) -> BudgetRecord {
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

pub(crate) fn subscription_record(item: domain::SubscriptionSummary) -> SubscriptionRecord {
    SubscriptionRecord {
        id: item.id,
        name: item.name,
        amount_minor: item.amount_minor,
        billing_cycle: item.billing_cycle,
        next_due_epoch_ms: item.next_due_epoch_ms,
        is_active: item.is_active,
    }
}

pub(crate) fn category_record(item: domain::CategorySummary) -> CategoryRecord {
    CategoryRecord {
        id: item.id,
        name: item.name,
        icon: item.icon,
        color_hex: item.color_hex,
        is_system: item.is_system,
        sort_order: item.sort_order,
    }
}

pub(crate) fn account_record_to_domain(input: AccountInput) -> domain::Account {
    domain::Account {
        id: input.id,
        name: input.name,
        provider: input.provider,
        last_four: input.last_four,
        balance_minor: input.starting_balance_minor,
        archived: input.archived,
    }
}

pub(crate) fn transaction_input_to_domain(input: TransactionInput) -> domain::NewTransaction {
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

pub(crate) fn budget_input_to_domain(input: BudgetInput) -> domain::NewBudget {
    domain::NewBudget {
        id: input.id,
        category_id: input.category_id,
        limit_minor: input.limit_minor,
        period: input.period,
        start_epoch_ms: input.start_epoch_ms,
        end_epoch_ms: input.end_epoch_ms,
    }
}

pub(crate) fn subscription_input_to_domain(input: SubscriptionInput) -> domain::NewSubscription {
    domain::NewSubscription {
        id: input.id,
        name: input.name,
        amount_minor: input.amount_minor,
        billing_cycle: input.billing_cycle,
        next_due_epoch_ms: input.next_due_epoch_ms,
        is_active: input.is_active,
    }
}

pub(crate) fn smart_rule_input_to_domain(
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

pub(crate) fn smart_rule_record(
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

pub(crate) fn sms_transaction_id(
    reference: Option<&str>,
    body: &str,
    sender_hint: Option<&str>,
) -> String {
    if let Some(reference) = reference.filter(|value| !value.trim().is_empty()) {
        return format!("sms-{}", reference.trim().to_uppercase());
    }

    let mut hasher = std::collections::hash_map::DefaultHasher::new();
    sender_hint
        .unwrap_or_default()
        .trim()
        .to_lowercase()
        .hash(&mut hasher);
    centwise_normalization::normalize_sms_text(body)
        .to_lowercase()
        .hash(&mut hasher);
    format!("sms-{:016x}", hasher.finish())
}

pub(crate) fn group_member_record(m: domain::GroupMember) -> GroupMemberRecord {
    let net = m.net_balance_minor();
    GroupMemberRecord {
        id: m.id,
        group_id: m.group_id,
        name: m.name,
        avatar: m.avatar,
        phone: m.phone,
        is_admin: m.is_admin,
        deposit_paid_minor: m.deposit_paid_minor,
        total_spent_share_minor: m.total_spent_share_minor,
        net_balance_minor: net,
    }
}

pub(crate) fn group_expense_item_record(item: domain::GroupExpenseItem) -> GroupExpenseItemRecord {
    GroupExpenseItemRecord {
        id: item.id,
        expense_id: item.expense_id,
        group_id: item.group_id,
        name: item.name,
        quantity: item.quantity,
        price_minor: item.price_minor,
    }
}

pub(crate) fn group_expense_record(e: domain::GroupExpenseEntry) -> GroupExpenseRecord {
    GroupExpenseRecord {
        id: e.id,
        group_id: e.group_id,
        spender_id: e.spender_id,
        spender_name: e.spender_name,
        spender_avatar: e.spender_avatar,
        occurred_at_epoch_ms: e.occurred_at_epoch_ms,
        date_formatted: e.date_formatted,
        total_amount_minor: e.total_amount_minor,
        split_per_member_minor: e.split_per_member_minor,
        notes: e.notes,
        items: e.items.into_iter().map(group_expense_item_record).collect(),
    }
}

pub(crate) fn group_deposit_record(d: domain::GroupDeposit) -> GroupDepositRecord {
    GroupDepositRecord {
        id: d.id,
        group_id: d.group_id,
        member_id: d.member_id,
        member_name: d.member_name,
        member_avatar: d.member_avatar,
        amount_minor: d.amount_minor,
        occurred_at_epoch_ms: d.occurred_at_epoch_ms,
        date_formatted: d.date_formatted,
        payment_method: d.payment_method,
        note: d.note,
    }
}

pub(crate) fn group_record(g: domain::SharedGroup) -> GroupRecord {
    let group_type_str = g.group_type.as_str().to_string();
    GroupRecord {
        id: g.id,
        name: g.name,
        group_type: group_type_str,
        description: g.description,
        join_code: g.join_code,
        month_name: g.month_name,
        monthly_target_minor: g.monthly_target_minor,
        members: g.members.into_iter().map(group_member_record).collect(),
        daily_expenses: g
            .daily_expenses
            .into_iter()
            .map(group_expense_record)
            .collect(),
        deposits: g.deposits.into_iter().map(group_deposit_record).collect(),
        has_new_activity: g.has_new_activity,
        created_at_epoch_ms: g.created_at_epoch_ms,
    }
}

pub(crate) fn group_analytics_record(a: domain::GroupAnalyticsSnapshot) -> GroupAnalyticsRecord {
    GroupAnalyticsRecord {
        group_id: a.group_id,
        group_name: a.group_name,
        total_deposit_minor: a.total_deposit_minor,
        total_expense_minor: a.total_expense_minor,
        remaining_balance_minor: a.remaining_balance_minor,
        transaction_count: a.transaction_count,
        expense_count: a.expense_count,
        deposit_count: a.deposit_count,
        member_shares: a
            .member_shares
            .into_iter()
            .map(|m| GroupMemberShareRecord {
                member_id: m.member_id,
                member_name: m.member_name,
                member_avatar: m.member_avatar,
                deposit_paid_minor: m.deposit_paid_minor,
                total_spent_share_minor: m.total_spent_share_minor,
                net_balance_minor: m.net_balance_minor,
            })
            .collect(),
        category_breakdown: a
            .category_breakdown
            .into_iter()
            .map(|c| GroupItemCategorySpendRecord {
                category_name: c.category_name,
                total_minor: c.total_minor,
                percentage: c.percentage,
                item_count: c.item_count,
            })
            .collect(),
        top_spenders: a
            .top_spenders
            .into_iter()
            .map(|m| AnalyticsMerchantRecord {
                merchant: m.merchant,
                total_minor: m.total_minor,
                transaction_count: m.transaction_count,
            })
            .collect(),
        monthly_trends: a
            .monthly_trends
            .into_iter()
            .map(|t| AnalyticsMonthlyRecord {
                year: t.year,
                month: t.month,
                total_expense_minor: t.total_expense_minor,
            })
            .collect(),
        settlements: a
            .settlements
            .into_iter()
            .map(|s| GroupSettlementRecord {
                from_member_id: s.from_member_id,
                from_member_name: s.from_member_name,
                to_member_id: s.to_member_id,
                to_member_name: s.to_member_name,
                amount_minor: s.amount_minor,
            })
            .collect(),
    }
}

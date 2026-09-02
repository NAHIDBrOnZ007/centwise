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

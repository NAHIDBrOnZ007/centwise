use centwise_domain::{Account, NewBudget, NewSubscription, NewTransaction, TransactionType};

use crate::{DbResult, Queries};

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct DemoDataSummary {
    pub accounts: u32,
    pub transactions: u32,
    pub budgets: u32,
    pub subscriptions: u32,
}

pub(crate) fn populate(queries: &Queries<'_>, now_epoch_ms: i64) -> DbResult<DemoDataSummary> {
    let accounts = [
        ("demo-bkash", "Personal bKash", "bkash", Some("8899")),
        ("demo-nagad", "Nagad Primary", "nagad", Some("4422")),
        ("demo-brac", "Salary Account", "brac-bank", Some("8839")),
        ("demo-city", "CityMaxx Card", "city-bank", Some("9912")),
    ];
    for (id, name, provider, last_four) in accounts {
        queries.insert_account(&Account {
            id: id.into(),
            name: name.into(),
            provider: provider.into(),
            last_four: last_four.map(str::to_string),
            balance_minor: 0,
            archived: false,
        })?;
    }

    let day_ms = 86_400_000_i64;
    let hour_ms = 3_600_000_i64;
    let expense_templates = [
        ("Foodpanda BD", "food", "demo-bkash"),
        ("Star Kabab Dinner", "food", "demo-bkash"),
        ("North End Coffee", "food", "demo-city"),
        ("Pathao Rides", "transport", "demo-nagad"),
        ("Unimart Superstore", "food", "demo-city"),
        ("Daraz Online Shopping", "shopping", "demo-bkash"),
        ("Aarong Lifestyle", "shopping", "demo-city"),
        ("Cineplex Tickets", "entertainment", "demo-bkash"),
        ("Lazz Pharma", "health", "demo-bkash"),
    ];
    let mut transaction_count = 0_u32;

    for month_offset in 0..12_i64 {
        let salary_time = now_epoch_ms - month_offset * 30 * day_ms;
        queries.insert_transaction(&NewTransaction {
            id: format!("demo-salary-{month_offset}"),
            title: "Salary Deposit".into(),
            amount_minor: 8_500_000,
            currency: "BDT".into(),
            transaction_type: TransactionType::Income,
            category_id: "salary".into(),
            occurred_at_epoch_ms: salary_time,
            account_id: "demo-brac".into(),
            reference: Some(format!("DEMO-SALARY-{month_offset}")),
            balance_after_minor: None,
            fee_minor: None,
            notes: None,
            raw_sms: Some("Demo salary credit from TECH CORP.".into()),
            is_auto_tracked: false,
        })?;
        transaction_count += 1;

        queries.insert_transaction(&NewTransaction {
            id: format!("demo-rent-{month_offset}"),
            title: "Apartment Rent".into(),
            amount_minor: 2_800_000,
            currency: "BDT".into(),
            transaction_type: TransactionType::Expense,
            category_id: "bills".into(),
            occurred_at_epoch_ms: salary_time + 3 * day_ms,
            account_id: "demo-brac".into(),
            reference: Some(format!("DEMO-RENT-{month_offset}")),
            balance_after_minor: None,
            fee_minor: None,
            notes: None,
            raw_sms: Some("Demo rent debit.".into()),
            is_auto_tracked: false,
        })?;
        transaction_count += 1;
    }

    for day_offset in 0..365_i64 {
        let count = if day_offset % 3 == 0 {
            2
        } else if day_offset % 5 == 0 {
            3
        } else {
            1
        };
        let base_time = now_epoch_ms - day_offset * day_ms;
        for index in 0..count {
            let template = expense_templates
                [((day_offset * 7 + index * 3) as usize) % expense_templates.len()];
            let amount_taka = 150 + ((day_offset * 97 + index * 613) % 3_351);
            queries.insert_transaction(&NewTransaction {
                id: format!("demo-expense-{day_offset}-{index}"),
                title: template.0.into(),
                amount_minor: amount_taka * 100,
                currency: "BDT".into(),
                transaction_type: TransactionType::Expense,
                category_id: template.1.into(),
                occurred_at_epoch_ms: base_time - index * 4 * hour_ms,
                account_id: template.2.into(),
                reference: Some(format!("DEMO-EXPENSE-{day_offset}-{index}")),
                balance_after_minor: None,
                fee_minor: None,
                notes: None,
                raw_sms: Some(format!("Demo payment to {}.", template.0)),
                is_auto_tracked: false,
            })?;
            transaction_count += 1;
        }
    }

    let budget_start = now_epoch_ms - 30 * day_ms;
    let budget_end = now_epoch_ms + day_ms;
    let budgets = [
        ("demo-budget-food", "food", 1_500_000),
        ("demo-budget-shopping", "shopping", 2_000_000),
        ("demo-budget-transport", "transport", 500_000),
        ("demo-budget-entertainment", "entertainment", 400_000),
    ];
    for (id, category_id, limit_minor) in budgets {
        queries.insert_budget(&NewBudget {
            id: id.into(),
            category_id: category_id.into(),
            limit_minor,
            period: "monthly".into(),
            start_epoch_ms: budget_start,
            end_epoch_ms: budget_end,
        })?;
    }

    let subscriptions = [
        ("demo-sub-netflix", "Netflix Standard", 120_000, 15_i64),
        ("demo-sub-spotify", "Spotify Premium", 29_900, 1_i64),
        ("demo-sub-carnival", "Carnival Internet", 115_000, 5_i64),
    ];
    for (id, name, amount_minor, day) in subscriptions {
        queries.insert_subscription(&NewSubscription {
            id: id.into(),
            name: name.into(),
            amount_minor,
            billing_cycle: "monthly".into(),
            next_due_epoch_ms: now_epoch_ms + day * day_ms,
            is_active: true,
        })?;
    }

    for rule in centwise_domain::default_rules() {
        queries.insert_rule(&rule)?;
    }

    Ok(DemoDataSummary {
        accounts: accounts.len() as u32,
        transactions: transaction_count,
        budgets: budgets.len() as u32,
        subscriptions: subscriptions.len() as u32,
    })
}

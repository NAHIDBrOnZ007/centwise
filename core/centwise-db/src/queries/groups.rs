use centwise_domain::{
    GroupAnalyticsSnapshot, GroupDeposit, GroupExpenseEntry, GroupExpenseItem,
    GroupItemCategorySpend, GroupMember, GroupMemberShareSummary, GroupSettlementSummary,
    GroupType, MerchantSpendSummary, MonthlySpend, SharedGroup,
};
use rusqlite::{params, Connection};

use crate::error::DbResult;
use crate::queries::analytics::{epoch_ms_to_year_month, month_start_n_months_ago};
use crate::queries::{collect, now_epoch_ms, Queries};

use std::sync::atomic::{AtomicU64, Ordering};

static ID_COUNTER: AtomicU64 = AtomicU64::new(1);

fn unique_id(prefix: &str) -> String {
    let nanos = std::time::SystemTime::now()
        .duration_since(std::time::UNIX_EPOCH)
        .map(|d| d.as_nanos())
        .unwrap_or(0);
    let count = ID_COUNTER.fetch_add(1, Ordering::Relaxed);
    format!("{}_{}_{}", prefix, nanos, count)
}

#[derive(Debug, Clone)]
pub struct NewGroupExpenseItemInput {
    pub name: String,
    pub quantity: String,
    pub price_minor: i64,
}

impl<'a> Queries<'a> {
    pub fn count_groups(&self) -> DbResult<i64> {
        self.connection
            .query_row("SELECT COUNT(*) FROM groups", [], |row| row.get(0))
            .map_err(Into::into)
    }

    pub fn list_groups(&self) -> DbResult<Vec<SharedGroup>> {
        let mut statement = self.connection.prepare(
            "SELECT id, name, group_type, description, join_code, month_name,
                    monthly_target_minor, has_new_activity, created_at_epoch_ms
             FROM groups
             ORDER BY created_at_epoch_ms DESC",
        )?;

        let group_ids: Vec<String> = statement
            .query_map([], |row| row.get(0))?
            .filter_map(|r| r.ok())
            .collect();

        let mut groups = Vec::with_capacity(group_ids.len());
        for id in group_ids {
            if let Some(group) = self.get_group(&id)? {
                groups.push(group);
            }
        }

        Ok(groups)
    }

    pub fn get_group(&self, group_id: &str) -> DbResult<Option<SharedGroup>> {
        let group_row = self.connection.query_row(
            "SELECT id, name, group_type, description, join_code, month_name,
                    monthly_target_minor, has_new_activity, created_at_epoch_ms
             FROM groups
             WHERE id = ?1",
            params![group_id],
            |row| {
                let g_type_str: String = row.get(2)?;
                Ok((
                    row.get::<_, String>(0)?,
                    row.get::<_, String>(1)?,
                    GroupType::from_str_value(&g_type_str),
                    row.get::<_, String>(3)?,
                    row.get::<_, String>(4)?,
                    row.get::<_, String>(5)?,
                    row.get::<_, i64>(6)?,
                    row.get::<_, i64>(7)? != 0,
                    row.get::<_, i64>(8)?,
                ))
            },
        );

        let (
            id,
            name,
            group_type,
            description,
            join_code,
            month_name,
            monthly_target_minor,
            has_new_activity,
            created_at_epoch_ms,
        ) = match group_row {
            Ok(tuple) => tuple,
            Err(rusqlite::Error::QueryReturnedNoRows) => return Ok(None),
            Err(e) => return Err(e.into()),
        };

        // 1. Fetch expenses
        let mut exp_stmt = self.connection.prepare(
            "SELECT id, group_id, spender_id, occurred_at_epoch_ms, date_formatted,
                    total_amount_minor, split_per_member_minor, notes
             FROM group_expenses
             WHERE group_id = ?1
             ORDER BY occurred_at_epoch_ms DESC",
        )?;

        let mut raw_expenses = Vec::new();
        let exp_rows = exp_stmt.query_map(params![group_id], |row| {
            Ok((
                row.get::<_, String>(0)?,
                row.get::<_, String>(1)?,
                row.get::<_, String>(2)?,
                row.get::<_, i64>(3)?,
                row.get::<_, String>(4)?,
                row.get::<_, i64>(5)?,
                row.get::<_, i64>(6)?,
                row.get::<_, String>(7)?,
            ))
        })?;

        for r in exp_rows {
            raw_expenses.push(r?);
        }

        // 2. Fetch all expense items for this group
        let mut items_stmt = self.connection.prepare(
            "SELECT id, expense_id, group_id, name, quantity, price_minor
             FROM group_expense_items
             WHERE group_id = ?1",
        )?;

        let mut items_by_expense: std::collections::HashMap<String, Vec<GroupExpenseItem>> =
            std::collections::HashMap::new();

        let item_rows = items_stmt.query_map(params![group_id], |row| {
            Ok(GroupExpenseItem {
                id: row.get(0)?,
                expense_id: row.get(1)?,
                group_id: row.get(2)?,
                name: row.get(3)?,
                quantity: row.get(4)?,
                price_minor: row.get(5)?,
            })
        })?;

        for item in item_rows {
            let item = item?;
            items_by_expense
                .entry(item.expense_id.clone())
                .or_default()
                .push(item);
        }

        // 3. Fetch deposits
        let mut dep_stmt = self.connection.prepare(
            "SELECT d.id, d.group_id, d.member_id, m.name, m.avatar,
                    d.amount_minor, d.occurred_at_epoch_ms, d.date_formatted,
                    d.payment_method, d.note
             FROM group_deposits d
             JOIN group_members m ON d.member_id = m.id
             WHERE d.group_id = ?1
             ORDER BY d.occurred_at_epoch_ms DESC",
        )?;

        let mut deposits = Vec::new();
        let dep_rows = dep_stmt.query_map(params![group_id], |row| {
            Ok(GroupDeposit {
                id: row.get(0)?,
                group_id: row.get(1)?,
                member_id: row.get(2)?,
                member_name: row.get(3)?,
                member_avatar: row.get(4)?,
                amount_minor: row.get(5)?,
                occurred_at_epoch_ms: row.get(6)?,
                date_formatted: row.get(7)?,
                payment_method: row.get(8)?,
                note: row.get(9)?,
            })
        })?;

        for dep in dep_rows {
            deposits.push(dep?);
        }

        // 4. Fetch members & compute member deposit totals & spent shares
        let mut mem_stmt = self.connection.prepare(
            "SELECT id, group_id, name, avatar, phone, is_admin
             FROM group_members
             WHERE group_id = ?1
             ORDER BY is_admin DESC, created_at_epoch_ms ASC",
        )?;

        let mut raw_members = Vec::new();
        let mem_rows = mem_stmt.query_map(params![group_id], |row| {
            Ok((
                row.get::<_, String>(0)?,
                row.get::<_, String>(1)?,
                row.get::<_, String>(2)?,
                row.get::<_, String>(3)?,
                row.get::<_, String>(4)?,
                row.get::<_, i64>(5)? != 0,
            ))
        })?;

        for m in mem_rows {
            raw_members.push(m?);
        }

        let total_split_share: i64 = raw_expenses.iter().map(|e| e.6).sum();

        let mut member_deposit_map: std::collections::HashMap<String, i64> =
            std::collections::HashMap::new();
        for dep in &deposits {
            *member_deposit_map.entry(dep.member_id.clone()).or_insert(0) += dep.amount_minor;
        }

        let mut member_map: std::collections::HashMap<String, (String, String)> =
            std::collections::HashMap::new();

        let mut members = Vec::with_capacity(raw_members.len());
        for (m_id, g_id, m_name, m_avatar, m_phone, is_admin) in raw_members {
            member_map.insert(m_id.clone(), (m_name.clone(), m_avatar.clone()));
            let deposit_paid = member_deposit_map.get(&m_id).copied().unwrap_or(0);
            members.push(GroupMember {
                id: m_id,
                group_id: g_id,
                name: m_name,
                avatar: m_avatar,
                phone: m_phone,
                is_admin,
                deposit_paid_minor: deposit_paid,
                total_spent_share_minor: total_split_share,
            });
        }

        // Assemble expenses with spender names and avatars
        let mut daily_expenses = Vec::with_capacity(raw_expenses.len());
        for (e_id, g_id, s_id, occ_ms, date_fmt, total_amt, split_share, notes) in raw_expenses {
            let (s_name, s_avatar) = member_map
                .get(&s_id)
                .cloned()
                .unwrap_or_else(|| ("Member".into(), "avatar_1".into()));
            let items = items_by_expense.remove(&e_id).unwrap_or_default();
            daily_expenses.push(GroupExpenseEntry {
                id: e_id,
                group_id: g_id,
                spender_id: s_id,
                spender_name: s_name,
                spender_avatar: s_avatar,
                occurred_at_epoch_ms: occ_ms,
                date_formatted: date_fmt,
                total_amount_minor: total_amt,
                split_per_member_minor: split_share,
                notes,
                items,
            });
        }

        Ok(Some(SharedGroup {
            id,
            name,
            group_type,
            description,
            join_code,
            month_name,
            monthly_target_minor,
            members,
            daily_expenses,
            deposits,
            has_new_activity,
            created_at_epoch_ms,
        }))
    }

    #[allow(clippy::too_many_arguments)]
    pub fn create_group(
        &self,
        id: &str,
        name: &str,
        group_type: &str,
        description: &str,
        join_code: &str,
        month_name: &str,
        monthly_target_minor: i64,
        creator_name: &str,
        creator_avatar: &str,
    ) -> DbResult<SharedGroup> {
        let now = now_epoch_ms();
        let creator_id = unique_id("mem");

        self.connection.execute(
            "INSERT INTO groups
                (id, name, group_type, description, join_code, month_name,
                 monthly_target_minor, has_new_activity, created_at_epoch_ms)
             VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, 0, ?8)",
            params![
                id,
                name,
                group_type,
                description,
                join_code,
                month_name,
                monthly_target_minor,
                now
            ],
        )?;

        self.connection.execute(
            "INSERT INTO group_members
                (id, group_id, name, avatar, phone, is_admin, created_at_epoch_ms)
             VALUES (?1, ?2, ?3, ?4, '', 1, ?5)",
            params![creator_id, id, creator_name, creator_avatar, now],
        )?;

        if monthly_target_minor > 0 {
            let dep_id = unique_id("dep");
            self.connection.execute(
                "INSERT INTO group_deposits
                    (id, group_id, member_id, amount_minor, occurred_at_epoch_ms,
                     date_formatted, payment_method, note)
                 VALUES (?1, ?2, ?3, ?4, ?5, 'Today', 'Cash', 'Initial deposit')",
                params![dep_id, id, creator_id, monthly_target_minor, now],
            )?;
        }

        self.get_group(id)?
            .ok_or_else(|| rusqlite::Error::QueryReturnedNoRows.into())
    }

    pub fn join_group(
        &self,
        join_code: &str,
        member_name: &str,
        member_avatar: &str,
    ) -> DbResult<Option<SharedGroup>> {
        let group_id: Option<String> = self
            .connection
            .query_row(
                "SELECT id FROM groups WHERE join_code = ?1",
                params![join_code],
                |row| row.get(0),
            )
            .map(Some)
            .unwrap_or(None);

        let group_id = match group_id {
            Some(gid) => gid,
            None => return Ok(None),
        };

        let now = now_epoch_ms();
        let member_id = unique_id("mem");

        self.connection.execute(
            "INSERT INTO group_members
                (id, group_id, name, avatar, phone, is_admin, created_at_epoch_ms)
             VALUES (?1, ?2, ?3, ?4, '', 0, ?5)",
            params![member_id, group_id, member_name, member_avatar, now],
        )?;

        self.get_group(&group_id)
    }

    pub fn add_group_expense(
        &self,
        group_id: &str,
        spender_id: &str,
        items: &[NewGroupExpenseItemInput],
        notes: &str,
        occurred_at_epoch_ms: i64,
        date_formatted: &str,
    ) -> DbResult<String> {
        let member_count: i64 = self.connection.query_row(
            "SELECT COUNT(*) FROM group_members WHERE group_id = ?1",
            params![group_id],
            |row| row.get(0),
        )?;
        let count = member_count.max(1);

        let total_minor: i64 = items.iter().map(|it| it.price_minor).sum();
        let split_per_member_minor = total_minor / count;

        let expense_id = unique_id("exp");

        self.connection.execute(
            "INSERT INTO group_expenses
                (id, group_id, spender_id, occurred_at_epoch_ms, date_formatted,
                 total_amount_minor, split_per_member_minor, notes)
             VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8)",
            params![
                expense_id,
                group_id,
                spender_id,
                occurred_at_epoch_ms,
                date_formatted,
                total_minor,
                split_per_member_minor,
                notes
            ],
        )?;

        let mut item_stmt = self.connection.prepare(
            "INSERT INTO group_expense_items
                (id, expense_id, group_id, name, quantity, price_minor)
             VALUES (?1, ?2, ?3, ?4, ?5, ?6)",
        )?;

        for (idx, item) in items.iter().enumerate() {
            let item_id = format!("{}_it_{}", expense_id, idx);
            item_stmt.execute(params![
                item_id,
                expense_id,
                group_id,
                item.name,
                item.quantity,
                item.price_minor
            ])?;
        }

        Ok(expense_id)
    }

    pub fn delete_group_expense(&self, group_id: &str, expense_id: &str) -> DbResult<bool> {
        let deleted = self.connection.execute(
            "DELETE FROM group_expenses WHERE id = ?1 AND group_id = ?2",
            params![expense_id, group_id],
        )?;
        let _ = self.connection.execute(
            "DELETE FROM group_expense_items WHERE expense_id = ?1",
            params![expense_id],
        );
        Ok(deleted > 0)
    }

    #[allow(clippy::too_many_arguments)]
    pub fn add_group_deposit(
        &self,
        group_id: &str,
        member_id: &str,
        amount_minor: i64,
        payment_method: &str,
        note: &str,
        occurred_at_epoch_ms: i64,
        date_formatted: &str,
    ) -> DbResult<String> {
        let dep_id = unique_id("dep");

        self.connection.execute(
            "INSERT INTO group_deposits
                (id, group_id, member_id, amount_minor, occurred_at_epoch_ms,
                 date_formatted, payment_method, note)
             VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8)",
            params![
                dep_id,
                group_id,
                member_id,
                amount_minor,
                occurred_at_epoch_ms,
                date_formatted,
                payment_method,
                note
            ],
        )?;

        Ok(dep_id)
    }

    pub fn group_analytics(
        &self,
        group_id: &str,
        start_epoch_ms: i64,
        end_epoch_ms: i64,
        months_back: u32,
        type_filter: &str,
    ) -> DbResult<GroupAnalyticsSnapshot> {
        let group_name: String = self.connection.query_row(
            "SELECT name FROM groups WHERE id = ?1",
            params![group_id],
            |row| row.get(0),
        )?;

        let (raw_deposit_minor, deposit_count): (i64, i64) = self.connection.query_row(
            "SELECT COALESCE(SUM(amount_minor), 0), COUNT(id)
             FROM group_deposits
             WHERE group_id = ?1 AND occurred_at_epoch_ms >= ?2 AND occurred_at_epoch_ms < ?3",
            params![group_id, start_epoch_ms, end_epoch_ms],
            |row| Ok((row.get(0)?, row.get(1)?)),
        )?;

        let (raw_expense_minor, expense_count): (i64, i64) = self.connection.query_row(
            "SELECT COALESCE(SUM(total_amount_minor), 0), COUNT(id)
             FROM group_expenses
             WHERE group_id = ?1 AND occurred_at_epoch_ms >= ?2 AND occurred_at_epoch_ms < ?3",
            params![group_id, start_epoch_ms, end_epoch_ms],
            |row| Ok((row.get(0)?, row.get(1)?)),
        )?;

        let (total_deposit_minor, total_expense_minor, transaction_count) = match type_filter {
            "debit" => (0, raw_expense_minor, expense_count),
            "credit" => (raw_deposit_minor, 0, deposit_count),
            _ => (
                raw_deposit_minor,
                raw_expense_minor,
                expense_count + deposit_count,
            ),
        };

        let remaining_balance_minor = raw_deposit_minor - raw_expense_minor;

        // Member shares (deposits and split shares within the period)
        let total_group_split_share: i64 = self.connection.query_row(
            "SELECT COALESCE(SUM(split_per_member_minor), 0)
             FROM group_expenses
             WHERE group_id = ?1 AND occurred_at_epoch_ms >= ?2 AND occurred_at_epoch_ms < ?3",
            params![group_id, start_epoch_ms, end_epoch_ms],
            |row| row.get(0),
        )?;

        let mut mem_stmt = self.connection.prepare(
            "SELECT id, name, avatar FROM group_members WHERE group_id = ?1 ORDER BY created_at_epoch_ms ASC",
        )?;

        let mut member_shares = Vec::new();
        let mem_rows = mem_stmt.query_map(params![group_id], |row| {
            Ok((
                row.get::<_, String>(0)?,
                row.get::<_, String>(1)?,
                row.get::<_, String>(2)?,
            ))
        })?;

        for m in mem_rows {
            let (m_id, m_name, m_avatar) = m?;
            let dep_sum: i64 = self.connection.query_row(
                "SELECT COALESCE(SUM(amount_minor), 0)
                 FROM group_deposits
                 WHERE member_id = ?1 AND group_id = ?2
                   AND occurred_at_epoch_ms >= ?3 AND occurred_at_epoch_ms < ?4",
                params![m_id, group_id, start_epoch_ms, end_epoch_ms],
                |row| row.get(0),
            )?;
            let net = dep_sum - total_group_split_share;
            member_shares.push(GroupMemberShareSummary {
                member_id: m_id,
                member_name: m_name,
                member_avatar: m_avatar,
                deposit_paid_minor: dep_sum,
                total_spent_share_minor: total_group_split_share,
                net_balance_minor: net,
            });
        }

        // Category breakdown (joined with expenses to respect the time period)
        let mut category_breakdown = Vec::new();
        if type_filter != "credit" {
            let mut items_stmt = self.connection.prepare(
                "SELECT i.name, i.price_minor
                 FROM group_expense_items i
                 JOIN group_expenses e ON i.expense_id = e.id
                 WHERE i.group_id = ?1
                   AND e.occurred_at_epoch_ms >= ?2
                   AND e.occurred_at_epoch_ms < ?3",
            )?;

            let mut cat_map: std::collections::HashMap<&'static str, (i64, i64)> =
                std::collections::HashMap::new();

            let item_rows = items_stmt
                .query_map(params![group_id, start_epoch_ms, end_epoch_ms], |row| {
                    Ok((row.get::<_, String>(0)?, row.get::<_, i64>(1)?))
                })?;

            for row in item_rows {
                let (name, price) = row?;
                let cat = categorize_group_item(&name);
                let entry = cat_map.entry(cat).or_insert((0, 0));
                entry.0 += price;
                entry.1 += 1;
            }

            let total_items_minor: i64 = cat_map.values().map(|(amt, _)| *amt).sum();
            let total_divisor = if total_items_minor > 0 {
                total_items_minor as f64
            } else {
                1.0
            };

            for (cat_name, (cat_total, cat_count)) in cat_map {
                category_breakdown.push(GroupItemCategorySpend {
                    category_name: cat_name.to_string(),
                    total_minor: cat_total,
                    percentage: (cat_total as f64) / total_divisor,
                    item_count: cat_count,
                });
            }
            category_breakdown.sort_by_key(|a| std::cmp::Reverse(a.total_minor));
        }

        // Top spenders / contributors
        let top_spenders = if type_filter == "credit" {
            let mut top_stmt = self.connection.prepare(
                "SELECT m.name, COALESCE(SUM(d.amount_minor), 0) AS total, COUNT(d.id) AS tx_count
                 FROM group_members m
                 JOIN group_deposits d ON d.member_id = m.id
                 WHERE m.group_id = ?1
                   AND d.occurred_at_epoch_ms >= ?2
                   AND d.occurred_at_epoch_ms < ?3
                 GROUP BY m.id, m.name
                 ORDER BY total DESC
                 LIMIT 5",
            )?;
            let rows =
                top_stmt.query_map(params![group_id, start_epoch_ms, end_epoch_ms], |row| {
                    Ok(MerchantSpendSummary {
                        merchant: row.get(0)?,
                        total_minor: row.get(1)?,
                        transaction_count: row.get(2)?,
                    })
                })?;
            collect(rows)?
        } else {
            let mut top_stmt = self.connection.prepare(
                "SELECT m.name, COALESCE(SUM(e.total_amount_minor), 0) AS total, COUNT(e.id) AS tx_count
                 FROM group_members m
                 JOIN group_expenses e ON e.spender_id = m.id
                 WHERE m.group_id = ?1
                   AND e.occurred_at_epoch_ms >= ?2
                   AND e.occurred_at_epoch_ms < ?3
                 GROUP BY m.id, m.name
                 ORDER BY total DESC
                 LIMIT 5",
            )?;
            let rows =
                top_stmt.query_map(params![group_id, start_epoch_ms, end_epoch_ms], |row| {
                    Ok(MerchantSpendSummary {
                        merchant: row.get(0)?,
                        total_minor: row.get(1)?,
                        transaction_count: row.get(2)?,
                    })
                })?;
            collect(rows)?
        };

        // Monthly trends
        let anchor = if end_epoch_ms > 0 && end_epoch_ms < i64::MAX - 100_000 {
            end_epoch_ms
        } else {
            now_epoch_ms()
        };
        let monthly_trends =
            self.group_spending_by_month_anchored(group_id, months_back, anchor)?;

        // Settlements calculation
        let settlements = compute_settlements(&member_shares);

        Ok(GroupAnalyticsSnapshot {
            group_id: group_id.to_string(),
            group_name,
            total_deposit_minor,
            total_expense_minor,
            remaining_balance_minor,
            transaction_count,
            expense_count,
            deposit_count,
            member_shares,
            category_breakdown,
            top_spenders,
            monthly_trends,
            settlements,
        })
    }

    pub fn group_spending_by_month_anchored(
        &self,
        group_id: &str,
        months_back: u32,
        anchor_epoch_ms: i64,
    ) -> DbResult<Vec<MonthlySpend>> {
        let cutoff_epoch_ms =
            month_start_n_months_ago(anchor_epoch_ms, months_back.saturating_sub(1));

        let mut statement = self.connection.prepare(
            "SELECT occurred_at_epoch_ms, total_amount_minor
             FROM group_expenses
             WHERE group_id = ?1
               AND occurred_at_epoch_ms >= ?2
             ORDER BY occurred_at_epoch_ms ASC",
        )?;

        let mut buckets: std::collections::BTreeMap<(i32, u32), i64> =
            std::collections::BTreeMap::new();
        let mut rows = statement.query(params![group_id, cutoff_epoch_ms])?;
        while let Some(row) = rows.next()? {
            let epoch_ms: i64 = row.get(0)?;
            let amount: i64 = row.get(1)?;
            let (year, month) = epoch_ms_to_year_month(epoch_ms);
            *buckets.entry((year, month)).or_insert(0) += amount;
        }

        Ok(buckets
            .into_iter()
            .map(|((year, month), total)| MonthlySpend {
                year,
                month,
                total_expense_minor: total,
            })
            .collect())
    }

    pub fn delete_all_groups(&self) -> DbResult<()> {
        self.connection.execute_batch(
            "DELETE FROM group_expense_items;
             DELETE FROM group_expenses;
             DELETE FROM group_deposits;
             DELETE FROM group_members;
             DELETE FROM groups;",
        )?;
        Ok(())
    }

    pub fn seed_demo_groups(&self, now_epoch_ms: i64) -> DbResult<()> {
        seed_default_demo_groups_on_connection(self.connection, now_epoch_ms)
    }
}

pub fn categorize_group_item(item_name: &str) -> &'static str {
    let lower = item_name.to_lowercase();
    if lower.contains("চাল")
        || lower.contains("rice")
        || lower.contains("ডাল")
        || lower.contains("lentil")
        || lower.contains("আটা")
        || lower.contains("ময়দা")
        || lower.contains("পোলাও")
        || lower.contains("খিচুড়ি")
    {
        "চাল, ডাল ও খাদ্যশস্য"
    } else if lower.contains("মুরগি")
        || lower.contains("chicken")
        || lower.contains("মাংস")
        || lower.contains("meat")
        || lower.contains("গরু")
        || lower.contains("beef")
        || lower.contains("খাসি")
        || lower.contains("খাসির")
        || lower.contains("মাছ")
        || lower.contains("fish")
        || lower.contains("রুই")
        || lower.contains("পাঙ্গাস")
        || lower.contains("ইলিশ")
    {
        "মাছ ও মাংস"
    } else if lower.contains("সবজি")
        || lower.contains("শাক")
        || lower.contains("vegetable")
        || lower.contains("আলু")
        || lower.contains("potato")
        || lower.contains("পটল")
        || lower.contains("পেঁয়াজ")
        || lower.contains("রসুন")
        || lower.contains("মরিচ")
        || lower.contains("আদা")
        || lower.contains("টমেটো")
    {
        "কাঁচাবাজার ও শাকসবজি"
    } else if lower.contains("তেল")
        || lower.contains("oil")
        || lower.contains("মশলা")
        || lower.contains("spice")
        || lower.contains("লবণ")
        || lower.contains("salt")
        || lower.contains("ঘি")
        || lower.contains("হলুদ")
        || lower.contains("জিরা")
    {
        "তেল ও মশলাপাতি"
    } else if lower.contains("ডিম")
        || lower.contains("egg")
        || lower.contains("দুধ")
        || lower.contains("milk")
        || lower.contains("দই")
    {
        "ডিম ও দুগ্ধজাত সামগ্রী"
    } else if lower.contains("বিদ্যুৎ")
        || lower.contains("electric")
        || lower.contains("কারেন্ট")
        || lower.contains("গ্যাস")
        || lower.contains("gas")
        || lower.contains("ওয়াইফাই")
        || lower.contains("wifi")
        || lower.contains("পানি")
        || lower.contains("water")
        || lower.contains("বিল")
    {
        "ইউটিলিটি ও বাসা বিল"
    } else if lower.contains("বাস")
        || lower.contains("bus")
        || lower.contains("ভাড়া")
        || lower.contains("সিএনজি")
        || lower.contains("রিকশা")
        || lower.contains("ট্রেন")
        || lower.contains("ফ্লাইট")
        || lower.contains("হোটেল")
        || lower.contains("রিসোর্ট")
    {
        "যাতায়াত ও ভাড়া"
    } else if lower.contains("নাস্তা")
        || lower.contains("চা")
        || lower.contains("কফি")
        || lower.contains("বিস্কুট")
        || lower.contains("মিষ্টি")
        || lower.contains("ফল")
    {
        "নাস্তা ও চা-কফি"
    } else {
        "নিত্যপ্রয়োজনীয় সামগ্রী"
    }
}

pub fn compute_settlements(members: &[GroupMemberShareSummary]) -> Vec<GroupSettlementSummary> {
    let mut creditors: Vec<(String, String, i64)> = members
        .iter()
        .filter(|m| m.net_balance_minor > 0)
        .map(|m| {
            (
                m.member_id.clone(),
                m.member_name.clone(),
                m.net_balance_minor,
            )
        })
        .collect();

    let mut debtors: Vec<(String, String, i64)> = members
        .iter()
        .filter(|m| m.net_balance_minor < 0)
        .map(|m| {
            (
                m.member_id.clone(),
                m.member_name.clone(),
                -m.net_balance_minor,
            )
        })
        .collect();

    let mut settlements = Vec::new();
    let mut c_idx = 0;
    let mut d_idx = 0;

    while c_idx < creditors.len() && d_idx < debtors.len() {
        let (c_id, c_name, c_amount) = &mut creditors[c_idx];
        let (d_id, d_name, d_amount) = &mut debtors[d_idx];

        let transfer = (*c_amount).min(*d_amount);
        if transfer > 0 {
            settlements.push(GroupSettlementSummary {
                from_member_id: d_id.clone(),
                from_member_name: d_name.clone(),
                to_member_id: c_id.clone(),
                to_member_name: c_name.clone(),
                amount_minor: transfer,
            });
        }

        *c_amount -= transfer;
        *d_amount -= transfer;

        if *c_amount == 0 {
            c_idx += 1;
        }
        if *d_amount == 0 {
            d_idx += 1;
        }
    }

    settlements
}

pub fn seed_default_demo_groups_on_connection(
    connection: &Connection,
    now_epoch_ms: i64,
) -> DbResult<()> {
    let day_ms = 86_400_000_i64;

    // 1. Dhanmondi Mess
    connection.execute(
        "INSERT OR IGNORE INTO groups
            (id, name, group_type, description, join_code, month_name,
             monthly_target_minor, has_new_activity, created_at_epoch_ms)
         VALUES ('group_dhanmondi_4b', 'ধানমন্ডি মেস ও ফ্ল্যাট', 'mess', 'Flat 4B, Road 27', 'GRP-7842', 'September 2026', 500000, 0, ?1)",
        params![now_epoch_ms - 10 * day_ms],
    )?;

    let members = [
        ("m1", "group_dhanmondi_4b", "Faysal (You)", "avatar_1", 1),
        ("m2", "group_dhanmondi_4b", "তানভীর আহমেদ", "avatar_2", 0),
        ("m3", "group_dhanmondi_4b", "রাকিবুল হাসান", "avatar_3", 0),
        ("m4", "group_dhanmondi_4b", "সাকিব মাহমুদ", "avatar_4", 0),
    ];
    for (m_id, g_id, name, av, admin) in members {
        connection.execute(
            "INSERT OR IGNORE INTO group_members
                (id, group_id, name, avatar, phone, is_admin, created_at_epoch_ms)
             VALUES (?1, ?2, ?3, ?4, '', ?5, ?6)",
            params![m_id, g_id, name, av, admin, now_epoch_ms - 10 * day_ms],
        )?;
    }

    // Deposits for Dhanmondi Mess
    let deposits = [
        (
            "d1",
            "group_dhanmondi_4b",
            "m1",
            500000,
            "01 Sep 2026",
            "bKash",
        ),
        (
            "d2",
            "group_dhanmondi_4b",
            "m2",
            500000,
            "02 Sep 2026",
            "Nagad",
        ),
        (
            "d3",
            "group_dhanmondi_4b",
            "m3",
            400000,
            "02 Sep 2026",
            "Cash",
        ),
        (
            "d4",
            "group_dhanmondi_4b",
            "m4",
            500000,
            "03 Sep 2026",
            "Bank",
        ),
    ];
    for (d_id, g_id, m_id, amt, date_fmt, method) in deposits {
        connection.execute(
            "INSERT OR IGNORE INTO group_deposits
                (id, group_id, member_id, amount_minor, occurred_at_epoch_ms,
                 date_formatted, payment_method, note)
             VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, 'মাসিক মিল ডিপোজিট')",
            params![
                d_id,
                g_id,
                m_id,
                amt,
                now_epoch_ms - 8 * day_ms,
                date_fmt,
                method
            ],
        )?;
    }

    // Expenses for Dhanmondi Mess
    let expenses = [
        (
            "e1",
            "group_dhanmondi_4b",
            "m1",
            now_epoch_ms - 86400000,
            "12 Sep 2026",
            112000,
            28000,
            "আজকের দুপুরের ও রাতের রান্নার বাজার",
        ),
        (
            "e2",
            "group_dhanmondi_4b",
            "m2",
            now_epoch_ms - 86400000 * 2,
            "11 Sep 2026",
            195000,
            48750,
            "শুক্রবার ছুটির দিনের দাওয়াত ও স্পেশাল বাজার",
        ),
    ];
    for (e_id, g_id, s_id, occ_ms, date_fmt, total, split, notes) in expenses {
        connection.execute(
            "INSERT OR IGNORE INTO group_expenses
                (id, group_id, spender_id, occurred_at_epoch_ms, date_formatted,
                 total_amount_minor, split_per_member_minor, notes)
             VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8)",
            params![e_id, g_id, s_id, occ_ms, date_fmt, total, split, notes],
        )?;
    }

    let items = [
        (
            "i1",
            "e1",
            "group_dhanmondi_4b",
            "মিনিকেট চাল",
            "৫ কেজি",
            38000,
        ),
        (
            "i2",
            "e1",
            "group_dhanmondi_4b",
            "ব্রয়লার মুরগি",
            "২ কেজি",
            44000,
        ),
        (
            "i3",
            "e1",
            "group_dhanmondi_4b",
            "রূপচাঁদা সয়াবিন তেল",
            "১ লিটার",
            19000,
        ),
        (
            "i4",
            "e1",
            "group_dhanmondi_4b",
            "আলু, পেঁয়াজ ও রসুন",
            "প্যাকেট",
            11000,
        ),
        (
            "i5",
            "e2",
            "group_dhanmondi_4b",
            "গরুর মাংস",
            "১.৫ কেজি",
            120000,
        ),
        ("i6", "e2", "group_dhanmondi_4b", "পোলাও চাল", "২ কেজি", 28000),
        (
            "i7",
            "e2",
            "group_dhanmondi_4b",
            "মসুর ডাল ও মশলা",
            "প্যাক",
            47000,
        ),
    ];
    for (i_id, e_id, g_id, name, qty, price) in items {
        connection.execute(
            "INSERT OR IGNORE INTO group_expense_items
                (id, expense_id, group_id, name, quantity, price_minor)
             VALUES (?1, ?2, ?3, ?4, ?5, ?6)",
            params![i_id, e_id, g_id, name, qty, price],
        )?;
    }

    // 2. Sylhet Tour
    connection.execute(
        "INSERT OR IGNORE INTO groups
            (id, name, group_type, description, join_code, month_name,
             monthly_target_minor, has_new_activity, created_at_epoch_ms)
         VALUES ('group_sylhet_tour', 'সিলেট ট্যুর ২০২৬', 'trip', 'Sreemangal & Tanguar Haor Vacation', 'GRP-4512', 'September 2026', 800000, 0, ?1)",
        params![now_epoch_ms - 5 * day_ms],
    )?;

    let tour_members = [
        ("tm1", "group_sylhet_tour", "Faysal (You)", "avatar_1", 1),
        ("tm2", "group_sylhet_tour", "নাজমুল হুদা", "avatar_5", 0),
        ("tm3", "group_sylhet_tour", "আসিফ ইকবাল", "avatar_6", 0),
    ];
    for (m_id, g_id, name, av, admin) in tour_members {
        connection.execute(
            "INSERT OR IGNORE INTO group_members
                (id, group_id, name, avatar, phone, is_admin, created_at_epoch_ms)
             VALUES (?1, ?2, ?3, ?4, '', ?5, ?6)",
            params![m_id, g_id, name, av, admin, now_epoch_ms - 5 * day_ms],
        )?;
    }

    let tour_deposits = [
        (
            "td1",
            "group_sylhet_tour",
            "tm1",
            800000,
            "05 Sep 2026",
            "bKash",
        ),
        (
            "td2",
            "group_sylhet_tour",
            "tm2",
            800000,
            "05 Sep 2026",
            "bKash",
        ),
        (
            "td3",
            "group_sylhet_tour",
            "tm3",
            800000,
            "06 Sep 2026",
            "Bank",
        ),
    ];
    for (d_id, g_id, m_id, amt, date_fmt, method) in tour_deposits {
        connection.execute(
            "INSERT OR IGNORE INTO group_deposits
                (id, group_id, member_id, amount_minor, occurred_at_epoch_ms,
                 date_formatted, payment_method, note)
             VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, 'ট্যুর অ্যাডভান্স ফি')",
            params![
                d_id,
                g_id,
                m_id,
                amt,
                now_epoch_ms - 4 * day_ms,
                date_fmt,
                method
            ],
        )?;
    }

    // 3. Family Household
    connection.execute(
        "INSERT OR IGNORE INTO groups
            (id, name, group_type, description, join_code, month_name,
             monthly_target_minor, has_new_activity, created_at_epoch_ms)
         VALUES ('group_family_budget', 'আমাদের পরিবার', 'family', 'Monthly Household & Family Budget', 'GRP-9901', 'September 2026', 1500000, 0, ?1)",
        params![now_epoch_ms - 3 * day_ms],
    )?;

    let fam_members = [
        ("fm1", "group_family_budget", "Faysal (You)", "avatar_1", 1),
        ("fm2", "group_family_budget", "আব্বু", "avatar_4", 0),
        ("fm3", "group_family_budget", "আম্মু", "avatar_3", 0),
    ];
    for (m_id, g_id, name, av, admin) in fam_members {
        connection.execute(
            "INSERT OR IGNORE INTO group_members
                (id, group_id, name, avatar, phone, is_admin, created_at_epoch_ms)
             VALUES (?1, ?2, ?3, ?4, '', ?5, ?6)",
            params![m_id, g_id, name, av, admin, now_epoch_ms - 3 * day_ms],
        )?;
    }

    let fam_deposits = [
        (
            "fd1",
            "group_family_budget",
            "fm1",
            1500000,
            "01 Sep 2026",
            "Bank",
        ),
        (
            "fd2",
            "group_family_budget",
            "fm2",
            2500000,
            "01 Sep 2026",
            "Bank",
        ),
    ];
    for (d_id, g_id, m_id, amt, date_fmt, method) in fam_deposits {
        connection.execute(
            "INSERT OR IGNORE INTO group_deposits
                (id, group_id, member_id, amount_minor, occurred_at_epoch_ms,
                 date_formatted, payment_method, note)
             VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, 'সংসার খরচ জমা')",
            params![
                d_id,
                g_id,
                m_id,
                amt,
                now_epoch_ms - 2 * day_ms,
                date_fmt,
                method
            ],
        )?;
    }

    Ok(())
}

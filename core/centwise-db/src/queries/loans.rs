use centwise_domain::{NewTransaction, TransactionType};
use rusqlite::params;

use super::{collect, Queries};
use crate::{DbError, DbResult};

const LAST_SUPPORTED_DATE: i64 = 253_402_279_199_999;

fn dhaka_day(epoch_ms: i64) -> i64 {
    (epoch_ms.saturating_add(21_600_000)).div_euclid(86_400_000)
}

pub struct LoanPayment {
    pub id: String,
    pub account_id: String,
    pub amount_minor: i64,
    pub occurred_at_epoch_ms: i64,
    pub existing_transaction_id: Option<String>,
}

pub struct Loan {
    pub id: String,
    pub person: String,
    pub lent: bool,
    pub note: String,
    pub due_at_epoch_ms: Option<i64>,
    pub principal_minor: i64,
    pub repaid_minor: i64,
    pub remaining_minor: i64,
    pub account_id: String,
    pub opened_at_epoch_ms: i64,
}

pub struct LoanMovement {
    pub transaction_id: String,
    pub repayment: bool,
    pub account_name: String,
    pub amount_minor: i64,
    pub occurred_at_epoch_ms: i64,
}

pub struct LoanMonth {
    pub month: String,
    pub incoming_minor: i64,
    pub outgoing_minor: i64,
}

impl Queries<'_> {
    pub fn create_loan(
        &self,
        id: &str,
        person: &str,
        lent: bool,
        due_at_epoch_ms: Option<i64>,
        note: &str,
        payment: &LoanPayment,
    ) -> DbResult<()> {
        if id.trim().is_empty() || person.trim().is_empty() {
            return Err(DbError::Invalid("A person is required".into()));
        }
        validate_due_date(due_at_epoch_ms, payment.occurred_at_epoch_ms)?;
        self.connection.execute(
            "INSERT INTO loans (id, person, lent, due_at_epoch_ms, note) VALUES (?1, ?2, ?3, ?4, ?5)",
            params![id, person.trim(), lent, due_at_epoch_ms, note.trim()],
        )?;
        self.post_loan_movement(id, person.trim(), !lent, false, payment)
    }

    pub fn repay_loan(&self, id: &str, payment: &LoanPayment) -> DbResult<()> {
        let loan = self.get_loan(id)?;
        if payment.amount_minor > loan.remaining_minor {
            return Err(DbError::Invalid(
                "Payment exceeds the outstanding amount".into(),
            ));
        }
        if dhaka_day(payment.occurred_at_epoch_ms) < dhaka_day(loan.opened_at_epoch_ms) {
            return Err(DbError::Invalid("Payment date precedes the loan".into()));
        }
        self.post_loan_movement(id, &loan.person, loan.lent, true, payment)
    }

    pub fn set_loan_due_date(&self, id: &str, due_at_epoch_ms: Option<i64>) -> DbResult<()> {
        let loan = self.get_loan(id)?;
        validate_due_date(due_at_epoch_ms, loan.opened_at_epoch_ms)?;
        self.connection.execute(
            "UPDATE loans SET due_at_epoch_ms = ?2 WHERE id = ?1",
            params![id, due_at_epoch_ms],
        )?;
        Ok(())
    }

    fn post_loan_movement(
        &self,
        loan_id: &str,
        person: &str,
        incoming: bool,
        repayment: bool,
        payment: &LoanPayment,
    ) -> DbResult<()> {
        if payment.amount_minor <= 0
            || !(1..=LAST_SUPPORTED_DATE).contains(&payment.occurred_at_epoch_ms)
            || payment.id.trim().is_empty()
        {
            return Err(DbError::Invalid(
                "Enter a positive amount and a valid date".into(),
            ));
        }
        let kind = if incoming {
            TransactionType::Income
        } else {
            TransactionType::Expense
        };
        let id = if let Some(id) = &payment.existing_transaction_id {
            let transaction = self
                .get_transaction(id)?
                .ok_or_else(|| DbError::Invalid("Transaction was not found".into()))?;
            if transaction.amount_minor != payment.amount_minor
                || transaction.account_id != payment.account_id
                || transaction.occurred_at_epoch_ms != payment.occurred_at_epoch_ms
                || transaction.transaction_type != kind
                || transaction.currency != "BDT"
            {
                return Err(DbError::Invalid(
                    "The selected transaction does not match this payment".into(),
                ));
            }
            id
        } else {
            if !self.connection.query_row(
                "SELECT EXISTS(SELECT 1 FROM accounts WHERE id = ?1 AND archived = 0)",
                [&payment.account_id],
                |row| row.get::<_, bool>(0),
            )? {
                return Err(DbError::Invalid("Select an active account".into()));
            }
            let delta = if incoming {
                payment.amount_minor
            } else {
                -payment.amount_minor
            };
            if self
                .account_balance(&payment.account_id)?
                .checked_add(delta)
                .is_none()
            {
                return Err(DbError::Invalid(
                    "The payment would exceed the supported account balance".into(),
                ));
            }
            let action = match (incoming, repayment) {
                (true, true) => "Loan repayment received",
                (false, true) => "Loan repayment sent",
                (true, false) => "Loan borrowed",
                (false, false) => "Loan lent",
            };
            self.insert_transaction(&NewTransaction {
                id: payment.id.clone(),
                title: format!("{action} · {person}"),
                amount_minor: payment.amount_minor,
                currency: "BDT".into(),
                transaction_type: kind,
                category_id: "other".into(),
                occurred_at_epoch_ms: payment.occurred_at_epoch_ms,
                account_id: payment.account_id.clone(),
                reference: None,
                balance_after_minor: None,
                notes: None,
                raw_sms: None,
                fee_minor: None,
                is_auto_tracked: false,
            })?;
            &payment.id
        };
        let inserted = self.connection.execute(
            "INSERT OR IGNORE INTO loan_movements (loan_id, transaction_id, repayment) VALUES (?1, ?2, ?3)",
            params![loan_id, id, repayment],
        )?;
        if inserted == 0 {
            return Err(DbError::Invalid(
                "This transaction is already linked to a loan".into(),
            ));
        }
        Ok(())
    }

    pub fn get_loan(&self, id: &str) -> DbResult<Loan> {
        self.read_loans(Some(id))?
            .pop()
            .ok_or_else(|| DbError::Invalid("Loan was not found".into()))
    }

    pub fn list_loans(&self) -> DbResult<Vec<Loan>> {
        self.read_loans(None)
    }

    pub fn loan_transaction_ids(&self) -> DbResult<Vec<String>> {
        let mut stmt = self
            .connection
            .prepare("SELECT transaction_id FROM loan_movements")?;
        let rows = stmt.query_map([], |row| row.get(0))?;
        collect(rows)
    }

    fn read_loans(&self, id: Option<&str>) -> DbResult<Vec<Loan>> {
        let mut stmt = self.connection.prepare(
            "SELECT l.id, l.person, l.lent, l.note, l.due_at_epoch_ms,
                    p.amount_minor, COALESCE(SUM(r.amount_minor), 0),
                    p.account_id, p.occurred_at_epoch_ms
             FROM loans l
             JOIN loan_movements pm ON pm.loan_id = l.id AND pm.repayment = 0
             JOIN transactions p ON p.id = pm.transaction_id
             LEFT JOIN loan_movements rm ON rm.loan_id = l.id AND rm.repayment = 1
             LEFT JOIN transactions r ON r.id = rm.transaction_id
             WHERE ?1 IS NULL OR l.id = ?1
             GROUP BY l.id ORDER BY p.occurred_at_epoch_ms DESC, l.id",
        )?;
        let rows = stmt.query_map([id], |row| {
            let principal_minor: i64 = row.get(5)?;
            let repaid_minor: i64 = row.get(6)?;
            Ok(Loan {
                id: row.get(0)?,
                person: row.get(1)?,
                lent: row.get(2)?,
                note: row.get(3)?,
                due_at_epoch_ms: row.get(4)?,
                principal_minor,
                repaid_minor,
                remaining_minor: principal_minor - repaid_minor,
                account_id: row.get(7)?,
                opened_at_epoch_ms: row.get(8)?,
            })
        })?;
        collect(rows)
    }

    pub fn loan_movements(&self, id: &str) -> DbResult<Vec<LoanMovement>> {
        let mut stmt = self.connection.prepare(
            "SELECT t.id, m.repayment, a.name, t.amount_minor, t.occurred_at_epoch_ms
             FROM loan_movements m JOIN transactions t ON t.id = m.transaction_id
             JOIN accounts a ON a.id = t.account_id WHERE m.loan_id = ?1
             ORDER BY t.occurred_at_epoch_ms DESC, t.id",
        )?;
        let rows = stmt.query_map([id], |row| {
            Ok(LoanMovement {
                transaction_id: row.get(0)?,
                repayment: row.get(1)?,
                account_name: row.get(2)?,
                amount_minor: row.get(3)?,
                occurred_at_epoch_ms: row.get(4)?,
            })
        })?;
        collect(rows)
    }

    pub fn loan_months(&self) -> DbResult<Vec<LoanMonth>> {
        let mut stmt = self.connection.prepare(
            "SELECT strftime('%Y-%m', t.occurred_at_epoch_ms / 1000, 'unixepoch', '+6 hours') AS month,
                    SUM(CASE WHEN t.transaction_type = 'income' THEN t.amount_minor ELSE 0 END),
                    SUM(CASE WHEN t.transaction_type = 'expense' THEN t.amount_minor ELSE 0 END)
             FROM loan_movements m JOIN transactions t ON t.id = m.transaction_id
             GROUP BY month ORDER BY month DESC",
        )?;
        let rows = stmt.query_map([], |row| {
            Ok(LoanMonth {
                month: row.get(0)?,
                incoming_minor: row.get(1)?,
                outgoing_minor: row.get(2)?,
            })
        })?;
        collect(rows)
    }
}

fn validate_due_date(due_at_epoch_ms: Option<i64>, opened_at_epoch_ms: i64) -> DbResult<()> {
    if due_at_epoch_ms.is_some_and(|due| {
        !(1..=LAST_SUPPORTED_DATE).contains(&due) || dhaka_day(due) < dhaka_day(opened_at_epoch_ms)
    }) {
        return Err(DbError::Invalid(
            "Choose a valid due date on or after the loan date".into(),
        ));
    }
    Ok(())
}

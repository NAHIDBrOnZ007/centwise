use centwise_db::queries::loans::{Loan, LoanPayment};
use centwise_domain::TransactionType;

use crate::{conversions::transaction_record, CentwiseCore, CentwiseError, TransactionRecord};

#[derive(uniffi::Record)]
pub struct LoanPaymentInput {
    pub id: String,
    pub account_id: String,
    pub amount: String,
    pub occurred_at_epoch_ms: i64,
    pub existing_transaction_id: Option<String>,
}

impl LoanPaymentInput {
    fn into_payment(self) -> Result<LoanPayment, CentwiseError> {
        let amount_minor = centwise_normalization::parse_amount_minor(&self.amount)
            .filter(|amount| *amount > 0)
            .ok_or_else(|| CentwiseError::Invalid {
                reason: "Enter a positive BDT amount".into(),
            })?;
        Ok(LoanPayment {
            id: self.id,
            account_id: self.account_id,
            amount_minor,
            occurred_at_epoch_ms: self.occurred_at_epoch_ms,
            existing_transaction_id: self.existing_transaction_id,
        })
    }
}

#[derive(uniffi::Record)]
pub struct LoanRecord {
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

impl From<Loan> for LoanRecord {
    fn from(value: Loan) -> Self {
        Self {
            id: value.id,
            person: value.person,
            lent: value.lent,
            note: value.note,
            due_at_epoch_ms: value.due_at_epoch_ms,
            principal_minor: value.principal_minor,
            repaid_minor: value.repaid_minor,
            remaining_minor: value.remaining_minor,
            account_id: value.account_id,
            opened_at_epoch_ms: value.opened_at_epoch_ms,
        }
    }
}

#[derive(uniffi::Record)]
pub struct LoanMovementRecord {
    pub transaction_id: String,
    pub repayment: bool,
    pub account_name: String,
    pub amount_minor: i64,
    pub occurred_at_epoch_ms: i64,
}

#[derive(uniffi::Record)]
pub struct LoanMonthRecord {
    pub month: String,
    pub incoming_minor: i64,
    pub outgoing_minor: i64,
}

#[uniffi::export]
impl CentwiseCore {
    pub fn create_loan(
        &self,
        id: String,
        person: String,
        lent: bool,
        due_at_epoch_ms: Option<i64>,
        note: String,
        payment: LoanPaymentInput,
    ) -> Result<LoanRecord, CentwiseError> {
        let payment = payment.into_payment()?;
        Ok(self.database.write(|q| {
            q.create_loan(&id, &person, lent, due_at_epoch_ms, &note, &payment)?;
            q.get_loan(&id).map(Into::into)
        })?)
    }

    pub fn repay_loan(
        &self,
        id: String,
        payment: LoanPaymentInput,
    ) -> Result<LoanRecord, CentwiseError> {
        let payment = payment.into_payment()?;
        Ok(self.database.write(|q| {
            q.repay_loan(&id, &payment)?;
            q.get_loan(&id).map(Into::into)
        })?)
    }

    pub fn set_loan_due_date(
        &self,
        id: String,
        due_at_epoch_ms: Option<i64>,
    ) -> Result<LoanRecord, CentwiseError> {
        Ok(self.database.write(|q| {
            q.set_loan_due_date(&id, due_at_epoch_ms)?;
            q.get_loan(&id).map(Into::into)
        })?)
    }

    pub fn list_loans(&self) -> Result<Vec<LoanRecord>, CentwiseError> {
        Ok(self
            .database
            .read(|q| Ok(q.list_loans()?.into_iter().map(Into::into).collect()))?)
    }

    pub fn loan_transaction_ids(&self) -> Result<Vec<String>, CentwiseError> {
        Ok(self.database.read(|q| q.loan_transaction_ids())?)
    }

    pub fn loan_movements(&self, id: String) -> Result<Vec<LoanMovementRecord>, CentwiseError> {
        Ok(self.database.read(|q| {
            Ok(q.loan_movements(&id)?
                .into_iter()
                .map(|m| LoanMovementRecord {
                    transaction_id: m.transaction_id,
                    repayment: m.repayment,
                    account_name: m.account_name,
                    amount_minor: m.amount_minor,
                    occurred_at_epoch_ms: m.occurred_at_epoch_ms,
                })
                .collect())
        })?)
    }

    pub fn loan_months(&self) -> Result<Vec<LoanMonthRecord>, CentwiseError> {
        Ok(self.database.read(|q| {
            Ok(q.loan_months()?
                .into_iter()
                .map(|m| LoanMonthRecord {
                    month: m.month,
                    incoming_minor: m.incoming_minor,
                    outgoing_minor: m.outgoing_minor,
                })
                .collect())
        })?)
    }

    pub fn loan_transaction_candidates(
        &self,
        incoming: bool,
    ) -> Result<Vec<TransactionRecord>, CentwiseError> {
        let kind = if incoming {
            TransactionType::Income
        } else {
            TransactionType::Expense
        };
        Ok(self.database.read(|q| {
            Ok(q.loan_transaction_candidates(kind)?
                .into_iter()
                .map(transaction_record)
                .collect())
        })?)
    }
}

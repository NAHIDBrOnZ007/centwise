use centwise_domain::TransactionType;
use serde::{Deserialize, Serialize};

/// Represents a successfully parsed transaction from an SMS message.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct ParsedTransaction {
    pub provider_id: String,
    pub transaction_type: TransactionType,
    pub amount_minor: i64,
    pub fee_minor: Option<i64>,
    pub balance_after_minor: Option<i64>,
    pub reference: Option<String>,
    pub party: Option<String>,
    pub merchant: Option<String>,
    pub category_id: Option<String>,
    pub account_last4: Option<String>,
    pub account_hint: Option<String>,
    pub raw_date: Option<String>,
}

/// Reasons why an SMS was rejected by the parsing pipeline.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum RejectReason {
    NotATransaction,
    OtpOrSecurity,
    PromotionOrSpam,
    NoAmountFound,
    UnsupportedProvider,
}

/// The final outcome of parsing an SMS.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum ParseOutcome {
    Parsed(Box<ParsedTransaction>),
    Rejected(RejectReason),
}

impl ParseOutcome {
    pub fn is_transaction(&self) -> bool {
        matches!(self, ParseOutcome::Parsed(_))
    }

    pub fn transaction(&self) -> Option<&ParsedTransaction> {
        match self {
            ParseOutcome::Parsed(t) => Some(t),
            _ => None,
        }
    }
}

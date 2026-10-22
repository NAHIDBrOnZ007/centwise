//! Completed payment receipts whose funding bank/operator is not identified.

use crate::types::ParsedTransaction;
use centwise_domain::TransactionType;
use regex::Regex;
use std::sync::LazyLock;

static BRTA_AMOUNT_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(r"(?i)\btran-no\s*:\s*[a-z0-9-]+\s*/\s*bdt\s*([0-9][0-9,.]*)")
        .expect("valid BRTA payment amount regex")
});
static METER_RECHARGE_AMOUNT_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(r"(?i)\b(?:tk\.?|bdt)\s*([0-9][0-9,.]*)\s*\(\s*details\s*:")
        .expect("valid meter recharge total regex")
});
static BUNDLE_CHARGE_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(r"(?i)\bthe\s+fee\s+for\s+(?:minute\s+bundle|[a-z0-9]+_at_app_[a-z0-9_]+)\s+you\s+purchased\s+is\s+(?:tk\.?|bdt)\s*([0-9][0-9,.]*)")
        .expect("valid purchased bundle charge regex")
});
static FEE_PAYMENT_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(
        r"(?i)\byour\s+fee\s+payment\s+of\s+(?:tk\.?|bdt)\s*([0-9][0-9,.]*)\s+is\s+successful\b",
    )
    .expect("valid completed fee payment regex")
});
static RYANS_PURCHASE_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(r"(?i)\bthanks for shopping from ryans\.\s*your purchase bill no is\s+[a-z0-9-]+\s*,\s*(?:tk\.?|bdt)\s*([0-9][0-9,.]*)")
        .expect("valid Ryans purchase confirmation regex")
});

#[derive(Clone, Copy)]
enum ReceiptKind {
    Brta,
    Dpdc,
    Bundle,
    Fee,
    Ryans,
}

#[derive(Clone, Copy)]
pub(super) struct ConfirmedReceipt {
    kind: ReceiptKind,
    pub amount_minor: i64,
}

impl ConfirmedReceipt {
    pub fn recognize(body: &str) -> Option<Self> {
        let lower = body.to_ascii_lowercase();
        if [
            "cancelled",
            "canceled",
            "quotation",
            "pro forma",
            "proforma",
            "unpaid",
            "not paid",
            "payment due",
            "payment has not been made",
            "payment has not been received",
            "not been paid",
        ]
        .iter()
        .any(|marker| lower.contains(marker))
        {
            return None;
        }
        let (kind, pattern) = if lower.contains("brta service portal")
            && lower.contains("your payment")
            && lower.contains("is successful")
        {
            (ReceiptKind::Brta, &*BRTA_AMOUNT_RE)
        } else if lower.contains("dpdc")
            && lower.contains("is successfully recharged")
            && lower.contains("meter no")
            && lower.contains("energy cost")
        {
            (ReceiptKind::Dpdc, &*METER_RECHARGE_AMOUNT_RE)
        } else if BUNDLE_CHARGE_RE.is_match(body) {
            (ReceiptKind::Bundle, &*BUNDLE_CHARGE_RE)
        } else if lower.contains("ryans computers") && lower.contains("invoice copy") {
            (ReceiptKind::Ryans, &*RYANS_PURCHASE_RE)
        } else {
            (ReceiptKind::Fee, &*FEE_PAYMENT_RE)
        };
        let value = pattern.captures(body)?.get(1)?.as_str();
        let amount_minor = crate::common::amount::parse_minor_units(value.trim_end_matches('.'))?;
        (amount_minor > 0).then_some(Self { kind, amount_minor })
    }

    pub fn apply(self, transaction: &mut ParsedTransaction) {
        let (merchant, category) = match self.kind {
            ReceiptKind::Brta => (Some("BRTA"), "fees"),
            ReceiptKind::Dpdc => (Some("DPDC"), "bills"),
            ReceiptKind::Bundle => (None, "recharge"),
            ReceiptKind::Fee => (None, "fees"),
            ReceiptKind::Ryans => (Some("Ryans"), "shopping"),
        };
        transaction.transaction_type = TransactionType::Expense;
        transaction.amount_minor = self.amount_minor;
        transaction.party = merchant.map(str::to_string);
        transaction.merchant = merchant.map(str::to_string);
        transaction.category_id = Some(category.to_string());
        // Meter credit and product identifiers are not funding-account information.
        transaction.balance_after_minor = None;
        transaction.account_last4 = None;
        transaction.account_hint = None;
        transaction.fee_minor = None;
    }
}

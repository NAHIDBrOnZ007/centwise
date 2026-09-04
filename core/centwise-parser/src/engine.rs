//! 3-Stage Pipeline Parser Engine for Centwise.
//!
//! Orchestrates:
//! - Stage 1 (Classify): Fast safety filters (OTPs, telco MB packs, loan ads, low balance notices).
//! - Stage 2 (Provider): Resolves canonical Bank, MFS, or Telco provider.
//! - Stage 3 (Extract): Hunts amount, fee, balance, type, reference, party, account, and date.

use crate::types::{ParseOutcome, ParsedTransaction, RejectReason};
use centwise_normalization::normalize_sms_text;

/// Parses an SMS body with an optional sender hint into a structured transaction or rejection.
pub fn parse_sms(body: &str, sender_hint: Option<&str>) -> ParseOutcome {
    let normalized = normalize_sms_text(body);
    let trimmed = normalized.trim();

    // -----------------------------------------------------------------------
    // Stage 1: Fast Safety & Spam Filters
    // -----------------------------------------------------------------------
    if let Some(reason) = crate::classify::classify_safety(trimmed, sender_hint) {
        return ParseOutcome::Rejected(reason);
    }

    // -----------------------------------------------------------------------
    // Stage 2: Provider Identification
    // -----------------------------------------------------------------------
    let provider = crate::providers::detect_provider(sender_hint, trimmed);

    // -----------------------------------------------------------------------
    // Stage 3: Field Extraction
    // -----------------------------------------------------------------------
    let reference = crate::extract::extract_reference(trimmed);
    let fee_minor = crate::extract::extract_fee(trimmed);
    let balance_after_minor = crate::extract::extract_balance(trimmed);

    let amount_minor =
        match crate::extract::extract_main_amount(trimmed, fee_minor, balance_after_minor) {
            Some(amt) => amt,
            None => return ParseOutcome::Rejected(RejectReason::NoAmountFound),
        };

    let transaction_type = match crate::extract::detect_transaction_type(trimmed) {
        Some(t) => t,
        None => return ParseOutcome::Rejected(RejectReason::NotATransaction),
    };

    let party = crate::extract::extract_party(trimmed);
    let (account_last4, account_hint) = crate::extract::extract_account_info(trimmed);
    let raw_date = crate::extract::extract_date_time(trimmed);

    let (merchant, category_id) =
        crate::extract::resolve_categorization(party.as_deref(), trimmed, transaction_type);

    ParseOutcome::Parsed(Box::new(ParsedTransaction {
        provider_id: provider,
        transaction_type,
        amount_minor,
        fee_minor,
        balance_after_minor,
        reference,
        party,
        merchant,
        category_id,
        account_last4,
        account_hint,
        raw_date,
    }))
}

#[cfg(test)]
mod tests {
    use super::*;
    use centwise_domain::TransactionType;

    #[test]
    fn parses_image_1_robi_recharge_confirmation() {
        let text = "Transaction number R250917.1531.34003b to recharge 50 TAKA from 1847662920 is successful and valid till 17/10/25. For best offers dial *0# for minute*4# for data";
        let outcome = parse_sms(text, Some("Robi"));
        let ParseOutcome::Parsed(tx) = outcome else {
            panic!("expected parsed transaction");
        };

        assert_eq!(tx.provider_id, "robi");
        assert_eq!(tx.amount_minor, 5_000);
        assert_eq!(tx.transaction_type, TransactionType::Expense);
        assert_eq!(tx.reference.as_deref(), Some("R250917.1531.34003b"));
        assert_eq!(tx.party.as_deref(), Some("01847662920"));
    }

    #[test]
    fn rejects_image_2_robi_internet_loan_offer() {
        let text = "Need extra internet balance? Take Internet loan of 150 MB for 3days. Tk 16 will be deducted from your next recharge. Dial *123*003# Now!";
        let outcome = parse_sms(text, Some("Robi"));
        assert_eq!(
            outcome,
            ParseOutcome::Rejected(RejectReason::PromotionOrSpam)
        );
    }

    #[test]
    fn parses_image_3_robi_emergency_balance_as_income_and_clean_merchant() {
        let text = "Tk15 has been added to your account. Tk15 + Service fee of Tk2.78 will be deducted from your next recharge. Your total outstanding is Tk 17.78. For Details dial *123*600#";
        let outcome = parse_sms(text, Some("123"));
        let ParseOutcome::Parsed(tx) = outcome else {
            panic!("expected parsed transaction");
        };

        assert_eq!(tx.provider_id, "robi");
        assert_eq!(tx.amount_minor, 1_500);
        assert_eq!(tx.fee_minor, Some(278));
        assert_eq!(tx.transaction_type, TransactionType::Income);
        // Merchant must NOT be "your next recharge"!
        assert_ne!(tx.party.as_deref(), Some("your next recharge"));
        assert_ne!(tx.merchant.as_deref(), Some("your next recharge"));
    }

    #[test]
    fn rejects_image_4_robi_low_balance_alert() {
        let text = "Your balance is finished. Dial *123*007# to get Jhotpot Loan. Recharge TK26 for 69P/Min +Taxes for 2 days";
        let outcome = parse_sms(text, Some("123"));
        assert_eq!(
            outcome,
            ParseOutcome::Rejected(RejectReason::PromotionOrSpam)
        );
    }

    #[test]
    fn rejects_image_5_robi_data_bundle_loan_ad() {
        let text = "Need extra data? Get a Balance of Tk 12 to purchase a data bundle and pay on your next recharge. Dial *123*007# now. Fee: Tk 2.67";
        let outcome = parse_sms(text, Some("Robi"));
        assert_eq!(
            outcome,
            ParseOutcome::Rejected(RejectReason::PromotionOrSpam)
        );
    }

    #[test]
    fn rejects_banglalink_mb_offer_list_and_never_enters_review() {
        let text = "1) 208TK 15GB 30DAYS (Dial *212*831#)\n2) 18GB+200Mins(30D) @299TK; Dial *212*974#\n10) 1P/sec(+tax) rate for 7 days @TK44 Recharge\nGhechang Recharge";
        let outcome = parse_sms(text, Some("Banglalink"));
        assert_eq!(
            outcome,
            ParseOutcome::Rejected(RejectReason::PromotionOrSpam)
        );
        assert!(!crate::classify::is_likely_financial_review(
            text,
            Some("Banglalink")
        ));
    }

    #[test]
    fn parses_bank_debit_without_tk_currency_marker() {
        let text = "Dear Customer, Your A/C XXXX1234 has been debited by 3,500.00 on 22/08/2026. Available Balance: 21,950.00. [Bank Name]";
        let outcome = parse_sms(text, Some("City Bank"));
        let ParseOutcome::Parsed(tx) = outcome else {
            panic!("expected parsed transaction");
        };

        assert_eq!(tx.provider_id, "city-bank");
        assert_eq!(tx.amount_minor, 350_000);
        assert_eq!(tx.balance_after_minor, Some(2_195_000));
        assert_eq!(tx.transaction_type, TransactionType::Expense);
        assert_eq!(tx.account_last4.as_deref(), Some("1234"));
    }

    #[test]
    fn parses_bank_card_transaction_with_clean_merchant() {
        let text = "Dear Customer, Card ending 4321 was used at SWAPNO for BDT 1,200.00 on 22/08/2026. Available Balance: BDT 15,000.00.";
        let outcome = parse_sms(text, Some("BRAC-BANK"));
        let ParseOutcome::Parsed(tx) = outcome else {
            panic!("expected parsed transaction");
        };

        assert_eq!(tx.provider_id, "brac-bank");
        assert_eq!(tx.amount_minor, 120_000);
        assert_eq!(tx.party.as_deref(), Some("SWAPNO"));
        assert_eq!(tx.category_id.as_deref(), Some("food"));
        assert_eq!(tx.account_last4.as_deref(), Some("4321"));
    }

    #[test]
    fn cleans_merchant_trailing_verb_is() {
        let text = "Payment of Tk 2,500.00 to 'Software Shop Ltd' is successful. TrxID 9A8B7C on 13/11/2023. Balance 5,000.";
        let outcome = parse_sms(text, Some("bKash"));
        let ParseOutcome::Parsed(tx) = outcome else {
            panic!("expected parsed transaction");
        };

        assert_eq!(tx.amount_minor, 250_000);
        assert_eq!(tx.party.as_deref(), Some("Software Shop Ltd"));
        assert_eq!(tx.reference.as_deref(), Some("9A8B7C"));
    }

    #[test]
    fn parses_utility_bill_payment_desco() {
        let text = "Bill Payment of Tk 1,850.00 to DESCO successful. Biller A/C 12345678. Fee Tk 0.00. Balance Tk 2,113.00. TrxID 5F4G3H2J on 12/05/2026";
        let outcome = parse_sms(text, Some("bKash"));
        let ParseOutcome::Parsed(tx) = outcome else {
            panic!("expected parsed transaction");
        };

        assert_eq!(tx.amount_minor, 185_000);
        assert_eq!(tx.party.as_deref(), Some("DESCO"));
        assert_eq!(tx.category_id.as_deref(), Some("bills"));
        assert_eq!(tx.reference.as_deref(), Some("5F4G3H2J"));
        assert_eq!(tx.transaction_type, TransactionType::Expense);
    }

    #[test]
    fn parses_bank_sms_alert_fee_debit() {
        let text = "SMS Alert Fee of BDT 230.00 has been debited from your A/C XXXX1234 on 30/06/2026. Available Balance: BDT 18,200.00. [Bank Name]";
        let outcome = parse_sms(text, Some("City Bank"));
        let ParseOutcome::Parsed(tx) = outcome else {
            panic!("expected parsed transaction");
        };

        assert_eq!(tx.amount_minor, 23_000);
        assert_eq!(tx.balance_after_minor, Some(1_820_000));
        assert_eq!(tx.transaction_type, TransactionType::Expense);
        assert_eq!(tx.category_id.as_deref(), Some("fees"));
        assert_eq!(tx.account_last4.as_deref(), Some("1234"));
    }

    #[test]
    fn parses_npsb_interbank_fund_transfer() {
        let text = "Fund transfer of BDT 10,000.00 from A/C *1234 via NPSB on 05/09/2026. Fee: BDT 10.00. Bal: BDT 45,000.00";
        let outcome = parse_sms(text, Some("EBL"));
        let ParseOutcome::Parsed(tx) = outcome else {
            panic!("expected parsed transaction");
        };

        assert_eq!(tx.provider_id, "ebl");
        assert_eq!(tx.amount_minor, 1_000_000);
        assert_eq!(tx.fee_minor, Some(1_000));
        assert_eq!(tx.balance_after_minor, Some(4_500_000));
        assert_eq!(tx.transaction_type, TransactionType::Transfer);
    }

    #[test]
    fn parses_grameenphone_recharge_confirmation() {
        let text = "Recharge of Tk 100.00 on 017XXXXXXXX successful on 22/08/2026 14:30. Current Balance: Tk 102.50. TrxID: GP987654321";
        let outcome = parse_sms(text, Some("GP"));
        let ParseOutcome::Parsed(tx) = outcome else {
            panic!("expected parsed transaction");
        };

        assert_eq!(tx.provider_id, "grameenphone");
        assert_eq!(tx.amount_minor, 10_000);
        assert_eq!(tx.party.as_deref(), Some("017XXXXXXXX"));
        assert_eq!(tx.reference.as_deref(), Some("GP987654321"));
        assert_eq!(tx.transaction_type, TransactionType::Expense);
        assert_eq!(tx.category_id.as_deref(), Some("recharge"));
    }

    #[test]
    fn parses_cellfin_fund_transfer() {
        let text = "CellFin Transfer: Tk 2,500.00 debited from A/C *1234 to bKash 017XXXXXXXX. Fee: Tk 0.00. TrxID: CF12345678";
        let outcome = parse_sms(text, Some("Cellfin"));
        let ParseOutcome::Parsed(tx) = outcome else {
            panic!("expected parsed transaction");
        };

        assert_eq!(tx.provider_id, "cellfin");
        assert_eq!(tx.amount_minor, 250_000);
        assert_eq!(tx.reference.as_deref(), Some("CF12345678"));
        assert_eq!(tx.transaction_type, TransactionType::Transfer);
    }
}

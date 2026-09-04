//! Field extraction: Transaction Direction (Income, Expense, Refund, Transfer).

use centwise_domain::TransactionType;

pub fn detect_transaction_type(text: &str) -> Option<TransactionType> {
    let lower = text.to_lowercase();

    // 1. Refunds and Reversals
    if lower.contains("refund")
        || lower.contains("reversal")
        || lower.contains("reversed")
        || lower.contains("ফেরত")
    {
        return Some(TransactionType::Refund);
    }

    // 2. Transfers between own accounts, wallet transfers, and interbank
    if lower.contains("between your own accounts")
        || lower.contains("between own accounts")
        || lower.contains("fund transfer")
        || lower.contains("transfer:")
        || lower.contains("transfer of")
        || lower.contains("transfer to")
        || lower.contains("transferred")
        || lower.contains("স্থানান্তর")
    {
        // If body mentions NPSB/BEFTN/RTGS with a debit verb, it's a transfer
        return Some(TransactionType::Transfer);
    }

    // 3. Interbank transfers via NPSB/BEFTN/RTGS (explicit signals)
    if (lower.contains("npsb") || lower.contains("beftn") || lower.contains("rtgs"))
        && (lower.contains("from a/c")
            || lower.contains("to a/c")
            || lower.contains("to your a/c")
            || lower.contains("debited")
            || lower.contains("credited")
            || lower.contains("credit"))
    {
        return Some(TransactionType::Transfer);
    }

    // 4. Emergency balance / loan addition to account (e.g. "Tk15 has been added to your account")
    if lower.contains("has been added to your account")
        || lower.contains("added to your account")
        || lower.contains("added to your balance")
    {
        return Some(TransactionType::Income);
    }

    // 5. Standard Income keywords
    if lower.contains("cash in")
        || lower.contains("received")
        || lower.contains("credited")
        || lower.contains("add money")
        || lower.contains("cashback")
        || lower.contains("interest")
        || lower.contains("salary")
        || lower.contains("deposit")
        || lower.contains("জমা হয়েছে")
        || lower.contains("পেয়েছেন")
    {
        return Some(TransactionType::Income);
    }

    // 6. Standard Expense keywords (safely ignoring future conditional "will be deducted")
    let has_current_debit = lower.contains("cash out")
        || lower.contains("send money")
        || lower.contains("payment")
        || lower.contains("debited")
        || lower.contains("withdrawal")
        || lower.contains("recharge")
        || lower.contains("emi")
        || lower.contains("purchase")
        || lower.contains("bill pay")
        || lower.contains("used at")
        || lower.contains("was used")
        || lower.contains("used for")
        || lower.contains("excise duty")
        || lower.contains("annual fee")
        || lower.contains("annual card fee")
        || lower.contains("sms alert fee")
        || lower.contains("maintenance fee")
        || lower.contains("ledger fee")
        || lower.contains("খরচ")
        || lower.contains("কাটা হয়েছে")
        || (lower.contains("deducted") && !lower.contains("will be deducted"));

    if has_current_debit {
        return Some(TransactionType::Expense);
    }

    None
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn classifies_added_emergency_loan_as_income() {
        let text = "Tk15 has been added to your account. Tk15 + Service fee of Tk2.78 will be deducted from your next recharge.";
        assert_eq!(detect_transaction_type(text), Some(TransactionType::Income));
    }

    #[test]
    fn classifies_bank_debit_as_expense() {
        let text = "Your A/C XXXX1234 has been debited by 5,000.00 on 22/08/2026.";
        assert_eq!(
            detect_transaction_type(text),
            Some(TransactionType::Expense)
        );
    }

    #[test]
    fn classifies_card_usage_as_expense() {
        let text = "Card ending 4321 was used at SWAPNO for BDT 1,200.00";
        assert_eq!(
            detect_transaction_type(text),
            Some(TransactionType::Expense)
        );
    }

    #[test]
    fn classifies_recharge_confirmation_as_expense() {
        let text = "Transaction number R250917.1531.34003b to recharge 50 TAKA from 1847662920 is successful";
        assert_eq!(
            detect_transaction_type(text),
            Some(TransactionType::Expense)
        );
    }

    #[test]
    fn classifies_npsb_transfer() {
        let text = "Fund transfer of BDT 10,000.00 from A/C *1234 via NPSB on 05/09/2026.";
        assert_eq!(
            detect_transaction_type(text),
            Some(TransactionType::Transfer)
        );
    }

    #[test]
    fn classifies_beftn_salary_credit_as_transfer() {
        let text = "BEFTN credit of BDT 25,000.00 to your A/C XXXX1234. Ref: BF12345678";
        assert_eq!(
            detect_transaction_type(text),
            Some(TransactionType::Transfer)
        );
    }

    #[test]
    fn classifies_excise_duty_as_expense() {
        let text = "Excise Duty BDT 500.00 has been debited from your A/C XXXX5678.";
        assert_eq!(
            detect_transaction_type(text),
            Some(TransactionType::Expense)
        );
    }

    #[test]
    fn classifies_annual_card_fee_as_expense() {
        let text = "Annual Card Fee of BDT 3,000.00 debited from your Card *1234.";
        assert_eq!(
            detect_transaction_type(text),
            Some(TransactionType::Expense)
        );
    }

    #[test]
    fn classifies_sms_alert_fee_as_expense() {
        let text = "SMS Alert Fee of BDT 230.00 has been debited from your A/C XXXX1234.";
        assert_eq!(
            detect_transaction_type(text),
            Some(TransactionType::Expense)
        );
    }

    #[test]
    fn classifies_add_money_as_income() {
        let text = "Add Money Tk 3,000.00 successful. Fee Tk 0.00. Balance Tk 10,458.00.";
        assert_eq!(detect_transaction_type(text), Some(TransactionType::Income));
    }
}

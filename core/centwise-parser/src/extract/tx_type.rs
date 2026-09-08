//! Field extraction: Transaction Direction (Income, Expense, Refund, Transfer).

use centwise_domain::TransactionType;

pub fn detect_transaction_type(text: &str) -> Option<TransactionType> {
    let lower = text.to_lowercase();

    // 1. Refunds and Reversals
    if lower.contains("refund") || lower.contains("reversal") || lower.contains("reversed") {
        return Some(TransactionType::Refund);
    }

    // 2. Incoming Credits via Fund Transfer / RTGS / NPSB / BEFTN / EFT (Inflows = Income)
    // When an account is credited via RTGS, NPSB, BEFTN, or Fund Transfer, it is incoming money.
    let is_account_credit = (lower.contains("credited")
        || lower.contains("deposit")
        || lower.contains("received deposit")
        || lower.contains("cr transaction")
        || lower.contains("cr. transaction")
        || (lower.contains("credit")
            && (lower.contains("to your a/c")
                || lower.contains("to a/c")
                || lower.contains("in your a/c")
                || lower.contains("funds transfer - credit"))))
        && !lower.contains("debited from your a/c")
        && !lower.contains("debited from your account")
        && !lower.contains("recharge request");

    if is_account_credit
        && (lower.contains("fund transfer")
            || lower.contains("funds transfer")
            || lower.contains("rtgs")
            || lower.contains("npsb")
            || lower.contains("beftn")
            || lower.contains("eft"))
    {
        return Some(TransactionType::Income);
    }

    // 3. Transfers between own accounts, wallet transfers, and interbank outflows
    if lower.contains("between your own accounts")
        || lower.contains("between own accounts")
        || lower.contains("fund transfer")
        || lower.contains("transfer:")
        || lower.contains("transfer of")
        || lower.contains("transfer to")
        || lower.contains("transfer money")
        || lower.contains("transferred")
    {
        return Some(TransactionType::Transfer);
    }

    // 4. Interbank transfers via NPSB/BEFTN/RTGS (explicit signals)
    if (lower.contains("npsb") || lower.contains("beftn") || lower.contains("rtgs"))
        && (lower.contains("from a/c")
            || lower.contains("to a/c")
            || lower.contains("to your a/c")
            || lower.contains("debited"))
    {
        return Some(TransactionType::Transfer);
    }

    // 4. Emergency balance / loan addition to account (e.g. "Tk15 has been added to your account")
    if lower.contains("has been added to your account")
        || lower.contains("added to your account")
        || lower.contains("added to your balance")
        || (lower.contains("emergency balance") && lower.contains("received"))
        || (lower.contains("jhotpot balance") && lower.contains("received"))
        || (lower.contains("emergency loan")
            && (lower.contains("credited") || lower.contains("received")))
    {
        return Some(TransactionType::Income);
    }

    // 5. Explicit Expense overrides:
    // - Recharge requests: "Received Recharge request of Tk 20.00 for 01615076000"
    // - Bank account debit with merchant/service credit: "debited from your a/c ***316 and credited to MOBILE RECHARGE"
    // - Cash Out / Send Money with promotional footers: "Cash Out Tk 4,000 ... Cashback 50 on 25,000 CashOut"
    if lower.contains("received recharge request")
        || lower.contains("recharge request")
        || lower.contains("debited from your a/c")
        || lower.contains("debited from your account")
        || lower.contains("cash out")
        || lower.contains("send money")
        || lower.contains("you have sent")
        || lower.contains("sent to")
    {
        return Some(TransactionType::Expense);
    }

    // 6. Standard Income keywords
    if lower.contains("cash in")
        || (lower.contains("received") && !lower.contains("recharge request"))
        || (lower.contains("credited") && !lower.contains("debited from your"))
        || lower.contains("cr transaction")
        || lower.contains("cr. transaction")
        || lower.contains("add money")
        || (lower.contains("cashback")
            && (lower.contains("received")
                || lower.contains("credited")
                || lower.contains("added")
                || !lower.contains("cashout")))
        || lower.contains("interest")
        || (lower.contains("salary")
            && (lower.contains("credited")
                || lower.contains("deposited")
                || lower.contains("received")
                || lower.contains("a/c")
                || lower.contains("account")
                || lower.contains("salary of")))
        || (lower.contains("deposit") && !lower.contains("deducted"))
        || lower.contains("remittance")
    {
        return Some(TransactionType::Income);
    }

    // 7. Standard Expense keywords (safely ignoring future conditional "will be deducted")
    let has_current_debit = lower.contains("cash out")
        || lower.contains("send money")
        || lower.contains("you have sent")
        || lower.contains("sent to")
        || (lower.contains("sent") && !lower.contains("sent from"))
        || lower.contains("payment")
        || lower.contains("debited")
        || lower.contains("withdrawal")
        || lower.contains("withdrawn")
        || lower.contains("recharge")
        || lower.contains("emi")
        || (lower.contains("purchase")
            && !lower.contains("min purchase")
            && !lower.contains("minimum purchase"))
        || lower.contains("spent")
        || lower.contains("charged")
        || lower.contains("bought")
        || lower.contains("activated")
        || lower.contains("pack purchase")
        || lower.contains("dr transaction")
        || lower.contains("dr. transaction")
        || lower.contains("auto debit")
        || lower.contains("auto-debit")
        || lower.contains("loan repayment")
        || lower.contains("bill pay")
        || lower.contains("pay bill")
        || lower.contains("used at")
        || lower.contains("was used")
        || lower.contains("used for")
        || lower.contains("done with your debit card")
        || lower.contains("done with your credit card")
        || lower.contains("done with your card")
        || lower.contains("excise duty")
        || lower.contains("annual fee")
        || lower.contains("annual card fee")
        || lower.contains("sms alert fee")
        || lower.contains("maintenance fee")
        || lower.contains("ledger fee")
        || lower.contains("recovered for")
        || lower.contains("has been recovered")
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
    fn negative_fund_transfer_is_not_a_refund_without_refund_wording() {
        let text = "Your A/C debited (Fund Transfer) by Tk-500.00. C/B Tk1,03,973.32.";
        assert_eq!(
            detect_transaction_type(text),
            Some(TransactionType::Transfer)
        );
    }

    #[test]
    fn classifies_beftn_salary_credit_as_income() {
        let text = "BEFTN credit of BDT 25,000.00 to your A/C XXXX1234. Ref: BF12345678";
        assert_eq!(detect_transaction_type(text), Some(TransactionType::Income));
    }

    #[test]
    fn classifies_rtgs_credit_as_income() {
        let text = "Dear Sir, your A/C ***5543 credited (RTGS Funds Transfer - Credit) by Tk5,00,000.00 on 28-08-2023 11:45:57 AM C/B Tk5,00,766.82. NexusPay https://bit.ly/nexuspay";
        assert_eq!(detect_transaction_type(text), Some(TransactionType::Income));
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

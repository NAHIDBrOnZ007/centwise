//! Stage 1: Fast Classification & Safety Filtering
//!
//! Evaluates messages before parsing to reject promotional spam, telco bundles,
//! loan advertisements, low balance alerts, bonus/free-data notices,
//! VAS subscriptions, congratulations marketing, and OTPs.

pub mod otp;
pub mod telco;

use crate::types::RejectReason;

/// Runs Stage 1 safety checks. Returns a `RejectReason` if the message should be discarded immediately.
pub fn classify_safety(body: &str, sender_hint: Option<&str>) -> Option<RejectReason> {
    let trimmed = body.trim();

    if otp::is_otp_or_security_message(trimmed) {
        return Some(RejectReason::OtpOrSecurity);
    }

    if telco::is_promotional_or_telco_offer(trimmed, sender_hint) {
        return Some(RejectReason::PromotionOrSpam);
    }

    false_or_none(trimmed)
}

fn false_or_none(_text: &str) -> Option<RejectReason> {
    None
}

/// Determines if an unparseable or rejected message should be queued for human review.
/// Strictly excludes spam, promotions, telco bundles, and non-financial messages.
pub fn is_likely_financial_review(body: &str, sender_hint: Option<&str>) -> bool {
    let lower = body.to_lowercase();
    let sender = sender_hint.unwrap_or_default().to_lowercase();

    // 1. If it was classified as OTP or Promotional/Telco Offer, it must NEVER be queued for review!
    if classify_safety(body, sender_hint).is_some() {
        return false;
    }

    // 2. Filter out non-financial apps
    let obvious_non_financial = [
        "uber",
        "pathao",
        "foodpanda",
        "daraz",
        "mercedes-benz",
        "netflix",
        "spotify",
    ]
    .iter()
    .any(|word| {
        (lower.contains(word) || sender.contains(word))
            && !lower.contains("debited")
            && !lower.contains("credited")
    });
    if obvious_non_financial {
        return false;
    }

    // 3. Genuine financial markers (debited, credited, account, balance, trxid, cards, etc.)
    [
        "bkash",
        "nagad",
        "rocket",
        "cellfin",
        "upay",
        "bank",
        "a/c",
        "account",
        "available balance",
        "avail bal",
        "trxid",
        "txnid",
        "debited",
        "credited",
        "cash out",
        "cash in",
        "send money",
        "withdraw",
        "deposit",
        "card used",
        "card ending",
        "fund transfer",
        "bill payment",
        "add money",
        "mobile recharge",
        "recharge of",
        "npsb",
        "beftn",
        "rtgs",
        "excise duty",
        "annual fee",
        "টাকা",
        "লেনদেন",
    ]
    .iter()
    .any(|word| lower.contains(word) || sender.contains(word))
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn promotional_and_telco_messages_never_enter_review_queue() {
        let banglalink_mb_list = "1) 208TK 15GB 30DAYS (Dial *212*831#)\n10) 1P/sec(+tax) rate for 7 days @TK44 Recharge";
        assert!(!is_likely_financial_review(
            banglalink_mb_list,
            Some("Banglalink")
        ));

        let robi_loan_ad = "Need extra internet balance? Take Internet loan of 150 MB for 3days. Tk 16 will be deducted from your next recharge. Dial *123*003# Now!";
        assert!(!is_likely_financial_review(robi_loan_ad, Some("Robi")));

        let low_balance_ad = "Your balance is finished. Dial *123*007# to get Jhotpot Loan. Recharge TK26 for 69P/Min +Taxes for 2 days";
        assert!(!is_likely_financial_review(low_balance_ad, Some("123")));
    }

    #[test]
    fn bonus_and_congratulations_never_enter_review_queue() {
        let bonus = "Enjoy 100MB bonus data! Valid for 3 days. Dial *121# to check balance.";
        assert!(!is_likely_financial_review(bonus, Some("GP")));

        let congrats =
            "Congratulations! You've won a chance to get 1GB free. Dial *121*99# to claim now!";
        assert!(!is_likely_financial_review(congrats, Some("GP")));

        let reward = "You have earned 50 reward points on your recent transaction. Check your points balance on the app.";
        assert!(!is_likely_financial_review(reward, Some("BRAC-BANK")));
    }

    #[test]
    fn genuine_financial_messages_with_unusual_formats_are_reviewable() {
        let ambiguous_bank_sms = "Your account has had a special debit of 5,000 without standard code. Available balance is unknown.";
        assert!(is_likely_financial_review(ambiguous_bank_sms, Some("BANK")));
    }

    #[test]
    fn npsb_beftn_transfers_are_reviewable() {
        let npsb_msg = "NPSB transfer of BDT 5,000 from unknown account format.";
        assert!(is_likely_financial_review(npsb_msg, Some("EBL")));
    }
}

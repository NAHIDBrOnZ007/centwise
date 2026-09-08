//! Stage 1: Fast Classification & Safety Filtering
//!
//! Evaluates messages before parsing to reject promotional spam, telco bundles,
//! loan advertisements, low balance alerts, bonus/free-data notices,
//! VAS subscriptions, congratulations marketing, and OTPs.

pub mod otp;
pub mod telco;

use crate::types::RejectReason;
use regex::Regex;
use std::sync::LazyLock;

static POSTED_REFUND_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(r"(?i)\b(?:credited|refunded|completed|successful)\b")
        .expect("valid posted refund regex")
});

static UNPOSTED_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(r"(?i)\b(?:pending|requested|reminder)\b").expect("valid unposted regex")
});

/// Returns true if the string contains any characters in the Bengali Unicode block (U+0980 - U+09FF).
pub fn contains_bengali_script(text: &str) -> bool {
    text.chars().any(|c| ('\u{0980}'..='\u{09FF}').contains(&c))
}

/// Runs Stage 1 safety checks. Returns a `RejectReason` if the message should be discarded immediately.
pub fn classify_safety(body: &str, sender_hint: Option<&str>) -> Option<RejectReason> {
    let trimmed = body.trim();

    // 0. Bangla Gatekeeper: Discard Bengali script early as unsupported language
    if contains_bengali_script(trimmed) {
        return Some(RejectReason::PromotionOrSpam);
    }

    if otp::is_otp_or_security_message(trimmed) {
        return Some(RejectReason::OtpOrSecurity);
    }

    if is_marketing_or_scam_message(trimmed) {
        return Some(RejectReason::PromotionOrSpam);
    }

    if telco::is_promotional_or_telco_offer(trimmed, sender_hint) {
        return Some(RejectReason::PromotionOrSpam);
    }

    let lower = trimmed.to_lowercase();
    // bKash intermediate recharge request notices without TrxID (e.g. "Your bKash Mobile Recharge request of Tk 40.00 for ... was successful. Use bKash App...").
    // Discard notice so only the authoritative confirmation receipt with TrxID and balance is parsed.
    if lower.contains("your bkash mobile recharge request") && lower.contains("use bkash app") {
        return Some(RejectReason::NotATransaction);
    }

    if is_unposted_or_registration_invite(trimmed) {
        return Some(RejectReason::NotATransaction);
    }

    if is_non_posted_financial_message(trimmed) {
        return Some(RejectReason::NotATransaction);
    }

    // Fixed Deposit (FD) rollover / renewal advice (not cash-flow or checking transaction)
    if lower.contains("has been renewed at an interest rate")
        || (lower.contains("fd a/c") && lower.contains("has been renewed"))
    {
        return Some(RejectReason::NotATransaction);
    }

    // Insurance policy expiry / renewal advisory notices (not a money debit/credit)
    if (lower.contains("insurance policy")
        && (lower.contains("will expire") || lower.contains("to renew")))
        || (lower.contains("sum insured") && lower.contains("premium"))
        || lower.contains("policy will expire")
    {
        return Some(RejectReason::NotATransaction);
    }

    // Educational course admission / tuition confirmation receipts
    // (e.g. "Dear Tasfia, your admission is successful for Utkorsho HSC 25 ... you paid Tk 2500")
    if lower.contains("admission is successful")
        || (lower.contains("admission")
            && lower.contains("roll number")
            && lower.contains("registration no"))
    {
        return Some(RejectReason::NotATransaction);
    }

    false_or_none(trimmed)
}

/// Detects commercial e-commerce marketing promotions, shopping discounts,
/// and recruitment/job WhatsApp scam messages.
pub fn is_marketing_or_scam_message(text: &str) -> bool {
    let lower = text.to_lowercase();

    // 1. Job recruitment / WhatsApp scams (e.g. "BOSCH is recruiting... salary is 23600 BDT... https://wa.me/...")
    let has_whatsapp_or_tg = lower.contains("wa.me/")
        || lower.contains("whatsapp")
        || lower.contains("contact the staff")
        || lower.contains("telegram")
        || lower.contains("t.me/");

    let is_recruitment_scam = lower.contains("is recruiting")
        || lower.contains("recruiting internet")
        || lower.contains("job vacancy")
        || lower.contains("part time job")
        || lower.contains("part-time job")
        || lower.contains("earn daily")
        || lower.contains("daily income")
        || lower.contains("work from home")
        || (has_whatsapp_or_tg
            && (lower.contains("salary")
                || lower.contains("daily income")
                || lower.contains("working in")
                || lower.contains("daily salary")
                || lower.contains("part time")
                || lower.contains("vacancy")));
    if is_recruitment_scam {
        return true;
    }

    // 2. Commercial / retail shopping marketing blasts (e.g. "1 TAKA-2 Products!! Min Purchase: 399TAKA Shop: www.TheMallBD.com")
    let has_shopping_ad_marker = lower.contains("min purchase")
        || lower.contains("minimum purchase")
        || lower.contains("min order")
        || lower.contains("minimum order")
        || lower.contains("shop:")
        || lower.contains("shop at www")
        || lower.contains("themallbd.com")
        || lower.contains("buy 1 get 1")
        || lower.contains("buy 1 get")
        || lower.contains("bogo")
        || (lower.contains("at 1 taka") && lower.contains("products"))
        || lower.contains("free delivery")
        || lower.contains("promo code")
        || lower.contains("coupon")
        || lower.contains("voucher")
        || (lower.contains("valid till") && lower.contains("purchase of"));

    let has_confirmed_bank_action = lower.contains("has been debited")
        || lower.contains("has been credited")
        || lower.contains("was debited")
        || lower.contains("was credited")
        || (lower.contains("a/c") && lower.contains("debited"))
        || (lower.contains("a/c") && lower.contains("credited"))
        || (lower.contains("successful") && (lower.contains("trxid") || lower.contains("txnid")));

    if has_shopping_ad_marker && !has_confirmed_bank_action {
        return true;
    }

    false
}

/// Detects registration invites and non-account holder claim notifications
/// (e.g. "Tk 200.00 has been sent from 018... To receive the money, open Account from bKash App within 31/05/2024").
pub fn is_unposted_or_registration_invite(text: &str) -> bool {
    let lower = text.to_lowercase();
    lower.contains("to receive the money, open account")
        || lower.contains("open account from bkash app")
        || (lower.contains("open account") && lower.contains("to receive"))
        || lower.contains("bka.sh/smnewreg")
        || lower.contains("smnewreg")
}

/// Rejects financial-looking messages that do not represent posted money movement.
fn is_non_posted_financial_message(text: &str) -> bool {
    let lower = text.to_lowercase();

    // A posted refund/reversal is a real credit even when it describes an earlier failure.
    let is_posted_refund = lower.split(". ").any(|clause| {
        (clause.contains("refund") || clause.contains("reversal") || clause.contains("reversed"))
            && POSTED_REFUND_RE.is_match(clause)
            && !clause.contains("not ")
            && !clause.contains("will be")
            && !clause.contains("unsuccessful")
            && !UNPOSTED_RE.is_match(clause)
    });
    if is_posted_refund {
        return false;
    }

    let failed = [
        "declined",
        "failed",
        "unsuccessful",
        "rejected",
        "cancelled",
        "canceled",
        "could not be processed",
        "not processed",
        "not been credited",
        "will be credited",
    ]
    .iter()
    .any(|marker| lower.contains(marker));

    let pending = [
        "is pending",
        "pending for",
        "pending transaction",
        "under process",
        "will be processed",
        "has been initiated",
    ]
    .iter()
    .any(|marker| lower.contains(marker));

    let has_posted_action = [
        "successful",
        "completed",
        "has been debited",
        "was debited",
        "has been credited",
        "was credited",
        "payment received",
    ]
    .iter()
    .any(|marker| lower.contains(marker));

    let request_or_reminder = !has_posted_action
        && (UNPOSTED_RE.is_match(&lower)
            || ([
                "payment request",
                "has requested",
                "requesting payment",
                "collect request",
                "minimum amount due",
                "min amount due",
                "payment is due",
                "payment of",
            ]
            .iter()
            .any(|marker| lower.contains(marker))
                && (lower.contains("approve")
                    || lower.contains("requested")
                    || lower.contains("request")
                    || lower.contains(" is due")
                    || lower.contains(" due by")
                    || lower.contains("minimum amount due")
                    || lower.contains("min amount due"))));

    let loan_or_payment_reminder = !has_posted_action
        && (((lower.contains("please deposit") || lower.contains("kindly deposit"))
            && (lower.contains("installment")
                || lower.contains("instalment")
                || lower.contains("loan")
                || lower.contains("sme")))
            || lower.contains("kindly ignore if you have already made the deposit"));

    failed || pending || request_or_reminder || loan_or_payment_reminder
}

fn false_or_none(_text: &str) -> Option<RejectReason> {
    None
}

/// Determines if an unparseable or rejected message should be queued for human review.
/// Strictly excludes spam, promotions, telco bundles, non-financial messages, and Bengali Unicode.
pub fn is_likely_financial_review(body: &str, sender_hint: Option<&str>) -> bool {
    // 0. Bangla Gatekeeper: Never queue Bengali Unicode script for human review in English pipeline
    if contains_bengali_script(body) {
        return false;
    }

    if is_marketing_or_scam_message(body)
        || is_unposted_or_registration_invite(body)
        || otp::is_otp_or_security_message(body)
    {
        return false;
    }

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

    #[test]
    fn rejects_unposted_financial_events_but_keeps_posted_refunds() {
        assert!(is_non_posted_financial_message(
            "Payment of BDT 2,000 was declined"
        ));
        assert!(is_non_posted_financial_message(
            "Fund transfer of BDT 5,000 is pending for processing"
        ));
        assert!(is_non_posted_financial_message(
            "Payment request of Tk 900 received. Open app to approve"
        ));
        assert!(!is_non_posted_financial_message(
            "Failed card transaction refund of BDT 2,000 credited to your account"
        ));
        assert!(!is_non_posted_financial_message(
            "Credit card payment of BDT 8,000 completed. Minimum amount due is now BDT 0"
        ));
    }

    #[test]
    fn bangla_gatekeeper_blocks_bengali_unicode_from_review_queue_and_parsing() {
        let bangla_sms = "আপনার বিকাশ একাউন্ট থেকে ৫০ টাকা রিচার্জ সফল হয়েছে। TrxID 8J7A6K9L";
        assert!(contains_bengali_script(bangla_sms));
        assert_eq!(
            classify_safety(bangla_sms, Some("bKash")),
            Some(RejectReason::PromotionOrSpam)
        );
        assert!(!is_likely_financial_review(bangla_sms, Some("bKash")));
    }
}

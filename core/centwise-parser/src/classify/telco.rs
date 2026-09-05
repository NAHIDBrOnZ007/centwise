//! Fast safety filter for Telecom (GP, Robi, Banglalink, Airtel, Teletalk)
//! promotional SMS, bundle menus, USSD prompts, loan advertisements,
//! low-balance warnings, bonus/free-data notices, VAS subscriptions,
//! congratulations marketing, and reward-point informational messages.

use regex::Regex;
use std::sync::LazyLock;

static USSD_DIAL_RE: LazyLock<Regex> =
    LazyLock::new(|| Regex::new(r"(?i)\bdial\s+\*[0-9*#]+").expect("valid ussd regex"));

static BUNDLE_MENU_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(r"(?i)(?:[0-9]+[)\.]\s*[0-9]+(?:TK|GB|MB)|@(?:TK\s*)?[0-9]+(?:\.[0-9]{1,2})?|Ghechang\s+Rechar[ge]{1,2})")
        .expect("valid bundle menu regex")
});

/// Checks if an SMS is a telecom promotional message, offer list, loan invitation,
/// bonus/free-data notice, VAS subscription, congratulations marketing,
/// or system upsell notice that must be discarded immediately.
pub fn is_promotional_or_telco_offer(body: &str, sender_hint: Option<&str>) -> bool {
    let lower = body.to_lowercase();
    let sender = sender_hint.unwrap_or_default().to_lowercase();

    // 1. Check for App download or generic marketing notices
    if lower.contains("app update")
        || lower.contains("download now")
        || lower.contains("download mygp")
        || lower.contains("download mybl")
        || lower.contains("download myrobi")
    {
        return true;
    }

    if lower.contains("trxid not applicable") && !lower.contains("successful") {
        return true;
    }

    // 2. Low-balance notifications and upsell prompts
    if lower.contains("your balance is finished")
        || lower.contains("your balance is low")
        || lower.contains("balance sesh")
        || lower.contains("your balance is 0")
    {
        return true;
    }

    // 3. Unaccepted loan offers, emergency balance invitations, or upsells
    if lower.contains("need extra data?")
        || lower.contains("need extra internet balance?")
        || lower.contains("take internet loan")
        || lower.contains("to get jhotpot loan")
        || lower.contains("get a balance of tk")
        || lower.contains("to settle your recent due loan")
        || lower.contains("to settle your due loan")
    {
        return true;
    }

    // 4. Telecom bundle menus / MB offer lists (e.g. Banglalink Ghechang, Robi/GP offer packs)
    if is_telco_bundle_menu(&lower, &sender) {
        return true;
    }

    // 5. Bonus / free data / free minutes notifications (not actual debits)
    if is_bonus_or_free_notification(&lower) && !is_confirmed_transaction_text(&lower) {
        return true;
    }

    // 6. VAS / subscription / auto-renewal charges that are micro-deductions
    if is_vas_subscription_notice(&lower) && !is_confirmed_transaction_text(&lower) {
        return true;
    }

    // 7. Congratulations / prize / lucky-draw / marketing blasts
    if is_congratulations_or_prize(&lower) && !is_confirmed_transaction_text(&lower) {
        return true;
    }

    // 8. FnF / Friends-and-Family / SIM migration / SIM replacement notices
    if is_fnf_or_migration_notice(&lower) {
        return true;
    }

    // 9. Reward points / loyalty informational notices (not transactions)
    if is_reward_points_notice(&lower) && !is_confirmed_transaction_text(&lower) {
        return true;
    }

    // 10. Balance check responses (e.g. "Your balance is Tk 102.50") without debit/credit action
    if is_balance_check_response(&lower) {
        return true;
    }

    // 11. Unexecuted USSD Call-To-Action (e.g., "Dial *123*003# Now!")
    // Note: If a message is a confirmed recharge or added balance,
    // a trailing advisory code like "For best offers dial *0#" or "For Details dial *123*600#" is just a footer.
    let has_confirmed_transaction = is_confirmed_transaction_text(&lower);
    if !has_confirmed_transaction && USSD_DIAL_RE.is_match(body) {
        return true;
    }

    false
}

/// Detects telco bundle offer lists such as Banglalink Ghechang or Robi/GP bundle menus.
pub fn is_telco_bundle_menu(lower: &str, sender: &str) -> bool {
    if lower.contains("ghechang") || lower.contains("amar offer") || lower.contains("my offer") {
        return true;
    }

    let is_telco_sender = sender.contains("gp")
        || sender.contains("robi")
        || sender.contains("bl")
        || sender.contains("banglalink")
        || sender.contains("airtel")
        || sender.contains("teletalk")
        || sender == "121"
        || sender == "123"
        || sender == "212";

    // Check for bundle menu patterns like "1) 208TK 15GB", "@299TK", "@TK44 Recharge", "1P/sec(+tax)"
    let menu_matches = BUNDLE_MENU_RE.find_iter(lower).count();
    if menu_matches >= 2 || (is_telco_sender && menu_matches >= 1 && lower.contains("dial *")) {
        return true;
    }

    // High density of telco units (GB, MB, Mins, 60 days, 30 days) combined with rate/@TK
    let has_bundle_units = (lower.contains("gb") || lower.contains("mb"))
        && (lower.contains("mins") || lower.contains("min"))
        && (lower.contains("days") || lower.contains("din"));
    if has_bundle_units && (lower.contains("dial") || lower.contains("@")) {
        return true;
    }

    false
}

/// Detects bonus, free data, or free minutes notifications that are NOT debits.
fn is_bonus_or_free_notification(lower: &str) -> bool {
    lower.contains("enjoy")
        && (lower.contains("mb") || lower.contains("gb") || lower.contains("min"))
        || lower.contains("bonus activated")
        || lower.contains("bonus mb")
        || lower.contains("bonus data")
        || lower.contains("free mb")
        || lower.contains("free data")
        || lower.contains("free minutes")
        || lower.contains("you got")
            && (lower.contains("mb free")
                || lower.contains("gb free")
                || lower.contains("min free"))
        || lower.contains("complimentary") && (lower.contains("mb") || lower.contains("gb"))
}

/// Detects VAS subscription / auto-renewal micro-charges.
fn is_vas_subscription_notice(lower: &str) -> bool {
    lower.contains("you subscribed to")
        || lower.contains("auto-renewal")
        || lower.contains("auto renewal")
        || lower.contains("subscription activated")
        || lower.contains("vas charge")
        || lower.contains("content charge")
        || (lower.contains("subscri") && lower.contains("/day"))
        || (lower.contains("subscri") && lower.contains("/week"))
}

/// Detects congratulations / prize / lucky-draw / marketing blasts.
fn is_congratulations_or_prize(lower: &str) -> bool {
    lower.contains("congratulations")
        || lower.contains("congrats")
        || lower.contains("you've won")
        || lower.contains("you have won")
        || lower.contains("lucky winner")
        || lower.contains("lucky draw")
        || lower.contains("prize money")
}

/// Detects FnF, SIM migration, and SIM replacement notices.
fn is_fnf_or_migration_notice(lower: &str) -> bool {
    lower.contains("fnf number")
        || lower.contains("friends and family")
        || lower.contains("sim replacement")
        || lower.contains("sim migration")
        || lower.contains("sim swap")
}

/// Detects reward points / loyalty informational notices.
fn is_reward_points_notice(lower: &str) -> bool {
    (lower.contains("reward point") || lower.contains("loyalty point"))
        && !lower.contains("debited")
        && !lower.contains("credited")
        && !lower.contains("redeemed")
}

/// Detects balance check response (no debit/credit action, just info).
fn is_balance_check_response(lower: &str) -> bool {
    (lower.starts_with("your balance is")
        || lower.starts_with("your main balance is")
        || lower.starts_with("current balance"))
        && !lower.contains("debited")
        && !lower.contains("credited")
        && !lower.contains("deducted")
        && !lower.contains("added")
        && !lower.contains("successful")
        && !lower.contains("received")
        && !lower.contains("payment")
}

fn is_confirmed_transaction_text(lower: &str) -> bool {
    lower.contains("is successful")
        || lower.contains("recharge successful")
        || lower.contains("recharge of")
        || lower.contains("mobile recharge")
        || lower.contains("bill payment")
        || lower.contains("add money")
        || lower.contains("credited with")
        || lower.contains("debited with")
        || lower.contains("has been debited")
        || lower.contains("has been credited")
        || lower.contains("has been added")
        || lower.contains("transaction number r")
        || (lower.contains("successful") && (lower.contains("trxid") || lower.contains("txnid")))
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn rejects_banglalink_ghechang_mb_offer_list() {
        let text = "1) 208TK 15GB 30DAYS (Dial *212*831#)\n2) 18GB+200Mins(30D) @299TK; Dial *212*974#\n10) 1P/sec(+tax) rate for 7 days @TK44 Recharge\nGhechang Recharge";
        assert!(is_promotional_or_telco_offer(text, Some("Banglalink")));
    }

    #[test]
    fn rejects_robi_internet_loan_offer() {
        let text = "Need extra internet balance? Take Internet loan of 150 MB for 3days. Tk 16 will be deducted from your next recharge. Dial *123*003# Now!";
        assert!(is_promotional_or_telco_offer(text, Some("Robi")));
    }

    #[test]
    fn rejects_robi_low_balance_alert() {
        let text = "Your balance is finished. Dial *123*007# to get Jhotpot Loan. Recharge TK26 for 69P/Min +Taxes for 2 days";
        assert!(is_promotional_or_telco_offer(text, Some("123")));
    }

    #[test]
    fn rejects_robi_data_bundle_loan_offer() {
        let text = "Need extra data? Get a Balance of Tk 12 to purchase a data bundle and pay on your next recharge. Dial *123*007# now. Fee: Tk 2.67";
        assert!(is_promotional_or_telco_offer(text, Some("Robi")));
    }

    #[test]
    fn allows_confirmed_recharge_with_promotional_footer() {
        let text = "Transaction number R250917.1531.34003b to recharge 50 TAKA from 1847662920 is successful and valid till 17/10/25. For best offers dial *0# for minute*4# for data";
        assert!(!is_promotional_or_telco_offer(text, Some("Robi")));
    }

    #[test]
    fn allows_confirmed_emergency_loan_disbursal() {
        let text = "Tk15 has been added to your account. Tk15 + Service fee of Tk2.78 will be deducted from your next recharge. Your total outstanding is Tk 17.78. For Details dial *123*600#";
        assert!(!is_promotional_or_telco_offer(text, Some("123")));
    }

    #[test]
    fn rejects_gp_bonus_data_notification() {
        let text = "Enjoy 100MB bonus data! Valid for 3 days. Dial *121# to check balance.";
        assert!(is_promotional_or_telco_offer(text, Some("GP")));
    }

    #[test]
    fn rejects_gp_free_data_notification() {
        let text = "You got 50 MB free! Use it before midnight. Terms apply.";
        assert!(is_promotional_or_telco_offer(text, Some("GP")));
    }

    #[test]
    fn rejects_vas_subscription_notice() {
        let text = "You subscribed to FunBox Games @ Tk 2.44/day. To unsubscribe dial *123*0# or SMS STOP to 5000";
        assert!(is_promotional_or_telco_offer(text, Some("Robi")));
    }

    #[test]
    fn rejects_congratulations_marketing() {
        let text =
            "Congratulations! You've won a chance to get 1GB free. Dial *121*99# to claim now!";
        assert!(is_promotional_or_telco_offer(text, Some("GP")));
    }

    #[test]
    fn rejects_reward_points_notice() {
        let text = "You have earned 50 reward points on your recent transaction. Check your points balance on the app.";
        assert!(is_promotional_or_telco_offer(text, Some("BRAC-BANK")));
    }

    #[test]
    fn rejects_balance_check_response() {
        let text = "Your balance is Tk 102.50. Last recharge Tk 50.00 on 01/09/2026.";
        assert!(is_promotional_or_telco_offer(text, Some("GP")));
    }

    #[test]
    fn allows_confirmed_recharge_even_with_enjoy_keyword() {
        let text = "Recharge of Tk 100.00 on 017XXXXXXXX successful. Enjoy calling at reduced rates. TrxID: GP987654321";
        assert!(!is_promotional_or_telco_offer(text, Some("GP")));
    }
}

use centwise_domain::TransactionType;
use centwise_parser::{
    classify::is_likely_financial_review, parse_sms, ParseOutcome, RejectReason,
};

#[test]
fn test_bangla_gatekeeper_blocks_all_bengali_sms() {
    let bangla_sms_samples = [
        (
            "bKash",
            "আপনার বিকাশ একাউন্ট থেকে ৫০ টাকা রিচার্জ সফল হয়েছে। TrxID 8J7A6K9L",
        ),
        (
            "NAGAD",
            "নগদ একাউন্টে ১,০০০ টাকা ক্যাশ ইন সফল হয়েছে। TxnID: 99KL22OP",
        ),
        ("City Bank", "আপনার একাউন্ট থেকে ১০,০০০ টাকা ডেবিট করা হয়েছে।"),
        ("GP", "আপনার একাউন্টে ৫০ টাকা রিচার্জ সফল হয়েছে। ব্যালেন্স ৫২.৪০ টাকা।"),
    ];

    for (sender, body) in bangla_sms_samples {
        let outcome = parse_sms(body, Some(sender));
        assert_eq!(
            outcome,
            ParseOutcome::Rejected(RejectReason::PromotionOrSpam),
            "Bangla SMS must be rejected by Bangla Gatekeeper"
        );
        assert!(
            !is_likely_financial_review(body, Some(sender)),
            "Bangla SMS must never enter the Review Queue"
        );
    }
}

#[test]
fn test_dbbl_nexuspay_eft_credit() {
    let sms = "Dear Sir, your A/C ***5543 credited(EFT by: BIPOULHOSSAIN adn: FRtk160,00000,gov) by Tk1,64,000.00 on 21-08-2022 01:07:46 PM C/B Tk1,64,528.94. NexusPay https";
    let tx = parse_sms(sms, Some("NexusPay"))
        .transaction()
        .unwrap()
        .clone();
    assert_eq!(tx.amount_minor, 16400000); // 1,64,000.00 Taka, NOT 16,000,000!
    assert_eq!(tx.balance_after_minor, Some(16452894)); // C/B Tk 1,64,528.94
    assert_eq!(tx.transaction_type, TransactionType::Income);
    assert_eq!(tx.account_last4.as_deref(), Some("5543"));
    assert_eq!(tx.party.as_deref(), Some("BIPOULHOSSAIN"));
}

#[test]
fn test_grameenphone_all_scenarios() {
    // 1. Data pack with USSD dial check
    let tx1 = parse_sms("You have successfully purchased 1.5GB Internet at Tk 43.00 (incl. VAT). Validity: 3 Days. Dial *121*1# to check balance.", Some("GP")).transaction().unwrap().clone();
    assert_eq!(tx1.amount_minor, 4300);
    assert_eq!(tx1.transaction_type, TransactionType::Expense);

    let tx2 = parse_sms(
        "You have purchased 30GB Internet at Tk 498.00. Validity: 30 Days. Dial *121*1#.",
        Some("GP"),
    )
    .transaction()
    .unwrap()
    .clone();
    assert_eq!(tx2.amount_minor, 49800);
    assert_eq!(tx2.transaction_type, TransactionType::Expense);

    // 2. Voice pack
    let tx3 = parse_sms("You have bought 50 Mins at Tk 34.00. Validity till 15/09/2026 23:59. Balance check dial *121*1*2#.", Some("GP")).transaction().unwrap().clone();
    assert_eq!(tx3.amount_minor, 3400);
    assert_eq!(tx3.transaction_type, TransactionType::Expense);

    // 3. Combo pack
    let tx4 = parse_sms(
        "You have purchased 5GB + 100 Mins combo pack at Tk 149.00. Validity 7 Days.",
        Some("GP"),
    )
    .transaction()
    .unwrap()
    .clone();
    assert_eq!(tx4.amount_minor, 14900);
    assert_eq!(tx4.transaction_type, TransactionType::Expense);

    // 4. Recharge
    let tx5 = parse_sms("Recharge of Tk 50.00 is successful. Current balance is Tk 52.40. Validity: 12-Oct-2026. Dial *566# for balance.", Some("GP")).transaction().unwrap().clone();
    assert_eq!(tx5.amount_minor, 5000);
    assert_eq!(tx5.balance_after_minor, Some(5240));

    // 5. Emergency Balance disbursal
    let tx6 = parse_sms("You have received Tk 25.00 as Emergency Balance. Current balance Tk 25.10. Dial *566*28# to check.", Some("GP")).transaction().unwrap().clone();
    assert_eq!(tx6.amount_minor, 2500);
    assert_eq!(tx6.transaction_type, TransactionType::Income);
    assert_eq!(tx6.balance_after_minor, Some(2510));

    // 6. Emergency Balance repayment
    let tx7 = parse_sms("Tk 25.00 has been deducted from your recharge for previous Emergency Balance. Remaining balance Tk 25.00.", Some("GP")).transaction().unwrap().clone();
    assert_eq!(tx7.amount_minor, 2500);
    assert_eq!(tx7.transaction_type, TransactionType::Expense);
    assert_eq!(tx7.balance_after_minor, Some(2500));
    assert_ne!(
        tx7.party.as_deref(),
        Some("your recharge for previous Emergency Balance")
    );

    // 7. VAS Deductions
    let tx8 = parse_sms("Tk 2.44 (incl. VAT) has been deducted for Welcome Tune subscription. Next renewal: 14-Sep-2026.", Some("GP")).transaction().unwrap().clone();
    assert_eq!(tx8.amount_minor, 244);
    assert_eq!(tx8.transaction_type, TransactionType::Expense);

    let tx9 = parse_sms(
        "Tk 12.18 has been deducted for Missed Call Alert monthly renewal.",
        Some("GP"),
    )
    .transaction()
    .unwrap()
    .clone();
    assert_eq!(tx9.amount_minor, 1218);
    assert_eq!(tx9.transaction_type, TransactionType::Expense);

    // 8. Balance Transfer
    let tx10 = parse_sms("You have transferred Tk 50.00 to 01711XXXXXX. Transfer fee Tk 2.44. Current balance Tk 120.00.", Some("GP")).transaction().unwrap().clone();
    assert_eq!(tx10.amount_minor, 5000);
    assert_eq!(tx10.transaction_type, TransactionType::Transfer);
    assert_eq!(tx10.fee_minor, Some(244));
    assert_eq!(tx10.balance_after_minor, Some(12000));
    assert_eq!(tx10.party.as_deref(), Some("01711XXXXXX"));

    // 10. Account recharge
    let tx11 = parse_sms(
        "Your account has been recharged with Tk 100.00. New balance is Tk 103.50.",
        Some("GP"),
    )
    .transaction()
    .unwrap()
    .clone();
    assert_eq!(tx11.amount_minor, 10000);
    assert_eq!(tx11.balance_after_minor, Some(10350));

    // 11. Emergency balance service fee
    let tx12 = parse_sms(
        "Tk 2.44 has been deducted as Emergency Balance service fee.",
        Some("GP"),
    )
    .transaction()
    .unwrap()
    .clone();
    assert_eq!(tx12.amount_minor, 244);
    assert_eq!(tx12.transaction_type, TransactionType::Expense);

    // 12. Transfer in
    let tx13 = parse_sms(
        "You have received Tk 50.00 from 01712XXXXXX. Current balance Tk 75.00.",
        Some("GP"),
    )
    .transaction()
    .unwrap()
    .clone();
    assert_eq!(tx13.amount_minor, 5000);
    assert_eq!(tx13.transaction_type, TransactionType::Income);
    assert_eq!(tx13.balance_after_minor, Some(7500));
    assert_eq!(tx13.party.as_deref(), Some("01712XXXXXX"));

    // 13. Postpaid bill payment received
    let tx14 = parse_sms("Payment of Tk 1,250.00 received for Postpaid Mobile No 017XXXXXXXX. Outstanding balance Tk 0.00.", Some("GP")).transaction().unwrap().clone();
    assert_eq!(tx14.amount_minor, 125000);
    assert_eq!(tx14.balance_after_minor, Some(0));

    // 14. Promo Offer rejection
    let promo = parse_sms(
        "Special Offer! Recharge Tk 48 and get 2GB for 3 days. Dial *121*5050# now.",
        Some("GP"),
    );
    assert_eq!(promo, ParseOutcome::Rejected(RejectReason::PromotionOrSpam));
}

#[test]
fn test_banglalink_all_scenarios() {
    let tx1 = parse_sms("Pack purchase successful! 1GB at Tk 40.00 valid till 12/09/2026. Dial *121*1# for remaining volume.", Some("Banglalink")).transaction().unwrap().clone();
    assert_eq!(tx1.amount_minor, 4000);
    assert_eq!(tx1.transaction_type, TransactionType::Expense);

    let tx2 = parse_sms(
        "You have successfully activated 10GB Data Pack at Tk 199.00 for 7 Days.",
        Some("Banglalink"),
    )
    .transaction()
    .unwrap()
    .clone();
    assert_eq!(tx2.amount_minor, 19900);
    assert_eq!(tx2.transaction_type, TransactionType::Expense);

    let tx3 = parse_sms(
        "You have bought 75 Minutes at Tk 53.00. Validity: 7 Days.",
        Some("Banglalink"),
    )
    .transaction()
    .unwrap()
    .clone();
    assert_eq!(tx3.amount_minor, 5300);
    assert_eq!(tx3.transaction_type, TransactionType::Expense);

    let tx4 = parse_sms(
        "Emergency Balance of Tk 30.00 received. Tk 30 will be adjusted on next recharge.",
        Some("Banglalink"),
    )
    .transaction()
    .unwrap()
    .clone();
    assert_eq!(tx4.amount_minor, 3000);
    assert_eq!(tx4.transaction_type, TransactionType::Income);

    let tx5 = parse_sms("Tk 30.00 has been deducted from your recharge for Emergency Balance. Main balance Tk 70.00.", Some("Banglalink")).transaction().unwrap().clone();
    assert_eq!(tx5.amount_minor, 3000);
    assert_eq!(tx5.transaction_type, TransactionType::Expense);
    assert_eq!(tx5.balance_after_minor, Some(7000));

    let tx6 = parse_sms(
        "Tk 2.44 deducted for Amar Tune service daily renewal.",
        Some("Banglalink"),
    )
    .transaction()
    .unwrap()
    .clone();
    assert_eq!(tx6.amount_minor, 244);
    assert_eq!(tx6.transaction_type, TransactionType::Expense);

    let tx7 = parse_sms("Your recharge of Tk. 100.00 is successful. New main balance is Tk. 104.25. Validity: 20-Nov-2026.", Some("Banglalink")).transaction().unwrap().clone();
    assert_eq!(tx7.amount_minor, 10000);
    assert_eq!(tx7.balance_after_minor, Some(10425));

    let tx8 = parse_sms("You have successfully transferred Tk 50.00 to 019XXXXXXXX. Service charge Tk 2.00. Balance Tk 48.00.", Some("Banglalink")).transaction().unwrap().clone();
    assert_eq!(tx8.amount_minor, 5000);
    assert_eq!(tx8.transaction_type, TransactionType::Transfer);
    assert_eq!(tx8.fee_minor, Some(200));
    assert_eq!(tx8.balance_after_minor, Some(4800));

    let tx9 = parse_sms(
        "You have received Tk 50.00 from 019YYYYYYYY. Main balance is Tk 65.50.",
        Some("Banglalink"),
    )
    .transaction()
    .unwrap()
    .clone();
    assert_eq!(tx9.amount_minor, 5000);
    assert_eq!(tx9.transaction_type, TransactionType::Income);
    assert_eq!(tx9.balance_after_minor, Some(6550));

    let promo = parse_sms(
        "Chomok Offer! 3GB @ only Tk 69 for 7 days! To activate dial *121*69# or visit MyBL app.",
        Some("Banglalink"),
    );
    assert_eq!(promo, ParseOutcome::Rejected(RejectReason::PromotionOrSpam));
}

#[test]
fn test_robi_all_scenarios() {
    let tx1 = parse_sms(
        "Successfully purchased 2GB 7Days at Tk 54.00. Balance: Tk 12.30. Check balance dial *3#.",
        Some("Robi"),
    )
    .transaction()
    .unwrap()
    .clone();
    assert_eq!(tx1.amount_minor, 5400);
    assert_eq!(tx1.transaction_type, TransactionType::Expense);
    assert_eq!(tx1.balance_after_minor, Some(1230));

    let tx2 = parse_sms(
        "You have activated 20GB Internet Pack at Tk 399.00 for 30 Days.",
        Some("Robi"),
    )
    .transaction()
    .unwrap()
    .clone();
    assert_eq!(tx2.amount_minor, 39900);
    assert_eq!(tx2.transaction_type, TransactionType::Expense);

    let tx3 = parse_sms(
        "Purchased 60 Min bundle at Tk 39.00. Valid till 14-Sep-2026. Balance check *222*2#.",
        Some("Robi"),
    )
    .transaction()
    .unwrap()
    .clone();
    assert_eq!(tx3.amount_minor, 3900);
    assert_eq!(tx3.transaction_type, TransactionType::Expense);
    assert_ne!(tx3.party.as_deref(), Some("Tk 39"));

    let tx4 = parse_sms(
        "Recharge of Tk 50.00 is successful. Current Balance: Tk 55.40, Valid till 25/10/2026.",
        Some("Robi"),
    )
    .transaction()
    .unwrap()
    .clone();
    assert_eq!(tx4.amount_minor, 5000);
    assert_eq!(tx4.balance_after_minor, Some(5540));

    // Jhotpot balance disbursal must NOT trigger OTP
    let tx5 = parse_sms("You have received Tk 20.00 Jhotpot balance. Outstanding loan Tk 20.00. Dial *222*16# to check.", Some("Robi")).transaction().unwrap().clone();
    assert_eq!(tx5.amount_minor, 2000);
    assert_eq!(tx5.transaction_type, TransactionType::Income);

    let tx6 = parse_sms("Tk 20.00 has been deducted from your recharge against Jhotpot balance. Remaining balance Tk 30.00.", Some("Robi")).transaction().unwrap().clone();
    assert_eq!(tx6.amount_minor, 2000);
    assert_eq!(tx6.transaction_type, TransactionType::Expense);
    assert_eq!(tx6.balance_after_minor, Some(3000));

    let tx7 = parse_sms(
        "Tk 2.44 has been deducted for GoonGoon service. Next renewal date 10/09/2026.",
        Some("Robi"),
    )
    .transaction()
    .unwrap()
    .clone();
    assert_eq!(tx7.amount_minor, 244);
    assert_eq!(tx7.transaction_type, TransactionType::Expense);

    let tx8 = parse_sms(
        "You have transferred Tk 40.00 to 018XXXXXXXX. Fee Tk 2.00. Current balance Tk 80.00.",
        Some("Robi"),
    )
    .transaction()
    .unwrap()
    .clone();
    assert_eq!(tx8.amount_minor, 4000);
    assert_eq!(tx8.transaction_type, TransactionType::Transfer);
    assert_eq!(tx8.fee_minor, Some(200));
    assert_eq!(tx8.balance_after_minor, Some(8000));

    let promo = parse_sms(
        "Super Offer! Recharge Tk 104 and get 5GB internet for 7 days. Dial *4*104#.",
        Some("Robi"),
    );
    assert_eq!(promo, ParseOutcome::Rejected(RejectReason::PromotionOrSpam));
}

#[test]
fn test_airtel_all_scenarios() {
    let tx1 = parse_sms("You have successfully bought 1GB at Tk 36.00 (Valid for 3 Days). Main balance is Tk 14.50.", Some("Airtel")).transaction().unwrap().clone();
    assert_eq!(tx1.amount_minor, 3600);
    assert_eq!(tx1.transaction_type, TransactionType::Expense);

    let tx2 = parse_sms(
        "Bought 45 Minutes at Tk 29.00. Validity: 3 Days. Dial *778*2#.",
        Some("Airtel"),
    )
    .transaction()
    .unwrap()
    .clone();
    assert_eq!(tx2.amount_minor, 2900);
    assert_eq!(tx2.transaction_type, TransactionType::Expense);

    let tx3 = parse_sms(
        "Recharge successful! Tk 100.00 added to your account. Current balance Tk 102.10.",
        Some("Airtel"),
    )
    .transaction()
    .unwrap()
    .clone();
    assert_eq!(tx3.amount_minor, 10000);
    assert_eq!(tx3.balance_after_minor, Some(10210));

    let tx4 = parse_sms(
        "Tk 20.00 emergency loan has been credited to your account. Service fee Tk 2.44.",
        Some("Airtel"),
    )
    .transaction()
    .unwrap()
    .clone();
    assert_eq!(tx4.amount_minor, 2000);
    assert_eq!(tx4.transaction_type, TransactionType::Income);
    assert_eq!(tx4.fee_minor, Some(244));

    let tx5 = parse_sms("Tk 22.44 has been recovered for emergency loan from your recharge. Current balance Tk 27.56.", Some("Airtel")).transaction().unwrap().clone();
    assert_eq!(tx5.amount_minor, 2244);
    assert_eq!(tx5.transaction_type, TransactionType::Expense);
    assert_eq!(tx5.balance_after_minor, Some(2756));

    let promo = parse_sms(
        "Hot Offer! Dial *123*034# to enjoy 2GB at Tk 34 for 3 days.",
        Some("Airtel"),
    );
    assert_eq!(promo, ParseOutcome::Rejected(RejectReason::PromotionOrSpam));
}

#[test]
fn test_bkash_send_money_transfer_and_savings() {
    // 1. Send Money with "You have sent"
    let tx1 = parse_sms(
        "You have sent Tk 500.00 to 017XXXXXXXX. Fee Tk 5.00. Balance Tk 1,250.00. TrxID 8J7A6K9L.",
        Some("bKash"),
    )
    .transaction()
    .unwrap()
    .clone();
    assert_eq!(tx1.amount_minor, 50000);
    assert_eq!(tx1.transaction_type, TransactionType::Expense);
    assert_eq!(tx1.fee_minor, Some(500));
    assert_eq!(tx1.balance_after_minor, Some(125000));
    assert_eq!(tx1.reference.as_deref(), Some("8J7A6K9L"));
    assert_eq!(tx1.party.as_deref(), Some("017XXXXXXXX"));

    // 2. Transfer Money to Bank
    let tx2 = parse_sms("Transfer Money Tk 10,000.00 to BRAC Bank A/C ...001 successful. Fee Tk 100.00. Balance Tk 1,450.00. TrxID 7Y8X9W0V.", Some("bKash")).transaction().unwrap().clone();
    assert_eq!(tx2.amount_minor, 1000000);
    assert_eq!(tx2.transaction_type, TransactionType::Transfer);
    assert_eq!(tx2.fee_minor, Some(10000));
    assert_eq!(tx2.balance_after_minor, Some(145000));
    assert_eq!(tx2.reference.as_deref(), Some("7Y8X9W0V"));

    // 3. Pay Bill (inverted verb order)
    let tx3 = parse_sms("Pay Bill Tk 1,420.00 to DESCO successful. Bill No 12345678. Fee Tk 0.00. Balance Tk 3,100.00. TrxID 2P3Q4R5S.", Some("bKash")).transaction().unwrap().clone();
    assert_eq!(tx3.amount_minor, 142000);
    assert_eq!(tx3.transaction_type, TransactionType::Expense);
    assert_eq!(tx3.merchant.as_deref(), Some("DESCO"));
    assert_eq!(tx3.reference.as_deref(), Some("2P3Q4R5S"));

    // 4. Savings Auto-Debit must be Expense
    let tx4 = parse_sms("Tk 1,000.00 deducted for monthly deposit in IDLC Savings Scheme via bKash. Balance Tk 4,200.00. TrxID 6T7U8V9W.", Some("bKash")).transaction().unwrap().clone();
    assert_eq!(tx4.amount_minor, 100000);
    assert_eq!(tx4.transaction_type, TransactionType::Expense);
    assert_eq!(tx4.balance_after_minor, Some(420000));
    assert_eq!(tx4.reference.as_deref(), Some("6T7U8V9W"));

    // 5. ATM Cash Out party is clean "ATM"
    let tx5 = parse_sms("Cash Out from ATM Tk 2,000.00 successful. Fee Tk 30.00. Balance Tk 4,500.00. TrxID 1A2B3C4D.", Some("bKash")).transaction().unwrap().clone();
    assert_eq!(tx5.party.as_deref(), Some("ATM"));
}

#[test]
fn test_nagad_clean_parties() {
    let tx1 = parse_sms(
        "Cash In from 017XXXXXXXX Tk 1,000.00 successful. Balance: Tk 2,200.00. TxnID: 99KL22OP.",
        Some("NAGAD"),
    )
    .transaction()
    .unwrap()
    .clone();
    assert_eq!(tx1.amount_minor, 100000);
    assert_eq!(tx1.party.as_deref(), Some("017XXXXXXXX"));

    let tx2 = parse_sms(
        "Add Money from Bank Tk 2,000.00 successful. Balance: Tk 4,200.00. TxnID: 55AA66BB.",
        Some("NAGAD"),
    )
    .transaction()
    .unwrap()
    .clone();
    assert_eq!(tx2.amount_minor, 200000);
    assert_eq!(tx2.party.as_deref(), Some("Bank"));
}

#[test]
fn test_bank_debit_card_pos_done_with_card() {
    let tx = parse_sms("Txn of BDT 1,200.00 done with your Debit Card 4021****1234 at SHWAPNO on 07-SEP-26 12:15. Available Bal BDT 18,340.50.", Some("BRAC Bank")).transaction().unwrap().clone();
    assert_eq!(tx.amount_minor, 120000);
    assert_eq!(tx.transaction_type, TransactionType::Expense);
    assert_eq!(tx.merchant.as_deref(), Some("Shwapno"));
    assert_eq!(tx.account_last4.as_deref(), Some("1234"));
    assert_eq!(tx.balance_after_minor, Some(1834050));
}

#[test]
fn test_teletalk_all_scenarios() {
    // 1. Data pack
    let tx1 = parse_sms(
        "Data pack 1GB at Tk 23.00 activated successfully. Validity: 3 days. To check dial *152#.",
        Some("Teletalk"),
    )
    .transaction()
    .unwrap()
    .clone();
    assert_eq!(tx1.amount_minor, 2300);
    assert_eq!(tx1.transaction_type, TransactionType::Expense);

    // 2. Voice pack
    let tx2 = parse_sms(
        "50 Min bundle at Tk 28.00 activated. Valid till 12-Sep-2026.",
        Some("Teletalk"),
    )
    .transaction()
    .unwrap()
    .clone();
    assert_eq!(tx2.amount_minor, 2800);
    assert_eq!(tx2.transaction_type, TransactionType::Expense);

    // 3. Recharge
    let tx3 = parse_sms(
        "Recharge Tk 50.00 is successful. Your current balance is Tk 51.20, validity 30-10-2026.",
        Some("Teletalk"),
    )
    .transaction()
    .unwrap()
    .clone();
    assert_eq!(tx3.amount_minor, 5000);
    assert_eq!(tx3.balance_after_minor, Some(5120));

    // 4. Emergency Balance
    let tx4 = parse_sms(
        "You have received Emergency Balance of Tk 20.00. Fee Tk 0.00.",
        Some("Teletalk"),
    )
    .transaction()
    .unwrap()
    .clone();
    assert_eq!(tx4.amount_minor, 2000);
    assert_eq!(tx4.transaction_type, TransactionType::Income);
}

#[test]
fn test_all_commercial_banks_from_spec() {
    // City Bank Credit Card POS
    let cb_pos = parse_sms("Purchase of BDT 2,450.00 on your Credit Card ending 4321 at UNIMART DHAKA on 07-Sep-2026 14:32. Avail Limit BDT 75,550.00.", Some("City Bank")).transaction().unwrap().clone();
    assert_eq!(cb_pos.amount_minor, 245000);
    assert_eq!(cb_pos.transaction_type, TransactionType::Expense);
    assert_eq!(cb_pos.account_last4.as_deref(), Some("4321"));

    // City Bank Credit Card E-com
    let cb_ecom = parse_sms("Online purchase of BDT 1,150.00 on your Credit Card ending 4321 at CHALDAL.COM on 07-Sep-2026. Avail Limit BDT 74,400.00.", Some("City Bank")).transaction().unwrap().clone();
    assert_eq!(cb_ecom.amount_minor, 115000);
    assert_eq!(cb_ecom.transaction_type, TransactionType::Expense);

    // City Bank Citytouch A/C Debit
    let cb_debit = parse_sms("Your A/C 110***901 has been debited for BDT 10,000.00 on 07-Sep-2026 via Citytouch. Ref: FT260907. Avail Bal BDT 42,300.00.", Some("City Bank")).transaction().unwrap().clone();
    assert_eq!(cb_debit.amount_minor, 1000000);
    assert_eq!(cb_debit.transaction_type, TransactionType::Expense);
    assert_eq!(cb_debit.reference.as_deref(), Some("FT260907"));
    assert_eq!(cb_debit.balance_after_minor, Some(4230000));

    // City Bank NPSB A/C Credit
    let cb_credit = parse_sms("Your A/C 110***901 has been credited by BDT 15,000.00 on 07-Sep-2026 via NPSB. Avail Bal BDT 57,300.00.", Some("City Bank")).transaction().unwrap().clone();
    assert_eq!(cb_credit.amount_minor, 1500000);
    assert_eq!(cb_credit.transaction_type, TransactionType::Income);
    assert_eq!(cb_credit.balance_after_minor, Some(5730000));

    // BRAC Bank ATM Withdrawal
    let brac_atm = parse_sms("Cash withdrawal of BDT 5,000.00 from ATM using Card 4021****1234 on 07-SEP-26. Available Bal BDT 13,340.50.", Some("BRAC Bank")).transaction().unwrap().clone();
    assert_eq!(brac_atm.amount_minor, 500000);
    assert_eq!(brac_atm.transaction_type, TransactionType::Expense);
    assert_eq!(brac_atm.balance_after_minor, Some(1334050));

    // BRAC Bank Salary Credit
    let brac_sal = parse_sms("Your A/C 1501******0001 has been credited by BDT 65,000.00 on 01-SEP-26 by SALARY. Available Bal BDT 72,120.00.", Some("BRAC Bank")).transaction().unwrap().clone();
    assert_eq!(brac_sal.amount_minor, 6500000);
    assert_eq!(brac_sal.transaction_type, TransactionType::Income);
    assert_eq!(brac_sal.balance_after_minor, Some(7212000));

    // DBBL ATM Withdrawal
    let dbbl_atm = parse_sms("A/C 115.110.***** debited by ATM WDL BDT 5,000.00 on 07/09/2026 15:40 at DBBL ATM DHAKA. Avail Bal BDT 12,400.00.", Some("DBBL")).transaction().unwrap().clone();
    assert_eq!(dbbl_atm.amount_minor, 500000);
    assert_eq!(dbbl_atm.transaction_type, TransactionType::Expense);
    assert_eq!(dbbl_atm.balance_after_minor, Some(1240000));

    // DBBL POS Purchase
    let dbbl_pos = parse_sms("A/C 115.110.***** debited by POS purchase BDT 1,850.00 at AGORA on 07/09/2026. Avail Bal BDT 10,550.00.", Some("DBBL")).transaction().unwrap().clone();
    assert_eq!(dbbl_pos.amount_minor, 185000);
    assert_eq!(dbbl_pos.transaction_type, TransactionType::Expense);
    assert_eq!(dbbl_pos.balance_after_minor, Some(1055000));

    // DBBL A/C Credit
    let dbbl_cr = parse_sms(
        "A/C 115.110.***** credited by BDT 15,000.00 on 07/09/2026. Avail Bal BDT 25,550.00.",
        Some("DBBL"),
    )
    .transaction()
    .unwrap()
    .clone();
    assert_eq!(dbbl_cr.amount_minor, 1500000);
    assert_eq!(dbbl_cr.transaction_type, TransactionType::Income);
    assert_eq!(dbbl_cr.balance_after_minor, Some(2555000));

    // IBBL Cellfin Transfer
    let cellfin = parse_sms("Cellfin: Tk 2,500.00 has been transferred from your A/C 2050***123 to 017XXXXXXXX. Fee Tk 0.00. Balance Tk 8,900.00. Trx ID: CF2609071234.", Some("IBBL")).transaction().unwrap().clone();
    assert_eq!(cellfin.amount_minor, 250000);
    assert_eq!(cellfin.transaction_type, TransactionType::Transfer);
    assert_eq!(cellfin.balance_after_minor, Some(890000));
    assert_eq!(cellfin.reference.as_deref(), Some("CF2609071234"));

    // IBBL Inward Remittance
    let ibbl_remit = parse_sms("Your A/C 2050***123 is credited with Tk 25,000.00 on 07-09-2026 by INWARD REMITTANCE. Available Balance Tk 33,900.00.", Some("IBBL")).transaction().unwrap().clone();
    assert_eq!(ibbl_remit.amount_minor, 2500000);
    assert_eq!(ibbl_remit.transaction_type, TransactionType::Income);
    assert_eq!(ibbl_remit.balance_after_minor, Some(3390000));
}

#[test]
fn test_all_remaining_mfs_scenarios_from_spec() {
    // bKash Cash Out Agent
    let bk_co = parse_sms("Cash Out Tk 1,000.00 to 018XXXXXXXX successful. Fee Tk 18.50. Balance Tk 2,300.00. TrxID 9K8L7M6N.", Some("bKash")).transaction().unwrap().clone();
    assert_eq!(bk_co.amount_minor, 100000);
    assert_eq!(bk_co.transaction_type, TransactionType::Expense);
    assert_eq!(bk_co.fee_minor, Some(1850));
    assert_eq!(bk_co.balance_after_minor, Some(230000));
    assert_eq!(bk_co.reference.as_deref(), Some("9K8L7M6N"));

    // bKash POS Payment
    let bk_pay = parse_sms("Payment Tk 1,200.00 to Shwapno successful. Fee Tk 0.00. Balance Tk 500.00. TrxID 7H6G5F4E.", Some("bKash")).transaction().unwrap().clone();
    assert_eq!(bk_pay.amount_minor, 120000);
    assert_eq!(bk_pay.transaction_type, TransactionType::Expense);
    assert_eq!(bk_pay.merchant.as_deref(), Some("Shwapno"));
    assert_eq!(bk_pay.reference.as_deref(), Some("7H6G5F4E"));

    // bKash E-commerce Payment
    let bk_ecom = parse_sms("Payment Tk 2,450.00 to Daraz Bangladesh successful. Fee Tk 0.00. Balance Tk 1,120.00. TrxID 5D6E7F8G.", Some("bKash")).transaction().unwrap().clone();
    assert_eq!(bk_ecom.amount_minor, 245000);
    assert_eq!(bk_ecom.transaction_type, TransactionType::Expense);
    assert_eq!(bk_ecom.merchant.as_deref(), Some("Daraz"));

    // bKash Mobile Recharge
    let bk_rech = parse_sms("Recharge Tk 40.00 to 017XXXXXXXX successful. Fee Tk 0.00. Balance Tk 1,100.00. TrxID 3F2E1D0C.", Some("bKash")).transaction().unwrap().clone();
    assert_eq!(bk_rech.amount_minor, 4000);
    assert_eq!(bk_rech.transaction_type, TransactionType::Expense);

    // bKash Cash In
    let bk_ci = parse_sms("You have received Tk 2,000.00 from 019XXXXXXXX. Fee Tk 0.00. Balance Tk 3,500.00. TrxID 6G5F4E3D.", Some("bKash")).transaction().unwrap().clone();
    assert_eq!(bk_ci.amount_minor, 200000);
    assert_eq!(bk_ci.transaction_type, TransactionType::Income);
    assert_eq!(bk_ci.party.as_deref(), Some("019XXXXXXXX"));

    // bKash Add Money from Bank
    let bk_add_bank = parse_sms("Add Money Tk 5,000.00 from City Bank Account successful. Fee Tk 0.00. Balance Tk 8,500.00. TrxID 5E4D3C2B.", Some("bKash")).transaction().unwrap().clone();
    assert_eq!(bk_add_bank.amount_minor, 500000);
    assert_eq!(bk_add_bank.transaction_type, TransactionType::Income);

    // bKash Add Money from Card
    let bk_add_card = parse_sms("Add Money Tk 3,000.00 from Visa Card ending 1234 successful. Fee Tk 0.00. Balance Tk 6,200.00. TrxID 4C3B2A1Z.", Some("bKash")).transaction().unwrap().clone();
    assert_eq!(bk_add_card.amount_minor, 300000);
    assert_eq!(bk_add_card.transaction_type, TransactionType::Income);
    assert_eq!(bk_add_card.account_last4.as_deref(), Some("1234"));

    // bKash Foreign Remittance
    let bk_remit = parse_sms("You have received Remittance of Tk 25,000.00 from Western Union. Govt. incentive Tk 625.00 added. Balance Tk 27,800.00. TrxID 8M7N6P5Q.", Some("bKash")).transaction().unwrap().clone();
    assert_eq!(bk_remit.amount_minor, 2500000);
    assert_eq!(bk_remit.transaction_type, TransactionType::Income);

    // bKash Cashback
    let bk_cashback = parse_sms(
        "Cashback Tk 50.00 received for Payment. Balance Tk 2,150.00. TrxID 9W8V7U6T.",
        Some("bKash"),
    )
    .transaction()
    .unwrap()
    .clone();
    assert_eq!(bk_cashback.amount_minor, 5000);
    assert_eq!(bk_cashback.transaction_type, TransactionType::Income);

    // Nagad Cash Out
    let ng_co = parse_sms("Cash Out Tk 500.00 to 016XXXXXXXX successful. Fee: Tk 7.50. Balance: Tk 1,200.00. TxnID: 72JH89KA.", Some("NAGAD")).transaction().unwrap().clone();
    assert_eq!(ng_co.amount_minor, 50000);
    assert_eq!(ng_co.transaction_type, TransactionType::Expense);
    assert_eq!(ng_co.fee_minor, Some(750));
    assert_eq!(ng_co.reference.as_deref(), Some("72JH89KA"));

    // Nagad Send Money
    let ng_sm = parse_sms("Send Money Tk 1,000.00 to 017XXXXXXXX successful. Fee: Tk 0.00. Balance: Tk 2,500.00. TxnID: 88PL99MN.", Some("NAGAD")).transaction().unwrap().clone();
    assert_eq!(ng_sm.amount_minor, 100000);
    assert_eq!(ng_sm.transaction_type, TransactionType::Expense);
    assert_eq!(ng_sm.reference.as_deref(), Some("88PL99MN"));

    // Nagad Merchant Payment
    let ng_pay = parse_sms(
        "Merchant Payment Tk 650.00 to Aarong successful. Balance: Tk 1,850.00. TxnID: 44KL33QR.",
        Some("NAGAD"),
    )
    .transaction()
    .unwrap()
    .clone();
    assert_eq!(ng_pay.amount_minor, 65000);
    assert_eq!(ng_pay.transaction_type, TransactionType::Expense);
    assert_eq!(ng_pay.merchant.as_deref(), Some("Aarong"));

    // Nagad Mobile Recharge
    let ng_rech = parse_sms("Mobile Recharge Tk 50.00 to 018XXXXXXXX successful. Balance: Tk 1,400.00. TxnID: 33MN22LK.", Some("NAGAD")).transaction().unwrap().clone();
    assert_eq!(ng_rech.amount_minor, 5000);
    assert_eq!(ng_rech.transaction_type, TransactionType::Expense);

    // Nagad Bill Payment
    let ng_bill = parse_sms(
        "Bill Payment of Tk 1,200.00 to DPDC successful. Balance: Tk 2,100.00. TxnID: 77CC88DD.",
        Some("NAGAD"),
    )
    .transaction()
    .unwrap()
    .clone();
    assert_eq!(ng_bill.amount_minor, 120000);
    assert_eq!(ng_bill.transaction_type, TransactionType::Expense);
    assert_eq!(ng_bill.merchant.as_deref(), Some("DPDC"));
}

#[test]
fn test_screenshot_issues_all_correctly_handled() {
    // Screenshot 1: Banglalink USSD bundle offer menu (Must be rejected, never -7 Tk expense)
    let screenshot_1 = "1) 13GB 30DAYS @208TK (Dial *212*712#)\n2) 15GB 30DAYS @208TK (Dial *212*717#)\n3) Limited Deal: 35GB(30D) @498TK; Dial *212*734#\n4) 1.5GB (7Days) @ 48TK ; Dial *212*911#\n5) 22min (3days) @TK15; Dial *212*502#\n6) 200Min (15 Days) @TK159 Recharge\n7) 1P/sec(+tax) rate for 90 days @TK304 Recharge\n8) 69P/Min(+tax) rate for 7 days @TK21 Ghechang Rechare\n9) 580min (30days) @TK350; Dial *212*350#\n10) 250Min (30 Days) @TK199 Recharge\n11) 45MIN (Meyad 2 Din)@TK29 Ghechang Recharge\n12) Recharge TK400 to get free 2GB -7D after 15th AprΓÇÖ24 (1 Time)\n13) 15min (2days) @TK10; Dial *212*501#\n14) 160min (15days) @TK100; Dial *212*500#\n15) 220min (30days) @TK140; Dial *212*514#\n16) 1P/sec(+tax) (10days) @ TK47 Recharge";
    assert_eq!(
        parse_sms(screenshot_1, Some("Banglalink")),
        ParseOutcome::Rejected(RejectReason::PromotionOrSpam)
    );
    assert!(!is_likely_financial_review(
        screenshot_1,
        Some("Banglalink")
    ));

    // Screenshot 2: E-commerce shopping ad (Must be rejected, never -1 Tk expense)
    let screenshot_2 = "1 TAKA-2 Products!!\nGet 1 Vaseline and 1 Makeup Remover Wipes at 1 Taka.\nMin Purchase: 399TAKA\nShop: www.TheMallBD.com";
    assert_eq!(
        parse_sms(screenshot_2, Some("TheMallBD")),
        ParseOutcome::Rejected(RejectReason::PromotionOrSpam)
    );
    assert!(!is_likely_financial_review(screenshot_2, Some("TheMallBD")));

    // Screenshot 3: WhatsApp recruitment scam (Must be rejected, never +23,600 salary income)
    let screenshot_3 = "Sir/Madam, BOSCH is recruiting Internet Marketing, salary is 23600 BDT. Please contact the staff https://wa.me/8801786806139";
    assert_eq!(
        parse_sms(screenshot_3, None),
        ParseOutcome::Rejected(RejectReason::PromotionOrSpam)
    );
    assert!(!is_likely_financial_review(screenshot_3, None));

    // Screenshot 4: Robi low balance loan offer (Must be rejected, never -15 Tk expense)
    let screenshot_4 = "Your balance is Tk.5 or less. Take balance loan of Tk 15 Fee: Tk 2.67. Tk 17.67 will be deducted from your next recharge. Dial *123*007# Now!";
    assert_eq!(
        parse_sms(screenshot_4, Some("Robi")),
        ParseOutcome::Rejected(RejectReason::PromotionOrSpam)
    );
    assert!(!is_likely_financial_review(screenshot_4, Some("Robi")));

    // Screenshot 5: bKash non-user money claim invite (Must be rejected as NotATransaction, never -200 Tk expense)
    let screenshot_5 = "Tk 200.00 has been sent from 01811552202. To receive the money, open Account from bKash App within 31/05/2024 11:11. Download App: https://bka.sh/smnewreg";
    assert_eq!(
        parse_sms(screenshot_5, Some("bKash")),
        ParseOutcome::Rejected(RejectReason::NotATransaction)
    );
    assert!(!is_likely_financial_review(screenshot_5, Some("bKash")));

    // Payment authorization OTP: Never enter review queue!
    let payment_otp = "Your OTP for bKash payment of Tk 500 is 123456. Do not share with anyone.";
    assert_eq!(
        parse_sms(payment_otp, Some("bKash")),
        ParseOutcome::Rejected(RejectReason::OtpOrSecurity)
    );
    assert!(!is_likely_financial_review(payment_otp, Some("bKash")));
}

#[test]
fn test_audit_fixes_real_user_sms() {
    // 1. Ryans Computers invoice: Bill number B-2242120 must not be parsed as amount!
    let ryans = "Thanks for shopping from Ryans. Your purchase bill no is B-2242120, Tk 1,000.\n\nClick to see your invoice copy: https://www.ryanscomputers.com/in-voice/ff985db19e3956359e71dd7f075efe7eysgte\n\nThanks\nRyans Computers Ltd.";
    let tx_ryans = parse_sms(ryans, None).transaction().unwrap().clone();
    assert_eq!(tx_ryans.amount_minor, 100_000); // 1,000.00 Taka, NOT 2,242,120!
    assert_eq!(tx_ryans.transaction_type, TransactionType::Expense);

    // 2. Rocket negative amount debit: Tk-500.00 must be parsed as 500.00, NOT closing balance!
    let rocket_neg = "Dear Sir, your A/C ***5543 debited (Fund Transfer) by Tk-500.00 on 02-02-2024 12:08:37 PM C/B Tk1,03,973.32. NexusPay https://bit.ly/nexuspay";
    let tx_rocket = parse_sms(rocket_neg, Some("Rocket"))
        .transaction()
        .unwrap()
        .clone();
    assert_eq!(tx_rocket.amount_minor, 50_000); // 500.00 Taka, NOT 1,03,973.32!
    assert_eq!(tx_rocket.balance_after_minor, Some(10_397_332));
    assert_eq!(tx_rocket.transaction_type, TransactionType::Transfer);

    // 3. bKash duplicate mobile recharge request notification must be rejected
    let bk_dup_notice = "Your bKash Mobile Recharge request of Tk 40.00 for 01615076000 was successful. Use bKash App for convenience & offers! TCA Download App: https://bKa.sh/5app";
    assert_eq!(
        parse_sms(bk_dup_notice, Some("bKash")),
        ParseOutcome::Rejected(RejectReason::NotATransaction)
    );

    // 4. bKash real recharge request receipt: Must be Expense, NOT Income!
    let bk_recharge_receipt = "Received Recharge request of Tk 20.00 for 01615076000. Fee Tk 0.00. Balance Tk 24.74. TrxID BF27UF7X1L at 02/06/2024 08:56. Wait for confirmation.";
    let tx_receipt = parse_sms(bk_recharge_receipt, Some("bKash"))
        .transaction()
        .unwrap()
        .clone();
    assert_eq!(tx_receipt.amount_minor, 2_000);
    assert_eq!(tx_receipt.transaction_type, TransactionType::Expense);
    assert_eq!(tx_receipt.reference.as_deref(), Some("BF27UF7X1L"));

    // 5. Cash Out with Cashback promo footer must be Expense, NOT Cashback Income!
    let bk_cashout_promo = "Cash Out Tk 4,000.00 to 01707376622 successful. Fee Tk 74.00. Balance Tk 4,108.82. TrxID 9DH2BR3JUW at 17/04/2022 19:18. Cashback 50 on 25,000 CashOut";
    let tx_cashout = parse_sms(bk_cashout_promo, Some("bKash"))
        .transaction()
        .unwrap()
        .clone();
    assert_eq!(tx_cashout.amount_minor, 400_000);
    assert_eq!(tx_cashout.transaction_type, TransactionType::Expense);
}

# Centwise: Bangladeshi Financial SMS Parser Specification & Update Plan

> **Source Document:** [Google Docs - Complete Bangladeshi Financial SMS Parser Engine Specification & Test Suite](https://docs.google.com/document/d/1okxLu6IibBLTGzBGm7-VEmnwq1ANVdcgXYoQ4T7QAfA/edit?tab=t.0#heading=h.ge821fdp5pxs)  
> **Target Module:** `core/centwise-parser` (Rust Core)  
> **Status:** Specification Ingested & Architectural Think-Check Complete  
> **Date:** September 2026  

---

## 1. Executive Summary & Context

The Google Document specifies a comprehensive Bangladeshi Financial SMS Parser Engine tailored for:
1. **Telecom Operators (Telcos):** Grameenphone (GP), Banglalink (BL), Robi, Airtel, and Teletalk.
2. **Mobile Financial Services (MFS):** bKash, Nagad, Rocket, and Upay.
3. **Traditional Commercial Banks:** City Bank, BRAC Bank, Dutch-Bangla Bank (DBBL), and Islami Bank Bangladesh (IBBL).
4. **Directionality Classification:** Eliminating debit/credit confusion across airtime recharges, pack purchases, VAS auto-deductions, emergency loans, and peer transfers.
5. **Spam/Marketing Gatekeeper:** Preventing promotional messages, USSD dial menus, and OTPs from entering parse pipelines or review queues.

---

## 2. Architectural "Think Check" & Deep Analysis

### 2.1 The Root Cause of Directionality (+/-) Confusion
In Bangladeshi SMS parsing, financial directionality depends on the **perspective of the account being tracked**:

| Context | Event / SMS Text | Raw Signal | Personal Finance Meaning | Telco Airtime Ledger Meaning |
|---|---|---|---|---|
| **MFS Wallet** | `Recharge Tk 40.00 to 017... successful` | Recharge | **Debit (-)** (Wallet spent funds) | N/A |
| **Telco SIM** | `Recharge of Tk 50.00 is successful. Current balance is Tk 52.40` | Recharge | **Credit (+)** to SIM balance *(or expense if cash paid)* | **Credit (+)** (Airtime increased) |
| **Telco SIM** | `You have successfully purchased 1.5GB Internet at Tk 43.00` | Purchased | **Debit (-)** (Airtime balance consumed) | **Debit (-)** |
| **Telco SIM** | `Tk 2.44 has been deducted for Welcome Tune subscription` | Deducted | **Debit (-)** (VAS micro-expense) | **Debit (-)** |
| **Telco SIM** | `You have received Tk 25.00 as Emergency Balance` | Received | **Credit (+)** (Emergency credit line added) | **Credit (+)** |
| **Telco SIM** | `Tk 25.00 has been deducted from your recharge for previous Emergency Balance` | Deducted | **Debit (-)** (Debt repayment) | **Debit (-)** |
| **Bank Account** | `Your A/C 110*** has been credited by BDT 15,000.00 via NPSB` | Credited | **Credit (+)** / Income / Transfer | **Credit (+)** |
| **Bank Account** | `Txn of BDT 1,200.00 done with your Debit Card at SHWAPNO` | Done / Spent | **Debit (-)** / Expense | **Debit (-)** |

#### Centwise Alignment:
- In `centwise-domain`, transactions are classified as `Expense`, `Income`, `Transfer`, or `Refund`.
- For MFS and Banks, the mapping is clear: debits are `Expense`, credits are `Income`, and own-account/interbank transfers are `Transfer`.
- For Telcos:
  - If Centwise tracks a **Telco Airtime Account**, an airtime recharge is a **Credit (+)** / Transfer In, while internet packs, voice bundles, and VAS auto-deductions are **Debit (-)** / Expense.
  - If Centwise does not treat Telco SIM as an independent balance account, airtime recharge represents an `Expense` from the user's pocket, while pack purchases from existing balance are internal balance deductions.
  - **Recommendation:** Implement a clean `Direction` abstraction in `centwise-parser` (`Debit`, `Credit`, `Neutral`, `Ignored`) and map it deterministically to `TransactionType` based on whether the provider is a Bank, MFS, or Telco.

---

### 2.2 VAS Auto-Deductions (Micro-charges)
- **Current Centwise State:** In `classify/telco.rs`, `is_vas_subscription_notice` historically classified Welcome Tune, Amar Tune, and GoonGoon messages as noise/promotions unless confirmed transaction text was found.
- **Specification Finding:** The document explicitly mandates tracking VAS deductions as valid **Debit (-)** expenses:
  - GP: `Tk 2.44 (incl. VAT) has been deducted for Welcome Tune subscription.`
  - GP: `Tk 12.18 has been deducted for Missed Call Alert monthly renewal.`
  - BL: `Tk 2.44 deducted for Amar Tune service daily renewal.`
  - Robi: `Tk 2.44 has been deducted for GoonGoon service.`
- **Action:** Update `classify/telco.rs` so that confirmed VAS deductions with `has been deducted` or `deducted for` are allowed through as legitimate micro-expenses rather than filtered as marketing notices.

---

### 2.3 Internet Data Packs & Minute Bundles
- **Current Centwise State:** Data and minute purchases are partially matched by generic expense regexes, but pack volume metadata (e.g., `1.5GB`, `50 Mins`, `Validity: 3 Days`) is not extracted into structured fields.
- **Specification Finding:** The doc specifies exact extraction for:
  - Data packs (`1.5GB`, `10GB`, `30GB`, `2GB 7Days`)
  - Voice bundles (`50 Mins`, `75 Minutes`, `60 Min bundle`)
  - Combo packs (`5GB + 100 Mins combo pack`)
- **Action:** Add helper extractors for bundle pack descriptions, placing them into the `merchant` or `party` metadata field (e.g., `Grameenphone (1.5GB Internet)`).

---

### 2.4 Spam & Promotion Rejection vs. Confirmed Transactions
The document highlights the critical need to drop marketing alerts before they reach entity extraction:
- `Special Offer! Recharge Tk 48 and get 2GB... Dial *121*5050#` -> **IGNORED**
- `Chomok Offer! 3GB @ only Tk 69...` -> **IGNORED**
- `Super Offer! Recharge Tk 104...` -> **IGNORED**
- `Hot Offer! Dial *123*034# to enjoy...` -> **IGNORED**

**Safety Rule:** A message with `Offer!`, `Chomok Offer`, `Super Offer`, or `Hot Offer` that contains a call-to-action (`Dial *...#`) must NEVER be treated as a transaction. Centwise already enforces this via `is_telco_bundle_menu` and `USSD_DIAL_RE`, which matches the document's prefilter intent.

---

## 3. Comprehensive Provider Specification Catalog

### 3.1 Telecom Operators

#### Grameenphone (Senders: `GP`, `Grameenphone`, `121`)
| Type | Direction | Raw SMS Template | Key Extraction Regex | Extracted Fields |
|---|---|---|---|---|
| **Data Pack** | `[DEBIT (-)]` | `You have successfully purchased 1.5GB Internet at Tk 43.00 (incl. VAT). Validity: 3 Days. Dial *121*1# to check balance.` | `purchased\s+([0-9.]+\s*(?:GB\|MB))\s+Internet\s+at\s+Tk\s+([0-9,.]+)` | Amount: 43.00, Pack: 1.5GB, Val: 3 Days |
| **Data Pack** | `[DEBIT (-)]` | `You have purchased 30GB Internet at Tk 498.00. Validity: 30 Days. Dial *121*1#.` | `purchased\s+([0-9.]+\s*GB)\s+Internet\s+at\s+Tk\s+([0-9,.]+)` | Amount: 498.00, Pack: 30GB, Val: 30 Days |
| **Voice Pack** | `[DEBIT (-)]` | `You have bought 50 Mins at Tk 34.00. Validity till 15/09/2026 23:59. Balance check dial *121*1*2#.` | `bought\s+([0-9]+\s*Mins)\s+at\s+Tk\s+([0-9,.]+)` | Amount: 34.00, Pack: 50 Mins |
| **Combo Pack** | `[DEBIT (-)]` | `You have purchased 5GB + 100 Mins combo pack at Tk 149.00. Validity 7 Days.` | `purchased\s+(.+?)\s+combo\s+pack\s+at\s+Tk\s+([0-9,.]+)` | Amount: 149.00, Pack: 5GB + 100 Mins |
| **Recharge** | `[CREDIT (+)]` | `Recharge of Tk 50.00 is successful. Current balance is Tk 52.40. Validity: 12-Oct-2026. Dial *566# for balance.` | `Recharge\s+of\s+Tk\s+([0-9,.]+)\s+is\s+successful` | Amount: 50.00, Bal: 52.40 |
| **Recharge** | `[CREDIT (+)]` | `Your account has been recharged with Tk 100.00. New balance is Tk 103.50.` | `recharged\s+with\s+Tk\s+([0-9,.]+)` | Amount: 100.00, Bal: 103.50 |
| **Emergency Loan** | `[CREDIT (+)]` | `You have received Tk 25.00 as Emergency Balance. Current balance Tk 25.10. Dial *566*28# to check.` | `received\s+Tk\s+([0-9,.]+)\s+as\s+Emergency\s+Balance` | Amount: 25.00, Bal: 25.10 |
| **Loan Repay** | `[DEBIT (-)]` | `Tk 25.00 has been deducted from your recharge for previous Emergency Balance. Remaining balance Tk 25.00.` | `Tk\s+([0-9,.]+)\s+has\s+been\s+deducted.*?Emergency\s+Balance` | Amount: 25.00, Bal: 25.00 |
| **Loan Fee** | `[DEBIT (-)]` | `Tk 2.44 has been deducted as Emergency Balance service fee.` | `Tk\s+([0-9,.]+)\s+has\s+been\s+deducted\s+as\s+Emergency\s+Balance\s+service\s+fee` | Amount: 2.44 |
| **VAS (Welcome Tune)** | `[DEBIT (-)]` | `Tk 2.44 (incl. VAT) has been deducted for Welcome Tune subscription. Next renewal: 14-Sep-2026.` | `Tk\s+([0-9,.]+).*?deducted\s+for\s+Welcome\s+Tune` | Amount: 2.44 |
| **VAS (Missed Call)** | `[DEBIT (-)]` | `Tk 12.18 has been deducted for Missed Call Alert monthly renewal.` | `Tk\s+([0-9,.]+)\s+has\s+been\s+deducted\s+for\s+Missed\s+Call\s+Alert` | Amount: 12.18 |
| **Transfer Out** | `[DEBIT (-)]` | `You have transferred Tk 50.00 to 01711XXXXXX. Transfer fee Tk 2.44. Current balance Tk 120.00.` | `transferred\s+Tk\s+([0-9,.]+)\s+to\s+(\d+)` | Amount: 50.00, Fee: 2.44, Bal: 120.00 |
| **Transfer In** | `[CREDIT (+)]` | `You have received Tk 50.00 from 01721XXXXXX. Current balance Tk 75.00.` | `received\s+Tk\s+([0-9,.]+)\s+from\s+(\d+)` | Amount: 50.00, Bal: 75.00 |
| **Postpaid Bill** | `[CREDIT (+)]` | `Payment of Tk 1,250.00 received for Postpaid Mobile No 017XXXXXXXX. Outstanding balance Tk 0.00.` | `Payment\s+of\s+Tk\s+([0-9,.]+)\s+received` | Amount: 1250.00, Bal: 0.00 |
| **Offer / Ad** | `[IGNORED]` | `Special Offer! Recharge Tk 48 and get 2GB for 3 days. Dial *121*5050# now.` | `Special\s+Offer!` | Rejected |

---

#### Banglalink (Senders: `Banglalink`, `BL`, `121`)
| Type | Direction | Raw SMS Template | Key Extraction Regex | Extracted Fields |
|---|---|---|---|---|
| **Data Pack** | `[DEBIT (-)]` | `Pack purchase successful! 1GB at Tk 40.00 valid till 12/09/2026. Dial *121*1# for remaining volume.` | `([0-9.]+\s*GB)\s+at\s+Tk\s+([0-9,.]+)` | Amount: 40.00, Pack: 1GB |
| **Data Pack** | `[DEBIT (-)]` | `You have successfully activated 10GB Data Pack at Tk 199.00 for 7 Days.` | `activated\s+([0-9.]+\s*GB)\s+Data\s+Pack\s+at\s+Tk\s+([0-9,.]+)` | Amount: 199.00, Pack: 10GB |
| **Voice Pack** | `[DEBIT (-)]` | `You have bought 75 Minutes at Tk 53.00. Validity: 7 Days.` | `bought\s+([0-9]+\s*Minutes)\s+at\s+Tk\s+([0-9,.]+)` | Amount: 53.00, Pack: 75 Mins |
| **Recharge** | `[CREDIT (+)]` | `Your recharge of Tk. 100.00 is successful. New main balance is Tk. 104.25. Validity: 20-Nov-2026.` | `recharge\s+of\s+Tk\.\s*([0-9,.]+)\s+is\s+successful` | Amount: 100.00, Bal: 104.25 |
| **Emergency Loan** | `[CREDIT (+)]` | `Emergency Balance of Tk 30.00 received. Tk 30 will be adjusted on next recharge.` | `Emergency\s+Balance\s+of\s+Tk\s+([0-9,.]+)\s+received` | Amount: 30.00 |
| **Loan Repay** | `[DEBIT (-)]` | `Tk 30.00 has been deducted from your recharge for Emergency Balance. Main balance Tk 70.00.` | `Tk\s+([0-9,.]+)\s+has\s+been\s+deducted.*?Emergency\s+Balance` | Amount: 30.00, Bal: 70.00 |
| **Transfer Out** | `[DEBIT (-)]` | `You have successfully transferred Tk 50.00 to 019XXXXXXXX. Service charge Tk 2.00. Balance Tk 48.00.` | `transferred\s+Tk\s+([0-9,.]+)\s+to\s+(\d+)` | Amount: 50.00, Fee: 2.00, Bal: 48.00 |
| **Transfer In** | `[CREDIT (+)]` | `You have received Tk 50.00 from 019YYYYYYYY. Main balance is Tk 65.50.` | `received\s+Tk\s+([0-9,.]+)\s+from\s+(\d+)` | Amount: 50.00, Bal: 65.50 |
| **VAS (Amar Tune)** | `[DEBIT (-)]` | `Tk 2.44 deducted for Amar Tune service daily renewal.` | `Tk\s+([0-9,.]+)\s+deducted\s+for\s+Amar\s+Tune` | Amount: 2.44 |
| **Offer / Ad** | `[IGNORED]` | `Chomok Offer! 3GB @ only Tk 69 for 7 days! To activate dial *121*69# or visit MyBL app.` | `Chomok\s+Offer!` | Rejected |

---

#### Robi (Senders: `Robi`, `123`)
| Type | Direction | Raw SMS Template | Key Extraction Regex | Extracted Fields |
|---|---|---|---|---|
| **Data Pack** | `[DEBIT (-)]` | `Successfully purchased 2GB 7Days at Tk 54.00. Balance: Tk 12.30. Check balance dial *3#.` | `purchased\s+([0-9.]+\s*GB.*?)\s+at\s+Tk\s+([0-9,.]+)` | Amount: 54.00, Pack: 2GB 7Days, Bal: 12.30 |
| **Data Pack** | `[DEBIT (-)]` | `You have activated 20GB Internet Pack at Tk 399.00 for 30 Days.` | `activated\s+([0-9.]+\s*GB)\s+Internet\s+Pack\s+at\s+Tk\s+([0-9,.]+)` | Amount: 399.00, Pack: 20GB |
| **Voice Pack** | `[DEBIT (-)]` | `Purchased 60 Min bundle at Tk 39.00. Valid till 14-Sep-2026. Balance check *222*2#.` | `Purchased\s+([0-9]+\s*Min)\s+bundle\s+at\s+Tk\s+([0-9,.]+)` | Amount: 39.00, Pack: 60 Min |
| **Recharge** | `[CREDIT (+)]` | `Recharge of Tk 50.00 is successful. Current Balance: Tk 55.40, Valid till 25/10/2026.` | `Recharge\s+of\s+Tk\s+([0-9,.]+)\s+is\s+successful` | Amount: 50.00, Bal: 55.40 |
| **Jhotpot Loan** | `[CREDIT (+)]` | `You have received Tk 20.00 Jhotpot balance. Outstanding loan Tk 20.00. Dial *222*16# to check.` | `received\s+Tk\s+([0-9,.]+)\s+Jhotpot\s+balance` | Amount: 20.00 |
| **Loan Repay** | `[DEBIT (-)]` | `Tk 20.00 has been deducted from your recharge against Jhotpot balance. Remaining balance Tk 30.00.` | `Tk\s+([0-9,.]+)\s+has\s+been\s+deducted.*?Jhotpot\s+balance` | Amount: 20.00, Bal: 30.00 |
| **VAS (GoonGoon)** | `[DEBIT (-)]` | `Tk 2.44 has been deducted for GoonGoon service. Next renewal date 10/09/2026.` | `Tk\s+([0-9,.]+)\s+has\s+been\s+deducted\s+for\s+GoonGoon` | Amount: 2.44 |
| **Transfer Out** | `[DEBIT (-)]` | `You have transferred Tk 40.00 to 018XXXXXXXX. Fee Tk 2.00. Current balance Tk 80.00.` | `transferred\s+Tk\s+([0-9,.]+)\s+to\s+(\d+)` | Amount: 40.00, Fee: 2.00, Bal: 80.00 |
| **Offer / Ad** | `[IGNORED]` | `Super Offer! Recharge Tk 104 and get 5GB internet for 7 days. Dial *4*104#.` | `Super\s+Offer!` | Rejected |

---

#### Airtel (Senders: `Airtel`, `121`)
| Type | Direction | Raw SMS Template | Key Extraction Regex | Extracted Fields |
|---|---|---|---|---|
| **Data Pack** | `[DEBIT (-)]` | `You have successfully bought 1GB at Tk 36.00 (Valid for 3 Days). Main balance is Tk 14.50.` | `bought\s+([0-9.]+\s*GB)\s+at\s+Tk\s+([0-9,.]+)` | Amount: 36.00, Pack: 1GB, Bal: 14.50 |
| **Voice Pack** | `[DEBIT (-)]` | `Bought 45 Minutes at Tk 29.00. Validity: 3 Days. Dial *778*2#.` | `Bought\s+([0-9]+\s*Minutes)\s+at\s+Tk\s+([0-9,.]+)` | Amount: 29.00, Pack: 45 Mins |
| **Recharge** | `[CREDIT (+)]` | `Recharge successful! Tk 100.00 added to your account. Current balance Tk 102.10.` | `Recharge\s+successful!\s+Tk\s+([0-9,.]+)\s+added` | Amount: 100.00, Bal: 102.10 |
| **Emergency Loan** | `[CREDIT (+)]` | `Tk 20.00 emergency loan has been credited to your account. Service fee Tk 2.44.` | `Tk\s+([0-9,.]+)\s+emergency\s+loan\s+has\s+been\s+credited` | Amount: 20.00, Fee: 2.44 |
| **Loan Repay** | `[DEBIT (-)]` | `Tk 22.44 has been recovered for emergency loan from your recharge. Current balance Tk 27.56.` | `Tk\s+([0-9,.]+)\s+has\s+been\s+recovered.*?emergency\s+loan` | Amount: 22.44, Bal: 27.56 |
| **Offer / Ad** | `[IGNORED]` | `Hot Offer! Dial *123*034# to enjoy 2GB at Tk 34 for 3 days.` | `Hot\s+Offer!` | Rejected |

---

#### Teletalk (Senders: `Teletalk`, `121`)
| Type | Direction | Raw SMS Template | Key Extraction Regex | Extracted Fields |
|---|---|---|---|---|
| **Data Pack** | `[DEBIT (-)]` | `Data pack 1GB at Tk 23.00 activated successfully. Validity: 3 days. To check dial *152#.` | `Data\s+pack\s+([0-9.]+\s*GB)\s+at\s+Tk\s+([0-9,.]+)` | Amount: 23.00, Pack: 1GB |
| **Voice Pack** | `[DEBIT (-)]` | `50 Min bundle at Tk 28.00 activated. Valid till 12-Sep-2026.` | `([0-9]+\s*Min)\s+bundle\s+at\s+Tk\s+([0-9,.]+)` | Amount: 28.00, Pack: 50 Min |
| **Recharge** | `[CREDIT (+)]` | `Recharge Tk 50.00 is successful. Your current balance is Tk 51.20, validity 30-10-2026.` | `Recharge\s+Tk\s+([0-9,.]+)\s+is\s+successful` | Amount: 50.00, Bal: 51.20 |
| **Emergency Loan** | `[CREDIT (+)]` | `You have received Emergency Balance of Tk 20.00. Fee Tk 0.00.` | `received\s+Emergency\s+Balance\s+of\s+Tk\s+([0-9,.]+)` | Amount: 20.00, Fee: 0.00 |

---

### 3.2 Mobile Financial Services (MFS)

#### bKash (Sender: `bKash`)
| Transaction Type | Direction | Raw SMS Template | Extracted Fields |
|---|---|---|---|
| **Send Money** | `[DEBIT (-)]` | `You have sent Tk 500.00 to 017XXXXXXXX. Fee Tk 5.00. Balance Tk 1,250.00. TrxID 8J7A6K9L.` | Amount: 500.00, Fee: 5.00, Bal: 1250.00, TrxID: 8J7A6K9L |
| **Send Money (Free)** | `[DEBIT (-)]` | `You have sent Tk 200.00 to 017XXXXXXXX. Fee Tk 0.00. Balance Tk 800.00. TrxID 9K8L7M6N.` | Amount: 200.00, Fee: 0.00, Bal: 800.00, TrxID: 9K8L7M6N |
| **Cash Out (Agent)** | `[DEBIT (-)]` | `Cash Out Tk 1,000.00 to 018XXXXXXXX successful. Fee Tk 18.50. Balance Tk 2,300.00. TrxID 9K8L7M6N.` | Amount: 1000.00, Fee: 18.50, Bal: 2300.00, TrxID: 9K8L7M6N |
| **Cash Out (ATM)** | `[DEBIT (-)]` | `Cash Out from ATM Tk 2,000.00 successful. Fee Tk 30.00. Balance Tk 4,500.00. TrxID 1A2B3C4D.` | Amount: 2000.00, Fee: 30.00, Bal: 4500.00, TrxID: 1A2B3C4D |
| **Merchant POS** | `[DEBIT (-)]` | `Payment Tk 1,200.00 to Shwapno successful. Fee Tk 0.00. Balance Tk 500.00. TrxID 7H6G5F4E.` | Amount: 1200.00, Merchant: Shwapno, Fee: 0.00, Bal: 500.00 |
| **E-commerce Pay** | `[DEBIT (-)]` | `Payment Tk 2,450.00 to Daraz Bangladesh successful. Fee Tk 0.00. Balance Tk 1,120.00. TrxID 5D6E7F8G.` | Amount: 2450.00, Merchant: Daraz Bangladesh, Bal: 1120.00 |
| **Mobile Recharge** | `[DEBIT (-)]` | `Recharge Tk 40.00 to 017XXXXXXXX successful. Fee Tk 0.00. Balance Tk 1,100.00. TrxID 3F2E1D0C.` | Amount: 40.00, Party: 017XXXXXXXX, Bal: 1100.00 |
| **Cash In (Agent)** | `[CREDIT (+)]` | `You have received Tk 2,000.00 from 019XXXXXXXX. Fee Tk 0.00. Balance Tk 3,500.00. TrxID 6G5F4E3D.` | Amount: 2000.00, Party: 019XXXXXXXX, Bal: 3500.00 |
| **Add Money (Bank)** | `[CREDIT (+)]` | `Add Money Tk 5,000.00 from City Bank Account successful. Fee Tk 0.00. Balance Tk 8,500.00. TrxID 5E4D3C2B.` | Amount: 5000.00, Source: City Bank Account, Bal: 8500.00 |
| **Add Money (Card)** | `[CREDIT (+)]` | `Add Money Tk 3,000.00 from Visa Card ending 1234 successful. Fee Tk 0.00. Balance Tk 6,200.00. TrxID 4C3B2A1Z.` | Amount: 3000.00, Source: Visa Card, Last4: 1234, Bal: 6200.00 |
| **Transfer to Bank** | `[DEBIT (-)]` | `Transfer Money Tk 10,000.00 to BRAC Bank A/C ...001 successful. Fee Tk 100.00. Balance Tk 1,450.00. TrxID 7Y8X9W0V.` | Amount: 10000.00, Fee: 100.00, Dest: BRAC Bank, Bal: 1450.00 |
| **Foreign Remittance** | `[CREDIT (+)]` | `You have received Remittance of Tk 25,000.00 from Western Union. Govt. incentive Tk 625.00 added. Balance Tk 27,800.00. TrxID 8M7N6P5Q.` | Amount: 25000.00, Source: Western Union, Incentive: 625.00, Bal: 27800.00 |
| **Pay Bill** | `[DEBIT (-)]` | `Pay Bill Tk 1,420.00 to DESCO successful. Bill No 12345678. Fee Tk 0.00. Balance Tk 3,100.00. TrxID 2P3Q4R5S.` | Amount: 1420.00, Merchant: DESCO, Fee: 0.00, Bal: 3100.00 |
| **Savings Auto-Debit**| `[DEBIT (-)]` | `Tk 1,000.00 deducted for monthly deposit in IDLC Savings Scheme via bKash. Balance Tk 4,200.00. TrxID 6T7U8V9W.` | Amount: 1000.00, Scheme: IDLC Savings, Bal: 4200.00 |
| **Cashback** | `[CREDIT (+)]` | `Cashback Tk 50.00 received for Payment. Balance Tk 2,150.00. TrxID 9W8V7U6T.` | Amount: 50.00, Bal: 2150.00, TrxID: 9W8V7U6T |

---

#### Nagad (Sender: `NAGAD`)
| Transaction Type | Direction | Raw SMS Template | Extracted Fields |
|---|---|---|---|
| **Cash Out** | `[DEBIT (-)]` | `Cash Out Tk 500.00 to 016XXXXXXXX successful. Fee: Tk 7.50. Balance: Tk 1,200.00. TxnID: 72JH89KA.` | Amount: 500.00, Fee: 7.50, Bal: 1200.00, TxnID: 72JH89KA |
| **Send Money** | `[DEBIT (-)]` | `Send Money Tk 1,000.00 to 017XXXXXXXX successful. Fee: Tk 0.00. Balance: Tk 2,500.00. TxnID: 88PL99MN.` | Amount: 1000.00, Bal: 2500.00, TxnID: 88PL99MN |
| **Merchant Pay** | `[DEBIT (-)]` | `Merchant Payment Tk 650.00 to Aarong successful. Balance: Tk 1,850.00. TxnID: 44KL33QR.` | Amount: 650.00, Merchant: Aarong, Bal: 1850.00 |
| **Mobile Recharge** | `[DEBIT (-)]` | `Mobile Recharge Tk 50.00 to 018XXXXXXXX successful. Balance: Tk 1,400.00. TxnID: 33MN22LK.` | Amount: 50.00, Party: 018XXXXXXXX, Bal: 1400.00 |
| **Cash In** | `[CREDIT (+)]` | `Cash In from 017XXXXXXXX Tk 1,000.00 successful. Balance: Tk 2,200.00. TxnID: 99KL22OP.` | Amount: 1000.00, Party: 017XXXXXXXX, Bal: 2200.00 |
| **Add Money** | `[CREDIT (+)]` | `Add Money from Bank Tk 2,000.00 successful. Balance: Tk 4,200.00. TxnID: 55AA66BB.` | Amount: 2000.00, Source: Bank, Bal: 4200.00 |
| **Bill Pay** | `[DEBIT (-)]` | `Bill Payment of Tk 1,200.00 to DPDC successful. Balance: Tk 2,100.00. TxnID: 77CC88DD.` | Amount: 1200.00, Merchant: DPDC, Bal: 2100.00 |

---

### 3.3 Traditional Commercial Banks

#### City Bank (Senders: `City Bank`, `CityBank`)
| Scenario | Direction | Raw SMS Template | Extracted Fields |
|---|---|---|---|
| **Credit Card POS** | `[DEBIT (-)]` | `Purchase of BDT 2,450.00 on your Credit Card ending 4321 at UNIMART DHAKA on 07-Sep-2026 14:32. Avail Limit BDT 75,550.00.` | Amount: 2450.00, Last4: 4321, Merchant: UNIMART DHAKA, Limit: 75550.00 |
| **Credit Card E-com** | `[DEBIT (-)]` | `Online purchase of BDT 1,150.00 on your Credit Card ending 4321 at CHALDAL.COM on 07-Sep-2026. Avail Limit BDT 74,400.00.` | Amount: 1150.00, Last4: 4321, Merchant: CHALDAL.COM, Limit: 74400.00 |
| **Citytouch A/C Debit** | `[DEBIT (-)]` | `Your A/C 110***901 has been debited for BDT 10,000.00 on 07-Sep-2026 via Citytouch. Ref: FT260907. Avail Bal BDT 42,300.00.` | Amount: 10000.00, Account: 110***901, Ref: FT260907, Bal: 42300.00 |
| **NPSB A/C Credit** | `[CREDIT (+)]` | `Your A/C 110***901 has been credited by BDT 15,000.00 on 07-Sep-2026 via NPSB. Avail Bal BDT 57,300.00.` | Amount: 15000.00, Account: 110***901, Bal: 57300.00 |

#### BRAC Bank (Senders: `BRAC Bank`, `BRACBANK`)
| Scenario | Direction | Raw SMS Template | Extracted Fields |
|---|---|---|---|
| **Debit Card POS** | `[DEBIT (-)]` | `Txn of BDT 1,200.00 done with your Debit Card 4021****1234 at SHWAPNO on 07-SEP-26 12:15. Available Bal BDT 18,340.50.` | Amount: 1200.00, Last4: 1234, Merchant: SHWAPNO, Bal: 18340.50 |
| **ATM Withdrawal** | `[DEBIT (-)]` | `Cash withdrawal of BDT 5,000.00 from ATM using Card 4021****1234 on 07-SEP-26. Available Bal BDT 13,340.50.` | Amount: 5000.00, Last4: 1234, Type: ATM Cash Withdrawal, Bal: 13340.50 |
| **Salary Credit** | `[CREDIT (+)]` | `Your A/C 1501******0001 has been credited by BDT 65,000.00 on 01-SEP-26 by SALARY. Available Bal BDT 72,120.00.` | Amount: 65000.00, Account: 1501******0001, Category: Salary, Bal: 72120.00 |

#### Dutch-Bangla Bank - DBBL (Senders: `DBBL`, `Dutch-Bangla`)
| Scenario | Direction | Raw SMS Template | Extracted Fields |
|---|---|---|---|
| **ATM Withdrawal** | `[DEBIT (-)]` | `A/C 115.110.***** debited by ATM WDL BDT 5,000.00 on 07/09/2026 15:40 at DBBL ATM DHAKA. Avail Bal BDT 12,400.00.` | Amount: 5000.00, Account: 115.110.*****, Location: DBBL ATM DHAKA, Bal: 12400.00 |
| **POS Purchase** | `[DEBIT (-)]` | `A/C 115.110.***** debited by POS purchase BDT 1,850.00 at AGORA on 07/09/2026. Avail Bal BDT 10,550.00.` | Amount: 1850.00, Account: 115.110.*****, Merchant: AGORA, Bal: 10550.00 |
| **A/C Credit** | `[CREDIT (+)]` | `A/C 115.110.***** credited by BDT 15,000.00 on 07/09/2026. Avail Bal BDT 25,550.00.` | Amount: 15000.00, Account: 115.110.*****, Bal: 25550.00 |

#### Islami Bank Bangladesh - IBBL (Senders: `IBBL`, `Cellfin`)
| Scenario | Direction | Raw SMS Template | Extracted Fields |
|---|---|---|---|
| **Cellfin Transfer** | `[DEBIT (-)]` | `Cellfin: Tk 2,500.00 has been transferred from your A/C 2050***123 to 017XXXXXXXX. Fee Tk 0.00. Balance Tk 8,900.00. Trx ID: CF2609071234.` | Amount: 2500.00, Account: 2050***123, Party: 017XXXXXXXX, Fee: 0.00, Bal: 8900.00, Ref: CF2609071234 |
| **Inward Remittance**| `[CREDIT (+)]` | `Your A/C 2050***123 is credited with Tk 25,000.00 on 07-09-2026 by INWARD REMITTANCE. Available Balance Tk 33,900.00.` | Amount: 25000.00, Account: 2050***123, Channel: INWARD REMITTANCE, Bal: 33900.00 |

---

## 4. Centwise Parser Gap Analysis & Action Plan

| Capability | Current Centwise Status | Document Specification | Action Needed |
|---|---|---|---|
| **Pipeline Stages** | 3 Stages (Classify, Provider, Extract) | 5 Stages (Sanitizer, Gatekeeper, Classifier, Intent Matcher, Extraction) | Keep Centwise 3-Stage pipeline as it already executes these functions efficiently; refine classify filters. |
| **VAS Subscriptions** | Rejected as noise if no transaction verb | Explicitly parsed as `[DEBIT (-)]` | Allow `deducted for Welcome Tune / Amar Tune / GoonGoon / Missed Call Alert` in `classify/telco.rs`. |
| **Telco Recharge Intent** | Classified universally as `Expense` | Classified as `[CREDIT (+)]` on SIM ledger | Clarify whether Telco SIM is tracked as a balance account. If tracking airtime balance, classify telco recharge confirmations as `Income` (top-up) while MFS recharges remain `Expense`. |
| **Emergency Balance Repayment** | Tricky with `deducted from your recharge` | Explicit regex for `deducted.*?Emergency Balance` | Ensure repayment is parsed as `Expense` without confusing the initial recharge amount. |
| **Internet / Voice Bundles** | Parsed as expense, but pack volume lost | `1.5GB Internet`, `50 Mins`, etc. extracted | Enhance `extract/party.rs` to include pack descriptors in merchant/notes. |
| **Test Fixtures** | Existing 7 fixture files in `fixtures/sms/` | New unified test cases across all BD operators & banks | Merge the document's test cases into `fixtures/sms/` to guarantee regression prevention. |

---

## 5. Next Steps for Developer

1. **Review Directionality Model:** Confirm whether Telco SIM cards are tracked as independent asset accounts in Centwise DB or if telco recharges should remain user-wallet expenses.
2. **Update Telco Filter Rules:** Whitelist confirmed VAS deductions (Welcome Tune, GoonGoon, etc.) in `classify/telco.rs`.
3. **Expand Fixtures:** Add all SMS templates from Section 3 above into `fixtures/sms/telco.json`, `fixtures/sms/banks-english.json`, and `fixtures/sms/bkash.json`.
4. **Run Verification Suite:** Run `cargo test -p centwise-parser` to ensure all 84+ tests continue to pass with new fixtures.

# Parser Design: Generic Field Extraction

Status: agreed 2026-08-22. Applies to the future `centwise-parser` crate.

## Implemented safety contract (2026-10-22)

The parser is implemented. The following rules supersede the early keyword-only
and transfer notes below; they apply identically to Android and iOS through Rust.

- A posted action determines direction. Account debits and outgoing transfers to
  unspecified recipients are expenses; incoming credits are income. Only an
  explicitly stated movement between the user's own accounts is neutral Transfer.
  A minus sign beside a transaction amount is not by itself a refund. Reported
  account balances retain their positive or negative sign.
- Loan offers, eligibility, schedules, requests waiting for confirmation, and
  repayments that will happen later are not transactions. A completed cash loan
  disbursement is income; an actual repayment/deduction is expense. Data/minutes
  advanced on credit are not cash income. Future service fees do not reduce a
  present cash disbursement.
- Telco offers such as "recharge and get" are instructions, not completed
  purchases. Completed pack purchases, recharge receipts, and VAS deductions
  remain supported, including receipts with marketing or security footers.
- Supplied OTP values are rejected. An advisory mentioning OTP must not capture
  a balance in a later sentence. Negated or pending refunds are not credits;
  posted partial refunds use the refund amount, not an earlier failed payment.
- A standalone fee is its own principal amount, not an additional fee as well.
  Invalid money precision is rejected instead of silently truncating digits.
- TrxID/TxnID, Ref No/ID, NPSB/BEFTN Ref, RRN, and STAN are transaction identifiers.
  Bare Ref/Reference may contain user memos, including numbers, and are not unique
  transaction identifiers. No-reference identities include the supplied SMS
  timestamp. The ten-minute account/amount/type/balance resend check additionally
  requires the same normalized receipt content, preserving different memos and
  provider references. Existing review items deduplicate by normalized sender,
  content and original timestamp even when their older IDs differ.
  Identical-looking genuine payments inside the ten-minute window remain
  ambiguous without a reference or changed balance; changed-text resends without
  an authoritative reference can require manual reconciliation.
- A destination wallet, biller account, or source-bank mention must not replace
  the tracked account/provider. Bank details are not required for recognized
  merchant purchases and completed service charges. BRTA payment confirmations,
  DPDC completed recharges, already-purchased telco bundle charges, successful
  fee payments, and Ryans purchase-bill confirmations are expenses. Ryans
  purchase confirmations count as spending under the agreed product policy;
  quotations, cancellations and explicit unpaid notices do not.
  Merchant/service names and categories are separate from the funding provider.
  These receipts use the existing generic Primary Account when funding is unknown,
  without assigning an arbitrary bank/operator. Meter balance, customer and meter
  numbers are not financial-account fields. DPDC uses the gross recharge total,
  not energy cost, VAT, rebate or remaining meter credit. Standalone bundle/fee
  charges are not counted a second time as an extra fee.
  Unrecognized receipt formats remain reviewable when financial evidence exists.
  Add future formats here with positive, negative and ingestion fixture tests;
  do not accept every message containing a price or invoice link.

The 2026-10-23 hardening pass adds explicit incoming-transfer direction checks
and security-code alias rejection even when instructions mention a successful
payment. Synthetic regressions cover bank and wallet senders, outgoing transfers,
and six security-code labels. Report 12 now asserts all 17 rows, exact amounts,
and reviewed directions instead of only printing results. Existing CSV snapshots
remain unchanged. These are deterministic parser fixes, not model training.

Android/iOS parity: both platforms consume these same Rust outcomes; there are
no FFI or database changes. Native library packaging and device testing remain
separate checks before release.

Public-source check: [bKash checkout terms](https://www.bkash.com/en/page/terms-of-use-checkout)
distinguish held payments from returned funds and notifications, but do not supply
an exact SMS-template contract. No unverified web template or sender ID was added.

Verification uses all 4,714 rows of `fixtures/audits/all-sms-test-input.csv` plus
the 16 original reports and anonymized adversarial regression cases. The aligned
`all-sms-expected.csv` freezes disposition, provider, type, amount, fee, and whether
a reported balance exists, without copying account balances into a second file.
It is a regression snapshot, not independently labeled production ground truth
or a guarantee of perfect recognition. Do not regenerate it merely to pass tests;
review each changed message family first.

Public Bangladesh bill-payment coverage is recorded in
[the source review](../providers/public-payment-format-research.md).
Additional fixtures cover BREBPrepaid electricity, MetLife premiums and Rocket
school fees, with synthetic customer data. Labeled bKash biller A/C fields cannot
become payer-wallet identifiers. Rocket's dotted Txn.Id and paid-successfully
wording are supported. The existing wallet format is also tested with 19 common
merchant/service names; these tests do not claim direct merchant SMS coverage.

Current consolidated result: 4,339 parsed, 375 ignored, 0 queued for review.
The receipt follow-up recovered all 40 review rows and 10 previously ignored
DPDC recharge receipts. Only these 50 individually inspected rows changed in the
regression snapshot. This does not imply that every future message can parse.
Historical exports' stored amounts and types
are not authoritative: the audit found date/account numbers previously mistaken
for money as well as unconfirmed requests labeled as transactions.

Verified on the Windows host: `cargo test --workspace --locked` (306 tests),
`cargo fmt --all --check`, and
`cargo clippy --workspace --all-targets --locked -- -D warnings`.
Cargo reported incremental-cache hard-link fallback warnings; all checks exited
successfully. The isolated DB-only command required an unavailable `dlltool`;
the full workspace run executed and passed those DB tests with its unified
dependency features.

Existing stored transactions are not rewritten or rescanned by these changes.
Separate merchant and bank SMS for one purchase can still produce two expenses
without shared authoritative transaction evidence; cross-source reconciliation
is not implemented. Unknown-source expenses affect the Primary Account, not a
known bank balance.
The FFI records, database schema, localization keys, and native UI contracts are
unchanged. Both platforms share the source behavior; native-library packaging
and emulator/simulator/device verification are separate checks.
Android packaging verified on 2026-10-22: release Rust libraries rebuilt for
arm64-v8a, armeabi-v7a, x86 and x86_64 using NDK 27.2.12479018/API 26; regenerated
Kotlin bindings matched the checked-in bindings. The repository Gradle wrapper
passed `:app:testDebugUnitTest` (4 tests) and `:app:assembleDebug`. The debug APK's
signature, ZIP alignment and packaged Rust-library hashes were verified. The
missing SDK was restored into ignored `core/target/` folders and the ignored
Android local SDK path was updated. Phone/emulator SMS ingestion remains
unverified. iOS native packaging and simulator verification require macOS/Xcode.

Public references checked for semantics, without submitting SMS data:
[Grameenphone emergency balance](https://www.grameenphone.com/personal/plans-offers/offers/emergency-balance)
and [bKash loan service](https://www.bkash.com/en/products-services/loan/loan-bkash).
These explain disbursement and later recovery; they do not validate exact SMS templates.

## Principle

**Never match templates. Hunt fields.**

Bank and MFS SMS formats change without notice. A parser that expects a fixed
sentence structure breaks on the first rewording. Instead, for every message
from a known provider, the parser independently searches for each field
wherever it appears:

| Field | Found by | Notes |
|---|---|---|
| Provider | Sender ID, or trailing `[Bank Name]` | sender catalog is the source of truth |
| Amount | `Tk <number>` that is NOT preceded by `Fee`/`Balance` | first standalone amount wins |
| Fee | number right after `Fee` | optional |
| Balance after | number right after `Balance`/`Available Balance` | optional |
| Reference | token after `TrxID` | also the dedup key |
| Type | keyword table, matched anywhere | see below |
| Merchant | text after `to <name> successful`, before `successful` | optional |
| Party (wallet no.) | phone pattern `01XXXXXXXXX` after `to`/`from` | optional |
| Account | `A/C XXXX1234` last four, or provider default | optional |
| Date/time | `dd/mm/yyyy` (+ `hh:mm` when present), else SMS receive time | optional |

Order independence: "Balance Tk 5.00 ... Cash Out Tk 2.00" parses the same as
the reverse order. Format changes that keep the same vocabulary keep working.

## Type keyword table (matched anywhere, first hit wins)

- **Income:** `Cash In`, `received`, `credited`, `Add Money`, `Cashback`, `Interest`, `Salary`
- **Expense:** `Cash Out`, `Send Money` (sender perspective), `Payment`, `debited`, `Withdrawal`, `Recharge`, `EMI`, `Bill Pay`, `Purchase`
- **Transfer:** `to A/C` between own accounts (later)
- Provider-specific overrides live in a small per-provider table (e.g. Rocket balance has no `Tk` prefix).

## Merchant dictionary → category

Extracted merchant text is matched against a dictionary (case-insensitive,
contains-match). Runs AFTER type detection, BEFORE user Smart Rules; Smart
Rules always win.

| Merchant contains | Category |
|---|---|
| Foodpanda, HungerStation, Sultan's Dine | Food & Dining |
| Pathao, Uber, Obhai, Shohoz | Transport |
| Daraz, Pickaboo, Unimart | Shopping |
| Airtel, GP, Grameenphone, Robi, Banglalink, Teletalk | Mobile Recharge |
| Netflix, Spotify, Hoichoi | Entertainment |
| (none matched) | per type fallback: Recharge→Mobile Recharge, ATM→Cash Withdrawal, else Other |

`Mobile Recharge` type keyword forces Mobile Recharge category even when the
destination is just a phone number.

## Safety rules

1. **No amount found → not a transaction.** Message goes to the review queue;
   the parser never invents a transaction.
2. **OTP / promo / spam** rejected by keyword filter before extraction
   (`OTP`, `One Time`, `offer`, `WIN`, `discount`, ...).
3. **Dedup:** a repeated TrxID (or amount+balance+minute for banks without
   references) within a short window is skipped, not re-inserted.
4. **Deterministic:** same message always produces the same result — every
   rule above is covered by fixture tests in `fixtures/sms/`.
5. **Parse failures are visible:** unsupported provider messages land in the
   review queue so new formats can be collected and added.

## Fixture extension: merchant + category cases

Add to `fixtures/sms/` when the parser is built:

- bKash payment to a named merchant (Foodpanda) → category `food`
- Recharge naming an operator (Airtel/Banglalink/GP/Robi) → category `recharge`
- Pathao ride payment → category `transport`
- Same message with words reordered → identical result (order-independence test)
- Message with no amount → rejected, review queue

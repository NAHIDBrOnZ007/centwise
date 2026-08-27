# Parser Design: Generic Field Extraction

Status: agreed 2026-08-22. Applies to the future `centwise-parser` crate.

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

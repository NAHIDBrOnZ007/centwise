# Automatic Account Resolution Design

**Status:** Approved for implementation

## Goal

Allow Centwise to save confirmed SMS and manual transactions when the user has
not created an account yet, without weakening the database rule that every
transaction belongs to a real account.

## Product behavior

Centwise uses these account-resolution rules on Android and iOS:

1. A transaction that names an existing account uses that account.
2. A confidently parsed SMS with exactly one matching active account uses it.
3. A confidently parsed SMS with no matching account atomically creates an
   account for the parsed provider and optional account suffix, then saves the
   transaction.
4. A confidently parsed SMS with multiple matching accounts enters the review
   queue because Centwise cannot safely choose one.
5. An uncertain or unparseable financial SMS enters the review queue. OTP,
   promotional, and other explicitly rejected messages remain ignored.
6. A manual transaction with no selected account atomically creates or reuses
   the stable Cash/Unassigned account and saves the transaction.
7. A manual transaction that explicitly chooses a provider for which no
   account exists atomically creates that provider account and saves the
   transaction.

Review is for ambiguity or uncertainty, not merely for a missing account.

## Rust-owned account resolution

Rust remains the only owner of account creation and transaction writes. Add one
internal account resolver used by SMS ingestion and manual insertion. It returns
one of three outcomes: an existing account ID, a newly created account ID, or
an ambiguous result.

Automatic account IDs are deterministic and collision-safe:

- Provider wallet without a suffix: `auto-<provider>-wallet`
- Provider account with a suffix: `auto-<provider>-<normalized-suffix>`
- Manual entry without a provider: `system-cash`

Provider IDs and suffixes are normalized to lowercase ASCII letters and digits
with single hyphens. Account display names use the canonical provider name,
optionally followed by the suffix. Existing account rows are reused; inserts
must be idempotent.

Account resolution and transaction insertion occur inside the same SQLite
write transaction. A failure rolls back both operations. A supplied non-empty
account ID that does not exist is an error and must never silently fall back to
Cash.

## FFI contract

Keep `TransactionInput` for native transaction CRUD and add optional account
creation hints needed only when inserting:

- `account_id`: existing selected account ID, or an empty string when native UI
  requests automatic resolution.
- `account_provider`: normalized provider ID when known.
- `account_name`: preferred display name when known.
- `account_last_four`: optional parsed or user-entered suffix.

`insert_transaction` resolves an empty account ID and inserts atomically.
`update_transaction` requires a valid non-empty existing account ID and never
creates an account. Regenerate and commit both Kotlin and Swift UniFFI bindings.

SMS ingestion does not depend on native hints; it uses parser output directly.

## Native behavior

The iOS and Android add-transaction forms no longer disable Save solely because
the account list is empty. Existing accounts remain selectable. When none is
selected, the form saves to Cash/Unassigned. If the user explicitly chooses a
provider without an existing account, the form sends that provider as an
account-creation hint.

Native repositories report insertion failure instead of showing success,
dismissing the sheet, or posting a notification. A successful insert refreshes
the Rust-backed UI cache once.

The UI implementation remains native SwiftUI and Jetpack Compose. PennyWise is
behavioral reference only; no source, schema, assets, or text are copied.

## Compatibility

The existing `transactions.account_id NOT NULL` foreign key remains unchanged.
No schema migration is required. Existing accounts and transactions keep their
IDs. The old provider-only IDs such as `auto-bkash` remain valid and can still
be matched; newly created accounts use the collision-safe format.

Reset-to-empty removes user and auto-created accounts as it does today. The
Cash/Unassigned account is recreated lazily on the next account-less manual
insert.

## Tests

Rust tests must prove:

- First bKash SMS with no account creates one account and one transaction.
- Repeating use of the same provider/suffix reuses the account.
- Different suffixes create distinct collision-safe account IDs.
- Multiple matching accounts still queue the SMS for review.
- Account-less manual insert creates/reuses `system-cash` atomically.
- Explicit manual provider creation creates/reuses its automatic account.
- An invalid supplied account ID fails without creating Cash or a transaction.
- Update never creates accounts.

Native tests or source-level boundary checks must prove:

- Empty account lists no longer block manual Save.
- Native inputs send an empty account ID plus Cash/provider hints when automatic
  resolution is requested.
- Success UI and notifications occur only after Rust confirms insertion.

Run the full Rust workspace test, format, and Clippy gates. Regenerate both
bindings. Compile Android when Android Build Tools 36.0.0 is available. Compile
and exercise iOS with Xcode on macOS; Windows source checks are not a substitute
for simulator/device verification.

## Superseded rule

This design supersedes only the sentence in the approved Rust ingestion design
that sends every message with no unambiguous account to review. The new rule is:
zero matches creates an account, while multiple matches enters review. All other
Rust ownership, privacy, deduplication, and native-boundary decisions remain in
force.

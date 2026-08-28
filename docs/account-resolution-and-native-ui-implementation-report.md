# Centwise Account Resolution and Native UI Implementation Report

**Date:** 2026-08-28  
**Implementation status:** Source implementation complete; platform acceptance still required where noted  
**Project:** Centwise  
**Reference boundary:** PennyWise was used only as a behavioral/design reference. No PennyWise source, database schema, assets, branding, secrets, or project structure were copied.

## 1. Why this work was needed

Centwise previously expected a real database account before a transaction could
be saved. That created an incorrect product behavior: a valid bKash, Nagad, bank,
or manual transaction could be blocked or sent to Review Queue only because the
user had not manually created an account.

The corrected rule is:

> Review Queue is for uncertainty or ambiguity. A missing account by itself is
> not an error and is not a reason to queue a confidently understood transaction.

This work also establishes a more native iOS interface. Ordinary iOS interactions
now use SwiftUI's standard navigation, tabs, lists, forms, search, toolbar actions,
buttons, alerts, sheets, and swipe actions. Custom UI remains only where Centwise
needs financial or brand-specific presentation.

## 2. Final account-resolution rules

| Situation | Centwise behavior | Reason |
|---|---|---|
| A valid existing account ID is supplied | Use that exact account | The user made an explicit choice |
| The supplied non-empty account ID does not exist | Reject the write | Never silently put the transaction in Cash or another account |
| Exactly one active account matches the parsed provider/suffix | Reuse that account | The match is unambiguous |
| No account matches a confident provider/suffix | Create the account and transaction atomically | Missing setup should not block valid tracking |
| Multiple active accounts match | Require explicit selection or Review Queue | Centwise must not guess which wallet/account is correct |
| Manual transaction has no selected account or provider | Create/reuse Cash/Unassigned | Manual entry remains quick and database-valid |
| An automatic deterministic account exists but is archived | Reactivate and reuse it | Avoid duplicate IDs and hidden transaction accounts |
| SMS is uncertain or cannot be safely interpreted | Put it in Review Queue | A human decision is required |
| SMS is OTP, promotional, duplicate, or explicitly non-financial | Ignore it | It is not a transaction candidate |

### Deterministic automatic account IDs

- Cash/Unassigned: `system-cash`
- Provider without an account suffix: `auto-<provider>-wallet`
- Provider with a suffix: `auto-<provider>-<suffix>`

Examples:

- `auto-bkash-wallet`
- `auto-nagad-wallet`
- `auto-city-bank-1111`
- `auto-city-bank-2222`

Provider names and suffixes are normalized before IDs are generated. Repeated
transactions therefore reuse the same account instead of creating duplicates.

## 3. Atomic database behavior

Account resolution and transaction insertion execute in one Rust-owned SQLite
write. This provides the following guarantee:

```text
resolve/reuse/create account
        +
insert transaction and update balance
        =
one atomic database operation
```

If any part fails, neither the new account nor the transaction is committed.
The existing `transactions.account_id NOT NULL` foreign-key design remains
unchanged, so every saved transaction still belongs to a real database account.
No database migration was required.

The main database implementation is in:

- `core/centwise-db/src/queries/accounts.rs`

It now provides account existence checking and deterministic
`resolve_or_create_account` behavior. It also prevents automatic reuse from
leaving an account archived.

## 4. Rust FFI organization

The old `core/centwise-ffi/src/lib.rs` contained more than one thousand lines
covering unrelated responsibilities. It was safely divided into focused modules:

- `lib.rs` — crate entry point, module declarations, UniFFI setup, and public exports
- `types.rs` — UniFFI records, enums, and input/output contracts
- `core.rs` — `CentwiseCore` database and domain operations
- `ingestion.rs` — SMS parsing and ingestion-facing functions
- `conversions.rs` — native/FFI-to-domain conversion helpers
- `error.rs` — exported error and change-listener contracts
- `tests.rs` — focused Rust FFI and ingestion tests

`lib.rs` is intentionally small, not empty. Rust allows the crate root to act as
an organized public doorway while implementation code lives in responsibility-
specific modules. Generated native APIs continue to receive the exported public
types from the same crate.

## 5. FFI contract changes

`TransactionInput` now includes three optional account-creation hints:

- `account_provider`
- `account_name`
- `account_last_four`

The existing `account_id` field is interpreted as follows:

- Non-empty ID: use and validate that exact existing account.
- Empty ID: Rust resolves or creates an account from the optional hints.

Important update rule:

> Insert may resolve/create an account. Update must use a valid existing account
> and never creates a new account implicitly.

Both generated native bindings were regenerated:

- Android Kotlin: `apps/android/app/src/main/kotlin/com/centwise/core/uniffi/centwise_ffi.kt`
- iOS Swift/C headers: `apps/ios/Centwise/Core/FFI/generated/`

## 6. SMS ingestion and Review Queue behavior

SMS ingestion continues to be owned by Rust. Native UI does not make the final
parsing, deduplication, categorization, or account-resolution decision.

The resulting flow is:

```text
SMS received/imported
    -> Rust parser and rejection checks
    -> deduplicate reference/message
    -> apply smart rules and category logic
    -> resolve account
         zero matches -> create account and save
         one match    -> reuse account and save
         many matches -> Review Queue
    -> refresh native repository after success
```

When a Review Queue item is converted on iOS, the form starts with an empty
account ID and a provider inferred from the sender. It no longer takes the first
unrelated account from the database. Android similarly sends the sender/payment
method as a provider hint when no exact account name is selected.

Android account selection was also tightened: an account is selected only by an
exact account-name match. Centwise no longer chooses the first account merely
because several accounts share the same provider such as bKash.

## 7. Native repository success/failure behavior

The iOS and Android repository methods involved in transaction insertion and
Review Queue conversion now return success status.

Native UI behavior is now:

- Keep the form open when Rust rejects or fails the write.
- Show an error instead of pretending the operation succeeded.
- Dismiss the form only after Rust reports success.
- Trigger success feedback/notification only after confirmed success.
- Refresh Rust-backed native data only after the mutation succeeds.

This prevents a failed database write from looking successful to the user.

## 8. iOS native SwiftUI redesign

### Application shell

`MainTabView.swift` now uses native `TabView(selection:)` with four independent
`NavigationStack` roots:

1. Home
2. Transactions
3. Analytics
4. Settings

The custom floating pill tab bar and matched-geometry tab animation were removed.
Native tab selection, SF Symbols, accessibility, and navigation behavior now come
from SwiftUI.

### Home

- Uses a native navigation title and toolbar Add action.
- Keeps Centwise financial summary components where they communicate real data.
- Presents recent activity in a cleaner list-like system surface.
- Preserves edit and delete actions.
- Removes the custom floating add button.

### Transactions

- Uses native `List`.
- Uses `.searchable` for merchant, account, and reference search.
- Uses toolbar menus for filter, sort, export, and add actions.
- Uses native swipe actions for edit and delete.
- Preserves totals, empty state, transaction detail, and add/edit sheets.

### Add/edit transaction

- Uses native `Form`, `Section`, `Picker`, `TextField`, `DatePicker`, and toolbar actions.
- Save is no longer disabled just because no accounts exist.
- Provides Cash/Unassigned and provider creation choices.
- Preserves the original account/provider while editing.
- Shows a save error and remains open if Rust returns failure.
- Dismisses only after successful persistence.

### Settings

Settings now uses a native inset-grouped `List` and preserves all destinations:

- Shortcuts
- Appearance
- Currency
- Categories
- Budgets
- Accounts
- Subscriptions
- Smart Rules
- Review Queue
- Data Management
- FAQ
- About

Profile editing and App Lock remain available. Standard `NavigationLink`,
`Toggle`, labels, and semantic colors replace repeated custom card rows.

### Review Queue

- Uses native grouped list rows.
- Keeps raw SMS readable in monospaced text.
- Uses system bordered and destructive-role buttons.
- Includes a privacy explanation for raw SMS.
- Does not expose the raw message as the VoiceOver label.
- Uses provider inference instead of assigning the first database account.

### Data and storage

- Uses native `List`, `Section`, `LabeledContent`, and `Label` rows.
- Preserves database size, record counts, demo data, CSV export, and reset actions.
- Keeps confirmation alerts and destructive roles for database reset.

### Analytics and accessibility

- Period and transaction-type controls use native menu pickers.
- Spending and category charts provide accessibility summaries.
- Spending animation respects Reduce Motion.
- Category count uses the view-model result instead of rescanning the global
  transaction repository for every category row.

### Other native button cleanup

- Onboarding Continue/Get Started actions use native prominent buttons.
- Empty Smart Rules state uses a native prominent Create Rule button.
- Standard forms for accounts, budgets, subscriptions, categories, and rules
  were audited and retained because they already use native Form/toolbar patterns.

## 9. iOS startup adjustment

`CentwiseApp.swift` no longer reloads the complete transaction and Review Queue
state every time the app scene becomes active. Initial Rust loading remains on
app appearance, and the shared Review Queue repository initializes normally.

This removes avoidable repeated database refresh work during foreground changes,
which can contribute to the slow-open/slow-resume feeling. Full runtime timing
still needs measurement on a real iPhone or simulator.

## 10. Android changes

- Transaction add/edit can save without a pre-created account.
- Payment methods include active account names and common provider defaults.
- Existing transaction identity, timestamp, note, and raw fields are preserved
  during editing.
- Sheets dismiss only when repository/Rust save succeeds.
- Review Queue conversion clears the editing item only after success.
- Provider strings are normalized consistently before crossing UniFFI.
- Exact account-name matching prevents silent selection of the first provider account.
- Transaction sort now changes ascending/descending order.
- CSV export action now invokes the existing exporter instead of doing nothing.

## 11. Tests added and behavior covered

Rust tests now cover:

- First provider SMS creates an automatic account when none exists.
- Repeated provider use reuses the same account.
- Different suffixes create different deterministic account IDs.
- Manual accountless transactions create/reuse `system-cash`.
- Invalid selected account IDs fail without creating fallback data.
- Review conversion can atomically create/reuse Cash.
- Multiple matching accounts remain ambiguous and go to Review Queue.
- Automatic archived accounts are reactivated and reused.
- Persisted smart rules are applied during ingestion.
- OTP and duplicate references remain ignored/deduplicated.
- Full account, transaction, budget, subscription, category, rule, migration,
  reset, analytics, and Review Queue CRUD behavior.

## 12. Verification performed on Windows

### Passed

- `cargo fmt --all -- --check`
- `cargo test --workspace`
  - 66 tests passed
  - 0 tests failed
- `cargo clippy --workspace --all-targets -- -D warnings`
- Focused account-resolution regression tests
- `git diff --check`
- Native source-boundary checks
- Generated Swift/Kotlin binding-field checks
- Xcode project source-reference check for all 18 changed Swift files

### Environment-blocked

Android compilation reached SDK dependency resolution but stopped because Android
SDK Build Tools `36.0.0` is not installed and its license has not been accepted
on this PC. The compile must be retried after installing/accepting that SDK item.

### Requires macOS/Xcode

Windows cannot provide final iOS acceptance. On macOS, run and verify:

- Xcode compile for the iOS 16 deployment target
- Small and large iPhone simulator layouts
- Real or simulated transaction add/edit/delete
- Accountless Cash, bKash, Nagad, and bank saves
- Multiple-account ambiguity behavior
- Review Queue conversion
- Light and dark mode
- Dynamic Type
- VoiceOver basics
- Reduce Motion
- Empty, success, and error states

## 13. What was intentionally not changed

- No PennyWise implementation was copied.
- No SQLite schema or migration was changed.
- No transaction foreign-key requirement was removed.
- Native apps did not take ownership of database writes or account resolution.
- Rust remains responsible for SMS parsing, deduplication, categorization,
  smart rules, Review Queue decisions, account resolution, and persistence.
- Existing Android Gradle/AGP upgrades in the working tree were preserved and
  were not reverted as part of this implementation.
- No commit, push, merge, or remote change was made.

## 14. Main implementation files

### Rust

- `core/centwise-db/src/queries/accounts.rs`
- `core/centwise-db/tests/crud_round_trip_test.rs`
- `core/centwise-ffi/src/lib.rs`
- `core/centwise-ffi/src/core.rs`
- `core/centwise-ffi/src/types.rs`
- `core/centwise-ffi/src/ingestion.rs`
- `core/centwise-ffi/src/conversions.rs`
- `core/centwise-ffi/src/error.rs`
- `core/centwise-ffi/src/tests.rs`

### iOS

- `apps/ios/Centwise/App/CentwiseApp.swift`
- `apps/ios/Centwise/App/MainTabView.swift`
- `apps/ios/Centwise/Core/FFI/CentwiseRustBackend.swift`
- `apps/ios/Centwise/Data/Repositories/TransactionRepository.swift`
- `apps/ios/Centwise/Data/Repositories/ReviewQueueRepository.swift`
- `apps/ios/Centwise/Features/Home/Screens/HomeScreen.swift`
- `apps/ios/Centwise/Features/Transactions/Screens/TransactionListView.swift`
- `apps/ios/Centwise/Features/Transactions/Screens/AddEditTransactionView.swift`
- `apps/ios/Centwise/Features/Transactions/Screens/ReviewQueueView.swift`
- `apps/ios/Centwise/Features/Settings/Screens/SettingsScreen.swift`
- `apps/ios/Centwise/Features/Settings/Screens/DataManagementScreen.swift`
- `apps/ios/Centwise/Features/Analytics/`

### Android

- `apps/android/app/src/main/kotlin/com/centwise/MainActivity.kt`
- `apps/android/app/src/main/kotlin/com/centwise/core/backend/CentwiseRustBackend.kt`
- `apps/android/app/src/main/kotlin/com/centwise/data/repository/ReviewQueueRepository.kt`
- `apps/android/app/src/main/kotlin/com/centwise/features/transactions/AddEditTransactionSheet.kt`
- `apps/android/app/src/main/kotlin/com/centwise/features/transactions/ReviewQueueScreen.kt`
- `apps/android/app/src/main/kotlin/com/centwise/features/transactions/TransactionListScreen.kt`

## 15. Related approved specifications and plans

- `docs/superpowers/specs/2026-08-28-automatic-account-resolution-design.md`
- `docs/superpowers/specs/2026-08-28-ios-native-ui-redesign.md`
- `docs/superpowers/plans/2026-08-28-automatic-account-resolution.md`
- `docs/superpowers/plans/2026-08-28-ios-native-ui-redesign.md`


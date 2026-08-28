# Automatic Account Resolution Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Save confirmed SMS and manual transactions without a pre-created account while retaining a non-null account foreign key.

**Architecture:** A Rust database resolver atomically reuses or creates provider/Cash accounts before transaction insertion. UniFFI carries optional creation hints; SwiftUI and Compose submit those hints and react only to confirmed Rust success.

**Tech Stack:** Rust, rusqlite, UniFFI, Swift/SwiftUI, Kotlin/Jetpack Compose

**Spec:** `docs/superpowers/specs/2026-08-28-automatic-account-resolution-design.md`

## Global Constraints

- Rust owns account resolution and all database writes.
- Keep `transactions.account_id` non-null; add no destructive migration.
- Queue only ambiguity or parser uncertainty, not zero account matches.
- Preserve unrelated dirty-worktree changes.
- PennyWise remains behavioral reference only.

---

### Task 1: Rust account resolver and SMS behavior

**Files:**
- Modify: `core/centwise-db/src/queries/accounts.rs`
- Modify: `core/centwise-ffi/src/core.rs`
- Test: `core/centwise-ffi/src/tests.rs`

**Interfaces:**
- Produces: deterministic IDs `auto-<provider>-wallet`, `auto-<provider>-<suffix>`, and `system-cash`.
- Produces: `Queries::resolve_or_create_account(provider_id, account_last4, preferred_name)`.

- [ ] **Step 1: Add failing tests** proving no-account SMS creation, account reuse, suffix separation, and unchanged ambiguity review.
- [ ] **Step 2: Run** `cargo test -p centwise-ffi ingestion_tests -- --nocapture` and confirm the new assertions fail for the old provider-only ID behavior.
- [ ] **Step 3: Implement** normalization plus idempotent account resolution in `accounts.rs`, and replace inline SMS account creation with the resolver.
- [ ] **Step 4: Re-run** the focused tests and confirm they pass.

### Task 2: Atomic manual insertion contract

**Files:**
- Modify: `core/centwise-ffi/src/types.rs`
- Modify: `core/centwise-ffi/src/conversions.rs`
- Modify: `core/centwise-ffi/src/core.rs`
- Test: `core/centwise-ffi/src/tests.rs`

**Interfaces:**
- Extends `TransactionInput` with `account_provider: Option<String>`, `account_name: Option<String>`, and `account_last_four: Option<String>`.
- `insert_transaction` resolves only an empty `account_id`; `update_transaction` requires an existing non-empty ID.

- [ ] **Step 1: Add failing tests** for `system-cash`, explicit provider creation, reuse, invalid supplied ID, and update rejection.
- [ ] **Step 2: Run** `cargo test -p centwise-ffi manual_account -- --nocapture` and confirm RED.
- [ ] **Step 3: Implement** atomic resolution and insertion inside one `database.write` closure.
- [ ] **Step 4: Run** the focused FFI tests and the database review/account tests.

### Task 3: Regenerate and wire native bindings

**Files:**
- Regenerate: `apps/android/app/src/main/kotlin/com/centwise/core/uniffi/centwise_ffi.kt`
- Regenerate: `apps/ios/Centwise/Core/FFI/generated/CentwiseCore.swift`
- Modify: `apps/android/app/src/main/kotlin/com/centwise/core/backend/CentwiseRustBackend.kt`
- Modify: `apps/android/app/src/main/kotlin/com/centwise/data/repository/TransactionRepository.kt`
- Modify: `apps/ios/Centwise/Core/FFI/CentwiseRustBackend.swift`
- Modify: `apps/ios/Centwise/Data/Repositories/TransactionRepository.swift`

**Interfaces:**
- Native inserts send an empty ID with Cash/provider hints when automatic resolution is requested.
- Native repository insertion methods return `Bool`.

- [ ] **Step 1: Build** `centwise-ffi` and regenerate committed Kotlin and Swift bindings using `core/README.md` commands.
- [ ] **Step 2: Update** both backend mappers with the new optional fields.
- [ ] **Step 3: Update** both repositories so success, reload, notifications, and dismissal depend on Rust returning success.
- [ ] **Step 4: Run** source checks proving both native mappers populate every generated field.

### Task 4: Native account-less forms

**Files:**
- Modify: `apps/ios/Centwise/Features/Transactions/Screens/AddEditTransactionView.swift`
- Modify: `apps/android/app/src/main/kotlin/com/centwise/features/transactions/AddEditTransactionSheet.kt`
- Modify: relevant save closures in `apps/android/app/src/main/kotlin/com/centwise/MainActivity.kt` and iOS callers.

**Interfaces:**
- Existing account selection passes its ID.
- No selection passes Cash/Unassigned hints and never invents a foreign-key ID.

- [ ] **Step 1: Add/run failing source boundary checks** for account-less Save and success-dependent dismissal.
- [ ] **Step 2: Implement** a native “Cash / Unassigned” option plus existing account choices.
- [ ] **Step 3: Ensure** failed saves remain visible and do not trigger success haptics/notifications.
- [ ] **Step 4: Re-run** source boundary checks.

### Task 5: Verification

**Files:** No production changes.

- [ ] **Step 1: Run** `cargo test --workspace`.
- [ ] **Step 2: Run** `cargo fmt --all -- --check` and `cargo clippy --workspace --all-targets -- -D warnings`.
- [ ] **Step 3: Run** Android compilation when installed Build Tools permit it; otherwise record the exact blocker.
- [ ] **Step 4: Run** `git diff --check` and inspect only scoped diffs.


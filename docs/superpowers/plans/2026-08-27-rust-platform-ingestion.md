# Rust-Owned Platform Ingestion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace duplicated Android/iOS SMS parsing, native transaction writes, and in-memory review queues with one tested Rust ingestion path exposed through UniFFI.

**Architecture:** Rust owns the parser, deduplication, account/category decisions, review queue, SQLite schema, and transaction writes. Native layers remain thin adapters for Android SMS/iOS Shortcut input, notifications, lifecycle, and UI invalidation.

**Tech Stack:** Rust, rusqlite, UniFFI 0.28.3, Kotlin/Android Gradle, Swift/XcodeGen/Xcode.

**Spec:** `docs/superpowers/specs/2026-08-27-rust-platform-ingestion-design.md`

## Global Constraints

- Preserve the existing Rust minor-unit money convention (`i64`, poisha for BDT).
- Do not copy PennyWise source, assets, schemas, or branding; use it only for behavior comparison.
- Never add real SMS or personal financial data to source or tests.
- Do not delete native code until the replacement path has tests and a passing platform build.
- Rust owns the single logical SQLite schema and migrations; native code does not create parallel tables.
- Native UI/ViewModels remain native and do not perform parsing, deduplication, balance arithmetic, or database filtering.

---

### Task 1: Repair and lock the Rust baseline

**Files:**
- Modify: `core/centwise-domain/Cargo.toml`
- Modify: `core/centwise-domain/src/lib.rs`
- Test: `core/centwise-domain/src/lib.rs`

**Interfaces:**
- Produces Serde-compatible `centwise_domain::TransactionType` for parser fixture models.

- [ ] Write a domain round-trip test that serializes and deserializes every `TransactionType` variant.
- [ ] Run `cargo test -p centwise-domain` and confirm it fails because the enum lacks Serde implementations.
- [ ] Add the minimal Serde dependency and derives to `TransactionType`.
- [ ] Run the domain test and then the full Rust workspace test.

### Task 2: Add Rust review-queue persistence and ingestion behavior

**Files:**
- Modify: `core/centwise-domain/src/lib.rs`
- Modify: `core/centwise-db/src/migrations.rs`
- Modify: `core/centwise-db/src/queries.rs`
- Modify: `core/centwise-db/src/lib.rs`
- Modify: `core/centwise-db/schemas/v2.sql`
- Create: `core/centwise-db/tests/ingestion_test.rs`
- Modify: `core/centwise-ffi/src/lib.rs`

**Interfaces:**
- `CentwiseCore.ingest_sms(body: String, sender_hint: Option<String>, occurred_at_epoch_ms: i64) -> Result<SmsIngestResult, CentwiseError>`.
- `CentwiseCore.list_review_queue(limit: u32) -> Result<Vec<ReviewQueueRecord>, CentwiseError>`.
- `CentwiseCore.dismiss_review_item(id: String) -> Result<bool, CentwiseError>`.
- `CentwiseCore.convert_review_item(id: String, account_id: String, category_id: String, title: String) -> Result<bool, CentwiseError>`.

- [ ] Add failing tests for parsed SMS insertion, reference deduplication, ambiguous-account review, review persistence after reopening, dismiss, and convert.
- [ ] Run the focused tests and confirm they fail because the methods/table do not exist.
- [ ] Add the v2 migration and schema snapshot with indexed reference/status columns.
- [ ] Implement the smallest database query methods with one transaction per ingest operation.
- [ ] Implement `ingest_sms` over the existing parser and database APIs; never select an arbitrary account when mapping is ambiguous.
- [ ] Expose typed UniFFI records/enums for ingest outcomes and review records.
- [ ] Run focused tests, full Rust tests, formatter, and clippy.

### Task 3: Generate and package bindings without native duplication

**Files:**
- Modify: `core/README.md`
- Modify: `apps/android/app/build.gradle.kts`
- Modify: `apps/ios/project.yml`
- Create: `apps/android/app/src/main/kotlin/com/centwise/rust/CentwiseRustRepository.kt`
- Create: `apps/ios/Centwise/Core/Rust/CentwiseRustRepository.swift`
- Create: platform binding/library output directories only through the documented build scripts.

**Interfaces:**
- Android adapter owns `CentwiseCore` and maps Rust records to `StateFlow` invalidation.
- iOS adapter owns `CentwiseCore` and maps Rust records to `ObservableObject`/Combine invalidation.

- [ ] Add adapter tests against the generated binding contract or a small injected core interface.
- [ ] Generate Kotlin and Swift bindings from the built `centwise-ffi` library.
- [ ] Build Android `arm64-v8a` and emulator ABI libraries and configure `jniLibs`.
- [ ] Build the iOS XCFramework on macOS and link it in the XcodeGen/Xcode project.
- [ ] Confirm app source imports the generated Rust module only through the adapter.

### Task 4: Wire Android SMS input and review UI

**Files:**
- Modify: `apps/android/app/src/main/kotlin/com/centwise/core/receiver/SmsBroadcastReceiver.kt`
- Modify: `apps/android/app/src/main/kotlin/com/centwise/core/scanner/HistoricalSmsScanner.kt`
- Modify: `apps/android/app/src/main/kotlin/com/centwise/features/transactions/ReviewQueueScreen.kt`
- Modify: Android tests for receiver/scanner/review behavior.
- Delete only after verification: `apps/android/app/src/main/kotlin/com/centwise/core/processor/SmsTransactionProcessor.kt`, native transaction SQLite/review implementations.

**Interfaces:**
- Receiver and scanner pass raw SMS input plus sender/time to `CentwiseRustRepository.ingestSms`.
- Review UI lists and mutates Rust review records.

- [ ] Add failing receiver/scanner tests proving the Rust adapter receives the raw input.
- [ ] Wire the adapter and keep notifications in the receiver/adapter boundary.
- [ ] Wire review list, convert, and dismiss to Rust APIs.
- [ ] Run JVM tests and Android debug build with the Rust library.
- [ ] Remove duplicate Android parser/queue/database code only after source audit shows no production references.

### Task 5: Wire iOS Shortcut/App Intent and review UI

**Files:**
- Modify: `apps/ios/Centwise/Core/Intents/ParseTransactionIntent.swift`
- Modify: `apps/ios/Centwise/App/CentwiseApp.swift`
- Modify: `apps/ios/Centwise/Features/Transactions/Screens/ReviewQueueView.swift`
- Modify: `apps/ios/project.yml`
- Delete only after verification: `apps/ios/Centwise/Core/Processor/SmsTransactionProcessor.swift`, native transaction SQLite/review implementations.

**Interfaces:**
- App Intent passes SMS text/sender/time to `CentwiseRustRepository.ingestSms`.
- Review UI lists and mutates Rust review records.

- [ ] Add failing Swift tests for parsed, duplicate, and review outcomes.
- [ ] Wire App Intent and app foreground reload to the Rust adapter.
- [ ] Wire review conversion and dismissal to Rust.
- [ ] Build in Xcode on macOS and run simulator tests; verify Shortcut ingestion on a physical device.
- [ ] Remove duplicate iOS parser/queue/database code only after Xcode/source verification.

### Task 6: Preserve data and complete the remaining Rust repository migration

**Files:**
- Modify: `core/centwise-db` schema/migrations and FFI query surface.
- Modify: platform repository adapters and screen models.
- Create: migration/backup compatibility tests for legacy native stores.

**Interfaces:**
- All account, transaction, budget, subscription, rule, analytics, export, and backup reads/writes use Rust FFI.

- [ ] Add legacy database import/backup tests before switching paths.
- [ ] Expose the remaining screen-shaped Rust queries and mutations.
- [ ] Replace platform repository internals with Rust adapters while preserving public UI behavior.
- [ ] Run Rust, Android, and iOS verification suites.
- [ ] Perform final source audit for native SQLite, parser regexes, in-memory deduplication, and in-memory review queues.

## Verification gate

Before claiming completion, run and record:

```text
cargo test --workspace
cargo fmt --check
cargo clippy --workspace --all-targets -- -D warnings
apps/android/gradlew.bat :app:assembleDebug
xcodebuild test -project apps/ios/Centwise.xcodeproj -scheme Centwise -destination 'platform=iOS Simulator,name=iPhone 15'
```

If Xcode is unavailable, report the exact limitation and do not claim iOS build or device verification.

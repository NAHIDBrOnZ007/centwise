# Rust Core SMS Intelligence Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver the approved Centwise Rust-only SMS intelligence and correctness upgrade without changing native source or the public UniFFI contract.

**Architecture:** Keep parsing, categorization, persistence, and orchestration in their existing Rust crates, but establish one ordered category-resolution pipeline. Add internal-only SQLite state for exact merchant mappings and provenance, then optimize ingestion without altering exported records or methods.

**Tech Stack:** Rust 2021, rusqlite, regex, UniFFI 0.28.3, JSON parser fixtures.

**Spec:** `docs/superpowers/specs/2026-09-02-rust-core-sms-intelligence-design.md`

## Global Constraints

- Modify only `core/**` and the Rust design/plan documents.
- Do not modify Kotlin, Swift, Android, iOS, or committed generated bindings.
- Do not change exported UniFFI signatures, records, or enums.
- Use append-only SQLite migrations and preserve existing user data.
- Keep all behavior deterministic, offline, Bangladesh-first, and BDT-oriented.
- Follow red-green TDD for every production behavior change.

---

### Task 1: System categories and append-only schema

**Files:**
- Modify: `core/centwise-domain/src/default_categories.rs`
- Modify: `core/centwise-db/src/migrations.rs`
- Create: `core/centwise-db/schemas/v4.sql`
- Modify: `core/centwise-db/tests/migrations_test.rs`
- Modify: `core/centwise-db/tests/demo_data_test.rs`

**Interfaces:**
- Produces: system category IDs `income`, `refunds`, `cashback`, `interest-profit`, `dividends`, `fees`, `cash-withdrawal`, `housing`, and `travel`; migration v4 mapping/provenance storage.

- [ ] Write migration tests proving old databases gain every new system category, a `merchant_category_mappings` table, and a nullable `category_source` transaction column without losing data.
- [ ] Run the focused migration tests and confirm the new assertions fail.
- [ ] Add categories and migration v4, and change seeding to insert missing system rows individually rather than returning when any category exists.
- [ ] Add the canonical `v4.sql` schema snapshot.
- [ ] Run database tests and confirm they pass.

### Task 2: One deterministic categorization engine

**Files:**
- Modify: `core/centwise-categorization/src/lib.rs`
- Modify: `core/centwise-parser/src/engine.rs`
- Modify: `core/centwise-parser/tests/fixture_tests.rs`
- Modify: `fixtures/sms/banks-generic.json`

**Interfaces:**
- Produces: internal categorization result containing category ID, matched merchant, and source; strong signal detection for salary, refund, cashback, interest/profit, fees, withdrawal, transfer, and safe type fallback.

- [ ] Write focused tests for salary false positives, Income/Other fallback, refund, cashback, interest/profit, fees, withdrawals, and existing Bangladesh merchants.
- [ ] Run categorization and parser tests and confirm the new cases fail for the expected branches.
- [ ] Implement strong-signal and merchant resolution in `centwise-categorization`, removing invalid category outputs.
- [ ] Make the parser extract facts and valid transaction types while leaving persisted precedence to ingestion.
- [ ] Run categorization and parser tests and confirm they pass.

### Task 3: Parser accuracy and cached extraction

**Files:**
- Modify: `core/centwise-normalization/src/text.rs`
- Modify: `core/centwise-parser/src/engine.rs`
- Modify: `core/centwise-parser/src/providers.rs`

**Interfaces:**
- Consumes: stable category IDs from Task 1.
- Produces: deterministic English transaction signals using the existing parser API.

- [ ] Run parser fixture tests and confirm they fail for unsupported signals.
- [ ] Cache regexes with standard-library lazy initialization and tighten provider aliases.
- [ ] Run normalization and parser tests and confirm they pass.

### Task 4: Exact merchant learning and ordered resolution

**Files:**
- Create: `core/centwise-db/src/queries/merchant_mappings.rs`
- Modify: `core/centwise-db/src/queries/mod.rs`
- Modify: `core/centwise-db/src/queries/transactions.rs`
- Modify: `core/centwise-ffi/src/core.rs`
- Modify: `core/centwise-ffi/src/tests.rs`

**Interfaces:**
- Produces: internal database operations `matching_merchant_category`, `upsert_merchant_category_mapping`, and ordered category resolution without any new UniFFI method.

- [ ] Add FFI integration tests showing a Foodpanda correction teaches future Foodpanda expenses, does not recategorize history, and never learns from provider-only titles.
- [ ] Run the focused FFI tests and confirm they fail because learning is absent.
- [ ] Add normalized exact mapping queries and indexes.
- [ ] During update, compare the stored category, parse the stored raw SMS, and upsert a mapping only for eligible auto-tracked merchant corrections.
- [ ] During ingestion, resolve mapping, Smart Rule, strong/system category, then Income/Other fallback and store internal provenance.
- [ ] Run database and FFI tests and confirm they pass.

### Task 5: Refunds, reported balances, metadata, and deduplication

**Files:**
- Modify: `core/centwise-db/src/queries/transactions.rs`
- Modify: `core/centwise-db/tests/queries_test.rs`
- Modify: `core/centwise-ffi/src/conversions.rs`
- Modify: `core/centwise-ffi/src/tests.rs`

**Interfaces:**
- Produces: correct refund deltas, monotonic reported-balance reconciliation, metadata-preserving edits, and stable normalized fallback SMS IDs.

- [ ] Add regression tests proving refunds increase balances and deletion/update correctly reverse their effects.
- [ ] Add tests proving a recent reported balance reconciles the account while an older imported SMS cannot overwrite it.
- [ ] Add tests proving omitted update metadata preserves raw SMS, fee, reported balance, reference, and auto-tracked state.
- [ ] Add deduplication tests for equivalent whitespace/casing and distinct senders when references are absent.
- [ ] Run focused tests and confirm each new assertion fails for the intended reason.
- [ ] Implement the smallest database and conversion changes that satisfy the tests without changing exported types.
- [ ] Run database and FFI tests and confirm they pass.

### Task 6: Review behavior and true batch ingestion

**Files:**
- Modify: `core/centwise-ffi/src/core.rs`
- Modify: `core/centwise-db/src/queries/rules.rs`
- Modify: `core/centwise-db/src/queries/review_queue.rs`
- Modify: `core/centwise-ffi/src/tests.rs`

**Interfaces:**
- Consumes: ordered resolver and mapping queries from Task 4.
- Produces: one internal ingestion routine usable for individual and batch operations, with cached rules/mappings and one batch write transaction.

- [ ] Add tests proving category uncertainty inserts Income/Other, structural ambiguity is queued, and batch results equal individual ingestion results.
- [ ] Add instrumentation-friendly tests proving the batch path loads reusable resolution data once and remains atomic on database errors.
- [ ] Run focused tests and confirm they fail against the per-message batch implementation.
- [ ] Extract a private ingestion routine that accepts preloaded resolution data and a shared `Queries` transaction context.
- [ ] Move pending review filtering/limiting into SQL and preserve current external result semantics.
- [ ] Run FFI and database tests and confirm they pass.

### Task 7: Full verification and contract guard

**Files:**
- Modify only files required to fix failures discovered by verification.

**Interfaces:**
- Produces: verified Rust workspace with unchanged native sources and generated bindings.

- [ ] Run `cargo fmt --all -- --check` from `core`.
- [ ] Run `cargo test --workspace` from `core` and confirm zero failures.
- [ ] Run `cargo clippy --workspace --all-targets -- -D warnings` from `core` and confirm zero warnings.
- [ ] Inspect `git diff -- apps/android apps/ios` and generated binding paths; confirm there are no changes.
- [ ] Review the implementation against every requirement in the design and close any tested gap.

# Rust-Owned Platform Ingestion Design

**Status:** Approved for implementation

## Goal

Make Android and iOS use one Rust-owned SMS ingestion and persistence path while keeping operating-system integration and UI native.

## Boundary

Rust owns normalization, parsing, provider detection, categorization, deduplication, account selection, transaction writes, review-queue persistence, migrations, and screen-shaped reads. Android and iOS own SMS/Shortcut capture, permissions, lifecycle, notifications, UI state, and file sharing.

The production path is:

```text
Android SMS Receiver / iOS App Intent
    -> CentwiseCore.ingest_sms(body, sender, timestamp)
    -> Rust parser and database transaction
    -> Rust transaction or persisted review item
    -> native invalidation callback and re-query
```

## Rust contract

Keep `parse_sms_message(body, sender_hint)` as a pure parser API for fixture and diagnostic tests. Add a higher-level `CentwiseCore.ingest_sms(body, sender_hint, occurred_at_epoch_ms)` API for production. It must perform parsing, reference deduplication in SQLite, safe account resolution, category selection, transaction insertion, or review-queue insertion. A message with no unambiguous account must enter review rather than being attributed to an arbitrary account.

Add a versioned review-queue table containing the raw SMS, sender, received timestamp, optional parser candidate fields, rejection reason, and status. Expose list, convert, and dismiss operations through Rust/UniFFI. Keep the raw body device-local and never add real SMS data to fixtures.

Preserve the existing minor-unit money convention. Native adapters must map `amount_minor` to the platform display model only at the UI boundary; no platform parser or balance arithmetic remains.

## Native contract

Each platform gets one small repository/adapter that owns a Rust core handle, supplies its database path, registers `ChangeListener`, invokes Rust APIs, and maps typed records to native screen models. Native processors become input adapters only, or are removed once all call sites use the adapter. No native SQLite schema, parser regex, in-memory reference set, or in-memory review queue remains in the production path.

Android uses an app-private Rust database path and packaged native libraries for supported ABIs. iOS uses the App Group container path so the app, Shortcut/App Intent, and future Share Extension share one database. The committed Xcode project or generated `project.yml` must link the Rust XCFramework and generated Swift bindings.

## Safe migration

Do not delete native code until Rust tests, adapter tests, and platform compilation pass. If an existing native database is detected, preserve it and perform an explicit one-time import or backup before switching the active path. Keep the migration observable and fail closed if account mapping is ambiguous.

## Verification

- Rust unit, fixture, migration, review-queue, deduplication, and FFI tests.
- Android JVM tests plus debug build with Rust `.so` libraries; emulator or physical-device SMS verification.
- iOS Swift tests and Xcode build on macOS with the XCFramework; real-device Shortcut/App Intent verification for carrier SMS.
- Source audit proving app code references the Rust adapter and no removed native parser/database/queue symbols remain in production call sites.

## Out of scope for the first vertical slice

Manual account, budget, subscription, rule, and analytics screen CRUD will migrate after the Rust SMS/transaction/review path is green. Their final storage contract remains Rust-owned, but they must not be deleted before corresponding FFI queries and mutations exist.

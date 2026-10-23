# Decision 0001: Single Rust-Owned Database with Native ViewModels

Date: 2026-08-22
Status: Accepted

## Context

Centwise originally planned separate platform databases: Room on Android and a
separate SQLite implementation on iOS, each with its own migration code for the
same logical model. The presentation layer question was also open: share
ViewModels via Rust (UniFFI async or Crux), keep them native, or introduce
Kotlin Multiplatform for ViewModels alongside the Rust parser.

Problems with the dual-database plan:

- Every schema change requires two migration implementations, two test suites,
  and two backup-compatibility checks.
- Two implementations of the same logical model can drift, which breaks
  cross-platform backup restore and CSV export correctness.
- Room provides no benefit once all database access goes through the Rust FFI,
  because no Kotlin code touches the database directly.

Problems with shared ViewModels (Rust or KMP):

- Sharing ViewModels requires hand-built state streaming across the FFI or a
  third toolchain and runtime (Kotlin/Native GC next to Swift ARC and Rust),
  while the ViewModel is the thinnest, cheapest layer to duplicate.
- The valuable shared logic inside ViewModels is business logic (aggregation,
  filtering, money arithmetic), which belongs in queries, not in presentation.

## Decision

1. One shared SQLite database owned by the Rust core (`centwise-db` crate).
2. Migrations are written once in Rust and run by a `user_version`-based
   migration runner. A schema snapshot is committed per released version for
   pull-request review. No Room, no platform-side migration code.
3. All writes go through Rust functions so balance updates stay transactional
   and data-change notifications fire reliably.
4. Reactivity: the Rust core exposes data-change notifications through a UniFFI
   callback interface. Android wraps them as StateFlow invalidation; iOS wraps
   them as Combine/ObservableObject invalidation. The database file uses WAL
   mode with a busy timeout and lives in a platform-provided location (App Group
   container on iOS for app, App Intents, and Share Extension access).
5. ViewModels stay native per platform and thin. ViewModels contain no money
   arithmetic, date logic, or filtering; those are Rust query functions
   returning screen-shaped structs. No Kotlin Multiplatform.
6. Platform repositories call the Rust database through the FFI; screens never
   touch storage directly.

## Consequences

### Backend hardening, 2026-10-23

- Database writes notify observers once after a successful commit and after
  releasing the connection lock. Rollbacks do not emit committed-data events.
  Observer callbacks run without holding the observer-list lock and may re-query.
- Migration startup rejects a database newer than this build before running
  migrations or seeding. The schema remains v8; no stored fields or serialized
  backup contracts change. A SQLite snapshot/reopen regression checks preservation
  of transaction amounts and account balances, including committed WAL data.
- Card receipts require exactly one active provider account for automatic
  association. Batch scans queue ambiguous receipts just as single-message
  ingestion does. Storage errors propagate rather than becoming review entries.
- Android/iOS parity: these guarantees live in shared Rust. Device scans and
  native packaging still require platform verification.

Read-only follow-up findings: the Android scanner catches inbox-read exceptions
and can persist scan progress after a partial read; it also collects the inbox
in memory before batching. Cross-provider recharge deduplication still relies on
amount/time proximity and merits stronger recipient evidence. Those paths are
not certified by these fixes. Native backup/export workflows were not exercised;
the snapshot test verifies SQLite data compatibility only.

- Schema and migration work is single-sourced; cross-platform backup
  compatibility holds by construction.
- We build and maintain the change-notification seam ourselves (a bounded,
  one-time cost) instead of using Room/GRDB invalidation.
- Screen query functions live in Rust, so UI-driven data changes may require
  Rust changes; queries are kept screen-shaped and versioned to bound this.
- Cross-process writes (iOS extensions) are detected via the change
  notification within a process and re-query on foreground for other processes.

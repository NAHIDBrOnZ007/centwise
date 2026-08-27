# Centwise Core (Rust)

Shared engine for Android and iOS: one SQLite database, one migration runner,
with SMS ingestion, review-queue persistence, and demo-data operations owned by
Rust. See
[`../docs/decisions/0001-single-rust-database.md`](../docs/decisions/0001-single-rust-database.md).

## Crates

| Crate | Purpose |
|---|---|
| `centwise-domain` | Shared models. Money = `i64` minor units (poisha). Dates = epoch millis. |
| `centwise-db` | SQLite via `rusqlite` (bundled). WAL mode, `user_version` migrations, screen queries, change notifications. |
| `centwise-ffi` | UniFFI surface for Kotlin and Swift. |
| `uniffi-bindgen` | Binding generator binary. |

## Verify (works on Windows)

```bash
cd core
cargo test --workspace
cargo fmt --check
cargo clippy --workspace --all-targets -- -D warnings
```

> The Rust toolchain was not available when this workspace was authored.
> Run `cargo test` before trusting it; `rustup` install: https://rustup.rs

## Generate bindings

### Android (works on Windows)

Verified command pattern (2026-08-22, host build):

```bash
cd core
cargo build -p centwise-ffi   # host build proves the pipeline
cargo run -p uniffi-bindgen -- generate \
  --library target/debug/centwise_ffi.dll \
  --language kotlin \
  --config centwise-ffi/uniffi.toml \
  --out-dir ../apps/android/app/src/main/kotlin
```

For the real app, cross-compile Android targets (requires the Android NDK
and linker configuration for `aarch64-linux-android` etc.), then run the same
`generate` command against each Android `.so`. The generated Kotlin file is
committed under `apps/android/app/src/main/kotlin/com/centwise/core/uniffi/`;
the matching `.so` files belong under `apps/android/app/src/main/jniLibs/<abi>/`.

### iOS (requires macOS)

```bash
cargo build --release --target aarch64-apple-ios
cargo run -p uniffi-bindgen -- generate \
  --library target/aarch64-apple-ios/release/libcentwise_ffi.a \
  --language swift \
  --config centwise-ffi/uniffi.toml \
  --out-dir ../apps/ios/Centwise/Core/FFI/generated
```

(Swift binding generation itself was verified from Windows; only the iOS
library compile and Xcode wiring need a Mac.)

The generated Swift/header/modulemap files are committed under
`apps/ios/Centwise/Core/FFI/generated/`. Build the iOS static library into
`apps/ios/Centwise/Core/FFI/lib/libcentwise_ffi.a`; `project.yml` already adds
the include and library search paths.

`CentwiseCore.ingestSms` is the single SMS entry point. Android calls it from
the SMS receiver/scanner and iOS calls it from the Shortcut/App Intent. The
platforms must not parse SMS, write SQLite, or maintain an in-memory review
queue around that call.

## Native platform adapters

Android and iOS initialize `CentwiseCore` with their platform-owned database
path. SMS ingestion, parsing, persistence, review-queue durability, and demo
data are Rust-owned; the native layers only adapt FFI records into UI models.

`CentwiseCore.loadDemoData()` explicitly replaces the current user records with
the deterministic Rust demo dataset. `resetToEmptyDatabase()` removes those
records while preserving system categories. Both operations are available from
each platform's data-management screen and are intended for local development
and QA only.

`CentwiseCore.listCategories()` is the canonical category read used by both
platforms. System categories are seeded by the Rust migration and are never
deleted by a user-data reset.

The generated Kotlin and Swift bindings are committed under the app folders.
Regenerate them whenever the public `centwise-ffi` API changes.

## Schema changes

1. Append a new `Migration` in `centwise-db/src/migrations.rs` with the next
   version — never edit a shipped migration.
2. Add a test in `centwise-db/tests/` that opens a v1 database fixture and
   upgrades.
3. Regenerate the snapshot: extract the new SQL into
   `centwise-db/schemas/vN.sql` and commit it for review.

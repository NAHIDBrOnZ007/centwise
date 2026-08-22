# Centwise Core (Rust)

Shared engine for Android and iOS: one SQLite database, one migration runner,
all writes through Rust. See
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
  --out-dir /tmp/bindings-preview
```

For the real app, cross-compile Android targets (requires the Android NDK
and linker configuration for `aarch64-linux-android` etc.), then run the same
`generate` command against the Android `.so` and copy the output into
`apps/android/app/src/main/kotlin/com/centwise/core/ffi/generated/`, plus add
`jniLibs` in `apps/android/app/build.gradle.kts`.

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

Add the generated `.modulemap`/`.swift` files and the static library to the
Xcode target (Xcode 16+ supports bundled frameworks via `SWIFT_INCLUDE_PATHS`).

## Native reactive wrappers

Paste after generating bindings. The app keeps building with fake data until
you flip `USE_RUST_BACKEND`.

### Android (`core/ffi/CentwiseDataWatcher.kt`, package `com.centwise.core.ffi`)

```kotlin
object CentwiseBackend {
    const val USE_RUST_BACKEND = false

    lateinit var core: com.centwise.core.CentwiseCore
        private set

    fun initialize(path: String) {
        core = com.centwise.core.CentwiseCore.open(path)
    }
}

class CentwiseDataWatcher(private val core: com.centwise.core.CentwiseCore) {
    private val _changeTick = kotlinx.coroutines.flow.MutableStateFlow(0)
    val changeTick: kotlinx.coroutines.flow.StateFlow<Int> = _changeTick.asStateFlow()

    private val listener = object : com.centwise.core.ChangeListener {
        override fun onDataChanged() {
            _changeTick.update { it + 1 }
        }
    }

    init { core.addListener(listener) }
}
```

In `MainActivity.onCreate` (after `super.onCreate`):

```kotlin
if (CentwiseBackend.USE_RUST_BACKEND) {
    CentwiseBackend.initialize(filesDir.resolve("centwise.db").absolutePath)
}
```

### iOS (`Core/FFI/CentwiseBackend.swift`)

```swift
import Foundation
import CentwiseCore
import Combine

enum CentwiseBackend {
    static let useRustBackend = false
    static var core: CentwiseCore?

    static func initialize() {
        let url = FileManager.default
            .containerURL(forSecurityApplicationGroupIdentifier: "group.com.centwise.shared")?
            .appendingPathComponent("centwise.db")
        // Fallback to documents when the App Group is not yet configured.
        core = try? CentwiseCore.open(
            url?.path ?? FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
                .appendingPathComponent("centwise.db").path
        )
    }
}

final class CentwiseDataWatcher: ObservableObject {
    let subject = PassthroughSubject<Void, Never>()
    private var handle: ChangeListener?

    init(core: CentwiseCore) {
        let sink = subject
        let listener = RustChangeListener()
        listener.onDataChanged = { sink.send() }
        handle = listener
        core.addListener(listener)
    }
}

private final class RustChangeListener: ChangeListener {
    var onDataChanged: (() -> Void)?
    func on_data_changed() { onDataChanged?() }
}
```

(App Group container on iOS lets the app, App Intents, and Share Extension
share one database file in WAL mode — see decision record.)

## Swapping the Home screen (end-to-end proof)

After generating bindings and setting `USE_RUST_BACKEND = true`:

**Android `HomeViewModel`** — replace repository flows with:

```kotlin
val watcher = CentwiseDataWatcher(CentwiseBackend.core)

val homeState = watcher.changeTick.flatMapLatest {
    flow {
        val monthStart = /* first day of month, epoch ms */
        val dashboard = CentwiseBackend.core.homeDashboard(
            startEpochMs = monthStart,
            endEpochMs = Long.MAX_VALUE,
            recentLimit = 5
        )
        emit(dashboard)
    }
}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
```

**iOS `HomeViewModel`** — on watcher subject, re-query
`CentwiseBackend.core.homeDashboard(...)` and map to published properties.

## Schema changes

1. Append a new `Migration` in `centwise-db/src/migrations.rs` with the next
   version — never edit a shipped migration.
2. Add a test in `centwise-db/tests/` that opens a v1 database fixture and
   upgrades.
3. Regenerate the snapshot: extract the new SQL into
   `centwise-db/schemas/vN.sql` and commit it for review.

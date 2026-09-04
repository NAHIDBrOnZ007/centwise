# Rust Platform Build and Runtime Verification Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build, package, link, and verify the shared Centwise Rust core in both the Android and iOS applications without restoring platform-side parser, database, category, or review-queue implementations.

**Architecture:** Rust is compiled once per mobile target and exposed through the committed UniFFI Kotlin and Swift bindings. Android and iOS retain only platform responsibilities—permissions, lifecycle, UI, and the platform-provided database path—while SMS ingestion, parsing, categorization, SQLite migrations, writes, queries, demo data, review-queue persistence, and system-category reads remain Rust-owned. The generated bindings are source artifacts; each platform still needs its own native Rust library at build time.

**Tech Stack:** Rust stable, Cargo, `centwise-ffi`, UniFFI 0.28.3, Android NDK, `cargo-ndk`, JDK 17, Gradle wrapper, Kotlin/Jetpack Compose, macOS/Xcode, Swift/SwiftUI, and XcodeGen.

**Spec:** `docs/decisions/0001-single-rust-database.md` and `core/README.md`

## Global Constraints

- Rust owns deterministic shared logic, the shared SQLite schema, migrations, writes, and screen queries.
- Native UI code does not parse SMS, write SQLite tables, maintain an in-memory review queue, or maintain a duplicate system-category catalog.
- The platform passes the database path to Rust; Rust does not choose an Android or iOS storage location.
- Android uses the repository Gradle wrapper and Java 17.
- iOS builds require macOS and Xcode; Windows cannot compile or run the iOS target.
- Only anonymized SMS fixtures may be used for tests; never use real names, phone numbers, account numbers, card numbers, OTPs, transaction IDs, or balances.
- Pennywise is reference-only and is outside this plan; do not copy or modify it.
- Do not commit signing keys, provisioning profiles, API tokens, or local SDK paths.

---

## Definition of Done

- [ ] Rust workspace tests, Clippy, build, and targeted formatting pass.
- [ ] UniFFI generated Kotlin and Swift sources match the current `centwise-ffi` API.
- [ ] Android packages the Rust `.so` for every supported ABI and launches without `UnsatisfiedLinkError`.
- [ ] iOS links the Rust library for the selected simulator or device and launches without a missing-library linker or `dyld` error.
- [ ] Both apps display the 11 Rust-seeded system categories through `listCategories()`.
- [ ] Loading demo data creates Rust-owned records and resetting data removes those records while preserving system categories.
- [ ] An anonymized SMS reaches `CentwiseCore.ingestSms` from each platform and produces a transaction or persisted review item.
- [ ] Build and runtime evidence is recorded separately for Rust, Android, and iOS.

## Current Repository Contract

These paths are part of the current project wiring and must not be changed casually:

| Item | Required location or entry point |
|---|---|
| Rust FFI crate | `core/centwise-ffi` |
| Android generated binding | `apps/android/app/src/main/kotlin/com/centwise/core/uniffi/centwise_ffi.kt` |
| Android native libraries | `apps/android/app/src/main/jniLibs/arm64-v8a/libcentwise_ffi.so`, `armeabi-v7a/libcentwise_ffi.so`, `x86_64/libcentwise_ffi.so`, and `x86/libcentwise_ffi.so` |
| Android native-library source set | `apps/android/app/build.gradle.kts` → `src/main/jniLibs` |
| iOS generated Swift/header/modulemap | `apps/ios/Centwise/Core/FFI/generated` |
| iOS native library | `apps/ios/Centwise/Core/FFI/lib/libcentwise_ffi.a` |
| iOS project settings | `apps/ios/project.yml` and generated `Centwise.xcodeproj` |
| Rust category API | `CentwiseCore.listCategories()` / `CentwiseCore.list_categories()` |
| Rust SMS API | `CentwiseCore.ingestSms()` / `CentwiseCore.ingest_sms()` |
| Rust reset API | `CentwiseCore.resetToEmptyDatabase()` / `CentwiseCore.reset_to_empty_database()` |

The current iOS project links `-lcentwise_ffi` from `Centwise/Core/FFI/lib`. A simulator library and a device library are different platform slices; replace that file with the slice matching the destination being tested. Do not copy a Windows `.dll` or Android `.so` into the iOS library directory.

---

### Task 1: Verify the Rust core and generated API before platform work

**Files:**
- Read: `core/Cargo.toml`
- Read: `core/centwise-ffi/src/lib.rs`
- Read: `apps/android/app/src/main/kotlin/com/centwise/core/uniffi/centwise_ffi.kt`
- Read: `apps/ios/Centwise/Core/FFI/generated/CentwiseCore.swift`

**Interfaces:**
- Consumes: the checked-in Rust FFI API and generated native bindings.
- Produces: a clean baseline before any platform-native artifact is rebuilt.

- [ ] **Step 1: Run the complete Rust test suite.**

From `core`:

```powershell
cargo +stable-x86_64-pc-windows-gnu test --workspace
```

Expected: all tests pass, including the category, demo-data, review-queue, parser, and FFI tests.

- [ ] **Step 2: Run Rust lint, build, and targeted formatting.**

```powershell
cargo +stable-x86_64-pc-windows-gnu clippy --workspace --all-targets -- -D warnings
cargo +stable-x86_64-pc-windows-gnu build --workspace
rustfmt --edition 2021 --check centwise-domain/src/lib.rs centwise-db/src/queries.rs centwise-db/src/lib.rs centwise-ffi/src/lib.rs
```

Expected: each command exits successfully. A full-workspace formatting failure in an unrelated pre-existing file must be reported rather than hidden or fixed as unrelated cleanup.

- [ ] **Step 3: Confirm binding parity.**

```powershell
rg -n "listCategories|CategoryRecord|ingestSms|resetToEmptyDatabase" `
  ..\apps\android\app\src\main\kotlin\com\centwise\core\uniffi\centwise_ffi.kt `
  ..\apps\ios\Centwise\Core\FFI\generated\CentwiseCore.swift
```

Expected: both generated bindings contain the current FFI operations and `CategoryRecord`.

---

### Task 2: Build and package the Android Rust libraries on Windows

**Files:**
- Create: `apps/android/app/src/main/jniLibs/arm64-v8a/libcentwise_ffi.so`
- Create: `apps/android/app/src/main/jniLibs/armeabi-v7a/libcentwise_ffi.so`
- Create: `apps/android/app/src/main/jniLibs/x86_64/libcentwise_ffi.so`
- Create: `apps/android/app/src/main/jniLibs/x86/libcentwise_ffi.so`
- Read: `apps/android/app/build.gradle.kts`

**Interfaces:**
- Consumes: `core/centwise-ffi` and the Android NDK toolchains.
- Produces: ABI-specific Rust shared libraries consumed by the Android app through JNI/UniFFI.

- [ ] **Step 1: Install and verify Android prerequisites.**

Install Android Studio with Android SDK Platform 35, Android SDK Build-Tools, and an installed NDK from SDK Manager. Install JDK 17 and make sure Android Studio uses it as its Gradle JDK.

From PowerShell, verify:

```powershell
java -version
adb version
Get-ChildItem "$env:LOCALAPPDATA\Android\Sdk\ndk" -Directory
```

Expected: Java reports version 17, `adb` reports a version, and at least one NDK directory is listed.

- [ ] **Step 2: Install the Rust Android targets and `cargo-ndk`.**

From `core`:

```powershell
rustup target add aarch64-linux-android armv7-linux-androideabi x86_64-linux-android i686-linux-android
cargo install cargo-ndk --locked
```

Expected: all four Rust targets are installed and `cargo ndk --version` succeeds.

- [ ] **Step 3: Build all Android ABIs into the checked-in app layout.**

From `core`:

```powershell
cargo ndk -t arm64-v8a -t armeabi-v7a -t x86_64 -t x86 `
  -o ..\apps\android\app\src\main\jniLibs `
  build --release -p centwise-ffi
```

Expected files:

```text
apps/android/app/src/main/jniLibs/arm64-v8a/libcentwise_ffi.so
apps/android/app/src/main/jniLibs/armeabi-v7a/libcentwise_ffi.so
apps/android/app/src/main/jniLibs/x86_64/libcentwise_ffi.so
apps/android/app/src/main/jniLibs/x86/libcentwise_ffi.so
```

- [ ] **Step 4: Check that no Windows library was accidentally packaged.**

```powershell
Get-ChildItem ..\apps\android\app\src\main\jniLibs -Recurse -File |
  Select-Object FullName
```

Expected: only ABI directories containing `libcentwise_ffi.so`; no `.dll`, `.a`, or files outside an ABI directory.

---

### Task 3: Regenerate and verify Android bindings when the FFI API changes

**Files:**
- Modify: `apps/android/app/src/main/kotlin/com/centwise/core/uniffi/centwise_ffi.kt`
- Read: `core/centwise-ffi/uniffi.toml`

**Interfaces:**
- Consumes: a host Rust build at `core/target/debug/centwise_ffi.dll`.
- Produces: the committed Kotlin binding at the exact package path above.

- [ ] **Step 1: Build the host library used by binding generation.**

From `core`:

```powershell
cargo +stable-x86_64-pc-windows-gnu build -p centwise-ffi
```

- [ ] **Step 2: Regenerate Kotlin into the source root, not a nested package directory.**

```powershell
cargo +stable-x86_64-pc-windows-gnu run -p uniffi-bindgen -- generate `
  --library target/debug/centwise_ffi.dll `
  --language kotlin `
  --config centwise-ffi/uniffi.toml `
  --out-dir ..\apps\android\app\src\main\kotlin
```

Expected: the output remains at `com/centwise/core/uniffi/centwise_ffi.kt`, with no nested `com/centwise/core/uniffi/com/...` directory.

- [ ] **Step 3: Compile and test the Android project.**

From `apps/android`:

```powershell
.\gradlew.bat test --no-daemon --console=plain
.\gradlew.bat :app:assembleDebug --no-daemon --console=plain
```

Expected: Kotlin unit tests and the debug APK build pass. If the command stops at `JAVA_HOME is not set`, install JDK 17 and configure Android Studio/Gradle; that is an environment failure, not evidence that the Kotlin source is correct.

---

### Task 4: Build and package the iOS Rust library on macOS

**Files:**
- Create or replace: `apps/ios/Centwise/Core/FFI/lib/libcentwise_ffi.a`
- Read: `apps/ios/project.yml`
- Read: `apps/ios/Centwise/Core/FFI/generated/CentwiseCore.swift`

**Interfaces:**
- Consumes: `core/centwise-ffi` and Apple Rust targets.
- Produces: an iOS static library that Xcode can link through the existing `-lcentwise_ffi` setting.

- [ ] **Step 1: Install and verify macOS prerequisites.**

Install Xcode and its command-line tools, then install Rust with `rustup`. From the repository root:

```bash
xcode-select -p
xcodebuild -version
cargo --version
rustc --version
```

Expected: Xcode and Cargo report installed versions.

- [ ] **Step 2: Install the iOS Rust targets.**

```bash
rustup target add aarch64-apple-ios aarch64-apple-ios-sim
```

For an Intel Mac, also install `x86_64-apple-ios` for an Intel simulator.

- [ ] **Step 3: Build the iOS libraries using the automated script.**

From repository root:

```bash
./scripts/build_ios_libs.sh
```

Alternatively, manually from `core`:

```bash
IPHONEOS_DEPLOYMENT_TARGET=16.0 cargo build --release -p centwise-ffi --target aarch64-apple-ios-sim
IPHONEOS_DEPLOYMENT_TARGET=16.0 cargo build --release -p centwise-ffi --target x86_64-apple-ios
IPHONEOS_DEPLOYMENT_TARGET=16.0 cargo build --release -p centwise-ffi --target aarch64-apple-ios
```

> **Note:** Always keep `IPHONEOS_DEPLOYMENT_TARGET=16.0` (also configured via `.cargo/config.toml`) so C dependencies like SQLite do not default to newer host SDK versions and trigger linker warnings.

---

### Task 5: Regenerate and verify Swift bindings when the FFI API changes

**Files:**
- Modify: `apps/ios/Centwise/Core/FFI/generated/CentwiseCore.swift`
- Modify: `apps/ios/Centwise/Core/FFI/generated/CentwiseCoreFFI.h`
- Modify: `apps/ios/Centwise/Core/FFI/generated/CentwiseCoreFFI.modulemap`

**Interfaces:**
- Consumes: the macOS Rust static library and `core/centwise-ffi/uniffi.toml`.
- Produces: the committed Swift UniFFI wrapper and low-level FFI declarations.

- [ ] **Step 1: Generate Swift from the macOS library.**

From `core`:

```bash
cargo run -p uniffi-bindgen -- generate \
  --library target/aarch64-apple-ios-sim/release/libcentwise_ffi.a \
  --language swift \
  --config centwise-ffi/uniffi.toml \
  --out-dir ../apps/ios/Centwise/Core/FFI/generated
```

Expected: the generated files contain `listCategories`, `CategoryRecord`, `ingestSms`, and `resetToEmptyDatabase`.

- [ ] **Step 2: Regenerate the Xcode project if `project.yml` changed.**

From `apps/ios`:

```bash
xcodegen generate
```

Expected: the generated project keeps the FFI generated directory in Swift/header search paths and keeps `Centwise/Core/FFI/lib` in the library search path with `-lcentwise_ffi`.

- [ ] **Step 3: Confirm the Swift wrapper is included in the application target.**

```bash
xcodebuild -project Centwise.xcodeproj -scheme Centwise -showBuildSettings |
  grep -E 'SWIFT_INCLUDE_PATHS|HEADER_SEARCH_PATHS|LIBRARY_SEARCH_PATHS|OTHER_LDFLAGS'
```

Expected: all four settings point at the generated FFI directory/library path and include `-lcentwise_ffi`.

---

### Task 6: Build and run the iOS application in Xcode

**Files:**
- Read: `apps/ios/Centwise.xcodeproj`
- Read: `apps/ios/project.yml`

**Interfaces:**
- Consumes: the matching simulator/device Rust library and generated Swift bindings.
- Produces: an iOS app that links and executes the Rust core.

- [ ] **Step 1: Open the project, not just the folder.**

Open `apps/ios/Centwise.xcodeproj` in Xcode. If it does not exist, run `xcodegen generate` from `apps/ios` first. Opening the folder alone does not create an executable app target.

- [ ] **Step 2: Select a simulator or connected device matching the Rust library slice.**

Use the simulator when `libcentwise_ffi.a` was built for `aarch64-apple-ios-sim`; use a physical iPhone when it was built for `aarch64-apple-ios`. Do not test a device with the simulator library or a simulator with the device-only library.

- [ ] **Step 3: Build from Xcode.**

Select the `Centwise` scheme and press Build/Run. A command-line build may also be used:

```bash
xcodebuild -project Centwise.xcodeproj -scheme Centwise -destination 'generic/platform=iOS Simulator' build
```

Expected: no `library not found for -lcentwise_ffi`, `Undefined symbols`, `module not found`, or `dyld: Library not loaded` errors.

- [ ] **Step 4: Run the category and reset smoke test.**

1. Open Settings → Categories and confirm the list is populated from Rust.
2. Open Data Management → Load Demo Data and confirm deterministic records appear.
3. Use Reset Data and confirm user/demo records disappear.
4. Return to Categories and confirm the Rust system categories remain.

Expected: the reset does not delete the system category rows.

---

### Task 7: Prove the runtime call path on both platforms

**Files:**
- Read: `apps/android/app/src/main/kotlin/com/centwise/core/backend/CentwiseRustBackend.kt`
- Read: `apps/android/app/src/main/kotlin/com/centwise/core/processor/SmsTransactionProcessor.kt`
- Read: `apps/ios/Centwise/Core/FFI/CentwiseRustBackend.swift`
- Read: `apps/ios/Centwise/Core/Processor/SmsTransactionProcessor.swift`

**Interfaces:**
- Consumes: the running Android/iOS app and an anonymized fixture under `fixtures/`.
- Produces: runtime evidence that parsing and persistence execute in Rust rather than in native duplicate code.

- [ ] **Step 1: Verify the source path before using a device.**

The Android and iOS SMS processor classes must delegate to `CentwiseRustBackend`, and the backend must call generated `CentwiseCore.ingestSms`. The native processors must not contain amount extraction, merchant extraction, categorization, SQL, or review-queue persistence logic.

```powershell
rg -n "CentwiseRustBackend|ingestSms|ingestSMS|parse|SQLite|SELECT|INSERT|ReviewQueue" `
  apps/android/app/src/main/kotlin/com/centwise/core/processor `
  apps/ios/Centwise/Core/Processor
```

Expected: processor code delegates to the Rust backend; parser and database implementation details are in `core`.

- [ ] **Step 2: Exercise an Android SMS fixture.**

On an emulator that supports injected SMS, use an anonymized fixture and the emulator SMS command:

```bash
adb emu sms send bKash "Payment Tk 850.00 to Foodpanda successful. Fee Tk 0.00. Balance Tk 7,558.00. TrxID XY98ZW76VU at 22/08/2026 17:20"
```

The fixture body must come from the repository’s anonymized test data and must not contain real personal data. On a physical device, send the same safe fixture from a test handset or use the historical scanner with the test message.

Expected: the app creates one Rust transaction or one persisted Rust review-queue item; a duplicate reference does not create a second record.

- [ ] **Step 3: Exercise an iOS Shortcut/App Intent fixture.**

Run the Centwise Parse Transaction Shortcut/App Intent with an anonymized fixture body. Confirm it reaches the same `CentwiseCore.ingestSms` path and produces one transaction or persisted review item.

Expected: the app and the App Intent see the same Rust-owned database when both use the configured shared-container path.

- [ ] **Step 4: Use a debugger breakpoint as direct FFI evidence.**

Set a breakpoint in `CentwiseRustBackend.listCategories()` or `CentwiseRustBackend.ingestSms/ingestSMS` on the relevant platform, then perform the category load or SMS test.

Expected: execution enters the native adapter and then the generated UniFFI method. A successful screen render without this call-path check is not sufficient proof that the app is using the intended Rust library.

---

### Task 8: Record build evidence and keep the integration maintainable

**Files:**
- Modify: `docs/STATUS.md`
- Modify: `core/README.md`
- Optional create: `docs/decisions/0002-rust-mobile-artifact-packaging.md` when the team accepts a permanent XCFramework/Android artifact policy

**Interfaces:**
- Consumes: command output and runtime results from Tasks 1–7.
- Produces: reproducible handoff information for the next developer.

- [ ] **Step 1: Record exact tool versions.**

Record the output of:

```text
rustc --version
cargo --version
java -version
adb version
./gradlew --version
xcodebuild -version
```

Keep secrets and personal device information out of the record.

- [ ] **Step 2: Record platform artifact coverage.**

Document which Android ABIs were built, whether the iOS artifact is simulator or device, and the exact test destination. Do not say “Android verified” or “iOS verified” when only Rust tests passed.

- [ ] **Step 3: Record known limitations honestly.**

If a platform cannot be tested on the current host, write the blocking prerequisite—Java/Android SDK/NDK for Android or macOS/Xcode for iOS—and leave the Rust/source verification results intact.

- [ ] **Step 4: Keep future FFI changes synchronized.**

Whenever `core/centwise-ffi/src/lib.rs` changes:

1. Run Rust tests and Clippy.
2. Regenerate Kotlin and Swift bindings.
3. Review the generated diff for the intended API only.
4. Rebuild Android `.so` files and the iOS library.
5. Run both platform compile checks before changing UI call sites.

Expected: no platform app is allowed to compile against stale generated bindings or a stale Rust binary.

## Review Checklist for Any Developer

- [ ] I opened `AGENTS.md` and stayed inside `centwise/`.
- [ ] I did not touch `pennywiseai-tracker/`.
- [ ] I can identify the Rust FFI method used by each changed native call site.
- [ ] I verified the native library matches the target platform and ABI.
- [ ] I ran Rust tests separately from Android/iOS build checks.
- [ ] I tested reset semantics: demo/user data is removed, system categories remain.
- [ ] I used only anonymized SMS fixtures.
- [ ] I did not claim an iOS or Android runtime test from a Windows Rust build alone.

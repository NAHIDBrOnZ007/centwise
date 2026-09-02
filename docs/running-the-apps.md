# Running the Apps (Rust-Backed Verification Guide)

How to build and run the Android and iOS apps against the shared Rust core.

Both apps use Rust-owned SQLite for categories, Smart Rules, transactions,
budgets, subscriptions, accounts, demo data, and the review queue. Native
repositories only adapt Rust records into UI state. The Rust mobile library
must be built and packaged before launching either app.

See [`docs/rust-mobile-build.md`](rust-mobile-build.md) for the authoritative
Android `.so` and iOS `.a` packaging checklist. Generated native libraries are
ignored by Git and must be rebuilt on a new machine or clean checkout.

## Android (works on this Windows PC)

### Requirements

- Android Studio: https://developer.android.com/studio
- JDK 17 selected as the Gradle JDK
- Android SDK Platform 35, Build Tools 35.0.0, and an installed NDK
- Rust stable, Android targets, and `cargo-ndk`

### Option A — Android Studio (easiest)

1. Build the Rust libraries from `core`:

   ```bash
   rustup target add aarch64-linux-android armv7-linux-androideabi x86_64-linux-android i686-linux-android
   cargo install cargo-ndk --locked
   cargo ndk -t arm64-v8a -t armeabi-v7a -t x86_64 -t x86 \
     -o ../apps/android/app/src/main/jniLibs \
     build --release -p centwise-ffi
   ```

2. Open Android Studio → **Open** → select `apps/android`
3. Confirm Android Studio uses JDK 17 for Gradle
4. Wait for Gradle sync to finish (first sync takes a few minutes)
5. Device Manager → create any emulator (e.g. Pixel 7, API 34+)
   — or plug in a real phone with USB debugging enabled
4. Press **▶ Run** (`Shift+F10`)

### Option B — Command line

```bash
cd apps/android
./gradlew :app:assembleDebug
# APK lands in app/build/outputs/apk/debug/app-debug.apk
# Install on a connected phone:
./gradlew :app:installDebug
```

### What to check

- Light + dark theme (Settings → Appearance actually switches)
- Bengali strings (device language → বাংলা)
- Onboarding flow (first run), biometric app lock

Android first run is intentionally sequenced: welcome, profile, privacy,
budgets and insights, notification permission, SMS permissions, then a
historical inbox scan before opening Home. SMS parsing, categorization,
deduplication, persistence, and review-queue decisions remain in the Rust core;
the Android layer only captures SMS and adapts the result for the UI.
- All screens: Home, Transactions, Analytics, Budgets, Subscriptions,
  Accounts, Settings sub-pages
- CSV export share sheet, home-screen widget
- System back button on sub-screens

### Android Rust persistence checks

- On first launch, confirm the 11 system categories are present.
- Add, edit, disable, and delete a custom category and a Smart Rule.
- Force-stop and reopen the app; the changes must still exist.
- Load demo data, force-stop/reopen, then reset data. User/demo records must
  disappear while system categories remain.
- Send an anonymized SMS through the receiver or historical scanner; parsing
  and persistence must come from Rust.

## iOS (requires a Mac — cannot run on Windows)

### Requirements

- A Mac with Xcode 15+ (App Store or developer.apple.com)
- Simulator comes bundled with Xcode

`apps/ios/project.yml` is the source of truth for the Xcode project. On a Mac:

1. Build the Rust libraries and generate the Swift bindings:

   ```bash
   rustup target add aarch64-apple-ios aarch64-apple-ios-sim x86_64-apple-ios
   cd core
   cargo build --release --target aarch64-apple-ios-sim -p centwise-ffi
   cargo build --release --target x86_64-apple-ios -p centwise-ffi
   cargo build --release --target aarch64-apple-ios -p centwise-ffi
   cargo run -p uniffi-bindgen -- generate \
     --library target/aarch64-apple-ios-sim/release/libcentwise_ffi.a \
     --language swift \
     --config centwise-ffi/uniffi.toml \
     --out-dir ../apps/ios/Centwise/Core/FFI/generated
   mkdir -p ../apps/ios/Centwise/Core/FFI/lib/iphonesimulator \
     ../apps/ios/Centwise/Core/FFI/lib/iphoneos
   xcrun lipo -create \
     target/aarch64-apple-ios-sim/release/libcentwise_ffi.a \
     target/x86_64-apple-ios/release/libcentwise_ffi.a \
     -output ../apps/ios/Centwise/Core/FFI/lib/iphonesimulator/libcentwise_ffi.a
   cp target/aarch64-apple-ios/release/libcentwise_ffi.a \
     ../apps/ios/Centwise/Core/FFI/lib/iphoneos/libcentwise_ffi.a
   ```

2. Generate the project and open it:

   ```bash
   cd ../apps/ios
   brew install xcodegen  # one time
   xcodegen generate
   open Centwise.xcodeproj
   ```

3. Select an iPhone Simulator and run (`Cmd+R`). The fat simulator archive
   supports Apple Silicon and Intel simulator builds; the device archive is
   separate and must be rebuilt whenever the Rust FFI API changes.

### No Mac? Alternatives

- **Static check only:** screenshots in `docs/screenshots/` (ios ui 1–3)
- **Cloud Mac:** MacinCloud / MacStadium / GitHub Actions macOS runner
  (the same path needed later for the iOS Rust build)

### What to check

Same list as Android minus widget/onboarding differences: app lock
(Face ID/Touch ID in simulator), themes, Bengali localization, analytics
charts, CSV export share sheet.

### iOS Rust persistence checks

- Confirm the App Group database is used at `group.com.centwise.shared`.
- Repeat the category, Smart Rule, demo-data, reset, and restart checks above.
- Run the Shortcut/App Intent with an anonymized SMS and verify the result
  appears through the Rust ingestion path.

## Troubleshooting (first build)

| Problem | Fix |
|---|---|
| Gradle sync fails | Check JDK: Android Studio → Settings → Build → Gradle JDK |
| `UnsatisfiedLinkError` | Build all Android Rust `.so` files into `app/src/main/jniLibs` |
| Emulator missing | Device Manager → create virtual device (API 34+) |
| Kotlin compile errors | Run `./gradlew :app:testDebugUnitTest` and fix the reported source error |
| iOS missing `-lcentwise_ffi` | Build and copy the Rust library into `Centwise/Core/FFI/lib` |
| iOS architecture mismatch | Use the simulator Rust target for a simulator and the device target for an iPhone |

## After the UI check

The remaining work is target-host verification: build the Android `.so` files
on Windows/Android CI, build the iOS simulator/device library on macOS, then
exercise the persistence and SMS checks above. The Rust core itself is already
covered by the workspace test suite.

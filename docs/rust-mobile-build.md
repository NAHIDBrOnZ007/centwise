# Rust Mobile Core Build and Packaging

Centwise has one shared Rust core. Android and iOS are platform adapters only:
they provide permissions, SMS access, lifecycle, and the database path. SMS
normalization, provider detection, filtering, parsing, categorization, SQLite
writes, demo data, and queries must run through Rust UniFFI.

## Android

The generated Kotlin file is only the UniFFI bridge. The compiled Rust engine
must also be present as native libraries. Build them from `core` after Android
NDK `27.2.12479018`, the Rust Android targets, and `cargo-ndk` are installed:

```powershell
cd C:\Users\freef\Downloads\centwise\core
$env:ANDROID_NDK_HOME="C:\Users\freef\AppData\Local\Android\Sdk\ndk\27.2.12479018"
cargo ndk -t arm64-v8a -t armeabi-v7a -t x86_64 -t x86 `
  -o ..\apps\android\app\src\main\jniLibs `
  build --release -p centwise-ffi
```

This creates local, generated files at:

```text
apps/android/app/src/main/jniLibs/arm64-v8a/libcentwise_ffi.so
apps/android/app/src/main/jniLibs/armeabi-v7a/libcentwise_ffi.so
apps/android/app/src/main/jniLibs/x86_64/libcentwise_ffi.so
apps/android/app/src/main/jniLibs/x86/libcentwise_ffi.so
```

The `.so` files are ignored by Git because they are platform build artifacts.
They must exist before building or installing Android. Verify the APK contains
them with:

```powershell
cd ..\apps\android
.\gradlew.bat :app:assembleDebug
& "$env:JAVA_HOME\bin\jar.exe" tf app\build\outputs\apk\debug\app-debug.apk | Select-String libcentwise_ffi
```

At runtime, Android must log `Rust core initialized successfully`. If it logs
`libcentwise_ffi.so not found`, SMS and Rust-owned demo/database operations are
not running through the shared core.

## iOS

iOS uses the same Rust core and UniFFI contract. A Mac must build the matching
simulator or device static library and place it at:

```text
apps/ios/Centwise/Core/FFI/lib/libcentwise_ffi.a
```

The generated Swift and C header files do not contain the Rust implementation;
the `.a` library is required for Xcode linking.

## Boundary rule

Never add an Android or iOS SMS parser fallback. If the native Rust library is
missing, initialization must be visible in logs and the build/verification
process must fix packaging rather than silently changing parsing behavior.

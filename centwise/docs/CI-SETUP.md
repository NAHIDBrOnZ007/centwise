# Centwise GitHub Actions Setup

Centwise uses GitHub Actions to build the shared Rust core before building
either native app. The native projects do not contain a fallback parser or
SQLite implementation, so a successful app build must include the Rust mobile
library.

## Workflows

### Pull requests and `main`

`.github/workflows/test.yml` runs three jobs:

1. Rust formatting, Clippy, workspace tests, and generated UniFFI binding parity.
2. Android JDK 17, `android-actions/setup-android@v4` with the pinned SDK/NDK,
   all four Rust ABIs, Kotlin tests, and a debug APK.
3. macOS/Xcode, Rust simulator and device libraries, Swift binding generation,
   XcodeGen, and an unsigned iOS Simulator app.

APK, iOS app, Rust-library, and failure-diagnostic artifacts are uploaded for
each run. Failed builds remain visible as failed GitHub checks; CI does not
automatically open issue spam.

### Version tags and manual release runs

`.github/workflows/release.yml` runs the Rust verification job first, then
builds an unsigned Android release APK and an unsigned iOS Simulator release
app. These artifacts prove that the Rust core is linked, but they are not
store-ready binaries.

## Repository setup

No secret is required for pull-request or simulator verification. In the GitHub
repository, enable Actions and make sure the workflow files are present on the
default branch. The workflows use read-only repository permissions.

The first run should show these artifacts:

- `centwise-android-debug` or `centwise-android-apk`
- `centwise-android-rust-libraries`
- `centwise-ios-app` or `centwise-ios-release`
- `centwise-ios-rust-libraries`
- platform diagnostic artifacts when reports/logs exist

## Store release prerequisites

Signing is deliberately not enabled yet. Before creating a production release,
add secrets through GitHub Settings → Secrets and variables → Actions; never
commit them:

- Android: an encrypted keystore plus store password, alias, and key password.
- iOS: Apple Developer signing certificates/profiles or an App Store Connect
  API key, plus the required bundle identifier and provisioning profile.
- Release automation: Fastlane may be added after signing is configured.

The current release workflow must be extended to decode those secrets into
temporary runner files, sign the Android AAB/APK, archive the iOS device build,
and upload to Play Console/TestFlight. Until then, use the uploaded unsigned
artifacts only for build verification.

## What CI cannot prove

GitHub Actions can compile and run automated tests, but it cannot replace the
final device checks:

- Android SMS permission, broadcast delivery, notification behavior, and
  emulator/device database persistence.
- iOS Shortcut/App Intent delivery, App Group storage, notification behavior,
  and physical iPhone signing/install.

Use [running-the-apps.md](running-the-apps.md) for the manual Rust persistence
and anonymized SMS checklist after downloading the CI artifact.

## Common failures

| Failure | Meaning | Action |
|---|---|---|
| `cargo ndk` cannot find the NDK | Runner setup or NDK version mismatch | Check the installed `27.2.12479018` path and `ANDROID_NDK_HOME`. |
| `UnsatisfiedLinkError` | Android `.so` is missing or wrong ABI | Confirm the four ABI folders are packaged in the APK build job. |
| `ld: library not found for -lcentwise_ffi` | iOS static library was not staged | Check the Rust iOS build and `Centwise/Core/FFI/lib`. |
| iOS architecture mismatch | Simulator/device library was mixed | Use `aarch64-apple-ios-sim` for Simulator and `aarch64-apple-ios` for iPhone. |
| Binding parity failure | Checked-in Kotlin/Swift bindings are stale | Regenerate from the current `centwise-ffi` library and commit the generated files. |
| Signing failure | Store credentials are missing or invalid | Configure signing secrets; do not disable signing by committing keys. |

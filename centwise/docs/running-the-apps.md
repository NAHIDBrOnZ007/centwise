# Running the Apps (UI Check Guide)

How to run and visually check the Android and iOS apps.

Both apps currently run on **fake data** — no Rust build, no NDK, no real
database needed. This guide is for checking the UI only.
Neither app has been compiled/run yet, so expect small fixes on first build.

## Android (works on this Windows PC)

### Requirements

- Android Studio (bundles JDK + Android SDK): https://developer.android.com/studio
- That's it. No Rust toolchain needed for UI.

### Option A — Android Studio (easiest)

1. Open Android Studio → **Open** → select `apps/android`
2. Wait for Gradle sync to finish (first sync takes a few minutes)
3. Device Manager → create any emulator (e.g. Pixel 7, API 34+)
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
- All screens: Home, Transactions, Analytics, Budgets, Subscriptions,
  Accounts, Settings sub-pages
- CSV export share sheet, home-screen widget
- System back button on sub-screens

## iOS (requires a Mac — cannot run on Windows)

### Requirements

- A Mac with Xcode 15+ (App Store or developer.apple.com)
- Simulator comes bundled with Xcode

### ⚠️ Known gap: no Xcode project file yet

`apps/ios/` contains only Swift sources (`Centwise/` folder) — there is **no
`.xcodeproj` yet**, despite what README says. First-time setup on a Mac:

1. Open Xcode → **File → New → Project → iOS App**
   - Product name: `Centwise`, interface: SwiftUI, language: Swift
2. Save the project **into `apps/ios/`**
3. Delete the auto-generated `ContentView.swift` and its files
4. Drag the existing `apps/ios/Centwise/` folder (App, Core, Data, Features,
   Assets.xcassets, en.lproj, bn.lproj) into the Xcode project
   (check "Copy items if needed" is OFF — keep files in place, target membership ON)
5. Build (`Cmd+B`), fix any small compile errors, then run (`Cmd+R`)
   on any iPhone simulator

### No Mac? Alternatives

- **Static check only:** screenshots in `docs/screenshots/` (ios ui 1–3)
- **Cloud Mac:** MacinCloud / MacStadium / GitHub Actions macOS runner
  (the same path needed later for the iOS Rust build)

### What to check

Same list as Android minus widget/onboarding differences: app lock
(Face ID/Touch ID in simulator), themes, Bengali localization, analytics
charts, CSV export share sheet.

## Troubleshooting (first build)

| Problem | Fix |
|---|---|
| Gradle sync fails | Check JDK: Android Studio → Settings → Build → Gradle JDK |
| Emulator missing | Device Manager → create virtual device (API 34+) |
| Kotlin compile errors | Expected — app never compiled before; fix as they appear |
| Xcode "no scheme" | The .xcodeproj was just created — set the app target's scheme |

## After the UI check

Next real step (per `docs/STATUS.md`): Android NDK cross-compile so the
apps run on the real Rust database instead of fake data.

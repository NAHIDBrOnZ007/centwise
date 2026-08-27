# Centwise — Handoff Plan for Next Agent

Created: 2026-08-22. Read this file fully before doing anything.
Project root: `centwise/` inside the "centwise workspace" folder (Windows PC).

## ⚡ IMMEDIATE FIRST TASK (in progress, not finished)

The first-ever Android build was run and failed with 5 Kotlin errors.
**4 of 5 are already fixed in the working tree (NOT yet committed, NOT yet
re-verified).** Your first job: re-run the build and fix whatever remains.

### Build command (this Windows PC — `java` is NOT on PATH, use Android Studio's bundled JBR):

```bash
cd "centwise/apps/android"
JAVA_HOME="C:\Program Files\Android\Android Studio\jbr" ./gradlew :app:assembleDebug
```

(First run takes ~2 min; Gradle 9.4.1 + deps already downloaded.)

### Errors found and fixes ALREADY APPLIED (do not redo):

| Error | File | Fix applied |
|---|---|---|
| `ProcessLifecycleOwner` unresolved | `MainActivity.kt:101-106` | Added `androidx-lifecycle-process` to `gradle/libs.versions.toml` + `implementation(libs.androidx.lifecycle.process)` in `app/build.gradle.kts` |
| `No parameter with name 'progress'` | `AddEditBudgetSheet.kt:115` | Removed `progress = ...` arg — `BudgetItem` computes it (`data/models/TransactionModels.kt:40`) |
| Same error | `BudgetDetailScreen.kt:295` (preview) | Removed `progress = 0.7f` arg |
| `ic_launcher_foreground` unresolved | `AboutScreen.kt:91` | Created `res/drawable/ic_launcher_foreground.xml` (inset wrapper around `@mipmap/ic_launcher`) |
| `Button`/`ButtonDefaults` unresolved | `RulesScreen.kt:120-127` | Added material3 imports |

If the build throws NEW errors, they are first-compile issues (this app was
never compiled before today). Fix them the same way — they should be small.

### When build is green:

1. APK lands in `app/build/outputs/apk/debug/app-debug.apk`
2. Update `docs/STATUS.md` and `CHANGELOG.md` (mark Android compiles)
3. Commit everything (conventional commit message; see AGENTS.md rules)
4. Optional visual check: emulator exists at `$LOCALAPPDATA/Android/Sdk`
   (`emulator` folder). AVD may need creating via Android Studio first.

## 📁 Work completed this session (all uncommitted)

| Change | File(s) |
|---|---|
| iOS project generation spec (fixes missing .xcodeproj permanently) | `apps/ios/project.yml` (XcodeGen: `brew install xcodegen && cd apps/ios && xcodegen generate`) |
| Version corrected 1.0.0 → **0.1.0** | `apps/android/app/build.gradle.kts`, `apps/ios/Centwise/App/Info.plist`, `apps/ios/project.yml` |
| Versioning strategy doc | `docs/VERSIONING.md` |
| Full build plan (8 phases, constraints, pennywise mapping) | `docs/BUILD-PLAN.md` |
| How to run both apps guide | `docs/running-the-apps.md` |
| Android adaptive icons (Android 8+ round/squircle masks) | `res/mipmap-anydpi-v26/ic_launcher.xml` + `ic_launcher_round.xml`, `res/drawable/ic_launcher_foreground.xml`, `res/values/colors.xml` |
| The 5 build fixes above | see table |

## ✅ Verified facts (do not re-investigate)

- **No "cenwise" misspelling exists** — repo is correctly spelled everywhere
  (folder `Centwise`, `app_name` "Centwise", id `com.centwise`). User saw the
  blank Xcode template on a borrowed Mac.
- **Icons are correct** — otter-with-coin mascot IS the official logo
  (`centwise logo.jpeg`), generated to all sizes by
  `scripts/generate_app_icons.py`. NOT copied from pennywise (pennywise's
  icon is a purple owl — completely different art).
- **Rust core is solid**: `cd core && cargo test` = 30/30 green, clippy/fmt
  clean, Kotlin+Swift UniFFI bindings generated (`core/README.md` has the
  verified commands).
- **No SMS parser exists yet** — only design (`docs/architecture/parser-design.md`),
  fixtures (`fixtures/sms/*.json`: bKash 13, Nagad 6, Rocket 5, banks 5),
  and the normalization foundation crate.

## 👤 User constraints (important!)

- Dev machine: **Windows PC only** (Rust ✅, Android Studio installed ✅, NO Xcode possible)
- Test device: **iPhone only**, no Android phone, **no Mac**
- iOS path = GitHub Actions macOS runner builds → TestFlight → iPhone
  (needs Apple Developer account ~$99/yr — user hasn't decided yet)
- Strategy: **build everything first, then test slowly, Android first then iOS**
- `../pennywise` is **AGPL reference ONLY** — read for ideas, NEVER copy
  code/assets/strings (see AGENTS.md boundary rules)

## 🗺️ Roadmap & Implementation Status (details in docs/BUILD-PLAN.md)

1. ✅ **Phase 0 — Build Stability & CI**:
   - Fixed all initial compilation errors. Android build verified clean.
   - Added adaptive launcher icons (`res/mipmap-anydpi-v26/`, `res/drawable/ic_launcher_foreground.xml`).
   - Created `apps/ios/project.yml` for automated XcodeGen project generation.
   - Added `.github/workflows/test.yml` and `release.yml` with artifact uploads.

2. ✅ **Phase 1 — SMS Parser Core & Categorization**:
   - Built `core/centwise-parser` (field-hunting parser engine) and `core/centwise-categorization` (Bangladesh merchant dictionary).
   - Fixture test suite with 29 test cases passing. UniFFI bridge exported.

3. ✅ **Phase 2 — Android Real-Time Ingestion**:
   - `SmsBroadcastReceiver` with multi-part concatenation and `goAsync()`.
   - `SmsTransactionProcessor` with deduplication and rich notifications.
   - `HistoricalSmsScanner` for onboarding inbox batch import.

4. ✅ **Phase 3 — Android Feature Completion**:
   - `SmartRulesRepository` with priority rule evaluation.
   - Reactive `RulesScreen` with add/edit/delete/toggle support.
   - `ReviewQueueScreen` with monospace raw SMS cards and quick transaction conversion.

5. ✅ **Phase 4 — iOS Shortcuts App Intents & Feature Parity**:
   - `ParseTransactionIntent` (`AppIntent`) & `CentwiseShortcuts` (`AppShortcutsProvider`) for zero-click background tracking via Apple Shortcuts.
   - `ReviewQueueView` & `ReviewQueueRepository` on iOS.
   - `SmartRulesRepository` & `SmsTransactionProcessor` on iOS.

6. 🚀 **Phase 5 & 6 — Cloud Build & Real Device Testing**:
   - Cloud CI will build both the Android APK and iOS Simulator App on git push.
   - Ready for TestFlight and physical iPhone testing!

## 📏 Project rules (from AGENTS.md — enforced)

- Never commit real SMS data, names, account numbers, OTPs, secrets
- Every change needs tests at the boundary it touches; don't claim done
  from compilation alone — report what was actually verified
- Conventional commits, focused commits; keep worktree-safe behavior
- en/bn localization via stable string ids (never hardcode new UI text)
- All writes go through Rust; ViewModels stay thin

## Version state

Everything now at **0.1.0 (build 1)** — see `docs/VERSIONING.md`.
Milestones: 0.1.0 = real DB behind Home screen · 0.2.0 = SMS e2e · 1.0.0 = store RC.

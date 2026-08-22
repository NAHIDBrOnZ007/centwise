# Centwise Full Build Plan

Created: 2026-08-22. Strategy: **build everything first on the Windows PC,
then test slowly** (emulator + iPhone via TestFlight).

## Real constraints (confirmed)

| Resource | Status |
|---|---|
| Dev machine | Windows PC — Rust ✅, Android ✅ (Studio + emulator), NO Xcode |
| Test devices | iPhone (personal) — no Android phone mentioned |
| Mac | **NONE** — all iOS building must happen in the cloud |
| Reference | `../pennywise` (AGPL — read ideas, never copy code) |

## Key build-system decisions (fixes the Xcode pain permanently)

1. **Android-first development.** Everything testable on PC: Rust, parser,
   emulator, APK builds. iOS code is written on PC but built in the cloud.
2. **No hand-made Xcode project.** Use **XcodeGen**: we commit a `project.yml`
   (a small text file, editable on Windows). Any Mac/CI runs
   `xcodegen generate` and gets a correct `.xcodeproj` automatically.
   No more blank ContentView problems, no manual drag-in.
3. **iOS builds via GitHub Actions macOS runner** (free for public repos).
   Pipeline: push → macOS runner builds the app → (later) TestFlight upload.
   This is the ONLY way to test on the iPhone without owning a Mac.
4. **Apple Developer account required for iPhone testing** (~$99/year) for
   TestFlight. Decision needed before Phase 6. Free alternative until then:
   Android emulator only.

---

## Phase 0 — Unbreak the build system (PC, half a day)

- [ ] Add `apps/ios/project.yml` (XcodeGen spec listing all Swift sources,
      assets, en/bn localization, app target) — replaces the missing
      `.xcodeproj`; CI generates it
- [ ] Add `.github/workflows/core.yml` — cargo test/fmt/clippy on every push
      (pennywise reference: `test.yml`)
- [ ] Commit `docs/running-the-apps.md` fix notes; Android stays as-is
- **Done when:** push to GitHub → CI runs Rust tests green

## Phase 1 — SMS parser core (PC, the big one: ~1–2 weeks of sessions)

- [ ] New crate `core/centwise-parser`:
      provider detection (sender ID), field-hunting extraction per
      `docs/architecture/parser-design.md`, type keywords, safety rules,
      review-queue result for unparsable messages
- [ ] bKash first — reference: pennywise `BkashParser.kt` (behavior only),
      fixtures `fixtures/sms/bkash.json` (13 cases)
- [ ] Nagad (6 fixtures) — no pennywise reference, we're on our own
- [ ] Rocket (5 fixtures) — no pennywise reference
- [ ] Banks-generic (5 fixtures)
- [ ] New crate `core/centwise-categorization`: merchant dictionary →
      category, type fallbacks (design table in parser-design.md)
- [ ] Fixture tests green per provider; parser exposed through `centwise-ffi`
- **Done when:** every fixture SMS produces the expected transaction;
      cargo test green. This is product milestone **v0.2 core**

## Phase 2 — Android app on real data (PC, needs Android Studio + NDK)

- [ ] Install Android Studio + NDK on PC; cross-compile Rust to
      `aarch64-linux-android`; wire `jniLibs` + generated Kotlin bindings
      (swap code ready in `core/README.md`)
- [ ] Flip Home screen from fake data to Rust `home_dashboard`
- [ ] SMS ingestion: `SmsBroadcastReceiver` (reference: pennywise
      `receiver/SmsBroadcastReceiver.kt` behavior) → parser → DB
      → notification
- [ ] First-run historical SMS scan + permission flow
- **Done when:** emulator receives a fake SMS → transaction appears on
  Home. **v0.1.0 + v0.2.0 milestone reached**

## Phase 3 — Complete Android features (PC)

- [ ] Q6: move hardcoded Settings strings into en/bn catalogs
- [ ] App lock timeout settings UI; subscription full date picker
- [ ] Review queue screen (unrecognized SMS → user decides)
- [ ] Smart Rules engine UI wired to categorization
- [ ] Backup/restore format + CSV import (reference: pennywise `backup/`)
- **Done when:** feature-complete Android build in emulator

## Phase 4 — iOS app code completion (PC — Swift written on Windows)

- [ ] Shortcuts App Intent + Share Extension source (design in
      `ios_shortcut.md`) — same ingestion path through Rust
- [ ] Same features as Phase 3 list where platform-appropriate
- [ ] Keep all Swift compiling-clean via CI once Phase 5 exists
- **Done when:** source complete, waiting on cloud build

## Phase 5 — Cloud iOS build pipeline (GitHub Actions macOS runner)

- [ ] Workflow: macOS runner → install Rust iOS targets → build staticlibs
      → `xcodegen generate` → xcodebuild → upload app artifact
- [ ] Rust iOS build task from STATUS.md happens HERE (no Mac needed)
- **Done when:** pushing code produces a downloadable iOS .app/.ipa

## Phase 6 — Test on real iPhone (needs Apple Developer account ~$99)

- [ ] TestFlight upload from CI; install on personal iPhone
- [ ] iOS Shortcuts automation setup on the phone (guide: `ios_shortcut.md`)
- [ ] Slow full-device test pass: both platforms, light/dark, bn/en,
      lock, CSV share, widgets

## Phase 7 — Release readiness

- [ ] fastlane (reference: pennywise `fastlane/`), Play Store listing,
      privacy declarations, signing
- [ ] Crowdin localization pass (`crowdin/glossary.csv` exists)
- [ ] v1.0.0 store release candidate

---

## What pennywise already solved that we copy as IDEAS (not code)

| Their file/folder | Our equivalent phase |
|---|---|
| `parser-core/bank/BkashParser.kt` + 150 parsers | Phase 1 reference |
| `receiver/SmsBroadcastReceiver.kt` + notification listener | Phase 2 |
| `backup/` folder backup system | Phase 3 |
| `.github/workflows/test.yml, release.yml` | Phases 0 & 5 |
| Committed `iosApp.xcodeproj` | Phase 0 (we use XcodeGen instead — better) |
| `fastlane/` | Phase 7 |
| On-device AI assistant, PDF import | Not planned — revisit post-1.0 |

## Order summary

```
0 build system  →  1 parser  →  2 Android real data  →  3 Android complete
→  4 iOS code  →  5 cloud iOS build  →  6 iPhone testing  →  7 release
```

Everything from 0–4 happens on this Windows PC. The iPhone enters at 6.

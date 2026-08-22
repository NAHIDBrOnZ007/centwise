# Centwise Status & Roadmap

Last updated: 2026-08-22

## ✅ Already Done

### Architecture & Docs
- Decision made: **single Rust-owned SQLite database** (`docs/decisions/0001-single-rust-database.md`)
  - One schema, one migration runner, all writes through Rust
  - ViewModels stay native (thin) on both platforms — no KMP, no shared ViewModels
  - Reactive seam: Rust change notifications → StateFlow (Android) / Combine (iOS)
- Updated: `tech.md`, `setup.md`, `AGENTS.md`, `docs/architecture/file-structure.md`

### Rust Core (verified — all tests pass)
- `core/` Cargo workspace with 4 crates:
  - `centwise-domain` — shared models (money = i64 minor units, dates = epoch ms)
  - `centwise-db` — rusqlite (bundled + WAL), user_version migrations, schema v1
    + snapshot, change notifications via update_hook, screen queries
    (home_dashboard), atomic balance writes
  - `centwise-ffi` — UniFFI surface: CentwiseCore.open(), insert/delete,
    home_dashboard, ChangeListener callback
  - `uniffi-bindgen` — binding generator
- **8/8 tests pass, clippy clean, fmt clean**
- **Kotlin bindings generated & verified (2,059 lines)**, **Swift bindings verified (1,524 lines)**
- Rust toolchain installed on this PC (GNU + portable GCC at `C:\Users\USE\tools\mingw64`)

### iOS App (UI complete, fake data)
- 17+ screens: settings sub-pages (Appearance, Categories, Rules, Currency,
  FAQ, About), account/budget/subscription details & add/edit, analytics
  components (summary, pie, trends, breakdown, merchants)
- App lock (Face ID/Touch ID gate), CSV export + share, en/bn localization,
  local notifications

### Android App (UI complete, fake data)
- Matching screen set with sub-screen navigation + back handling
- Persisted theme (actually switches dark/light), biometric app lock,
  CSV export via FileProvider, onboarding flow, home-screen widget,
  notifications, en/bn localization

---

## ⚡ Quick Wins (small tasks, no big setup — do anytime)

All doable on this Windows PC, each under an hour, no NDK/Mac/emulator needed:

- ~~**Q1. Root `README.md`**~~ ✅ done 2026-08-22
- ~~**Q2. `PRIVACY.md`**~~ ✅ done 2026-08-22
- ~~**Q3. `docs/ui/icon-map.md`**~~ ✅ done 2026-08-22
- ~~**Q4. `centwise-normalization` crate**~~ ✅ done 2026-08-22 — Bangla ↔
  English digits, SMS text normalization, integer-minor amount parsing
  (lakh grouping, currency prefixes, no floats), amount-candidate scanner;
  15 unit tests green
- ~~**Q5. Rust query expansion**~~ ✅ done 2026-08-22 — analytics queries
  (category breakdown, top merchants, monthly trends with anchored windows),
  accounts/budgets/subscriptions list + insert queries, budget live-progress
  computation; 7 new tests (30 total workspace-wide, all green)
- ~~**Q6. Android Build Verification**~~ ✅ done 2026-08-22 — Gradle assembleDebug
  compiled cleanly with 0 errors, adaptive icons (API 26+) generated, version set to 0.1.0.
### Real-Time SMS Ingestion & Core Features (Phases 1, 2, 3 & 4)
- **Phase 1: Rust SMS Parser Core (`centwise-parser` & `centwise-categorization`)** ✅ done 2026-08-22
  - Generic field-hunting parser engine (amount, fee, balance after, TrxID, merchant/party, date/time)
  - Bangladeshi merchant dictionary & category fallback rules
  - 29/29 SMS fixtures verified (bKash, Nagad, Rocket, Banks)
  - UniFFI bridge `parse_sms_message` exported
- **Phase 2: Android Real-Time Background SMS Ingestion** ✅ done 2026-08-22
  - `SmsBroadcastReceiver` with multi-part concatenation and `goAsync()` coroutines
  - `SmsTransactionProcessor` with TrxID deduplication, field extraction, repository storage, and notifications
  - `HistoricalSmsScanner` for onboarding inbox batch import
- **Phase 3: Android Smart Rules Engine & Review Queue** ✅ done 2026-08-22
  - `SmartRulesRepository` with priority rule evaluation
  - Interactive `RulesScreen` with add/edit/delete/toggle
  - `ReviewQueueScreen` with monospace raw SMS cards, convert-to-tx modal, and badge indicators
- **Phase 4: iOS Apple Shortcuts Ingestion & Review Queue** ✅ done 2026-08-22
  - `ParseTransactionIntent` (Swift AppIntent) and `CentwiseShortcuts` (AppShortcutsProvider) for zero-click background tracking via Shortcuts automations
  - `ReviewQueueView` & `ReviewQueueRepository` on iOS
  - `SmartRulesRepository` on iOS with live category matching
  - `XcodeGen` specification verified with `apps/ios/project.yml`

---

## ⚡ Quick Wins & Verification Status
- **Android App:** `./gradlew :app:assembleDebug` compiles cleanly with 0 errors (`BUILD SUCCESSFUL in 1s`).
- **iOS App:** XcodeGen configuration and Swift source tree ready for automated CI compilation.

## 🔜 Current Phase (Phase 1 — SMS Parser Core)

1. **SMS Parser crate (`centwise-parser`)** — Generic field hunting for
   bKash (13 fixtures), Nagad (6 fixtures), Rocket (5 fixtures), and Bangladeshi Banks (5 fixtures).
2. **Merchant Categorization crate (`centwise-categorization`)** — Merchant dictionary
   and category mapping engine.
3. **UniFFI Bridge exposure** — Exposing SMS parser to Kotlin and Swift bindings.

## ⏳ Next Phases

4. **Android cross-compile + real integration** — install Android NDK,
   build `aarch64-linux-android`, generate bindings against the real `.so`,
   add `jniLibs` to Gradle, flip `USE_RUST_BACKEND = true`, wire `SmsBroadcastReceiver`.
5. **iOS Rust build & TestFlight** — via GitHub Actions macOS runner.
6. **Backup/restore format** + full CSV import.
7. **Store release setup** — app IDs, signing, fastlane, privacy declarations.

## ⚠️ Known Gaps / Reminders

- Apps run on fake data until the Rust backend is wired to Android/iOS UI.
- All writes must go through Rust core; ViewModels stay thin.
- AGPL reminder: Pennywise is reference-only — never copy its code/assets/strings.


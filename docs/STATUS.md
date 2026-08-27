# Centwise Status & Roadmap

Last updated: 2026-08-27

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
  - `centwise-db` — rusqlite (bundled + WAL), user_version migrations, schema v3
    + snapshot, change notifications via update_hook, screen queries
    (home_dashboard), atomic balance writes, categories and Smart Rules (schema v3)
  - `centwise-ffi` — UniFFI surface: CentwiseCore.open(), typed CRUD,
    SMS ingestion, review queue, home_dashboard, ChangeListener callback
  - `uniffi-bindgen` — binding generator
- **Full workspace tests pass, clippy clean, formatting clean for all crates**
- **Kotlin and Swift bindings regenerated from the current FFI API**
- Rust toolchain installed on this PC (GNU + portable GCC at `C:\Users\USE\tools\mingw64`)

### iOS App (UI complete, Rust-backed source wiring)
- 17+ screens: settings sub-pages (Appearance, Categories, Rules, Currency,
  FAQ, About), account/budget/subscription details & add/edit, analytics
  components (summary, pie, trends, breakdown, merchants)
- App lock (Face ID/Touch ID gate), CSV export + share, en/bn localization,
  local notifications

### Android App (UI complete, Rust-backed source wiring)
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
  - `SmsTransactionProcessor` as a thin adapter; Rust performs parsing,
    deduplication, storage, and review-queue decisions
  - `HistoricalSmsScanner` for onboarding inbox batch import
- **Phase 3: Android Smart Rules Engine & Review Queue** ✅ done 2026-08-22
  - Rust-backed `SmartRulesRepository` with persisted rule CRUD
  - Interactive `RulesScreen` with add/edit/delete/toggle
  - `ReviewQueueScreen` with monospace raw SMS cards, convert-to-tx modal, and badge indicators
- **Phase 4: iOS Apple Shortcuts Ingestion & Review Queue** ✅ done 2026-08-22
  - `ParseTransactionIntent` (Swift AppIntent) and `CentwiseShortcuts` (AppShortcutsProvider) for zero-click background tracking via Shortcuts automations
  - `ReviewQueueView` with a thin Rust-backed repository adapter on iOS
  - Rust-backed `RulesViewModel` with persisted rule CRUD
  - `XcodeGen` specification verified with `apps/ios/project.yml`

---

## ⚡ Quick Wins & Verification Status
- **Rust workspace:** Full workspace tests and `clippy --workspace --all-targets -- -D warnings` pass on Windows.
- **Native source wiring:** Android and iOS adapters call the generated Rust FFI for SMS ingestion, review queue operations, demo data, categories, Smart Rules, and manual CRUD.
- **System categories:** Seeded and read from Rust through `listCategories()` on both platforms; native category catalogs are removed.
- **Native builds:** Platform builds still require the Android Rust `.so` files/Java toolchain or the iOS Rust static library/Xcode on their respective hosts.
- **CI build wiring:** Android CI now cross-compiles all four Rust ABIs before
  Gradle; macOS CI builds the Rust simulator library before XcodeGen/Xcode.

## ✅ Completed Core Work

1. **SMS Parser crate (`centwise-parser`)** — Generic field hunting for
   bKash (13 fixtures), Nagad (6 fixtures), Rocket (5 fixtures), and Bangladeshi Banks (5 fixtures).
2. **Merchant Categorization crate (`centwise-categorization`)** — Merchant dictionary
   and category mapping engine.
3. **UniFFI Bridge exposure** — Exposing SMS ingestion, persistence, review queue, and typed records to Kotlin and Swift bindings.
4. **Rust-owned demo data** — Deterministic accounts, transactions, budgets, and subscriptions with idempotent reset/load tests.
5. **Native demo cleanup** — Removed platform mock/demo providers and starter seed records; both data-management screens call Rust.
6. **Rust-owned categories, Smart Rules, and manual CRUD** — Categories, rules,
   transactions, accounts, budgets, and subscriptions now persist through the
   same Rust SQLite handle on both platforms. System categories are seeded and
   protected; custom categories and rules are editable/deletable.

## ⏳ Next Phases

6. **Android packaging + device integration** — install Android NDK, build the
   supported `.so` ABIs, package them under `jniLibs`, and run the app on a device/emulator.
7. **iOS Rust build & device integration** — CI now builds the simulator
   library; build the device library on macOS and run the Xcode scheme. Follow
   the complete [Rust platform build and runtime verification plan](superpowers/plans/rust-platform-build-plan.md).
8. **Backup/restore format** + full CSV import.
9. **Store release setup** — app IDs, signing, fastlane, privacy declarations.

## ⚠️ Known Gaps / Reminders

- Demo data is loaded only through `CentwiseCore.loadDemoData()`; native starter/mock datasets are removed.
- SMS ingestion, review queue, demo operations, categories, Smart Rules, and manual CRUD go through Rust; native arrays are UI caches only.
- ViewModels stay thin and native platform code must not parse SMS or own the Rust database.
- AGPL reminder: Pennywise is reference-only — never copy its code/assets/strings.


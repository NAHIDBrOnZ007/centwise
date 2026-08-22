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
- **Q6. Localization extraction (part 1)** — move hardcoded strings from the
  Settings screens into the en/bn catalogs (strings already exist there)

Next quick win: **Q4** (normalization crate — parser foundation).

## 🔜 Next (bigger tasks — when time allows)

1. **Android cross-compile + real integration** — install Android NDK,
   build `aarch64-linux-android`, generate bindings against the real `.so`,
   add `jniLibs` to Gradle, flip `USE_RUST_BACKEND = true`, swap Home screen
   (code ready in `core/README.md`)
2. **Run both apps in emulators** — visual check light/dark, Bengali strings,
   lock flow, CSV share, widget (never compiled yet — expect small fixes)
3. **Commit + push everything** (see commit message in chat/notes)

## ⏳ Later (after the above)

4. **iOS Rust build** — needs a Mac or GitHub Actions macOS runner
   (xcframework + Swift bindings into Xcode + App Group container)
5. **iOS widget** — needs Widget Extension target created in Xcode (manual step)
6. **Parser crates** — `centwise-normalization`, `centwise-parser`,
   `centwise-categorization`; providers in order: bKash → Nagad → Rocket → banks
   (fixtures first, anonymized only)
7. **Platform ingestion** — Android SMS BroadcastReceiver + iOS Shortcuts
   App Intent + Share Extension (all writes go through Rust)
8. **Backup/restore format** + full CSV import
9. **CI** (GitHub Actions): cargo test/fmt/clippy + Android build + iOS build (macOS runner)
10. **Localization completion** — move hardcoded UI strings into the en/bn catalogs
11. **Store release setup** — app IDs, signing, fastlane, privacy declarations

## ⚠️ Known Gaps / Reminders

- Apps still run on **fake data** until step 1 completes
- Neither app has been compiled/run since the UI work (no emulator run yet)
- App Lock: preference + gate work; timeout settings UI is minimal
- Subscription sheet date picker is quick-pick only
- AGPL reminder: Pennywise is reference-only — never copy its code/assets/strings

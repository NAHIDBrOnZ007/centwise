# Changelog

All notable changes to Centwise are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/); project is pre-1.0.

## [Unreleased] — 2026-08-22

### Added — Architecture & Documentation
- **Decision 0001**: single Rust-owned SQLite database with one migration
  runner; ViewModels stay native and thin on each platform; no KMP
  (`docs/decisions/0001-single-rust-database.md`)
- Rewrote storage/migration sections in `tech.md`, `setup.md`, `AGENTS.md`,
  and `docs/architecture/file-structure.md` to match the decision
- `docs/STATUS.md` — progress tracker (done / next / later / known gaps)
- `.gitignore` — Rust target, Android/iOS build output, signing keys, secrets
- This `CHANGELOG.md`
- Root `README.md` — project overview, architecture, dev commands, doc index
- `PRIVACY.md` — full SMS privacy policy (required before real-user testing)
- `docs/ui/icon-map.md` — SF Symbols ↔ Material Symbols mapping table
- `docs/supported-providers.json` — expanded to all 13 providers
  (3 MFS + 10 banks)
- `fixtures/sms/` — anonymized SMS fixture files (bKash 13 messages incl.
  merchant/recharge/order-independence/rejection cases, Nagad 6, Rocket 5,
  banks-generic 5) with expected parser results
- `docs/architecture/parser-design.md` — generic field-hunting parser
  design: no template matching, merchant dictionary, safety rules

### Added — Rust Core (`core/`, machine-verified)
- Cargo workspace with `centwise-domain`, `centwise-db`, `centwise-ffi`,
  `uniffi-bindgen`
- `centwise-normalization` crate: Bengali ↔ ASCII digit conversion,
  whole-SMS text normalization, integer-minor amount parsing (lakh/western
  grouping, Tk/৳/BDT prefixes, float-free, strict rejection of invalid
  tokens), and amount-candidate scanning — 15 unit tests green
- Domain models: money as i64 minor units (poisha), dates as epoch millis,
  default category seed
- Database: rusqlite (bundled SQLite) in WAL mode with busy timeout and
  foreign keys; `user_version` migration runner; committed schema snapshot
  `centwise-db/schemas/v1.sql`
- Change notifications: SQLite `update_hook` → observer registry → UniFFI
  `ChangeListener` callback (the reactive seam for StateFlow/Combine)
- Screen queries: `home_dashboard` (period totals + recent list with joins),
  transaction insert/delete with atomic account-balance updates
- Analytics queries: `category_breakdown` (period expenses grouped by
  category, biggest first), `top_merchants` (grouped by title),
  `spending_by_month` (calendar-month buckets with clock-independent
  anchored windows); account/budget/subscription list + insert queries with
  live budget progress — 7 more tests (30 total green)
- UniFFI surface: `CentwiseCore.open(path)`, insert account/transaction,
  delete, balance, `home_dashboard`, `add_listener`
- Tests: 8/8 passing (migrations idempotent, atomic balances, notification
  fires, delete reversal, dashboard math, limits, input validation);
  clippy and fmt clean
- Kotlin bindings generated and verified (2,059 lines); Swift bindings
  generated and verified (1,524 lines)
- `core/README.md` — verified build/bindgen commands and native wrapper code
- Dev toolchain installed on the Windows PC: rustup (GNU toolchain) +
  portable MinGW-w64 GCC (no admin required)

### Added — iOS App
- 17 missing screens recreated natively: Appearance, Categories,
  Add/Edit Category, Smart Rules, Add/Edit Rule, Currency Picker, FAQ,
  About, App Lock manager, Account Detail, Add/Edit Budget, Budget Detail,
  Add/Edit Subscription, Analytics Summary Card, Category Pie Chart,
  Spending Trends Chart, Category Breakdown List, Top Merchants List
- Settings rows wired with navigation; Analytics screen now shows Summary →
  Cash Flow → Pie → Trends → Categories → Merchants
- App Lock: gate at app entry (locks on cold start/background, Face ID /
  Touch ID unlock)
- CSV export with share sheet (`CsvExporter`, `CsvExportSheet`)
- Local notifications for new transactions and budget warnings
- English + Bengali localization catalogs (`en.lproj`, `bn.lproj`)

### Added — Android App
- Matching screen set: Settings sub-pages, Accounts (list/detail/add),
  Budgets (list/detail/add), Subscriptions (list/add), Analytics components
  (summary, donut chart, trend bars) integrated into AnalyticsScreen
- Sub-screen navigation with system back handling; floating tab bar hides
  on sub-screens
- Persisted appearance: theme mode (System/Light/Dark/AMOLED) and accent
  color actually apply app-wide
- Biometric App Lock: lock screen, cold-start + background timeout gating,
  BiometricPrompt unlock (added `androidx.biometric`)
- First-run onboarding (3-page pager with skip, persisted completion)
- CSV export via FileProvider + share intent
- Local notifications: channel, permission check, fires on new transaction
- Home-screen spending widget (RemoteViews, 30-min refresh)
- English + Bengali `strings.xml`

### Fixed
- Removed all Pennywise-derived UI code and the `PennywiseCompat` shim
  (AGPL compliance — Pennywise is visual reference only); restored the
  independent native implementation from git history

## [Unreleased] — earlier commits (pre-2026-08-22)

- `ba06310` — Centwise agent and developer guide (`AGENTS.md`)
- `808bab5` — repository file structure rework
- `1ca5d6d` — avatar and icon assets
- `3b90db3` — iOS UI design system (CentwiseColors/Spacing/Typography,
  CentwiseCard, glass components)
- `48932c9` — Android UI design system (theme, components, tab bar)
- `f58cffd` — docs and plan updates (tech.md, setup.md, features.md)
- `29cf4ca` — iOS system UI screens
- `46c971d` — Android UI screens
- `219b708` — complete iOS and Android feature screens with app foundations

## Versioning plan

- `0.1.0` — first working local build: Rust database behind Home screen on
  Android, all screens on fake-to-real data swap
- `0.2.0` — SMS ingestion (bKash, Nagad, Rocket) end-to-end
- `1.0.0` — first store release candidate

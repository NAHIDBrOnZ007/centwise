# Centwise Project Setup

This document describes the foundation that must be prepared before building product features.

The canonical long-term structure is documented in
[`docs/architecture/file-structure.md`](docs/architecture/file-structure.md).

## Repository Foundation

- [ ] Git repository initialized with a clear branch strategy
- [ ] Root `README.md`
- [ ] Root `AGENTS.md` or equivalent contributor guidance
- [ ] `LICENSE`
- [ ] `PRIVACY.md`
- [ ] `SECURITY.md`
- [ ] `CONTRIBUTING.md`
- [ ] `.gitignore`
- [ ] No secrets, personal SMS, real account numbers, or real credentials committed
- [ ] `.gitmessage` commit template
- [ ] Conventional commit types documented
- [ ] Issue and pull-request templates when public collaboration begins

## Important Project Files

## Centwise Files We Must Create

These are Centwise-owned files. PennyWise is only a reference for the purpose
of each file; its contents, IDs, secrets, and schemas must not be copied.

| File or folder | Purpose | Safe to create now? |
|---|---|---|
| `gradlew`, `gradlew.bat` | Reproducible Android Gradle commands | Yes, when the Android project is scaffolded |
| `settings.gradle.kts` | Declares the Gradle project and modules | Yes, with Centwise module names |
| `build.gradle.kts` | Shared Android build plugins | Yes, with pinned Centwise versions |
| `gradle.properties` | Android/Kotlin build settings | Yes, without secrets |
| `gradle/libs.versions.toml` | Central dependency version catalog | Yes |
| `core/Cargo.toml` | Rust workspace and core crate definition | Yes, when Rust core is scaffolded |
| `core/uniffi.toml` | Rust-to-Kotlin/Swift binding configuration | Yes, with Centwise names |
| `docs/supported-providers.json` | Bangladesh provider source of truth | Created with initial safe structure |
| `assets/` | Brand logos, README banners, and screenshots | Yes |
| `core/centwise-db/` | Rust-owned shared database, migrations, and schema snapshots | Yes, when Rust core is scaffolded |
| `.gitmessage` | Consistent commit messages | Created |
| `crowdin/glossary.csv` | Consistent English/Bengali terminology | Created |
| `crowdin.yml` | Crowdin localization workflow | Later, after the Crowdin project exists |
| `fastlane/Fastfile` | Android/iOS release lanes | Later, after app IDs exist |
| `fastlane/Appfile` | Store application identifiers | Later, after package IDs exist |
| `fastlane/metadata/` | Store descriptions, screenshots, and release notes | Later, before store release |
| `.github/workflows/test.yml` | CI build and test checks | After the first buildable project exists |
| `.github/workflows/release.yml` | Manual release workflow | Later, after signing and store setup |
| `scripts/verify.sh` | Local verification command matching CI | After CI checks are defined |
| `README.md` | Project overview and developer setup | Yes |
| `ios_shortcut.md` | iOS Shortcut message automation setup guide | Created |
| `PRIVACY.md` | SMS, local storage, and Shortcut privacy explanation | Yes, before testing with real users |
| `SECURITY.md` | Security reporting and data-handling rules | Yes |
| `LICENSE` | Centwise source-code license | Requires an explicit license decision |

The numbered schema snapshots are generated outputs. We should not manually
create fake versions now; the first snapshot is generated from Centwise’s actual
database and every later snapshot is committed when the database changes.

Fastlane also remains configuration-only until Centwise has its own Android
application ID, iOS bundle identifier, signing setup, and store accounts. Never
put those credentials in the repository.

### Gradle

Gradle is the Android build system. It downloads dependencies, compiles Kotlin,
runs tests, packages Android builds, and connects Android modules.

- [ ] Gradle wrapper (`gradlew` and `gradlew.bat`)
- [ ] Root `settings.gradle.kts`
- [ ] Root `build.gradle.kts`
- [ ] `gradle.properties`
- [ ] Version catalog at `gradle/libs.versions.toml`
- [ ] Debug and release build types
- [ ] Debug application identifier
- [ ] Release signing values loaded only from local/CI secrets

### Fastlane

Fastlane is release automation. It builds mobile releases, uploads Android
builds to Google Play, uploads iOS builds to TestFlight/App Store Connect, and
manages store descriptions, screenshots, and release notes.

- [ ] Android Fastfile
- [ ] iOS Fastfile
- [ ] Android store metadata folders
- [ ] iOS store metadata folders
- [ ] Screenshot folders
- [ ] Google Play internal-testing lane
- [ ] TestFlight lane
- [ ] Metadata-only lane
- [ ] Dry-run release lane
- [ ] No signing keys or API tokens in Git

Fastlane is not required for the first local screen, but its folders and lanes
should be added before public release.

### Git commit template

Use commit types such as `feat`, `fix`, `docs`, `test`, `chore`, and `refactor`,
with scopes such as `parser`, `sms`, `android`, `ios`, `ui`, `db`, and `build`.

## Project Structure

```text
centwise/
├── apps/
│   ├── android/          Kotlin + Jetpack Compose Android application
│   └── ios/              Swift + SwiftUI iOS application
├── core/                 Rust shared parser, domain, and UniFFI core
├── fixtures/             Anonymized cross-platform SMS fixtures
├── docs/                 Product, architecture, and technical documentation
├── crowdin/              Localization resources
├── fastlane/             Release and deployment automation
├── scripts/              Build and maintenance scripts
└── .github/workflows/    Continuous integration and release workflows
```

## Data and Generated Files

### Supported-provider catalog

Create one checked-in JSON catalog for Bangladesh banks and MFS providers. It
should contain a stable provider ID, display name, provider type, sender IDs,
BDT currency, parser status, fixture coverage, and review date. This catalog
must be the single source of truth for provider lists.

- [ ] `docs/supported-providers.json`
- [ ] Provider registration validation
- [ ] Generated support documentation

### Database schema snapshots

Centwise uses one shared SQLite database owned by the Rust core. Instead of
Room's generated JSON, each released database version gets a committed schema
snapshot under `core/centwise-db/schemas/` so schema changes are reviewable in
pull requests, similar to how PennyWise reviews Room schema JSON.

- [ ] Commit a schema snapshot for every released database version
- [ ] Review schema changes in pull requests
- [ ] Add Rust migration tests for every released schema change
- [ ] Never use destructive migration for released user data

### Database migration policy

The Rust core owns the database schema and migrations for both platforms.
Migrations are written once in `centwise-db` and run through a versioned
migration runner, so Android and iOS can never drift apart.

```text
core/centwise-db/src/migrations/   Rust migration code, single source of truth
core/centwise-db/schemas/          committed schema snapshot per released version
core/centwise-db/tests/            migration and upgrade tests with fixture databases
docs/architecture/data-model.md    shared table/field contract
```

Every released schema change must have:

- [ ] An incremented database version
- [ ] A committed schema snapshot for review
- [ ] A Rust migration test from the previous released version
- [ ] A fixture database with representative old data for upgrade tests
- [ ] Fresh-install test
- [ ] Upgrade test with representative old data
- [ ] Backup/restore compatibility check
- [ ] Rollback or recovery decision

Schema snapshots are generated records of the database at each version. They
are committed only after the real database change exists, never hand-written
in advance.

### Other configured resources

- [ ] Android `strings.xml` for localizable UI text
- [ ] Bengali and English string resources
- [ ] Android manifest and permission declarations
- [ ] iOS `Info.plist`
- [ ] iOS entitlements for App Groups, Shortcuts, and extensions
- [ ] Android backup/data-extraction rules
- [ ] App icons and launch assets
- [ ] Database seed data for categories
- [ ] Anonymized parser fixture files
- [ ] Versioned backup schema

## Rust Core

- [ ] Cargo workspace created
- [ ] Rust core crate created
- [ ] `centwise-db` crate created with schema and versioned migration runner
- [ ] Data-change notification callback defined for reactive native layers
- [ ] Public typed API defined for SMS parsing
- [ ] Transaction model defined
- [ ] Bangladesh currency and number normalization defined
- [ ] Parser error and unsupported-message result types defined
- [ ] UniFFI configuration created
- [ ] Android native libraries configured
- [ ] iOS XCFramework build configured
- [ ] Kotlin bindings generated
- [ ] Swift bindings generated
- [ ] Rust formatter and clippy checks configured

## Android Application

- [ ] Kotlin Android project created
- [ ] Jetpack Compose and Material 3 configured
- [ ] SMS permissions declared and onboarding flow defined
- [ ] SMS `BroadcastReceiver` connected to the Rust core
- [ ] Historical SMS scan connected to the Rust core
- [ ] Optional notification listener boundary defined
- [ ] Android repository layer connected to the Rust database through FFI
- [ ] Data-change notifications wired to reactive UI state
- [ ] Android notifications configured
- [ ] Biometric lock configured
- [ ] Android app signing configuration kept outside Git

## iOS Application

- [ ] SwiftUI iOS project created
- [ ] Rust XCFramework linked
- [ ] Swift bindings linked
- [ ] App Group/shared-container decision documented
- [ ] Shortcuts App Intent target created
- [ ] App Intent accepts SMS text and calls the Rust core
- [ ] iOS Message Automation onboarding guide created
- [ ] Share Extension target created
- [ ] Database file placed in the App Group container so the app, App Intents, and Share Extension share one Rust database
- [ ] iOS repository layer connected to the Rust database through FFI
- [ ] Data-change notifications wired to reactive UI state
- [ ] Face ID/Touch ID lock configured
- [ ] iOS signing and provisioning kept outside Git

## Data Model

- [ ] Transaction model
- [ ] Account model
- [ ] Card model
- [ ] Category model
- [ ] Budget model
- [ ] Subscription model
- [ ] Merchant mapping model
- [ ] Parser result and review status model
- [ ] Backup schema version
- [ ] Stable identifiers and timestamps

## Bangladesh Parser Foundation

- [ ] Supported-provider list created
- [ ] Sender-ID list created
- [ ] Anonymized SMS fixture format defined
- [ ] bKash fixtures collected
- [ ] Nagad fixtures collected
- [ ] Rocket fixtures collected
- [ ] Bangladesh bank fixtures collected
- [ ] Debit cases covered
- [ ] Credit cases covered
- [ ] Transfer cases covered
- [ ] ATM and card cases covered
- [ ] Balance cases covered
- [ ] OTP and promotional cases rejected
- [ ] Bangla and English SMS cases covered
- [ ] Unknown-message review path defined

## Testing Foundation

- [ ] Rust unit tests
- [ ] Parser fixture tests for every supported provider
- [ ] Rust property/fuzz tests for amount and number normalization
- [ ] Android receiver tests
- [ ] iOS App Intent tests
- [ ] Share Extension tests
- [ ] Database tests
- [ ] Migration tests
- [ ] Categorization tests
- [ ] Budget tests
- [ ] Subscription reminder tests
- [ ] Backup/restore tests
- [ ] UI and navigation tests
- [ ] Test data contains no personally identifiable information

## Continuous Integration

- [ ] Rust format check
- [ ] Rust clippy check
- [ ] Rust tests
- [ ] Android unit tests
- [ ] Android instrumentation tests
- [ ] iOS build/test job on macOS
- [ ] Binding-generation check
- [ ] Fixture and privacy scan
- [ ] Build artifacts and test reports uploaded
- [ ] Release workflow separated from pull-request checks

## Localization

- [ ] English source strings
- [ ] Bengali translations
- [ ] Localization string identifiers
- [ ] Translation glossary at `crowdin/glossary.csv`
- [ ] Currency and number formatting rules
- [ ] Bengali text layout checks
- [ ] No untranslated brand names or technical terms
- [ ] Crowdin configuration added when the translation workflow is ready

## Backup, Privacy, and Security

- [ ] Local backup format defined
- [ ] Backup versioning defined
- [ ] Restore compatibility tests defined
- [ ] Raw SMS retention policy defined
- [ ] Data deletion flow defined
- [ ] Privacy disclosure for SMS access written
- [ ] Android Play SMS-permission declaration prepared
- [ ] iOS Shortcut data-flow disclosure written
- [ ] No analytics or cloud upload by default

## Release Foundation

- [ ] Android application ID selected
- [ ] iOS bundle identifier selected
- [ ] App name and icon finalized
- [ ] Versioning strategy defined
- [ ] Android Play Console listing prepared
- [ ] Apple App Store listing prepared
- [ ] Internal Android testing track configured
- [ ] TestFlight configured
- [ ] Fastlane scripts added
- [ ] Signing keys stored outside the repository
- [ ] Store privacy declarations completed

## Future Setup

These are product capabilities, not prerequisites for the local-first foundation:

- Cloud account and synchronization
- Web application
- In-app billing
- F-Droid distribution
- Advanced widgets
- Advanced rule builder
- Shared and family budgets

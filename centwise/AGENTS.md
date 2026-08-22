# Centwise Agent and Developer Guide

Centwise is a Bangladesh-focused personal finance application for automatically
tracking bank, card, and mobile-financial-service transactions from SMS.

This file is the primary operating guide for agents and developers working in
the Centwise repository.

## Product Direction

- Bangladesh first: BDT, Bangla/English SMS, local banks, cards, bKash, Nagad,
  Rocket, and other Bangladesh MFS providers.
- Android and iOS are first-class products.
- Android uses native Kotlin and Jetpack Compose.
- iOS uses native Swift and SwiftUI.
- Shared parsing and domain logic uses Rust through UniFFI bindings.
- The product is local-first. Cloud accounts, sync, web, billing, and F-Droid
  are planned capabilities, not assumptions for local features.
- Privacy is a product requirement: raw SMS stays on the device by default.

## PennyWise Reference Boundary

The `../pennywise` checkout is a reference for product behavior, architecture,
testing ideas, and UI inspiration. It is not Centwise source code.

Do not copy from PennyWise:

- Source code
- Parser implementations
- UI assets or screenshots
- Brand names, text, or icons
- Database schema files
- Test data containing personal information
- Secrets, signing keys, app IDs, or release configuration

PennyWise is AGPL-licensed. Reimplement behavior and designs independently for
Centwise unless the project explicitly approves a compatible licensing strategy.

## Canonical Structure

The authoritative file structure is:

[`docs/architecture/file-structure.md`](docs/architecture/file-structure.md)

Keep these boundaries intact:

```text
apps/android       Native Android UI and Android platform integrations
apps/ios           Native iOS UI, App Intents, and Share Extension
core               Rust domain, database, normalization, parser, category, and FFI code
fixtures           Anonymized shared SMS parser fixtures
docs               Architecture, provider, feature, and decision records
crowdin            Localization resources
fastlane           Store and release automation
scripts             Build, verification, and maintenance tools
```

## Feature Ownership

Features are vertical slices. A feature owns its UI, state, use cases, and
tests on each platform:

```text
features/transactions/
├── model/
├── state/
├── viewmodel/
├── screens/
├── components/
├── usecases/
└── tests/
```

Rules:

1. A feature must not import another feature's private screen, ViewModel,
   repository, or database implementation.
2. Cross-feature communication uses public use cases, domain models,
   navigation contracts, or repository interfaces.
3. Platform code may call Rust and repositories; UI code does not call Rust
   directly.
4. Screens do not access database tables directly.
5. New banks and MFS providers are parser/fixture changes, not UI changes.
6. A feature change must include tests at the boundary it changes.
7. Large features may later become separate Gradle modules, Swift packages, or
   Rust crates without changing their public contract.

## Android and iOS Feature Parity

Every user-facing feature must have a parity entry in its feature document.
Parity means the same user goal, data meaning, validation, and result—not
identical source code or identical controls.

For every feature, verify:

- Same feature name and purpose
- Same transaction/account/budget behavior
- Same required and optional fields
- Same validation rules
- Same empty, loading, error, and success states
- Same backup/export meaning
- Same accessibility intent
- Same localization keys
- Same parser and domain results

Platform differences are allowed for system integration:

- Android SMS Receiver versus iOS Shortcuts App Intent
- Android notification actions versus iOS local notifications
- Android Material components versus SwiftUI components
- Android widgets versus iOS widgets
- Android biometric prompt versus Face ID/Touch ID

Do not silently ship a feature on one platform with different business rules.

## UI Reference and Visual Language

PennyWise's iOS UI may be used as a visual reference for layout, hierarchy,
spacing, cards, navigation, empty states, and interaction flow. Recreate the
design independently using Centwise components and assets.

### iOS

- SwiftUI
- System materials and glass effects where supported
- Native blur, vibrancy, depth, and translucency
- Safe-area-aware sheets and navigation
- Dynamic Type and accessibility support

### Android

- Jetpack Compose
- Material 3 tokens and semantic colors
- Tonal surfaces, elevation, blur, and translucency where supported
- Android-native bottom sheets, navigation, and gestures
- Dynamic color where appropriate
- Light and dark theme support

Android does not need to copy an iOS glass API literally. It should reproduce
the same visual intent—depth, softness, hierarchy, and focus—using Android-native
Material 3 surfaces. Avoid fragile cross-platform tricks or a WebView for the
main product UI.

Every significant UI change requires:

- Light-theme check
- Dark-theme check
- Small-screen check
- Large-screen check where applicable
- Loading/empty/error-state check
- Screenshot or emulator verification when feasible

## Rust Core Rules

Rust owns deterministic shared logic:

- Text normalization
- Bangla and English number parsing
- Currency and BDT normalization
- Bank/MFS parser selection
- Amount, type, merchant, balance, reference, account, and card extraction
- OTP/promotional/non-transaction filtering
- Categorization rules
- Shared domain models and validation
- The shared SQLite database: schema, migrations, writes, and screen queries

Rust must not own:

- Android or iOS permissions
- UI state
- Notifications
- Platform lifecycle
- App navigation
- Platform storage locations or permissions; the platform passes the database
  path to the Rust core
- Store or account credentials

Keep the FFI surface small, typed, synchronous where possible, and stable.
Prefer value objects and explicit result types over unstructured JSON strings.

## SMS and Provider Rules

- Use only anonymized SMS fixtures in Git.
- Never add a real person's name, phone number, account number, card number,
  OTP, transaction ID, or balance to source or tests.
- Add every provider to `docs/supported-providers.json`.
- Add sender IDs only after verifying them from safe test data.
- Add debit, credit, transfer, reversal, fee, ATM, card, balance, and invalid
  message cases where the provider supports them.
- OTP and promotional messages must not become transactions.
- Unknown messages must be reviewable without silently inventing a transaction.
- Parser output must be deterministic and covered by fixture tests.

## Database and Migration Rules

- The Rust core owns the schema, migrations, and all writes for the single
  shared SQLite database used by both platforms.
- ViewModels contain no money arithmetic, date logic, or filtering; those are
  Rust query functions.
- A schema snapshot is committed for every released database version and
  reviewed in pull requests.
- Migrations are explicit Rust code run by the versioned migration runner.
- Never use destructive migration for released user data.
- Every schema change requires a migration test, upgrade test, fresh-install
  test, and backup compatibility review.
- Backup-serialized fields need backward-compatible defaults.
- Database changes require a data-model decision record when they affect more
  than one feature.

## Testing Requirements

Minimum checks for changes:

### Parser/provider change

- Rust unit tests
- Fixture tests
- Invalid/unsupported message tests
- Provider catalog update

### Database change

- Schema snapshot
- Migration test
- Backup/restore test
- Existing data compatibility test

### Android change

- Kotlin unit tests
- Android instrumentation tests when platform behavior changes
- Emulator verification for SMS, notification, permission, or UI changes

### iOS change

- Swift unit tests
- App Intent tests for Shortcut changes
- Share Extension tests for share changes
- Simulator/device verification for UI and background behavior

Do not claim a change is complete based only on compilation. Report separately
what was verified by source tests, runtime tests, device tests, and visual tests.

## Build and Verification

- Pin dependency versions in the project version catalogs.
- Use the repository Gradle wrapper, not a random globally installed Gradle.
- Keep Rust, Android SDK, JDK, Xcode, and tool versions documented.
- CI must run the same core checks developers run locally.
- Keep release builds separate from debug builds.
- Store signing keys, App Store Connect keys, Play credentials, and tokens
  outside Git and load them from local/CI secrets.

Planned commands after scaffolding:

```text
./scripts/verify.sh
./gradlew test
cargo test --workspace
cargo fmt --check
cargo clippy --workspace --all-targets -- -D warnings
```

Use the exact commands defined by the final project scripts once they exist.

## Localization

- Use stable string identifiers, not hardcoded user-facing text.
- English is the source language.
- Bengali is the first translation target.
- Keep brand names and technical terms consistent with
  `crowdin/glossary.csv`.
- Test long Bengali strings and mixed Bangla/Latin text on both platforms.
- Do not translate provider names, SMS, OTP, BDT, Rust, UniFFI, or F-Droid
  when the glossary marks them as fixed terms.

## Documentation and Decisions

- Update the relevant feature document when behavior changes.
- Record significant architecture choices in `docs/decisions/`.
- Keep provider support documentation generated from the provider catalog where
  practical.
- Update setup instructions when build or release requirements change.
- Do not create speculative documentation for features that have no agreed
  behavior.

## Git and Review Rules

- Use `.gitmessage` and clear conventional commits.
- Keep commits focused on one feature or setup concern.
- Do not mix formatting-only rewrites with behavior changes.
- Review generated schema changes carefully.
- Review privacy impact for every SMS, notification, backup, analytics, or
  cloud-related change.
- Preserve unrelated user changes in a dirty worktree.
- Do not reset, delete, or overwrite broad paths without explicit approval.

## Definition of Done

A change is ready only when:

- The feature boundary remains clear.
- Android and iOS behavior is aligned or the difference is documented.
- Rust/parser/database contracts are tested.
- Migrations and backups are safe when storage changes.
- Localization keys and glossary impact are handled.
- No PII or secrets were introduced.
- Relevant tests and runtime checks were run.
- Documentation and provider catalogs are updated.
- The final response reports verified checks and known limitations.


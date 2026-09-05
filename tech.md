# Centwise Technology

Centwise is a Bangladesh-focused personal finance app for automatically tracking transactions from bank and mobile-finance SMS messages.

## Applications

### Android

- Kotlin
- Jetpack Compose
- Material 3
- Native Android SMS receiver
- Optional Android notification listener for supported bank and MFS apps
- Opens the shared Rust database from Android-app storage
- Android notifications
- Android biometric lock
- Android home-screen widgets

### iOS

- Swift
- SwiftUI
- Apple Shortcuts integration
- Swift `AppIntent` for importing SMS text from a Message Automation
- iOS Share Extension for manually sharing an SMS to Centwise
- Opens the shared Rust database from the App Group container
- iOS biometric lock
- iOS widgets

## Shared Core

The shared business logic is written in Rust and exposed to Android and iOS through generated bindings.

Rust handles:

- SMS text normalization
- English number parsing
- Bangladesh bank and MFS parser logic
- Amount extraction
- Income and expense detection
- Merchant and counterparty extraction
- Account and card last-four extraction
- Balance and transaction-reference extraction
- Transaction categorization rules
- OTP, promotional, and non-transaction SMS filtering
- Transaction validation
- Shared transaction models
- Shared SQLite database schema, migrations, and screen-shaped queries

The Rust core does not manage platform permissions, UI, notifications, or platform lifecycle events.

## Rust Integration

- Rust library compiled for Android and iOS
- UniFFI-generated Kotlin and Swift bindings
- Small, typed, synchronous core APIs
- Data-change notifications through a UniFFI callback interface so native StateFlow and Combine layers can re-query
- Platform-specific adapters around the Rust core
- Rust unit, fixture, and fuzz testing for parser correctness

## Storage

- Local-first storage for the first release
- One shared SQLite database owned by the Rust core, used by Android and iOS
- Each platform opens the database by path from a platform-appropriate location (App Group container on iOS so the app, App Intents, and Share Extension share one file)
- WAL mode with a busy timeout for multi-process access
- No required account or server for local operation
- Local backup and restore
- CSV export
- Optional encrypted backup support

Cloud storage and cross-device synchronization are planned product capabilities, not required for local operation.

## Database and Migrations

- The Rust core owns the database schema and migrations in the `centwise-db` crate.
- Android and iOS use the same SQLite database through the same Rust code, so cross-platform backup and restore are correct by construction.
- Migrations run through a `user_version`-based migration runner in Rust and are written once.
- A reviewed schema snapshot is committed for each released database version so schema changes are reviewable in pull requests.
- Every released schema version has Rust migration tests, upgrade tests from representative old databases, and backup compatibility checks.
- Destructive migration is not used for released user data.
- All writes go through Rust functions so balances stay transactional and data-change notifications fire for reactive UI updates.

## Architecture

```text
Android SMS Receiver ─┐
Android Notification ─┤
                      ├── Rust Centwise Core ── Shared SQLite Database
iOS App Intent ───────┤
iOS Share Extension ─┘
                              │
                         Native App UI
```

## Application Architecture

- Native platform UI
- MVVM-style presentation structure
- Native ViewModels stay thin: money arithmetic, date logic, and filtering live in Rust query functions, not ViewModels
- Repository-based data access
- Unidirectional state flow
- Background processing for historical SMS scanning and maintenance tasks
- Platform-specific permission and lifecycle handling
- Shared parser fixtures used by Android and iOS validation

## Testing

- Rust unit tests
- Rust parser fixture tests
- Rust property and fuzz tests
- Bangladesh bank/MFS SMS samples with anonymized data
- Android receiver tests
- iOS App Intent tests
- Share Extension tests
- Database tests
- Migration tests
- Deduplication tests
- Category and budget tests
- Backup/restore tests
- UI and navigation tests
- Android instrumentation tests
- CI test runs on every change

## Delivery and Future Technology

- GitHub Actions for CI
- Fastlane for mobile release automation
- Google Play distribution for Android
- Apple App Store distribution for iOS
- Optional F-Droid Android distribution later
- Optional cloud backend and account system later
- Optional web application later
- Optional in-app billing later

## Product Boundaries

Centwise is initially focused on Bangladesh. The core model may support future expansion, but the first supported data sources are Bangladeshi banks, cards, bKash, Nagad, Rocket, and other local MFS providers.

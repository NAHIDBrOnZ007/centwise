# Centwise Technology

Centwise is a Bangladesh-focused personal finance app for automatically tracking transactions from bank and mobile-finance SMS messages.

## Applications

### Android

- Kotlin
- Jetpack Compose
- Material 3
- Native Android SMS receiver
- Optional Android notification listener for supported bank and MFS apps
- Android local database
- Android notifications
- Android biometric lock
- Android home-screen widgets

### iOS

- Swift
- SwiftUI
- Apple Shortcuts integration
- Swift `AppIntent` for importing SMS text from a Message Automation
- iOS Share Extension for manually sharing an SMS to Centwise
- iOS local database
- iOS biometric lock
- iOS widgets

## Shared Core

The shared business logic is written in Rust and exposed to Android and iOS through generated bindings.

Rust handles:

- SMS text normalization
- Bangla and English number parsing
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

The Rust core does not manage platform permissions, UI, notifications, or platform lifecycle events.

## Rust Integration

- Rust library compiled for Android and iOS
- UniFFI-generated Kotlin and Swift bindings
- Small, typed, synchronous core APIs
- Platform-specific adapters around the Rust core
- Rust unit, fixture, and fuzz testing for parser correctness

## Storage

- Local-first storage for the first release
- Android local database for Android app data
- iOS local database for iOS app data
- No required account or server for local operation
- Local backup and restore
- CSV export
- Optional encrypted backup support

Cloud storage and cross-device synchronization are planned product capabilities, not required for local operation.

## Database and Migrations

- Android and iOS share the same logical transaction/account data model.
- Android uses Room schema export and committed versioned JSON snapshots.
- iOS uses its own migration implementation for the same logical model.
- Safe additions may use automatic migration.
- Renames, transformations, removals, and relationship changes require explicit migration code.
- Every released schema version has migration tests, upgrade tests, and backup compatibility checks.
- Destructive migration is not used for released user data.

## Architecture

```text
Android SMS Receiver ─┐
Android Notification ─┤
                      ├── Rust Centwise Core ── Platform Database
iOS App Intent ───────┤
iOS Share Extension ─┘
                              │
                         Native App UI
```

## Application Architecture

- Native platform UI
- MVVM-style presentation structure
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

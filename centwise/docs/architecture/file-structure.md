# Centwise Final File Structure

Centwise uses a feature-oriented monorepo. Android and iOS keep native UI and
platform integrations, while Rust owns shared parsing and domain rules.

The structure is designed so developers can work on separate features without
editing unrelated feature code.

## Canonical Structure

```text
centwise/
├── apps/
│   ├── android/
│   │   ├── app/
│   │   │   ├── src/main/
│   │   │   │   ├── AndroidManifest.xml
│   │   │   │   ├── kotlin/com/centwise/
│   │   │   │   │   ├── core/
│   │   │   │   │   │   ├── design/
│   │   │   │   │   │   │   ├── theme/
│   │   │   │   │   │   │   ├── components/
│   │   │   │   │   │   │   ├── glass/
│   │   │   │   │   │   │   └── formatters/
│   │   │   │   │   │   ├── navigation/
│   │   │   │   │   │   ├── ffi/
│   │   │   │   │   │   ├── di/
│   │   │   │   │   │   └── common/
│   │   │   │   │   ├── data/
│   │   │   │   │   │   ├── repositories/
│   │   │   │   │   │   ├── fakes/
│   │   │   │   │   │   └── backup/
│   │   │   │   │   ├── platform/
│   │   │   │   │   │   ├── sms/
│   │   │   │   │   │   ├── notifications/
│   │   │   │   │   │   ├── permissions/
│   │   │   │   │   │   └── widgets/
│   │   │   │   │   └── features/
│   │   │   │   │       ├── onboarding/
│   │   │   │   │       ├── home/
│   │   │   │   │       ├── transactions/
│   │   │   │   │       ├── accounts/
│   │   │   │   │       ├── categories/
│   │   │   │   │       ├── budgets/
│   │   │   │   │       ├── subscriptions/
│   │   │   │   │       ├── analytics/
│   │   │   │   │       └── settings/
│   │   │   │   └── res/
│   │   │   │       ├── drawable/
│   │   │   │       ├── mipmap-anydpi-v26/
│   │   │   │       ├── mipmap-*/
│   │   │   │       └── values/
│   │   │   ├── src/test/
│   │   │   ├── src/androidTest/
│   │   ├── build.gradle.kts
│   │   ├── settings.gradle.kts
│   │   ├── gradle.properties
│   │   ├── gradlew
│   │   ├── gradlew.bat
│   │   └── gradle/
│   │       └── libs.versions.toml
│   │
│   └── ios/
│       ├── Centwise/
│       │   ├── App/
│       │   │   ├── CentwiseApp.swift
│       │   │   └── Info.plist
│       │   ├── Assets.xcassets/
│       │   │   ├── AppIcon.appiconset/
│       │   │   ├── AppLogo.imageset/
│       │   │   └── AccentColor.colorset/
│       │   ├── Core/
│       │   │   ├── Design/
│       │   │   │   ├── Theme/
│       │   │   │   ├── Components/
│       │   │   │   ├── Glass/
│       │   │   │   └── Formatters/
│       │   │   ├── Navigation/
│       │   │   ├── FFI/
│       │   │   └── Common/
│       │   ├── Data/
│       │   │   ├── Repositories/
│       │   │   ├── Fakes/
│       │   │   └── Backup/
│       │   ├── Platform/
│       │   │   ├── Permissions/
│       │   │   ├── Notifications/
│       │   │   ├── Storage/
│       │   │   └── AppGroup/
│       │   └── Features/
│       │       ├── Onboarding/
│       │       ├── Home/
│       │       ├── Transactions/
│       │       ├── Accounts/
│       │       ├── Categories/
│       │       ├── Budgets/
│       │       ├── Subscriptions/
│       │       ├── Analytics/
│       │       └── Settings/
│       ├── CentwiseIntents/
│       ├── CentwiseShareExtension/
│       └── Centwise.xcodeproj/
│
├── core/
│   ├── Cargo.toml
│   ├── centwise-domain/
│   │   └── src/
│   ├── centwise-normalization/
│   │   └── src/
│   ├── centwise-parser/
│   │   └── src/
│   │       ├── common/
│   │       ├── banks/
│   │       └── mfs/
│   ├── centwise-categorization/
│   │   └── src/
│   ├── centwise-db/
│   │   ├── src/
│   │   │   └── migrations/
│   │   ├── schemas/
│   │   └── tests/
│   ├── centwise-ffi/
│   │   ├── src/
│   │   ├── build.rs
│   │   └── uniffi.toml
│   └── uniffi-bindgen/
│       └── src/
│
├── assets/
│   ├── logo.jpeg
│   ├── banner.png
│   └── screenshots/
│
├── fixtures/
│   ├── sms/
│   │   ├── bkash/
│   │   ├── nagad/
│   │   ├── rocket/
│   │   └── banks/
│   └── expected/
│
├── docs/
│   ├── architecture/
│   ├── features/
│   ├── decisions/
│   ├── providers/
│   └── supported-providers.json
│
├── crowdin/
│   └── glossary.csv
├── crowdin.yml
├── fastlane/
│   ├── Fastfile
│   ├── Appfile
│   └── metadata/
│       ├── android/
│       └── ios/
├── scripts/
├── .github/
│   ├── workflows/
│   ├── ISSUE_TEMPLATE/
│   └── pull_request_template.md
├── .gitignore
├── .gitmessage
├── README.md
├── setup.md
├── tech.md
├── features.md
├── ios_shortcut.md
├── PRIVACY.md
├── SECURITY.md
├── CONTRIBUTING.md
└── LICENSE
```

## Feature Ownership

Every feature owns its own boundary:

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

The same feature shape is used on Android and iOS. A developer assigned to
Transactions can work mostly inside `Features/Transactions` on both platforms.

## Dependency Rules

1. Features may depend on `core`, `data` interfaces, and shared design systems.
2. Features must not import another feature’s private screens, ViewModels, or repositories.
3. Features communicate through navigation contracts, use cases, and shared domain models.
4. Platform code may call the Rust core and data repositories, but UI code does not call Rust directly.
5. Rust must not depend on Android, iOS, UI, notification, or database frameworks.
6. Bank parsers must not depend on UI or app database code.
7. New providers are added only under the Rust parser provider folders and fixtures.
8. Database tables are accessed through repositories, not directly from screens.

## Database Ownership

```text
Logical model:           core/centwise-domain
Database and migrations: core/centwise-db, single source of truth
Schema snapshots:        core/centwise-db/schemas/
Platform repositories:   apps/android and apps/ios data layers call the
                         database through the Rust FFI
Contract document:       docs/architecture/data-model.md
```

The database file lives in a platform-provided location (App Group container on
iOS so the app, App Intents, and Share Extension share one file) and is opened
by path. All reads and writes go through Rust so migrations, aggregation
queries, and change notifications behave identically on Android and iOS, and
cross-platform backups are compatible by construction.

## Adding a New Feature

1. Add the domain model or interface to Rust only if it is genuinely shared.
2. Add the Android feature folder and tests.
3. Add the iOS feature folder and tests.
4. Add repository/database changes through a versioned migration.
5. Add backup compatibility handling if stored data changes.
6. Add navigation only through the central navigation contract.
7. Add localized strings to the localization source files.
8. Add CI tests before merging.

## Adding a New Bank or MFS Provider

1. Add the provider entry to `docs/supported-providers.json`.
2. Add anonymized SMS fixtures under `fixtures/sms/`.
3. Add the Rust parser under the correct bank or MFS folder.
4. Add expected parser results under `fixtures/expected/`.
5. Run Rust parser tests.
6. Update generated support documentation if applicable.

## Scaling Without Premature Complexity

At the beginning, Android can remain one Gradle application module and iOS can
remain one Xcode application target plus the Intent and Share Extension targets.
When a feature becomes large or needs independent ownership, it can be moved
into a dedicated Gradle module, Swift package, or Rust crate without changing
the public feature contract.

The structure gives teams clear ownership without creating dozens of modules
before the product needs them.


# Centwise

Bangladesh-focused personal finance app that turns bank and MFS SMS messages
into tracked transactions automatically — privately, on your device.

```text
bKash / Nagad / Rocket / Bank SMS  →  Centwise  →  Transactions, budgets, insights
```

## Highlights

- **Automatic tracking** from bKash, Nagad, Rocket, and Bangladeshi bank SMS
  (bKash → Nagad → Rocket first; banks following)
- **Bangla + English** message parsing, including Bangla numerals
- **Local-first & private** — everything stays on your phone. No account,
  no cloud, no upload
- **Budgets & analytics** — category budgets, spending trends, top merchants
- **Smart Rules** — auto-categorize by merchant keyword
- **Native apps** — SwiftUI on iOS, Jetpack Compose on Android
- **One shared core** — parsing, database, and queries written once in Rust

## Architecture

```text
apps/android      Kotlin + Jetpack Compose (native UI)
apps/ios          Swift + SwiftUI (native UI, App Intents, Share Extension)
core/             Rust: domain models, SQLite database, parsers, UniFFI bindings
fixtures/sms      Anonymized SMS samples = parser test data
docs/             Architecture, decisions, provider catalog, status
```

One Rust-owned SQLite database with a single migration runner serves both
platforms; ViewModels stay native and thin. See
[docs/decisions/0001-single-rust-database.md](docs/decisions/0001-single-rust-database.md).

## Development

```bash
# Rust core (tests, lint)
cd core
cargo test --workspace
cargo fmt --all --check
cargo clippy --workspace --all-targets

# Android
cd apps/android && ./gradlew :app:assembleDebug

# iOS (requires macOS + Xcode)
open apps/ios/Centwise.xcodeproj
```

Binding generation and native integration steps: [core/README.md](core/README.md).

## Documentation

| File | Purpose |
|---|---|
| [docs/STATUS.md](docs/STATUS.md) | What's done / next / later |
| [CHANGELOG.md](CHANGELOG.md) | All notable changes |
| [tech.md](tech.md) | Technology and architecture |
| [features.md](features.md) | Product features |
| [setup.md](setup.md) | Project setup checklists |
| [docs/architecture/parser-design.md](docs/architecture/parser-design.md) | SMS parser design |
| [docs/supported-providers.json](docs/supported-providers.json) | Supported banks & MFS providers |
| [PRIVACY.md](PRIVACY.md) | Privacy and SMS data handling |
| [AGENTS.md](AGENTS.md) | Developer and agent guide |

## Contributing

Read [AGENTS.md](AGENTS.md) first — it defines structure, testing rules, and
the provider fixture workflow. Never commit real SMS data, account numbers,
or secrets (see `.gitignore`).

## License

Proprietary — license decision pending (see `setup.md`).

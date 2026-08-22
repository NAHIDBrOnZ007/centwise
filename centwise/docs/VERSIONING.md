# Centwise Versioning

Single source of truth for app versions. Keep in sync with `CHANGELOG.md`.

## Current version

**0.1.0** (build 1) — first working local build, UI on fake data.

## Where versions live

| Platform | File | Field |
|---|---|---|
| Android | `apps/android/app/build.gradle.kts` | `versionCode` / `versionName` |
| iOS | `apps/ios/project.yml` | `MARKETING_VERSION` / `CURRENT_PROJECT_VERSION` |
| iOS plist | `apps/ios/Centwise/App/Info.plist` | `CFBundleShortVersionString` / `CFBundleVersion` |
| Rust core | `core/Cargo.toml` crates | per-crate semver (independent of app version) |

All three iOS places must match. Android `versionCode` and iOS
`CURRENT_PROJECT_VERSION` always increase by 1 per release and never reset.

## Roadmap versions (from CHANGELOG.md)

| Version | Meaning |
|---|---|
| 0.1.0 | First working local build: real Rust DB behind Home screen |
| 0.2.0 | SMS ingestion (bKash, Nagad, Rocket) end-to-end |
| 1.0.0 | First store release candidate |

## Rules

1. Semantic versioning: `MAJOR.MINOR.PATCH`.
2. Bump PATCH for fixes, MINOR for features, MAJOR for store-grade releases.
3. Bump versions in the same commit that ships the feature.
4. Never ship a store build with version 1.0.0 unless it passes the 1.0.0
   checklist (release readiness in `docs/BUILD-PLAN.md` Phase 7).
5. Update `CHANGELOG.md` with every version bump.

# Repository Guidelines

## Project Structure
- `app/` – Android app module with Compose UI, navigation host, DI wiring (Hilt), controllers, and assets.
- `data/` – Android library providing Room entities/DAOs, repositories, pack parser/downloader, and Preferences DataStore-backed settings.
- `domain/` – Pure Kotlin module implementing the deterministic game engine and shared models (no Android dependencies).
- `scripts/` – Helper scripts for builds, SDK setup, reproducibility snapshots, and pack downloads.

## Tooling & Versions
- Requires JDK 21 and Android SDK 34. The Gradle Wrapper configures AGP 8.5.2 and Kotlin 1.9.23 via `gradle/libs.versions.toml`.
- Compose compiler 1.5.11, Compose BOM 2024.02.00, Hilt 2.51, Room 2.6.1.
- Formatting uses Spotless (ktlint 0.50.0). Static analysis runs with Detekt 1.23.8 (reports enabled, failures ignored locally).

## Build & Test Commands
Run these before pushing or opening a PR:
- `./gradlew spotlessCheck detekt` – Formatting + static analysis.
- `./gradlew :domain:test :data:test :app:testDebugUnitTest` – JVM/unit tests for every module.
- `./gradlew :app:assembleDebug` – Build debug APK for smoke-testing.
- Optional: `./gradlew :app:assembleDevRelease` for a release-like, installable build.
- Convenience script: `scripts/dev-build.sh` runs tests and assembles the debug APK; pass `--clean`/`--no-tests` as needed.

## Coding Conventions
- Kotlin official style; keep files Spotless-compliant.
- Keep `domain` deterministic and Android-free. Inject randomness via seeds and expose immutable state through flows.
- Surface business logic in controllers/repositories, not Compose UI. Keep composables focused on rendering state and invoking callbacks.
- When touching Room schemas, add migrations in `data/.../db/Migrations.kt` and adjust tests; no destructive fallback in release or `devRelease`.
- Pack import changes must update parser, validator, and documentation together. Preserve security constraints (HTTPS-only, allow-listed hosts, checksum verification).
- Local storage only: no telemetry, analytics, or background networking. Any new network feature must be opt-in, HTTPS, and policy-checked.

## Testing Notes
- `domain` tests live under `domain/src/test` and should stay deterministic (use fake clocks/seeds where necessary).
- `data` tests can use in-memory Room and MockWebServer. Avoid Robolectric unless absolutely required.
- `app` tests currently cover controllers and localization; Compose UI tests are limited—add them if contributing UI features.

## Assets & Localization
- Bundled decks reside in `app/src/main/assets/decks/` and are hashed to detect updates/deletions. Update hashes/migrations if assets change.
- Strings are localized in English and Russian. New UI must externalize strings and provide translations.

## Security & Privacy
- Manual pack downloads must respect the trusted-host allow list and 40 MB cap (`PackDownloader`).
- Do not commit secrets or keystores. Bundle only vetted assets with attribution metadata.
- Provide “Reset local data” pathways when adding new persistent storage.

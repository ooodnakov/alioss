# Repository Guidelines

## Project Structure & Module Organization
- `app/`: Android app built with Jetpack Compose UI and Hilt wiring. Assets live in `app/src/main/assets/` (e.g., bundled `decks/`).
- `data/`: Android library for Room entities/DAOs, repositories, JSON pack parser, preferences storage, and optional pack download helpers.
- `domain/`: Pure Kotlin game engine and immutable state machines. No Android dependencies allowed.
- `scripts/`: Helper shell scripts (e.g., `dev-build.sh`, `run-domain-tests.sh`, `download-pack.sh`, `setup-android-env.sh`).
- `.github/workflows/`: GitHub Actions CI that runs formatting, detekt, unit tests for all modules, and assembles the debug APK.

## Tooling, Build, and Test Commands
- Requires the Java 21 toolchain and Kotlin 1.9.x (configured via Gradle wrappers/toolchains).
- Formatting: `./gradlew spotlessApply` (Spotless + ktlint). Check with `./gradlew spotlessCheck`.
- Static analysis: `./gradlew detekt` (uses `detekt.yml`).
- Unit tests: `./gradlew :domain:test :data:test :app:testDebugUnitTest`.
- Build APK: `./gradlew :app:assembleDebug`.
- Convenience scripts:
  - `scripts/dev-build.sh` — Runs Spotless check, detekt, all unit tests, and assembles the debug APK.
  - `scripts/run-domain-tests.sh` — Runs only the domain module tests.
  - `scripts/assemble-apk.sh` — Assembles the debug APK after ensuring dependencies.
- Headless Android SDK setup (CI/Linux): `scripts/setup-android-env.sh` (installs CLI tools, accepts licenses, optional build).

## Coding Style & Conventions
- Kotlin official code style (`gradle.properties`). Tooling is enforced by Spotless (ktlint).
- Target JVM/Java version 21 across modules.
- Package naming: lowercase (e.g., `com.example.alias.domain`).
- Class/Object naming: `PascalCase`; functions/vars: `lowerCamelCase`; constants: `UPPER_SNAKE_CASE`.
- Suffix conventions: `ViewModel`, `Dao`, `Entity`, `Repository`, etc.
- `domain` must remain platform-agnostic (pure Kotlin, deterministic, seed-driven logic).

## Testing Guidelines
- `domain`: Pure JVM tests with `kotlin("test")` and `kotlinx-coroutines-test`. Keep tests deterministic (same seed ⇒ same order).
- `data`: JVM Robolectric-friendly tests for Room/JSON/HTTP helpers; prefer in-memory DB and mocked HTTP via OkHttp MockWebServer.
- `app`: Compose/Robolectric unit tests live under `app/src/test/`. Favor state-driven tests over timing-based ones.
- Avoid flakiness: use virtual time for coroutine tests and deterministic seeds for sampling logic.

## Commit & Pull Request Guidelines
- Commits: concise, present-tense subjects; include optional scope tags (e.g., `[domain] Implement skip penalty`).
- Before opening a PR, ensure the following pass locally: `./gradlew spotlessCheck detekt :domain:test :data:test :app:testDebugUnitTest :app:assembleDebug`.
- PRs should remain focused and include:
  - Summary, motivation, and modules touched.
  - Screenshots or short clips for UI changes.
  - Steps to reproduce/test locally and any migration notes.
  - Confirmation that the CI-equivalent commands above succeed.

## Security & Configuration Tips
- No telemetry, ads, background networking, or dynamic code loading. Optional pack downloads must use allow-listed HTTPS origins with hash/signature verification.
- Do not commit secrets or keystores. Bundle only vetted assets under `assets/decks/`.
- Validate pack inputs (sizes, languages, difficulty range) and enforce reasonable limits to avoid oversized assets.

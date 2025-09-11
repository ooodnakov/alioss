# Repository Guidelines

## Project Structure & Module Organization
- `app/`: Android app (Jetpack Compose UI, Hilt wiring). Assets in `app/src/main/assets/` (e.g., `decks/`).
- `data/`: Room entities/DAOs, repository, and pack parser (`data/.../db`, `data/.../pack`).
- `domain/`: Pure Kotlin game engine and immutable state. No Android deps.
- `scripts/`: Helpers for local/dev CI (e.g., `setup-android-env.sh`).

## Build, Test, and Development Commands
- Build APK: `./gradlew assembleDebug` — produces a debug build of `app`.
- Domain tests: `./gradlew domain:test` — runs JVM unit tests for the engine.
- Clean: `./gradlew clean` — removes build outputs.
- Headless setup (Linux): `scripts/setup-android-env.sh` — installs SDK CLI tools and builds.

## Coding Style & Naming Conventions
- Kotlin official style (`gradle.properties`), JVM target 17.
- Packages: lowercase (e.g., `com.example.alias.domain`).
- Classes/objects: `PascalCase`; functions/vars: `lowerCamelCase`; constants: `UPPER_SNAKE_CASE`.
- Suffixes: `ViewModel`, `Dao`, `Entity`, `Repository`.
- `domain` must remain platform-agnostic (no Android). Keep logic deterministic (accept a seed).

## Testing Guidelines
- Frameworks: `kotlin("test")`, `kotlinx-coroutines-test` in `domain`.
- Place unit tests under `domain/src/test/kotlin/...`; name files `FooTest.kt`.
- Prefer property-like tests for determinism (same seed → same order). Avoid time-based flakiness; use virtual time in coroutines tests.
- For Room, use in-memory DB in JVM or add Android tests later (Compose/Instrumented) as needed.

## Commit & Pull Request Guidelines
- Commits: clear, present-tense subject; include scope in brackets when helpful (e.g., `[domain] Implement skip penalty`).
- Keep PRs focused. Include:
  - Summary, motivation, and module(s) touched.
  - Screenshots or short clips for UI changes.
  - Steps to test locally and any migration notes.
  - Ensure `./gradlew domain:test` and `assembleDebug` pass locally.

## Security & Configuration Tips
- No telemetry, ads, or background networking. If adding downloads, enforce allow-listed HTTPS origins and hash/signature checks.
- Do not commit secrets or keystores. Bundle only data under `assets/decks/`.
- Validate pack inputs (sizes, languages, difficulty range) and avoid oversized assets.


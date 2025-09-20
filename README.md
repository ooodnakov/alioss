# Alias (Local-Only, Pass-and-Play)

Alias is a privacy-first, offline-only party game for Android built with Kotlin and Jetpack Compose. The project targets a relaxed, pass-and-play experience: no ads, analytics, accounts, or background networking. Gameplay focuses on predictable state, reusable decks, and local-first storage so the app works completely without internet access.

## Project Status

The current app (version 0.2) is feature-complete for local multiplayer nights:

- ✅ Bundled English and Russian decks are imported on first launch with deterministic word ordering.
- ✅ Compose-driven navigation covers Home, Game, Decks, Deck Detail, Settings, History, and About screens.
- ✅ Turn flow includes timer controls, swipe/press input, haptics/sounds, score-to-target mode, and tutorial overlays for new players.
- ✅ Deck management supports enable/disable toggles, metadata filters (language, difficulty, categories, word classes), trusted-source downloads, JSON file imports, cover images, and per-deck statistics.
- ✅ Turn history persists outcomes in Room, powers a recents feed, and surfaces deck word samples and difficulty histograms.

Roadmap highlights: richer statistics, automated migrations without destructive fallbacks, and deeper test coverage. See `TODO.md` for the evolving backlog.

## Feature Highlights

### Gameplay & Match Flow
- Deterministic word queue seeded per match for reproducible ordering.【F:domain/src/main/kotlin/com/example/alias/domain/DefaultGameEngine.kt†L56-L86】【F:domain/src/main/kotlin/com/example/alias/domain/DefaultGameEngine.kt†L141-L158】
- Supports 2–6 teams with reorderable names, configurable round length, target score/words, skip penalties, and orientation lock.【F:app/src/main/java/com/example/alias/ui/settings/SettingsScreen.kt†L70-L177】【F:data/src/main/java/com/example/alias/data/settings/SettingsRepository.kt†L90-L157】
- Timer-driven turns with resume/pause states, swipe gestures (up=correct, down=skip), one-handed layout mode, and peek-next-word helper.【F:domain/src/main/kotlin/com/example/alias/domain/DefaultGameEngine.kt†L207-L270】【F:data/src/main/java/com/example/alias/data/settings/SettingsRepository.kt†L120-L148】【F:domain/src/main/kotlin/com/example/alias/domain/GameEngine.kt†L37-L60】
- Audio and haptic cues for countdowns, correct/skip actions, and turn boundaries, all toggleable in settings.【F:data/src/main/java/com/example/alias/data/settings/SettingsRepository.kt†L108-L138】
- Turn summaries support outcome overrides that recalculate scores and respect goal completion logic.【F:domain/src/main/kotlin/com/example/alias/domain/DefaultGameEngine.kt†L233-L305】

### Deck Management
- Bundled decks live under `app/src/main/assets/decks/` and are hashed to detect changes or deletions across launches.【F:app/src/main/assets/decks/general_en.json†L1-L40】【F:app/src/main/java/com/example/alias/DeckManager.kt†L55-L153】
- Decks can be imported from JSON via Storage Access Framework or downloaded from allow-listed HTTPS hosts with optional SHA-256 verification.【F:app/src/main/java/com/example/alias/ui/decks/DecksScreen.kt†L57-L152】【F:data/src/main/java/com/example/alias/data/download/PackDownloader.kt†L17-L89】
- Cover images are parsed, validated, and persisted; failures fall back to snackbar notifications without crashing the flow.【F:app/src/main/java/com/example/alias/MainViewModel.kt†L65-L118】【F:data/src/main/java/com/example/alias/data/pack/CoverImageException.kt†L1-L32】
- Deck detail view surfaces counts, difficulty histograms, recent words, category/word-class chips, and safe delete flows (including permanent removal for imports).【F:app/src/main/java/com/example/alias/ui/decks/DeckDetailScreen.kt†L18-L162】【F:app/src/main/java/com/example/alias/MainViewModel.kt†L119-L227】
- Filters persist across sessions for difficulty range, languages, categories, and word classes, gating which decks feed the word queue.【F:app/src/main/java/com/example/alias/ui/decks/DecksScreen.kt†L90-L210】【F:app/src/main/java/com/example/alias/DeckManager.kt†L155-L220】

### Settings, Localization & Accessibility
- Full English/Russian localization (strings, team suggestions, onboarding) with runtime language toggle and system-default mode.【F:app/src/main/java/com/example/alias/ui/settings/SettingsScreen.kt†L70-L177】【F:data/src/main/java/com/example/alias/data/settings/SettingsRepository.kt†L103-L139】
- Tutorial overlay appears on first play and persists dismissal state via DataStore.【F:app/src/main/java/com/example/alias/MainViewModel.kt†L98-L136】
- Trusted source editor allows normalized host/origin entries, powering safe pack downloads.【F:app/src/main/java/com/example/alias/ui/decks/DecksScreen.kt†L147-L210】【F:app/src/main/java/com/example/alias/SettingsController.kt†L16-L63】
- Orientation lock, one-handed layout tweaks, vertical swipe toggle, haptic/audio switches, and NSFW/difficulty gates tailor the experience for different groups.【F:app/src/main/java/com/example/alias/ui/settings/SettingsScreen.kt†L70-L177】【F:data/src/main/java/com/example/alias/data/settings/SettingsRepository.kt†L90-L157】

### History & Analytics
- Every turn outcome persists to `turn_history` with difficulty metadata, enabling resettable history feeds and deck-specific recents.【F:data/src/main/java/com/example/alias/data/db/TurnHistoryDao.kt†L1-L33】【F:app/src/main/java/com/example/alias/GameController.kt†L33-L71】
- Home screen surfaces the latest results and provides resume/continue affordances alongside settings/decks shortcuts.【F:app/src/main/java/com/example/alias/navigation/AliasNavHost.kt†L42-L78】
- Deck samples and histograms use DAO queries to compute stats without duplicating logic at the UI layer.【F:app/src/main/java/com/example/alias/ui/decks/DeckDetailScreen.kt†L37-L118】【F:data/src/main/java/com/example/alias/data/db/DeckDao.kt†L1-L120】

## Architecture & Modules

```
app/     – Android app: Compose UI, navigation, DI wiring, controllers.
data/    – Android library: Room entities/DAOs, repositories, pack parser/downloader, DataStore settings.
domain/  – Pure Kotlin engine handling deterministic match state and rules.
scripts/ – Automation helpers for builds, downloads, reproducibility snapshots.
```

- Dependency injection is powered by Hilt with module wiring in `AppModule.kt` and scoped controllers in the app module.【F:app/src/main/java/com/example/alias/AppModule.kt†L1-L120】
- Room manages deck metadata, word storage, word classes, and turn history with migrations housed in `data/src/main/java/com/example/alias/data/db/Migrations.kt`.【F:data/src/main/java/com/example/alias/data/db/Migrations.kt†L1-L160】
- Settings persist via Preferences DataStore and stream into Compose screens through `SettingsController`/`MainViewModel`.【F:app/src/main/java/com/example/alias/SettingsController.kt†L12-L84】【F:app/src/main/java/com/example/alias/MainViewModel.kt†L27-L117】
- The domain module exports the `GameEngine` interface and `DefaultGameEngine` implementation, ensuring platform-agnostic logic that tests can exercise without Android dependencies.【F:domain/src/main/kotlin/com/example/alias/domain/GameEngine.kt†L1-L78】【F:domain/src/main/kotlin/com/example/alias/domain/DefaultGameEngine.kt†L1-L205】

## Data Packs & Formats

- Bundled decks ship as JSON (`alias-deck@1`) and live under `app/src/main/assets/decks/`. Deck hashes are tracked to avoid redundant imports and respect decks that the user deleted manually.【F:app/src/main/java/com/example/alias/DeckManager.kt†L55-L153】
- Import sources:
  - **JSON/JSONL**: Parsed by `PackParser`, validated via `PackValidator`, and inserted atomically to replace existing deck content.【F:data/src/main/java/com/example/alias/data/pack/PackParser.kt†L1-L188】【F:data/src/main/java/com/example/alias/data/DeckRepository.kt†L40-L74】
  - **CSV**: Supported through the parser for lightweight bulk additions (semicolon-delimited stems).【F:data/src/main/java/com/example/alias/data/pack/PackParser.kt†L100-L173】
  - **ZIP**: Optional wrapper containing deck metadata, words, optional signatures, and cover art; images are decoded and stored with each deck.【F:data/src/main/java/com/example/alias/data/pack/PackParser.kt†L174-L254】【F:data/src/main/java/com/example/alias/data/pack/CoverImageException.kt†L1-L32】
- Downloads require HTTPS and an allow-listed host/origin, enforce a 40 MB cap, disable redirects, and optionally verify SHA-256 checksums.【F:data/src/main/java/com/example/alias/data/download/PackDownloader.kt†L20-L89】

## Build, Run & Test

- **Requirements**: JDK 21, Android SDK 34, Gradle Wrapper (7.6+) bundled via `./gradlew`. Kotlin 1.9.23 and AGP 8.5.2 are configured in `gradle/libs.versions.toml`.【F:gradle/libs.versions.toml†L1-L44】
- **Android Studio**: Open the root project, let Gradle sync, and run the `app` configuration on an emulator or device. `devRelease` build type produces a release-like APK signed with the debug key for easier installs.【F:app/build.gradle.kts†L33-L71】
- **Command line**:
  - `./gradlew :app:assembleDebug` – Build debug APK.
  - `./gradlew :domain:test :data:test :app:testDebugUnitTest` – Run JVM/unit tests across modules.
  - `./gradlew spotlessCheck detekt` – Enforce formatting (ktlint) and static analysis via Detekt (configured to ignore failures locally but still report issues).【F:build.gradle.kts†L1-L92】
  - `./gradlew :app:assembleDevRelease` – Build the installable release-like variant.
- **Scripts**:
  - `scripts/dev-build.sh` – Optional helper to clean, run JVM tests, and assemble the debug APK in one command.【F:scripts/dev-build.sh†L1-L37】
  - `scripts/assemble-apk.sh` – Assemble APK after verifying the Android SDK/NDK presence.
  - `scripts/download-pack.sh` – Fetch a pack over HTTPS with optional checksum and allow-list enforcement (mirrors in-app rules).【F:scripts/download-pack.sh†L1-L55】
  - `scripts/repro-snapshot.sh` – Emit dependency versions and APK hashes for reproducibility notes.【F:scripts/repro-snapshot.sh†L1-L28】
  - `scripts/setup-android-env.sh` – Headless SDK installation and license acceptance for CI or fresh Linux machines.

## Security, Privacy & Networking

- No telemetry, analytics, ads, or background networking. Networking only occurs when the user explicitly downloads a deck, and even then only against allow-listed HTTPS origins with strict size and checksum policies.【F:data/src/main/java/com/example/alias/data/download/PackDownloader.kt†L17-L89】
- Deck imports validate schemas, enforce NSFW flags, sanitize stems, and reject malformed cover images to prevent crashes.【F:data/src/main/java/com/example/alias/data/pack/PackValidator.kt†L1-L180】【F:data/src/main/java/com/example/alias/data/pack/CoverImageException.kt†L1-L32】
- Settings, deck metadata, and history live entirely on-device via Room and DataStore; “Reset local data” clears everything without touching system storage beyond the app sandbox.【F:app/src/main/java/com/example/alias/MainViewModel.kt†L221-L286】【F:data/src/main/java/com/example/alias/data/settings/SettingsRepository.kt†L150-L189】

## Testing & Quality

- JVM unit tests cover the game engine, deck repository, DataStore wiring, localization, downloader policies, and Room migrations (in-memory DB).【F:domain/src/test/kotlin/com/example/alias/domain/DefaultGameEngineTest.kt†L1-L200】【F:data/src/test/java/com/example/alias/data/DeckRepositoryTest.kt†L1-L200】
- Compose/UI tests are currently minimal; contributions that add instrumentation or Robolectric coverage are welcome.
- Spotless (ktlint 0.50.0) enforces Kotlin style; Detekt runs with `detekt.yml` tuned for this project. Both run in CI and via the Gradle tasks above.【F:build.gradle.kts†L1-L116】

## Contributing

- Keep the `domain` module Android-free and deterministic. Inject randomness via seeds so tests stay reproducible.【F:domain/src/main/kotlin/com/example/alias/domain/DefaultGameEngine.kt†L56-L86】
- Prefer immutable state models flowing from repositories/view models into Compose; business logic belongs in controllers or repositories, not UI composables.
- Run `./gradlew spotlessCheck detekt :domain:test :data:test :app:testDebugUnitTest` before pushing a PR. CI mirrors these tasks along with APK assembly.
- When touching deck formats, update documentation and validators together, and ensure migrations cover schema changes (no destructive fallback in release/`devRelease`).【F:data/build.gradle.kts†L13-L43】

## License

A license has not been finalized. MPL-2.0 or Apache-2.0 remain likely candidates. Bundled and imported decks must respect their original licenses; attribution metadata is surfaced in-app where provided.【F:app/src/main/java/com/example/alias/ui/decks/DeckDetailScreen.kt†L18-L162】

## Acknowledgements

Alias is inspired by the classic Alias™ gameplay but is an independent, open implementation with no affiliation to the trademark holders.

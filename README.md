# Alioss (Local-Only, Pass-and-Play)

Alioss is a privacy-first, offline-only party game for Android built with Kotlin and Jetpack Compose. The project targets a relaxed, pass-and-play experience: no ads, analytics, accounts, or background networking. Gameplay focuses on predictable state, reusable decks, and local-first storage so the app works completely without internet access.

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
- Deterministic word queue seeded per match for reproducible ordering.
- Supports 2–6 teams with reorderable names, configurable round length, target score/words, skip penalties, and orientation lock.
- Timer-driven turns with resume/pause states, swipe gestures (up=correct, down=skip), one-handed layout mode, and peek-next-word helper.
- Audio and haptic cues for countdowns, correct/skip actions, and turn boundaries, all toggleable in settings.
- Turn summaries support outcome overrides that recalculate scores and respect goal completion logic.

### Deck Management
- Bundled decks live under `app/src/main/assets/decks/` and are hashed to detect changes or deletions across launches.
- Decks can be imported from JSON via Storage Access Framework or downloaded from allow-listed HTTPS hosts with optional SHA-256 verification.
- Cover images are parsed, validated, and persisted; failures fall back to snackbar notifications without crashing the flow.
- Deck detail view surfaces counts, difficulty histograms, recent words, category/word-class chips, and safe delete flows (including permanent removal for imports).
- Filters persist across sessions for difficulty range, languages, categories, and word classes, gating which decks feed the word queue.

### Settings, Localization & Accessibility
- Full English/Russian localization (strings, team suggestions, onboarding) with runtime language toggle and system-default mode.
- Tutorial overlay appears on first play and persists dismissal state via DataStore.
- Trusted source editor allows normalized host/origin entries, powering safe pack downloads.
- Orientation lock, one-handed layout tweaks, vertical swipe toggle, haptic/audio switches, and NSFW/difficulty gates tailor the experience for different groups.

### History & Analytics
- Every turn outcome persists to `turn_history` with difficulty metadata, enabling resettable history feeds and deck-specific recents.
- Home screen surfaces the latest results and provides resume/continue affordances alongside settings/decks shortcuts.
- Deck samples and histograms use DAO queries to compute stats without duplicating logic at the UI layer.

## Architecture & Modules

```
app/     – Android app: Compose UI, navigation, DI wiring, controllers.
data/    – Android library: Room entities/DAOs, repositories, pack parser/downloader, DataStore settings.
domain/  – Pure Kotlin engine handling deterministic match state and rules.
scripts/ – Automation helpers for builds, downloads, reproducibility snapshots.
```

- Dependency injection is powered by Hilt with module wiring in `AppModule.kt` and scoped controllers in the app module.
- Room manages deck metadata, word storage, word classes, and turn history with migrations housed in `data/src/main/java/com/example/alioss/data/db/Migrations.kt`.
- Settings persist via Preferences DataStore and stream into Compose screens through `SettingsController`/`MainViewModel`.
- The domain module exports the `GameEngine` interface and `DefaultGameEngine` implementation, ensuring platform-agnostic logic that tests can exercise without Android dependencies.

## Data Packs & Formats

- Bundled decks ship as JSON (`alioss-deck@1`) and live under `app/src/main/assets/decks/`. Deck hashes are tracked to avoid redundant imports and respect decks that the user deleted manually.
- Deck metadata may include an optional `author` field; when provided it is validated, stored alongside the deck, and displayed in the decks UI for attribution.
- JSON packs are parsed by `PackParser`, validated via `PackValidator`, and inserted atomically to replace existing deck content.
- Downloads require HTTPS and an allow-listed host/origin, enforce a 40 MB cap, disable redirects, and optionally verify SHA-256 checksums.

## Build, Run & Test

- **Requirements**: JDK 21, Android SDK 34, Gradle Wrapper (7.6+) bundled via `./gradlew`. Kotlin 1.9.23 and AGP 8.5.2 are configured in `gradle/libs.versions.toml`.
- **Android Studio**: Open the root project, let Gradle sync, and run the `app` configuration on an emulator or device. `devRelease` build type produces a release-like APK signed with the debug key for easier installs.
- **Command line**:
  - `./gradlew :app:assembleDebug` – Build debug APK.
  - `./gradlew :domain:test :data:test :app:testDebugUnitTest` – Run JVM/unit tests across modules.
  - `./gradlew detekt` – Enforce formatting (via `detekt-formatting` auto-correct) and static analysis using the shared `detekt.yml`.
  - `./gradlew :app:assembleDevRelease` – Build the installable release-like variant.
- **Scripts**:
  - `scripts/dev-build.sh` – Optional helper to clean, run JVM tests, and assemble the debug APK in one command.
  - `scripts/assemble-apk.sh` – Assemble APK after verifying the Android SDK/NDK presence.
  - `scripts/download-pack.sh` – Fetch a pack over HTTPS with optional checksum and allow-list enforcement (mirrors in-app rules).
  - `scripts/repro-snapshot.sh` – Emit dependency versions and APK hashes for reproducibility notes.
  - `scripts/setup-android-env.sh` – Headless SDK installation and license acceptance for CI or fresh Linux machines.

## Security, Privacy & Networking

- No telemetry, analytics, ads, or background networking. Networking only occurs when the user explicitly downloads a deck, and even then only against allow-listed HTTPS origins with strict size and checksum policies.
- Deck imports validate schemas, enforce NSFW flags, sanitize stems, and reject malformed cover images to prevent crashes.
- Settings, deck metadata, and history live entirely on-device via Room and DataStore; “Reset local data” clears everything without touching system storage beyond the app sandbox.

## Testing & Quality

- JVM unit tests cover the game engine, deck repository, DataStore wiring, localization, downloader policies, and Room migrations (in-memory DB).
- Compose/UI tests are currently minimal; contributions that add instrumentation or Robolectric coverage are welcome.
- Detekt with the `detekt-formatting` plugin enforces Kotlin style (auto-correct enabled) and static analysis using `detekt.yml`. CI mirrors the Gradle task above.

## Contributing

- Keep the `domain` module Android-free and deterministic. Inject randomness via seeds so tests stay reproducible.
- Prefer immutable state models flowing from repositories/view models into Compose; business logic belongs in controllers or repositories, not UI composables.
- Run `./gradlew detekt :domain:test :data:test :app:testDebugUnitTest` before pushing a PR. CI mirrors these tasks along with APK assembly.
- When touching deck formats, update documentation and validators together, and ensure migrations cover schema changes (no destructive fallback in release/`devRelease`).

## License

A license has not been finalized. MPL-2.0 or Apache-2.0 remain likely candidates. Bundled and imported decks must respect their original licenses; attribution metadata is surfaced in-app where provided.

## Acknowledgements

Alioss is inspired by classic word-guessing party gameplay but is an independent, open implementation with no affiliation to trademark holders.

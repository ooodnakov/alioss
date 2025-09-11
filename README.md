# Alias (Local-Only, Pass-and-Play)

A lean, privacy-first, local-only Alias-style party game for Android built with Kotlin + Jetpack Compose. No ads, no telemetry, no accounts, and no background networking. The only network feature planned is optional, manual deck pack downloads from user-allow‑listed URLs using open formats.

Status: early skeleton wired end-to-end — sample deck import, Room schema, Hilt DI, and a minimal Compose UI that exercises the domain game engine.


**What You Get Today**
- Local-only pass-and-play loop with a basic UI.
- Deterministic word order with a seed; no repeats during the target window.
- Minimal state machine: timed turns, correct/skip, next turn, match summary.
- Data layer with Room, JSON pack parser, and a bundled sample deck.


**Non-Goals**
- No monetization, ads, analytics, crash reporting, A/B, or push.
- No accounts, no cloud saves, no online play.


## Tech Stack
- UI: Jetpack Compose, Material 3
- DI: Hilt
- Concurrency: Coroutines + Flow
- Local storage: Room (SQLite)
- I/O: Kotlinx Serialization (JSON packs)
- Build: Gradle (AGP), Kotlin 1.9.x, Java 17 toolchain


## Architecture
- Modules
  - `app`: Compose UI, navigation entry, DI wiring.
  - `domain`: Pure Kotlin game engine (state machine, scoring, timer).
  - `data`: Repositories for Decks/Words (Room) + JSON pack parser.

- Game State Machine
  - Idle → TurnActive → TurnFinished → MatchFinished
  - Actions: `startMatch(config, teams, seed)`, `correct()`, `skip()`, `nextTurn()`

- Determinism
  - Word order uses a provided seed; per-match seed supports reproducibility.


## Repository Layout
- `app/src/main/assets/decks/sample_en.json`: Bundled sample deck (JSON format).
- `data/src/main/java/com/example/alias/data/db`: Room entities/DAOs and database.
- `data/src/main/java/com/example/alias/data/pack`: JSON deck pack parser.
- `domain/src/main/kotlin/com/example/alias/domain`: Game engine and state.
- `scripts/setup-android-env.sh`: Headless environment bootstrap + build helper.


## Build & Run
- Android Studio
  - Open the root project and run the `app` configuration on an emulator/device.

- CLI
  - `./gradlew assembleDebug` to build the APK.
  - Optional first-time env setup on Linux: `scripts/setup-android-env.sh` (installs SDK CLI tools, accepts licenses, builds).

- Requirements
  - JDK 17+, Android SDK 34, Gradle Wrapper included.


## Gameplay (MVP)
- Pass-and-play with 2+ teams.
- Configurable: round seconds, target words, skip limit/penalty.
- Large “Correct/Skip” buttons; simple turn and match summaries.
- Sample deck used at startup (see `MainViewModel`).

Planned (post-MVP, still offline): challenge/override, high-contrast theme, haptics/audio toggles, category/difficulty mixing, stemming-based forbidden forms.


## Decks & Pack Formats
- Built-in decks: bundled in APK under `assets/decks/`.
- User packs (planned):
  - Import via Storage Access Framework (JSON/CSV/ZIP).
  - Download only from user-defined allow-listed HTTPS origins; manual fetch only.

- JSON (recommended)
```
{
  "format": "alias-deck@1",
  "deck": {
    "id": "movies_en_v1",
    "name": "Movies (EN)",
    "language": "en",
    "version": 1,
    "categories": ["movies"],
    "isNSFW": false
  },
  "words": [
    {
      "text": "Director",
      "difficulty": 2,
      "category": "movies",
      "tabooStems": ["direct", "direction"]
    }
  ],
  "meta": {
    "license": "CC-BY-4.0",
    "attribution": "Example Org",
    "stemsProvided": false
  }
}
```

- CSV (lightweight)
```
text,language,difficulty,category,isNSFW,tabooStems
Director,en,2,movies,false,"direct;direction"
```

- ZIP (optional)
  - `deck.json` + `words.jsonl` or `words.csv`
  - Optional `signature.txt` and `sha256sum.txt`
  - UTF-8, LF endings


## Permissions & Privacy
- Core gameplay requires no runtime permissions.
- Optional (future):
  - `INTERNET` only when the user explicitly downloads a deck pack.
  - Storage import via SAF (no broad storage permission on modern Android).
- Privacy: no telemetry, no background networking, all settings stored locally.


## Networking Model (planned, packs only)
- Manual fetch from allow-listed HTTPS sources set by the user in Settings.
- TLS only, timeouts, no cookies, `User-Agent: AliasLocal/<version>`.
- Cache raw pack and extracted DB rows; verify hash/signature if present.


## Testing Strategy (roadmap)
- Unit: state machine, sampling/no-repeat, skip policy.
- Property: determinism given a seed.
- Integration: JSON/CSV/ZIP import, checksum/signature verification, DB migrations.
- UI: Compose tests for timer and large-button hit targets.

Current repo has minimal tests; contributions welcome to expand coverage.


## Performance Targets
- Cold start < 1.5s on mid-range device.
- Word reveal < 50ms.
- Timer drift < 100 ms/min using a monotonic clock in ViewModel.
- APK size budget < 40 MB (optimize deck formats).


## Roadmap
- v0.1: Pass-and-play with bundled decks, timer, correct/skip, deterministic order.
- v1.0: Deck manager (local import), NSFW toggle, language/difficulty mix, accessibility polish.
- v1.1+: Allow-listed downloads, checksums/signatures, on-device stemming cache.


## Security & Safety
- No dynamic code loading or reflection for packs.
- Strict validation of pack inputs (lengths, languages, difficulty range).
- Cap deck size to avoid memory issues (planned safeguards in import path).
- Clear local data reset (planned in Settings).


## Contributing
- Keep modules focused: `domain` pure Kotlin (no Android), `data` Android with Room, `app` Compose UI.
- Prefer deterministic behavior; thread safety in the engine is guarded by a mutex.
- Follow Kotlin official code style; use the included toolchains.


## License
- License file not yet selected. MPL-2.0 or Apache-2.0 are recommended for this project. Third-party deck pack licenses must be respected and shown in-app.


## Acknowledgements
Inspired by Alias-style gameplay. This project is not affiliated with or endorsed by the original trademark holders.


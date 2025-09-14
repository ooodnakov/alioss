# TODO / Open Tasks

## Completed
- Gate bundled deck import to first run (DB empty) to avoid re-imports.
- Dedupe deck imports by replacing words per deck on import.
- Emit TurnFinished before MatchFinished; updated timer test accordingly.
- Add Room destructive migration fallback for development.
- Show tutorial overlay on first play and mark as seen.
- Improve WordCard accessibility text using localized instructions.
- Add vertical swipe controls (up=correct, down=skip) with Settings toggle.
 - Localize key hardcoded strings and labels across Decks/Settings/Game.
 - Normalize trusted source input and surface invalid host/origin.
 - Surface language validation errors via snackbar.
 - Add Reset local data (clears DB + preferences) with confirmation.
- Add indices for faster word queries; bump DB version.
- Auto-enable decks matching new language with Undo prompt in Settings.
- Show last word after timer ends and allow marking it in the summary without over-penalizing.
- Localize remaining visible literals (Home, Game chips, About).
- Difficulty filter: added min/max difficulty in Settings (stored) and filtering in word queries; UI controls under Decks → Filters.
- Display word info: show difficulty and category chips during turns.
- Category filters: added persistent category selection, filter chips in Decks → Filters, applied in queries and word metadata.

## Backlog
- Add ui localization setting English and Russian
- Add word info to db, like, difficulty, category, and word classes. Display it on card and make filters in deck menu to filter words out 
  - Partially done: uses existing difficulty/category; added difficulty filter. Consider category and word-class filters next.
- Bundled assets change detection: implemented; consider per-deck id tracking and pruning removed assets.
- Room migrations: implement proper migrations for DB version upgrades; remove destructive fallback in production builds.
- Localization: complete pass for any residual literals (e.g., minor labels) to `strings.xml`.
- Make sure that last shown word is also visible in screen after time ends and you can also mark last shown word.
- Performance: add indices for `words(language, isNSFW)` to accelerate filtered queries.
- Tests:
  - Repository: verify re-import of the same deck does not duplicate words (and that updated decks replace content).
  - App-layer: verify last-turn outcomes are persisted when match ends (target reached or timer with no words left).
  - Engine: property-like tests for multi-team rotation and cumulative scoring across many turns.
- UX: when language changes, prompt to auto-enable decks matching the new language (with undo action).
- Settings: add "Reset local data" action (clear DB + preferences) with confirmation dialog.
- Snackbar UX: replace manual 1s autohide with appropriate built-in durations for non-indefinite events.
- Trusted sources: normalize host/origin entries on input (e.g., lowercase host, strip trailing slashes, allow `https://host[:port]`) and surface validation errors in UI.
- Data validation feedback: surface language-tag validation errors from `updateLanguagePreference` via a Snackbar instead of silently ignoring.
- Add sound effects for word Correct and Skip.
  - Implemented: toggle in Settings, tone feedback (ACK/NACK) on actions.

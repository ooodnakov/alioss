# Sound Effects

The app expects two audio clips under `app/src/main/res/raw/`:

- `correct.wav`
- `skip.wav`

They are omitted from version control to avoid licensing issues.

You can generate simple placeholder beeps by running:

```
./scripts/generate-sfx.sh
```

Alternatively, obtain your own sounds. Example CC0/royalty-free sources:

- https://freesound.org/people/InspectorJ/sounds/403007/
- https://freesound.org/people/kickhat/sounds/258020/

Place the downloaded files in `app/src/main/res/raw/` with the names above.

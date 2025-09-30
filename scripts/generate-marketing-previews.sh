#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BUILD_OUTPUT_DIR="$ROOT_DIR/app/build/marketing-previews"
EXPORT_DIR="$ROOT_DIR/marketing-previews"

export ROOT_DIR BUILD_OUTPUT_DIR EXPORT_DIR

cd "$ROOT_DIR"

./gradlew --console=plain :app:testDebugUnitTest --tests "com.example.alioss.ui.preview.MarketingPreviewScreenshotTest"

if [[ ! -d "$BUILD_OUTPUT_DIR" ]]; then
  echo "Marketing previews were not generated at $BUILD_OUTPUT_DIR" >&2
  exit 1
fi

shopt -s nullglob
preview_files=($BUILD_OUTPUT_DIR/*.png)
shopt -u nullglob

if (( ${#preview_files[@]} == 0 )); then
  echo "No marketing preview screenshots were produced" >&2
  exit 1
fi

rm -rf "$EXPORT_DIR"
mkdir -p "$EXPORT_DIR"

for file in "${preview_files[@]}"; do
  cp "$file" "$EXPORT_DIR/"
done

python3 - <<'PY'
import os
import pathlib
import re
import struct
import sys

root_dir = pathlib.Path(os.environ["ROOT_DIR"])
export_dir = pathlib.Path(os.environ["EXPORT_DIR"])
source_file = root_dir / "app/src/main/java/com/example/alioss/ui/preview/MarketingPreviews.kt"

try:
    source_text = source_file.read_text(encoding="utf-8")
except OSError as exc:
    raise SystemExit(f"Failed to read {source_file}: {exc}") from exc

width_match = re.search(r"MARKETING_PREVIEW_WIDTH_DP\s*=\s*(\d+)", source_text)
height_match = re.search(r"MARKETING_PREVIEW_HEIGHT_DP\s*=\s*(\d+)", source_text)

if not width_match or not height_match:
    raise SystemExit(
        "Unable to determine marketing preview dimensions from MarketingPreviews.kt"
    )

expected_width = int(width_match.group(1))
expected_height = int(height_match.group(1))

errors: list[str] = []

for path in sorted(export_dir.glob("*.png")):
    try:
        size_bytes = path.stat().st_size
    except OSError as exc:
        errors.append(f"Failed to stat {path.name}: {exc}")
        continue

    if size_bytes <= 0:
        errors.append(f"{path.name} is empty")

    try:
        with path.open("rb") as handle:
            signature = handle.read(8)
            if signature != b"\x89PNG\r\n\x1a\n":
                errors.append(f"{path.name} is not a valid PNG file")
                continue

            length_bytes, chunk_type = struct.unpack(">I4s", handle.read(8))
            if chunk_type != b"IHDR" or length_bytes < 8:
                errors.append(f"{path.name} missing IHDR chunk")
                continue

            width, height = struct.unpack(">II", handle.read(8))
    except OSError as exc:
        errors.append(f"Failed to read {path.name}: {exc}")
        continue

    if width != expected_width or height != expected_height:
        errors.append(
            f"{path.name} has unexpected dimensions {width}x{height}; "
            f"expected {expected_width}x{expected_height}"
        )

    print(f"Copied {path.name}: {width}x{height}px ({size_bytes} bytes)")

if errors:
    for message in errors:
        print(message, file=sys.stderr)
    raise SystemExit(1)

print(f"Marketing preview screenshots available in {export_dir}")
PY

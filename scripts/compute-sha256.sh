#!/usr/bin/env bash
# Compute SHA-256 for a file in a cross-platform way.
# Usage: scripts/compute-sha256.sh <file>
set -euo pipefail

if [ $# -ne 1 ]; then
  echo "Usage: $0 <file>" >&2
  exit 1
fi

FILE="$1"
if [ ! -f "$FILE" ]; then
  echo "Not a file: $FILE" >&2
  exit 1
fi

if command -v sha256sum >/dev/null 2>&1; then
  sha256sum "$FILE"
elif command -v shasum >/dev/null 2>&1; then
  shasum -a 256 "$FILE"
else
  echo "Neither sha256sum nor shasum found" >&2
  exit 1
fi


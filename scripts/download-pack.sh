#!/usr/bin/env bash
# Download a pack file from an HTTPS URL with optional SHA-256 verification.
# Usage: scripts/download-pack.sh <https-url> [expected_sha256]
# Optional env: TRUSTED_HOSTS=host1,host2 or full origins like https://host:443
set -euo pipefail

cd "$(dirname "$0")/.."

if [ $# -lt 1 ] || [ $# -gt 2 ]; then
  echo "Usage: $0 <https-url> [expected_sha256]" >&2
  exit 1
fi

URL="$1"
EXPECT="${2:-}"

case "$URL" in
  https://*) ;;
  *) echo "Only HTTPS URLs are allowed" >&2; exit 1 ;;
esac

# Enforce allow-list if provided
if [ -n "${TRUSTED_HOSTS:-}" ]; then
  IFS=',' read -r -a ALLOWED <<< "${TRUSTED_HOSTS}"
  HOST="$(printf '%s' "$URL" | sed -E 's|https://([^/]+).*|\1|')"
  ORIGIN_443="https://$HOST:443"
  OK=false
  for entry in "${ALLOWED[@]}"; do
    if [ "$entry" = "$HOST" ] || [ "$entry" = "$ORIGIN_443" ] || [ "$entry" = "https://$HOST" ]; then
      OK=true; break
    fi
  done
  if [ "$OK" != true ]; then
    echo "Host not allow-listed via TRUSTED_HOSTS: $HOST" >&2
    exit 1
  fi
fi

OUT_DIR="downloads"
mkdir -p "$OUT_DIR"
STAMP="$(date +%Y%m%d-%H%M%S)"
NAME="pack-$(echo "$URL" | sed -E 's|https?://||; s|/|_|g')-$STAMP"
OUT_FILE="$OUT_DIR/$NAME"

echo "Downloading to $OUT_FILE ..."
curl -fL --retry 2 --connect-timeout 10 --max-time 120 -o "$OUT_FILE" "$URL"

if [ -n "$EXPECT" ]; then
  if command -v sha256sum >/dev/null 2>&1; then
    GOT="$(sha256sum "$OUT_FILE" | awk '{print $1}')"
  else
    GOT="$(shasum -a 256 "$OUT_FILE" | awk '{print $1}')"
  fi
  if [ "${GOT,,}" != "${EXPECT,,}" ]; then
    echo "Checksum mismatch" >&2
    echo " expected: $EXPECT" >&2
    echo "   actual: $GOT" >&2
    exit 2
  fi
  echo "Checksum OK: $GOT"
fi

echo "Saved: $OUT_FILE"
file "$OUT_FILE" || true


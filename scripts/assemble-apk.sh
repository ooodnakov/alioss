#!/usr/bin/env bash
# Assemble the debug APK and print its SHA256.
# Usage: scripts/assemble-apk.sh
set -euo pipefail

cd "$(dirname "$0")/.."
./gradlew :app:assembleDebug --no-daemon --console=plain

APK="app/build/outputs/apk/debug/app-debug.apk"
echo "\nAPK: $APK"
if command -v sha256sum >/dev/null 2>&1; then
  sha256sum "$APK"
elif command -v shasum >/dev/null 2>&1; then
  shasum -a 256 "$APK"
fi


#!/usr/bin/env bash
# Convenience script: run JVM tests and assemble the debug APK.
# Usage: scripts/dev-build.sh [--clean] [--no-tests] [--assemble-only]
set -euo pipefail

cd "$(dirname "$0")/.."

CLEAN=false
RUN_TESTS=true
ASSEMBLE_ONLY=false

for arg in "$@"; do
  case "$arg" in
    --clean) CLEAN=true ;;
    --no-tests) RUN_TESTS=false ;;
    --assemble-only) ASSEMBLE_ONLY=true ; RUN_TESTS=false ;;
    -h|--help)
      echo "Usage: $0 [--clean] [--no-tests] [--assemble-only]"; exit 0 ;;
  esac
done

if [ "$CLEAN" = true ]; then
  ./gradlew clean --no-daemon --console=plain
fi

if [ "$RUN_TESTS" = true ]; then
  ./gradlew :domain:test :data:test --no-daemon --console=plain
fi

./gradlew :app:assembleDebug --no-daemon --console=plain

APK="app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$APK" ]; then
  echo "\nBuilt: $APK"
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$APK"
  elif command -v shasum >/dev/null 2>&1; then
    shasum -a 256 "$APK"
  fi
fi


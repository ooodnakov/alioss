#!/usr/bin/env bash
# Produce a quick reproducible-build snapshot: wrapper, AGP, Kotlin, SHA256 of APK if present.
# Usage: scripts/repro-snapshot.sh
set -euo pipefail

cd "$(dirname "$0")/.."

echo "# Reproducible Build Snapshot"

WRAP_PROPS="gradle/wrapper/gradle-wrapper.properties"
if [ -f "$WRAP_PROPS" ]; then
  echo "\nGradle Wrapper:"; grep -E 'distributionUrl=' "$WRAP_PROPS" | sed 's/^/  /'
fi

echo "\nPlugin Versions (root build.gradle.kts):"
grep -E 'id\("com.android|org.jetbrains.kotlin|com.google.dagger.hilt.android"\).*version' -n build.gradle.kts | sed 's/^/  /'

echo "\nAndroid SDK platform/build-tools:"; grep -E 'android-3|build-tools' -n scripts/setup-android-env.sh | sed 's/^/  /'

APK="app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$APK" ]; then
  echo "\nAPK SHA256:"
  if command -v sha256sum >/dev/null 2>&1; then sha256sum "$APK"; else shasum -a 256 "$APK"; fi
fi

echo "\nDependency snapshots (top-level):"
rg -n "version \"[0-9]" build.gradle.kts app/build.gradle.kts data/build.gradle.kts domain/build.gradle.kts | sed 's/^/  /' || true


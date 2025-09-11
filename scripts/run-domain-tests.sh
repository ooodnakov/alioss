#!/usr/bin/env bash
# Run deterministic JVM tests for the pure Kotlin domain engine.
# Usage: scripts/run-domain-tests.sh [--info]
set -euo pipefail

cd "$(dirname "$0")/.."

EXTRA_ARGS=("--no-daemon" "--console=plain")
if [ "${1:-}" = "--info" ]; then EXTRA_ARGS+=("--info"); fi

./gradlew :domain:test "${EXTRA_ARGS[@]}"


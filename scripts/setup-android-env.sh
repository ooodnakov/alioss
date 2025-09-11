#!/usr/bin/env bash
# Installs JDK and Android SDK command line tools, then builds project.
# Designed for fresh Ubuntu/Debian-like environments.
set -euo pipefail

REPO_ROOT="$(pwd)"
SDK_DIR="$HOME/android-sdk"
CMD_TOOLS_ZIP="https://dl.google.com/android/repository/commandlinetools-linux-10406996_latest.zip"

if ! command -v java >/dev/null 2>&1; then
  echo "Installing JDK..."
  sudo apt-get update -y
  sudo apt-get install -y openjdk-21-jdk curl unzip
fi

mkdir -p "$SDK_DIR/cmdline-tools"
cd "$SDK_DIR/cmdline-tools"
if [ ! -d "latest" ]; then
  echo "Downloading Android command line tools..."
  curl -Lo commandlinetools.zip "$CMD_TOOLS_ZIP"
  unzip -q commandlinetools.zip -d latest
  mv latest/cmdline-tools/* latest
  rm -rf latest/cmdline-tools
  rm commandlinetools.zip
fi

cd "$SDK_DIR/cmdline-tools/latest/bin"
yes | ./sdkmanager --licenses >/dev/null
./sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"

# Point Gradle at the SDK
echo "sdk.dir=$SDK_DIR" > "$REPO_ROOT/local.properties"

# Build and test project
cd "$REPO_ROOT"
./gradlew domain:test --no-daemon --console=plain
./gradlew assembleDebug --no-daemon --console=plain

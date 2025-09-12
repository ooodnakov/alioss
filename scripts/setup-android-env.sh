#!/usr/bin/env bash
# Installs JDK 21 and Android SDK command line tools, then builds project.
# Designed for Ubuntu/Debian; includes basic macOS handling.
set -euo pipefail

REPO_ROOT="$(pwd)"
# Allow overriding the SDK install dir via env
SDK_DIR="${SDK_DIR:-$HOME/android-sdk}"
# NOTE: Update this URL when a new command line tools version is released.
CMD_TOOLS_ZIP="https://dl.google.com/android/repository/commandlinetools-linux-10406996_latest.zip"

# Tunables (override via env):
#  - ANDROID_PLATFORM: e.g., android-34
#  - BUILD_TOOLS: e.g., 34.0.0
#  - INSTALL_NDK: true|false
#  - NDK_VERSION: e.g., 26.1.10909125
ANDROID_PLATFORM="${ANDROID_PLATFORM:-android-34}"
BUILD_TOOLS="${BUILD_TOOLS:-34.0.0}"
INSTALL_NDK="${INSTALL_NDK:-false}"
NDK_VERSION="${NDK_VERSION:-26.1.10909125}"

if ! command -v java >/dev/null 2>&1 || ! command -v curl >/dev/null 2>&1 || ! command -v unzip >/dev/null 2>&1; then
  OS_NAME="$(uname -s)"
  if [ "$OS_NAME" = "Darwin" ]; then
    echo "Missing deps on macOS. Install with Homebrew: brew install openjdk@21 curl unzip" >&2
    exit 1
  elif command -v apt-get >/dev/null 2>&1; then
    echo "Installing dependencies (JDK 21, curl, unzip) via apt-get..."
    export DEBIAN_FRONTEND=noninteractive
    if command -v sudo >/dev/null 2>&1; then SUDO="sudo"; else SUDO=""; fi
    $SUDO apt-get update -y
    $SUDO apt-get install -y openjdk-21-jdk curl unzip ca-certificates
  else
    echo "Missing dependencies. Please install JDK 21, curl, unzip and re-run." >&2
    exit 1
  fi
fi

# Export JAVA_HOME to help Gradle locate the JDK
if [ -z "${JAVA_HOME:-}" ]; then
  if [ "$(uname -s)" = "Darwin" ] && command -v /usr/libexec/java_home >/dev/null 2>&1; then
    JAVA_HOME="$(_JAVA_HOME=$(/usr/libexec/java_home -v 21 2>/dev/null || true); echo "${_JAVA_HOME:-}")"
  else
    JAVA_BIN="$(command -v java)"
    if [ -n "$JAVA_BIN" ]; then
      JAVA_HOME="$(dirname "$(dirname "$JAVA_BIN")")"
    fi
  fi
  if [ -n "${JAVA_HOME:-}" ]; then export JAVA_HOME; fi
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

# Ensure tools are executable
chmod +x "$SDK_DIR/cmdline-tools/latest/bin"/* || true

# Point SDK-related env vars to ensure installs land under SDK_DIR
export ANDROID_SDK_ROOT="$SDK_DIR"
export ANDROID_HOME="$SDK_DIR"
export PATH="$SDK_DIR/cmdline-tools/latest/bin:$SDK_DIR/platform-tools:$PATH"

# Accept licenses and install required packages explicitly into SDK_DIR
# Avoid pipefail issues from `yes` SIGPIPE on short reads
set +o pipefail
yes | sdkmanager --sdk_root="$SDK_DIR" --licenses >/dev/null || true
set -o pipefail
PACKAGES=(
  "platform-tools"
  "platforms;$ANDROID_PLATFORM"
  "build-tools;$BUILD_TOOLS"
)
if [ "$INSTALL_NDK" = "true" ]; then
  PACKAGES+=("ndk;$NDK_VERSION" "cmake;3.22.1")
fi
sdkmanager --sdk_root="$SDK_DIR" "${PACKAGES[@]}"

# Point Gradle at the SDK (create/update local.properties)
printf "sdk.dir=%s\n" "$SDK_DIR" > "$REPO_ROOT/local.properties"

# Build and test project (can skip with SKIP_BUILD=true)
cd "$REPO_ROOT"
GRADLE_ARGS="${GRADLE_ARGS:---no-daemon --console=plain}"
if [ "${SKIP_BUILD:-false}" != "true" ]; then
  ./gradlew --version $GRADLE_ARGS
  ./gradlew domain:test $GRADLE_ARGS
  ./gradlew assembleDebug $GRADLE_ARGS
fi

echo "\nAndroid SDK setup complete at: $SDK_DIR"
echo "Installed: ${PACKAGES[*]}"

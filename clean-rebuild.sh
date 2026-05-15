#!/usr/bin/env bash
# Full clean + debug APK — avoids stale Gradle/AGP incremental outputs.
set -euo pipefail
cd "$(dirname "$0")"
./gradlew clean --no-configuration-cache
./gradlew :app:assembleDebug --no-build-cache --rerun-tasks --no-configuration-cache
echo "Debug APK: app/build/outputs/apk/debug/app-debug.apk"

#!/usr/bin/env bash
# Generate KobentoShowcases.xcodeproj from project.yml (xcodegen) and build the app.
#   ./build.sh                          a Simulator build (iPhone 17 Pro)
#   DEST='generic/platform=iOS' ./build.sh   a device build (needs a signing team)
# Needs the engine libraries: (cd ../../kobento-ios/ios && ./build.sh sim) for the Simulator, ./build.sh for a device.
set -euo pipefail
cd "$(dirname "$0")"
command -v xcodegen >/dev/null || brew install xcodegen
xcodegen generate --quiet
DEST="${DEST:-platform=iOS Simulator,name=iPhone 17 Pro}"
xcodebuild -project KobentoShowcases.xcodeproj -scheme KobentoShowcases -configuration "${CONFIG:-Debug}" \
  -destination "$DEST" -derivedDataPath build "$@" build | grep -E "error:|warning: .*\.swift|BUILD (SUCCEEDED|FAILED)" || true

#!/usr/bin/env bash
# release.sh — Factor V: Release stage
#
# Combines a build (identified by BUILD_ID) with the current deploy config
# to produce an immutable, timestamped release stored in releases/<RELEASE_ID>/.
#
# A release is an append-only ledger entry: once created it is never mutated.
# Any config change requires creating a new release.
#
# Usage:
#   ./release.sh <build-id>
#
# Example:
#   BUILD_ID=$(./build.sh)
#   ./release.sh "$BUILD_ID"

set -euo pipefail

if [[ $# -lt 1 ]]; then
  echo "Usage: $0 <build-id>" >&2
  exit 1
fi

BUILD_ID="$1"
RELEASE_ID="v$(date -u '+%Y%m%d-%H%M%S')"
RELEASE_DIR="releases/${RELEASE_ID}"

# Verify the build images actually exist before creating a release from them.
for IMAGE in "currency-rate-provider:build-${BUILD_ID}" "rate-printer:build-${BUILD_ID}"; do
  if ! docker image inspect "${IMAGE}" > /dev/null 2>&1; then
    echo "[release] ERROR: image '${IMAGE}' not found. Run ./build.sh first." >&2
    exit 1
  fi
done

mkdir -p "${RELEASE_DIR}"

cat > "${RELEASE_DIR}/manifest.json" <<EOF
{
  "release_id": "${RELEASE_ID}",
  "build_id": "${BUILD_ID}",
  "git_sha": "$(git rev-parse HEAD)",
  "created_at": "$(date -u '+%Y-%m-%dT%H:%M:%SZ')",
  "images": {
    "currency-rate-provider": "currency-rate-provider:build-${BUILD_ID}",
    "rate-printer": "rate-printer:build-${BUILD_ID}"
  }
}
EOF

cat > "${RELEASE_DIR}/docker-compose.release.yml" <<EOF
# Release ${RELEASE_ID} — built from ${BUILD_ID}
# This file is immutable. Do not edit it; create a new release instead.
services:
  service1:
    image: currency-rate-provider:build-${BUILD_ID}
    build: !reset null
  service2:
    image: currency-rate-provider:build-${BUILD_ID}
    build: !reset null
  client:
    image: rate-printer:build-${BUILD_ID}
    build: !reset null
EOF

echo "[release] Created release ${RELEASE_ID} in ${RELEASE_DIR}/" >&2
echo "[release] Images pinned to build-${BUILD_ID}" >&2
echo "${RELEASE_ID}"

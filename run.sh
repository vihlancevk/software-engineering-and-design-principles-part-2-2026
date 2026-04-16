#!/usr/bin/env bash
# run.sh — Factor V: Run stage
#
# Starts the application from a specific, immutable release.
# No compilation or image building happens here — the run stage is kept as
# simple as possible so it can recover automatically (e.g. on server reboot)
# without developer intervention.
#
# Usage:
#   ./run.sh <release-id>            # start a specific release
#   ./run.sh latest                  # start the most recently created release
#   ./run.sh <release-id> down       # stop a running release
#
# Example full workflow:
#   BUILD_ID=$(./build.sh)
#   RELEASE_ID=$(./release.sh "$BUILD_ID")
#   ./run.sh "$RELEASE_ID"

set -euo pipefail

if [[ $# -lt 1 ]]; then
  echo "Usage: $0 <release-id|latest> [down]" >&2
  exit 1
fi

RELEASE_ID="$1"
COMMAND="${2:-up}"

if [[ "${RELEASE_ID}" == "latest" ]]; then
  RELEASE_ID=$(ls -1t releases/ 2>/dev/null | head -n 1)
  if [[ -z "${RELEASE_ID}" ]]; then
    echo "[run] ERROR: no releases found. Run ./build.sh and ./release.sh first." >&2
    exit 1
  fi
  echo "[run] Resolved 'latest' to ${RELEASE_ID}" >&2
fi

RELEASE_DIR="releases/${RELEASE_ID}"

if [[ ! -d "${RELEASE_DIR}" ]]; then
  echo "[run] ERROR: release '${RELEASE_ID}' not found in releases/." >&2
  echo "[run] Available releases:" >&2
  ls releases/ 2>/dev/null || echo "  (none)" >&2
  exit 1
fi

echo "[run] Release: ${RELEASE_ID}" >&2
cat "${RELEASE_DIR}/manifest.json" >&2
echo "" >&2

if [[ "${COMMAND}" == "down" ]]; then
  docker compose \
    -f docker-compose.yml \
    -f "${RELEASE_DIR}/docker-compose.release.yml" \
    down
else
  docker compose \
    -f docker-compose.yml \
    -f "${RELEASE_DIR}/docker-compose.release.yml" \
    up -d
  echo "[run] Stack is up. To stop: ./run.sh ${RELEASE_ID} down"
fi

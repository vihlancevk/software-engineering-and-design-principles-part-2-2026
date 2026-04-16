#!/usr/bin/env bash
# build.sh — Factor V: Build stage
#
# Builds all application images inside Docker (no local Maven/JDK required)
# and tags them with the current git SHA as the build ID.
#
# Output: prints the BUILD_ID so it can be passed to release.sh.
#
# Usage:
#   ./build.sh
#   BUILD_ID=$(./build.sh)

set -euo pipefail

BUILD_ID=$(git rev-parse --short HEAD)
echo "[build] BUILD_ID=${BUILD_ID}" >&2

for MODULE in currency-rate-provider rate-printer; do
  docker build \
    --file "${MODULE}/Dockerfile" \
    --tag "${MODULE}:build-${BUILD_ID}" \
    .
  echo "[build] Tagged ${MODULE}:build-${BUILD_ID}" >&2
done

echo "${BUILD_ID}"

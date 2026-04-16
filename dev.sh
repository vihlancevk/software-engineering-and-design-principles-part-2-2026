#!/usr/bin/env bash
# Starts the full dev stack, building images from source.
# Usage: ./dev.sh [down]
set -euo pipefail

COMMAND="${1:-up}"

if [[ "${COMMAND}" == "down" ]]; then
  docker compose -f docker-compose.yml -f docker-compose.dev.yml down
else
  docker compose -f docker-compose.yml -f docker-compose.dev.yml up --build -d
  echo "[dev] Stack is up. To stop: ./dev.sh down"
fi

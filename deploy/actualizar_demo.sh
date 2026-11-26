#!/usr/bin/env bash
# Actualiza el servidor de demostración con los últimos cambios de la rama y reconstruye.
#   bash ~/altos_de_carpay/deploy/actualizar_demo.sh
set -euo pipefail
cd "$(dirname "$0")/.."
RAMA="${RAMA:-$(git branch --show-current)}"
git pull --ff-only origin "$RAMA"
sudo docker compose -f docker-compose.yml -f deploy/docker-compose.demo.yml up -d --build
sudo docker compose -f docker-compose.yml -f deploy/docker-compose.demo.yml ps

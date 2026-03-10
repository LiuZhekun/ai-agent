#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOY_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
OUT_DIR="${SCRIPT_DIR}/images"
OUT_FILE="${OUT_DIR}/ai-agent-images.tar"

mkdir -p "${OUT_DIR}"

declare -a IMAGES=(
  "mysql:8.0"
  "redis:7"
  "nginx:1.27"
  "milvusdb/milvus:v2.4.13"
  "ai-agent-demo:latest"
)

if command -v docker >/dev/null 2>&1; then
  echo "[INFO] 尝试通过 docker compose 获取镜像清单..."
  if docker compose -f "${DEPLOY_DIR}/docker-compose.yml" config >/dev/null 2>&1; then
    mapfile -t COMPOSE_IMAGES < <(docker compose -f "${DEPLOY_DIR}/docker-compose.yml" config --images | sort -u)
    if [[ "${#COMPOSE_IMAGES[@]}" -gt 0 ]]; then
      IMAGES=("${COMPOSE_IMAGES[@]}")
    fi
  fi
fi

echo "[INFO] 导出镜像列表:"
printf '  - %s\n' "${IMAGES[@]}"
docker save "${IMAGES[@]}" -o "${OUT_FILE}"
echo "[INFO] 已导出到 ${OUT_FILE}"

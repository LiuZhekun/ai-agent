#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
IMAGE_FILE="${SCRIPT_DIR}/images/ai-agent-images.tar"

if [[ ! -f "${IMAGE_FILE}" ]]; then
  echo "[ERROR] 未找到镜像包: ${IMAGE_FILE}" >&2
  echo "请先在联网机器执行 ./export-images.sh 生成该文件。" >&2
  exit 1
fi

docker load -i "${IMAGE_FILE}"
echo "[INFO] 镜像导入完成。"

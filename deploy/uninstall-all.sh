#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "${SCRIPT_DIR}"

REMOVE_VOLUMES="false"
REMOVE_IMAGES="false"

for arg in "$@"; do
  case "${arg}" in
    --purge-volumes) REMOVE_VOLUMES="true" ;;
    --remove-images) REMOVE_IMAGES="true" ;;
    -h|--help)
      echo "用法: ./uninstall-all.sh [--purge-volumes] [--remove-images]"
      exit 0
      ;;
    *)
      echo "[WARN] 未识别参数: ${arg}" >&2
      ;;
  esac
done

DOWN_ARGS=(down --remove-orphans)
if [[ "${REMOVE_VOLUMES}" == "true" ]]; then
  DOWN_ARGS+=(--volumes)
fi
if [[ "${REMOVE_IMAGES}" == "true" ]]; then
  DOWN_ARGS+=(--rmi local)
fi

docker compose "${DOWN_ARGS[@]}"
echo "[INFO] 服务已卸载。volumes=${REMOVE_VOLUMES}, images=${REMOVE_IMAGES}"

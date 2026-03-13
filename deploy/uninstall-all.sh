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
      echo ""
      echo "  --purge-volumes  同时删除数据卷（MySQL/Redis/Milvus/Etcd/MinIO 数据将丢失）"
      echo "  --remove-images  同时删除本地镜像"
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

docker compose --profile full "${DOWN_ARGS[@]}"
echo "[INFO] 服务已卸载（mysql/redis/milvus/etcd/minio/nginx/backend）。volumes=${REMOVE_VOLUMES}, images=${REMOVE_IMAGES}"

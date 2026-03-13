#!/usr/bin/env bash
# ==========================================================================
# AI Agent 一键部署脚本
# ==========================================================================
# 用法:
#   ./install-all.sh              → 仅启动中间件（MySQL + Redis + Milvus，自动带起 etcd + minio）
#   ./install-all.sh --full       → 启动全部服务（含 Nginx + Backend）
#   ./install-all.sh --init-db    → 强制重新初始化数据库（慎用）
#   ./install-all.sh --help       → 显示帮助
# ==========================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "${SCRIPT_DIR}"

# ---------- 参数解析 ----------
MODE="dev"
FORCE_INIT_DB="false"

for arg in "$@"; do
  case "${arg}" in
    --full)        MODE="full" ;;
    --init-db)     FORCE_INIT_DB="true" ;;
    -h|--help)
      echo "用法: ./install-all.sh [选项]"
      echo ""
      echo "选项:"
      echo "  (无参数)     仅启动中间件（MySQL + Redis + Milvus，自动带起 etcd + minio），适合本地开发"
      echo "  --full       启动全部服务（含 Nginx + Backend），需要先构建后端镜像"
      echo "  --init-db    强制重新执行数据库初始化脚本（会清空并重建表和数据）"
      echo "  -h, --help   显示此帮助"
      exit 0
      ;;
    *)
      echo "[WARN] 未识别参数: ${arg}" >&2
      ;;
  esac
done

# ---------- 日志工具 ----------
log()  { echo -e "[INFO]  $*"; }
warn() { echo -e "[WARN]  $*" >&2; }
die()  { echo -e "[ERROR] $*" >&2; exit 1; }

# ---------- 权限辅助 ----------
require_root_if_needed() {
  if [[ "${EUID}" -ne 0 ]] && ! command -v sudo >/dev/null 2>&1; then
    die "需要 root 或 sudo 权限执行安装。"
  fi
}

run_privileged() {
  if [[ "${EUID}" -eq 0 ]]; then
    "$@"
  else
    sudo "$@"
  fi
}

# ---------- 系统检测 ----------
detect_os() {
  if [[ -f /etc/os-release ]]; then
    . /etc/os-release
    echo "${ID:-unknown}"
  else
    echo "unknown"
  fi
}

detect_version_id() {
  if [[ -f /etc/os-release ]]; then
    . /etc/os-release
    echo "${VERSION_ID:-0}"
  else
    echo "0"
  fi
}

# ---------- Docker 安装 ----------
ensure_docker() {
  if command -v docker >/dev/null 2>&1; then
    log "Docker 已安装：$(docker --version)"
    return
  fi
  require_root_if_needed
  local os_id
  os_id="$(detect_os)"
  log "检测到未安装 Docker，开始自动安装（${os_id}）..."
  case "${os_id}" in
    ubuntu|debian)
      run_privileged apt-get update -y
      run_privileged apt-get install -y ca-certificates curl gnupg lsb-release
      run_privileged install -m 0755 -d /etc/apt/keyrings
      if [[ ! -f /etc/apt/keyrings/docker.asc ]]; then
        curl -fsSL https://download.docker.com/linux/ubuntu/gpg \
          | run_privileged gpg --dearmor -o /etc/apt/keyrings/docker.asc
      fi
      run_privileged chmod a+r /etc/apt/keyrings/docker.asc
      echo \
        "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] \
        https://download.docker.com/linux/ubuntu \
        $(. /etc/os-release && echo "${VERSION_CODENAME}") stable" \
        | run_privileged tee /etc/apt/sources.list.d/docker.list >/dev/null
      run_privileged apt-get update -y
      run_privileged apt-get install -y docker-ce docker-ce-cli containerd.io \
        docker-buildx-plugin docker-compose-plugin
      ;;
    centos|rhel|rocky|almalinux|fedora)
      local pkg_cmd="yum"
      local version_id major_ver repo_added repo_url
      command -v dnf >/dev/null 2>&1 && pkg_cmd="dnf"
      version_id="$(detect_version_id)"
      major_ver="${version_id%%.*}"
      [[ -z "${major_ver}" || ! "${major_ver}" =~ ^[0-9]+$ ]] && major_ver=0

      run_privileged "${pkg_cmd}" install -y ca-certificates curl yum-utils
      run_privileged update-ca-trust

      repo_added="false"
      for repo_url in \
        "https://mirrors.aliyun.com/docker-ce/linux/centos/docker-ce.repo" \
        "https://download.docker.com/linux/centos/docker-ce.repo"
      do
        if run_privileged "${pkg_cmd}" config-manager --add-repo "${repo_url}"; then
          log "Docker 仓库已添加：${repo_url}"
          repo_added="true"
          break
        else
          warn "添加仓库失败：${repo_url}"
        fi
      done

      [[ "${repo_added}" == "true" ]] || die "无法添加 Docker 仓库，请检查网络或手动配置镜像源。"

      # Docker 官方仓库当前尚未提供 centos/10 元数据，这里固定到 9 以提升兼容性。
      if [[ "${major_ver}" -ge 10 ]] && [[ -f /etc/yum.repos.d/docker-ce.repo ]]; then
        run_privileged sed -i 's/\$releasever/9/g' /etc/yum.repos.d/docker-ce.repo
        warn "检测到系统大版本 ${major_ver}，已将 Docker repo releasever 固定为 9。"
      fi

      if ! run_privileged "${pkg_cmd}" install -y docker-ce docker-ce-cli containerd.io \
        docker-buildx-plugin docker-compose-plugin; then
        warn "Docker 安装失败，尝试切换到阿里云镜像源后重试..."
        if [[ -f /etc/yum.repos.d/docker-ce.repo ]]; then
          run_privileged sed -i \
            's#https://download.docker.com/linux/centos#https://mirrors.aliyun.com/docker-ce/linux/centos#g' \
            /etc/yum.repos.d/docker-ce.repo
        fi
        run_privileged "${pkg_cmd}" clean all || true
        run_privileged bash -c "rm -rf /var/cache/dnf /var/cache/yum"
        run_privileged "${pkg_cmd}" makecache || true
        run_privileged "${pkg_cmd}" install -y docker-ce docker-ce-cli containerd.io \
          docker-buildx-plugin docker-compose-plugin \
          || die "Docker 安装失败（官方源与镜像源均不可用），请检查网络或代理设置。"
      fi
      ;;
    *)
      die "当前系统不在自动安装覆盖范围：${os_id}。请先手动安装 Docker。"
      ;;
  esac
  run_privileged systemctl enable docker
  run_privileged systemctl start docker
}

ensure_compose() {
  if docker compose version >/dev/null 2>&1; then
    log "Docker Compose 可用：$(docker compose version)"
    return
  fi
  die "docker compose 不可用，请确认 Docker Compose Plugin 已安装。"
}

# ---------- 环境文件 ----------
ensure_env() {
  if [[ ! -f .env ]]; then
    cp .env.example .env
    log "未检测到 .env，已由 .env.example 自动生成。"
  fi

  # 兼容 Windows CRLF：避免 `source .env` 时出现 `$'\r': command not found`
  sed -i 's/\r$//' .env

  set -a
  # shellcheck disable=SC1091
  source .env
  set +a
}

# ---------- 启动容器 ----------
run_compose_up_with_retry() {
  local retry_mirror="$1"
  shift
  local -a compose_cmd=("$@")

  # 直接执行，保留 docker compose 原生交互输出样式
  if "${compose_cmd[@]}"; then
    return 0
  fi

  warn "容器启动失败，可能是镜像仓库网络超时或镜像标签不可用。"
  warn "建议先检查 .env 中镜像配置（MILVUS_IMAGE / ETCD_IMAGE / MINIO_IMAGE）。"
  warn "尝试使用镜像源重试：${retry_mirror}"

  if DOCKERHUB_MIRROR="${retry_mirror}" "${compose_cmd[@]}"; then
    return 0
  fi

  die "重试仍失败。请检查网络与镜像标签，或在 .env 中设置 DOCKERHUB_MIRROR 后重试。"
}

start_stack() {
  local fallback_mirror="${DOCKERHUB_FALLBACK_MIRROR:-docker.m.daocloud.io}"
  local active_mirror="${DOCKERHUB_MIRROR:-docker.io}"
  local retry_mirror="${DOCKERHUB_MIRROR:-${fallback_mirror}}"

  if [[ "${MODE}" == "full" ]]; then
    log "完整模式：启动全部服务（mysql + redis + milvus + nginx + backend）..."
    log "提示：Milvus 会自动拉起其依赖服务（etcd + minio）"
    if ! docker image inspect ai-agent-demo:latest >/dev/null 2>&1; then
      warn "未找到 ai-agent-demo:latest 镜像。"
      warn "请先执行：cd deploy/backend && ./build-and-deploy.sh"
      die "后端镜像不存在，无法以 --full 模式启动。"
    fi
    log "当前镜像源：${active_mirror}"
    run_compose_up_with_retry "${retry_mirror}" docker compose --env-file .env --profile full up -d
  else
    log "开发模式：仅启动中间件（mysql + redis + milvus）..."
    log "提示：Milvus 会自动拉起其依赖服务（etcd + minio）"
    log "当前镜像源：${active_mirror}"
    run_compose_up_with_retry "${retry_mirror}" docker compose --env-file .env up -d mysql redis milvus
  fi
}

# ---------- 等待服务就绪 ----------
wait_for_healthy() {
  local service="$1"
  local max_wait="${2:-120}"
  local interval=5
  local elapsed=0

  log "等待 ${service} 就绪（最长 ${max_wait}s）..."
  while [[ ${elapsed} -lt ${max_wait} ]]; do
    local health
    health=$(docker inspect --format='{{.State.Health.Status}}' "ai-agent-${service}" 2>/dev/null || echo "missing")
    if [[ "${health}" == "healthy" ]]; then
      log "${service} 已就绪。"
      return 0
    fi
    sleep ${interval}
    elapsed=$((elapsed + interval))
  done
  warn "${service} 在 ${max_wait}s 内未通过健康检查，当前状态：${health:-unknown}"
  return 1
}

wait_for_started() {
  local service="$1"
  local max_wait="${2:-60}"
  local interval=3
  local elapsed=0

  log "等待 ${service} 进入运行态（最长 ${max_wait}s）..."
  while [[ ${elapsed} -lt ${max_wait} ]]; do
    local running
    running=$(docker inspect --format='{{.State.Running}}' "ai-agent-${service}" 2>/dev/null || echo "missing")
    if [[ "${running}" == "true" ]]; then
      log "${service} 已运行。"
      return 0
    fi
    sleep ${interval}
    elapsed=$((elapsed + interval))
  done
  warn "${service} 在 ${max_wait}s 内未进入运行态。"
  return 1
}

wait_for_all_middleware() {
  local failed=0
  wait_for_healthy "mysql"  120 || failed=$((failed + 1))
  wait_for_healthy "redis"  30  || failed=$((failed + 1))
  wait_for_started "etcd"   60  || failed=$((failed + 1))
  wait_for_started "minio"  60  || failed=$((failed + 1))
  wait_for_healthy "milvus" 180 || failed=$((failed + 1))
  if [[ ${failed} -gt 0 ]]; then
    warn "${failed} 个服务未通过健康检查，请检查日志：docker compose logs"
  fi
}

# ---------- 数据库验证/强制初始化 ----------
verify_or_init_database() {
  if [[ ! -d "./init-db" ]]; then
    warn "未找到 init-db 目录，跳过数据库检查。"
    return
  fi

  if [[ "${FORCE_INIT_DB}" == "true" ]]; then
    log "强制重新初始化数据库..."
    docker compose exec -T mysql sh -c \
      "mysql --default-character-set=utf8mb4 -uroot -p\"${MYSQL_ROOT_PASSWORD}\" \"${MYSQL_DATABASE}\" < /docker-entrypoint-initdb.d/schema.sql" \
      || warn "schema.sql 执行失败。"
    docker compose exec -T mysql sh -c \
      "mysql --default-character-set=utf8mb4 -uroot -p\"${MYSQL_ROOT_PASSWORD}\" \"${MYSQL_DATABASE}\" < /docker-entrypoint-initdb.d/data.sql" \
      || warn "data.sql 执行失败。"
    log "数据库强制初始化完成。"
    return
  fi

  log "验证数据库初始化状态..."
  local table_count
  table_count=$(docker compose exec -T mysql sh -c \
    "mysql -uroot -p\"${MYSQL_ROOT_PASSWORD}\" -N -e \
    \"SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='${MYSQL_DATABASE}' AND table_name='sys_user'\"" \
    2>/dev/null | tr -d '[:space:]' || echo "0")

  if [[ "${table_count}" == "1" ]]; then
    log "数据库已初始化（sys_user 表存在），跳过重复初始化。"
  else
    warn "数据库表不存在，可能是首次启动初始化尚未完成。"
    warn "如果持续出现此提示，可执行：./install-all.sh --init-db 手动初始化。"
  fi
}

# ---------- 打印状态和地址 ----------
print_status() {
  local access_host="${ACCESS_HOST:-127.0.0.1}"
  echo ""
  log "========== 部署完成 =========="
  echo ""
  docker compose ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}"
  echo ""
  log "中间件访问地址："
  local mysql_auth="(账号: root / ${MYSQL_ROOT_PASSWORD:-root})"
  local redis_auth=""
  local milvus_auth=""
  local etcd_auth=""
  local minio_auth=""
  if [[ -n "${MILVUS_MINIO_ACCESS_KEY:-}" || -n "${MILVUS_MINIO_SECRET_KEY:-}" ]]; then
    minio_auth="(账号: ${MILVUS_MINIO_ACCESS_KEY:-minioadmin} / ${MILVUS_MINIO_SECRET_KEY:-minioadmin})"
  fi
  echo "  MySQL:   ${access_host}:${MYSQL_PORT:-3306}${mysql_auth:+  ${mysql_auth}}"
  echo "  Redis:   ${access_host}:${REDIS_PORT:-6379}${redis_auth:+  ${redis_auth}}"
  echo "  Milvus:  ${access_host}:${MILVUS_PORT:-19530}${milvus_auth:+  ${milvus_auth}}"
  echo "  Etcd:    ${access_host}:${ETCD_PORT:-2379}${etcd_auth:+  ${etcd_auth}}"
  echo "  MinIO API:      http://${access_host}:${MINIO_API_PORT:-9000}${minio_auth:+  ${minio_auth}}"
  echo "  MinIO Console:  http://${access_host}:${MINIO_CONSOLE_PORT:-9001}${minio_auth:+  ${minio_auth}}"

  if [[ "${MODE}" == "full" ]]; then
    echo ""
    log "应用访问地址："
    echo "  后端 API:    http://${access_host}:${BACKEND_PORT:-8080}/api/agent/chat"
    echo "  Nginx 入口:  http://${access_host}:${NGINX_PORT:-80}"
  fi

  echo ""
  log "常用命令："
  echo "  查看日志:    docker compose logs -f mysql"
  echo "  查看状态:    docker compose ps"
  echo "  停止服务:    docker compose stop"
  echo "  卸载服务:    ./uninstall-all.sh"
  echo "  卸载并清数据: ./uninstall-all.sh --purge-volumes"
  echo ""
}

# ---------- 主流程 ----------
main() {
  log "AI Agent 一键部署开始（模式: ${MODE}）"
  echo ""

  ensure_docker
  ensure_compose
  ensure_env
  start_stack
  wait_for_all_middleware
  verify_or_init_database
  print_status

  log "一键部署流程完成。"
}

main "$@"

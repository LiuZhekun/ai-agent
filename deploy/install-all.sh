#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "${SCRIPT_DIR}"

log() { echo "[INFO] $*"; }
warn() { echo "[WARN] $*" >&2; }
die() { echo "[ERROR] $*" >&2; exit 1; }

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

detect_os() {
  if [[ -f /etc/os-release ]]; then
    . /etc/os-release
    echo "${ID:-unknown}"
  else
    echo "unknown"
  fi
}

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
        curl -fsSL https://download.docker.com/linux/ubuntu/gpg | run_privileged gpg --dearmor -o /etc/apt/keyrings/docker.asc
      fi
      run_privileged chmod a+r /etc/apt/keyrings/docker.asc
      echo \
        "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu \
        $(. /etc/os-release && echo "${VERSION_CODENAME}") stable" | run_privileged tee /etc/apt/sources.list.d/docker.list >/dev/null
      run_privileged apt-get update -y
      run_privileged apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
      ;;
    centos|rhel|rocky|almalinux|fedora)
      local pkg_cmd
      pkg_cmd="yum"
      command -v dnf >/dev/null 2>&1 && pkg_cmd="dnf"
      run_privileged "${pkg_cmd}" install -y yum-utils
      run_privileged "${pkg_cmd}" config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
      run_privileged "${pkg_cmd}" install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
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
  die "docker compose 不可用，请确认 Docker Compose Plugin 安装完成。"
}

ensure_env() {
  if [[ ! -f .env ]]; then
    cp .env.example .env
    log "未检测到 .env，已由 .env.example 自动生成。"
  fi
  set -a
  # shellcheck disable=SC1091
  source .env
  set +a
}

start_stack() {
  log "启动容器编排..."
  docker compose --env-file .env up -d
}

wait_for_mysql() {
  local retries=30
  local i
  for ((i=1; i<=retries; i++)); do
    if docker compose exec -T mysql mysqladmin ping -uroot -p"${MYSQL_ROOT_PASSWORD}" --silent >/dev/null 2>&1; then
      log "MySQL 健康检查通过。"
      return
    fi
    sleep 2
  done
  warn "MySQL 健康检查超时，后续初始化可能失败。"
}

init_database() {
  if [[ ! -d "./init-db" ]]; then
    warn "未找到 init-db 目录，跳过数据库初始化。"
    return
  fi
  wait_for_mysql
  if [[ -f "./init-db/schema.sql" ]]; then
    docker compose exec -T mysql sh -c "mysql -uroot -p\"${MYSQL_ROOT_PASSWORD}\" \"${MYSQL_DATABASE}\" < /docker-entrypoint-initdb.d/schema.sql" || warn "schema.sql 执行失败，请手动检查。"
  fi
  if [[ -f "./init-db/data.sql" ]]; then
    docker compose exec -T mysql sh -c "mysql -uroot -p\"${MYSQL_ROOT_PASSWORD}\" \"${MYSQL_DATABASE}\" < /docker-entrypoint-initdb.d/data.sql" || warn "data.sql 执行失败，请手动检查。"
  fi
  log "数据库初始化步骤执行完成。"
}

print_health_and_urls() {
  log "当前容器状态："
  docker compose ps
  local host_ip="127.0.0.1"
  log "访问地址："
  echo "  - 后端 API: http://${host_ip}:${BACKEND_PORT}/api/agent/chat"
  echo "  - 后端健康: http://${host_ip}:${BACKEND_PORT}/actuator/health (若已启用 Actuator)"
  echo "  - Nginx 入口: http://${host_ip}:${NGINX_PORT}"
  echo "  - MySQL: ${host_ip}:${MYSQL_PORT}"
  echo "  - Redis: ${host_ip}:${REDIS_PORT}"
  echo "  - Milvus: ${host_ip}:${MILVUS_PORT}"
}

main() {
  ensure_docker
  ensure_compose
  ensure_env
  start_stack
  init_database
  print_health_and_urls
  log "一键安装与启动流程完成。"
}

main "$@"

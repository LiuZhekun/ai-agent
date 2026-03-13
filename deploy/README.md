# deploy

## 目的

提供在线/离线部署能力，覆盖 MySQL、Redis、Milvus、Nginx、后端服务，并支持 Linux 一键安装启动。

## 目录说明

- `docker-compose.yml`：总编排文件。
- `install-all.sh`：一键安装 Docker/Compose、启动编排、初始化数据库、输出访问地址。
- `uninstall-all.sh`：一键卸载（支持可选清卷/删镜像）。
- `mysql/`：MySQL 独立部署与配置。
- `backend/`：后端镜像构建与单体部署。
- `offline/`：离线镜像导入导出工具。

## 快速部署（Linux）

```bash
cd deploy
cp -n .env.example .env
chmod +x install-all.sh uninstall-all.sh
./install-all.sh
```

## 常用运维命令

```bash
# 查看状态
docker compose ps

# 查看后端日志
docker compose logs -f backend

# 卸载（保留数据卷）
./uninstall-all.sh

# 卸载并清理数据卷
./uninstall-all.sh --purge-volumes
```

## 子模块文档

- MySQL：`deploy/mysql/README.md`
- 后端：`deploy/backend/README.md`
- 离线：`deploy/offline/README.md`

## 常见问题

- Docker 未安装：`install-all.sh` 会在 Ubuntu/CentOS 自动尝试安装。
- 端口冲突：修改 `.env` 中端口映射后重新执行安装脚本。
- 后端无法连接数据库：检查 `.env` 与 `application.yml` 的数据库账号和库名是否一致。
- 拉取镜像超时：在 `.env` 中设置 `DOCKERHUB_MIRROR=docker.m.daocloud.io` 后重试。

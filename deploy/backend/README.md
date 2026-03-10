# deploy/backend

## 目的

提供后端镜像构建与容器化运行模板，适用于单独部署 demo 后端服务。

## 前置条件

- Docker / Docker Compose 可用
- 已在仓库根目录准备好可编译代码

## 构建并启动

```bash
cd deploy/backend
chmod +x build-and-deploy.sh
./build-and-deploy.sh
```

该脚本会执行：
1. `docker build` 构建 `ai-agent-demo:latest`
2. `docker compose up -d` 启动后端容器

## 验证

```bash
docker compose ps
curl http://127.0.0.1:8080/actuator/health
```

## 常见问题

- 镜像构建失败：先在本地执行 `mvn clean package -DskipTests` 排查编译问题。
- 启动后 8080 无响应：查看 `docker compose logs -f backend`。

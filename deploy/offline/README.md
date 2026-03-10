# deploy/offline

## 目的

为无公网环境提供镜像离线迁移能力，配合 `deploy/install-all.sh` 完成完整部署。

## 在线机器导出

```bash
cd deploy/offline
chmod +x export-images.sh
./export-images.sh
```

导出产物：`deploy/offline/images/ai-agent-images.tar`

## 传输镜像包

将 `images/ai-agent-images.tar` 拷贝到目标机器同目录。

## 离线机器导入

```bash
cd deploy/offline
chmod +x import-images.sh
./import-images.sh
```

## 启动服务

```bash
cd ../
cp -n .env.example .env
./install-all.sh
```

## 常见问题

- 导入失败：确认磁盘空间充足并检查 tar 文件完整性。
- 缺少某镜像：重新在在线机器执行导出，确保 `docker compose` 所有镜像均已拉取。

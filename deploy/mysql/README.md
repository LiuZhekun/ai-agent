# deploy/mysql

## 目的

提供 MySQL 独立部署模板，便于本地开发或拆分式部署。

## 文件说明

- `docker-compose.yml`：MySQL 8.0 容器。
- `my.cnf`：字符集、连接数等基础配置。
- `install.sh`：快速启动脚本。

## 启动步骤

```bash
cd deploy/mysql
chmod +x install.sh
./install.sh
```

## 初始化数据库

如需导入示例库，可在仓库根目录执行：

```bash
mysql -h127.0.0.1 -P3306 -uroot -p < deploy/init-db/schema.sql
mysql -h127.0.0.1 -P3306 -uroot -p ai_agent < deploy/init-db/data.sql
```

## 常见问题

- 连接被拒绝：确认 3306 端口未被占用。
- 字符集乱码：确认 `my.cnf` 与库/表编码均为 `utf8mb4`。

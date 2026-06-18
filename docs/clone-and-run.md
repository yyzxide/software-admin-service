# 干净环境克隆和运行指南

这份文档用于保证项目离开当前虚拟机后仍然可以复现运行。核心原则是：源码、初始化 SQL、Docker 依赖、配置模板、启动命令全部放在仓库内；个人环境只保留在 `.env`，不提交。

## 1. 基础依赖

推荐 Linux / WSL2 环境：

```bash
sudo apt update
sudo apt install -y openjdk-17-jdk maven mysql-client git make curl python3
```

还需要 Docker 和 Docker Compose：

```bash
docker --version
docker compose version
```

如果 Docker 命令需要 sudo，可以把当前用户加入 docker 组，或者临时使用 sudo 执行 Docker 命令。

## 2. 克隆项目

```bash
git clone <your-repo-url>
cd java-software-admin-service
```

生成本地配置：

```bash
cp .env.example .env
make doctor
```

`.env` 是本机配置文件，可以改端口、数据库密码、Token 密钥和上传目录。`.env` 已被 `.gitignore` 忽略，不要提交。

## 3. 默认端口

为了避免和本机已有 MySQL / Redis 冲突，本项目默认使用：

```text
admin-service: 8090
MySQL Docker host port: 3308
Redis Docker host port: 6381
```

如果这些端口也被占用，编辑 `.env`：

```dotenv
ADMIN_SERVER_PORT=8091
ADMIN_BASE_URL=http://127.0.0.1:8091
ADMIN_OPENAPI_SERVER_URL=http://127.0.0.1:8091
ADMIN_DB_HOST_PORT=3310
ADMIN_DB_PORT=3310
ADMIN_DB_URL=jdbc:mysql://127.0.0.1:3310/db_java_software_admin?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
ADMIN_REDIS_HOST_PORT=6382
ADMIN_REDIS_PORT=6382
```

## 4. 启动依赖

```bash
make docker-up
```

首次启动 MySQL 容器时，Docker 会自动执行：

```text
database/mysql/001_init_admin_schema.sql
```

`001_init_admin_schema.sql` 是完整初始化脚本，已经包含软件、分类、标签、审核、操作日志和 RBAC 默认数据。`002_rbac_management_upgrade.sql` 用于已有旧库升级，`make init-db` 会按顺序执行 `database/mysql/*.sql`。

如果你清空过数据卷，或者想手动重新初始化，可以执行：

```bash
make init-db
```

## 5. 测试和启动

方式 A：本地 Maven 运行后端：

```bash
make test
make build
make run
```

方式 B：Docker 一键启动 MySQL、Redis 和后端：

```bash
make docker-app-up
```

`docker-app-up` 会使用 `admin-service/Dockerfile` 构建后端镜像，并通过 Docker Compose 的 `app` profile 启动完整后台栈。

打开：

```text
管理页面：http://127.0.0.1:8090/admin/index.html
接口文档：http://127.0.0.1:8090/swagger-ui.html
OpenAPI JSON：http://127.0.0.1:8090/v3/api-docs
```

默认账号：

```text
admin / admin123456
```

服务启动后执行冒烟检查：

```bash
make smoke
```

## 6. 常见问题

### Docker 端口被占用

先检查：

```bash
ss -ltnp
```

然后修改 `.env` 中的 `ADMIN_DB_HOST_PORT`、`ADMIN_DB_PORT`、`ADMIN_DB_URL`、`ADMIN_REDIS_HOST_PORT`、`ADMIN_REDIS_PORT`。

### MySQL 密码不对

如果 Docker volume 已经存在，修改 `.env` 里的 `ADMIN_DB_PASSWORD` 不会自动修改旧 MySQL 数据目录里的 root 密码。

处理方式：

```bash
make docker-down
docker volume rm java-software-admin-service_java_admin_mysql_data
make docker-up
```

这会删除本地 Docker MySQL 数据卷，适合开发环境重置。不要对生产库这样做。

### 后端连不上数据库

确认三处一致：

- `docker-compose.yml` 暴露的 `ADMIN_DB_HOST_PORT`
- Spring 使用的 `ADMIN_DB_PORT`
- Spring 使用的 `ADMIN_DB_URL`

本项目默认三者都是 `3308`。

### Swagger 页面打不开

先确认后端是否启动成功：

```bash
curl http://127.0.0.1:8090/api/v1/admin/health
```

如果你改了 `ADMIN_SERVER_PORT`，浏览器地址也要跟着改。

### 上传目录权限问题

默认上传目录：

```text
admin-service/storage/packages
```

可以在 `.env` 中覆盖：

```dotenv
ADMIN_UPLOAD_PACKAGE_DIR=/tmp/java-admin-packages
```

## 7. 推荐本地验收顺序

Maven 本地运行：

```bash
make setup
make doctor
make docker-up
make test
make build
make run
make smoke
```

Docker 后端运行：

```bash
make setup
make docker-app-up
make smoke
```

正常通过后，这个项目就可以脱离当前虚拟机，在新环境中复现运行。

# Windows / WSL 运行说明

## 推荐方式

推荐使用 WSL2。这样 Java、Maven、Docker、MySQL 客户端和 Shell 脚本都接近 Linux 开发环境，和项目当前验证环境最一致。

推荐工具：

- Windows 10 / 11，并启用 WSL2。
- WSL 中安装 Ubuntu 22.04 或 24.04。
- JDK 17。
- Maven 3.8 及以上。
- Docker Desktop，并启用 WSL 集成。
- Git。

## WSL 命令

安装依赖：

```bash
sudo apt update
sudo apt install -y openjdk-17-jdk maven mysql-client git make
```

检查版本：

```bash
java -version
mvn -version
git --version
```

启动 MySQL 和 Redis：

```bash
cp .env.example .env
make doctor
make docker-up
```

初始化数据库：

```bash
make init-db
```

启动后端：

```bash
make run
```

在 Windows 浏览器中打开：

```text
http://localhost:8090/admin/index.html
```

默认账号：

```text
admin / admin123456
```

## 原生 Windows 说明

原生 Windows 也可以运行，但整体不如 WSL 省心。

如果坚持原生 Windows，需要准备：

- JDK 17。
- Maven。
- MySQL 8。
- Redis 的 Windows 替代方案，或者通过 Docker 启动 Redis。
- Git Bash 或 PowerShell。

项目的 `Makefile` 按 Linux Shell 习惯编写。原生 Windows 下建议使用 WSL 或 Git Bash。

## 常见问题

### 端口冲突

如果宿主机已经运行 MySQL 或 Redis，项目默认仍然可以工作，因为 Docker 端口默认映射到 `3308` 和 `6381`，不会占用常见的 `3306` 和 `6379`。

如果 `3308` 或 `6381` 也被占用，修改 `.env` 中的 `ADMIN_DB_HOST_PORT`、`ADMIN_DB_PORT`、`ADMIN_DB_URL`、`ADMIN_REDIS_HOST_PORT` 和 `ADMIN_REDIS_PORT`。

### 数据库不存在

执行：

```bash
make init-db
```

默认服务数据库是：

```text
db_java_software_admin
```

### 上传保存失败

检查本地存储目录权限：

```text
admin-service/storage/packages
```

也可以通过环境变量覆盖：

```bash
ADMIN_UPLOAD_PACKAGE_DIR=/tmp/java-admin-packages make run
```

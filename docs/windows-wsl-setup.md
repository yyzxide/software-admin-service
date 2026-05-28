# Windows And WSL Setup

## Recommended Path

Use WSL2. It keeps Java, Maven, Docker, MySQL client, and shell scripts close to the Linux environment used during development.

Recommended tools:

- Windows 10/11 with WSL2.
- Ubuntu 22.04 or 24.04 in WSL.
- JDK 17.
- Maven 3.8+.
- Docker Desktop with WSL integration enabled.
- Git.

## WSL Commands

Install dependencies:

```bash
sudo apt update
sudo apt install -y openjdk-17-jdk maven mysql-client git make
```

Check versions:

```bash
java -version
mvn -version
git --version
```

Start MySQL and Redis:

```bash
make docker-up
```

Initialize database:

```bash
make init-db
```

Run backend:

```bash
make run
```

Open in Windows browser:

```text
http://localhost:8090/admin/index.html
```

Default account:

```text
admin / admin123456
```

## Native Windows Notes

Native Windows can work, but WSL is easier.

If running natively, install:

- JDK 17.
- Maven.
- MySQL 8.
- Redis for Windows alternative or Docker Redis.
- Git Bash or PowerShell.

The `Makefile` is Linux-style, so in native Windows you may prefer WSL or Git Bash.

## Common Issues

### Port Conflict

If MySQL or Redis already runs on the host, Docker Compose may fail because ports `3306` or `6379` are occupied.

Either stop the existing service or change ports in `docker-compose.yml`.

### Database Not Found

Run:

```bash
make init-db
```

The default service database is:

```text
db_java_software_admin
```

### Upload Save Failed

Check local storage directory permissions:

```text
admin-service/storage/packages
```

You can override it:

```bash
ADMIN_UPLOAD_PACKAGE_DIR=/tmp/java-admin-packages make run
```

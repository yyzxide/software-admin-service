# Java Software Admin Service

Standalone Java backend for an internal software store admin system.

This project is organized as an independent Java implementation. The previous software store codebase is treated only as business reference, not as a required runtime dependency.

## What This Project Does

The system provides admin-side capabilities for software store operations:

- Admin login and token authorization.
- Category management.
- Tag management.
- Software package upload.
- Software metadata persistence.
- Version and package metadata persistence.
- Software list/detail query.
- Publish and unpublish state transitions.
- Redis cache integration.
- MySQL transaction-based writes.
- Static admin UI for local verification.

## Current Progress

Implemented:

- `admin-service` Spring Boot backend.
- Controller/Service/Mapper/DTO layered structure.
- Unified response and global exception handling.
- Local admin login with HMAC token.
- Category CRUD, tree query, status toggle, delete checks, unique validation.
- Tag CRUD, hot tag toggle, delete checks, unique validation.
- Software list/detail query with filters.
- Multipart package upload with local file save and SHA256 calculation.
- Transactional writes to `apps`, `app_versions`, `app_packages`, and `app_tags`.
- Software metadata edit with category/tag replacement and cache invalidation.
- Version append with package upload, SHA256 calculation, latest-version handling, and optional immediate publish.
- Package variant append for existing versions, covering different OS/CPU architecture combinations.
- Version list and package list query APIs.
- Publish/unpublish state transition.
- Independent MySQL schema: `db_java_software_admin`.
- Static `admin-ui` for browser verification.
- MySQL + Redis Docker Compose.
- Unit/controller tests.

Known incomplete areas:

- Operation audit write.
- Review workflow.
- Chunk upload.
- SDK/client download APIs.
- Production-grade deployment.

## Directory Structure

```text
java-software-admin-service/
  admin-service/          # Main Spring Boot backend service
  admin-ui/               # Static admin UI for local verification
  database/mysql/         # Standalone MySQL schema
  docs/                   # Design, status, setup notes
  operation-log-service/  # Optional/reference log query service
  docker-compose.yml      # MySQL + Redis local runtime
  Makefile                # Local commands
```

## Tech Stack

- Java 17
- Spring Boot 3
- Spring MVC
- MyBatis XML
- MySQL 8
- Redis
- Bean Validation
- JUnit 5
- Static HTML/CSS/JavaScript admin UI

## Quick Start

Start MySQL and Redis:

```bash
make docker-up
```

Initialize database:

```bash
make init-db
```

Run tests:

```bash
make test
```

Run backend:

```bash
make run
```

Open:

```text
http://127.0.0.1:8090/admin/index.html
```

Default account:

```text
admin / admin123456
```

## Database

The backend uses its own database:

```text
db_java_software_admin
```

Schema file:

```text
database/mysql/001_init_admin_schema.sql
```

If MySQL is not local:

```bash
make init-db ADMIN_DB_HOST=<mysql-host> ADMIN_DB_PORT=3306 ADMIN_DB_USERNAME=root ADMIN_DB_PASSWORD=root123456
```

The service can also be pointed to another database with:

```bash
ADMIN_DB_URL='jdbc:mysql://<mysql-host>:3306/db_java_software_admin?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true' make run
```

## Windows / WSL

WSL2 is recommended for Windows.

See:

```text
docs/windows-wsl-setup.md
```

## Project Notes

Business and design outline:

```text
docs/project-outline.md
```

Current progress and next steps:

```text
docs/status-and-next-steps.md
```

Admin software API notes:

```text
docs/admin-software-api.md
```

## GitHub

After moving this directory outside the old workspace:

```bash
git init
git add .
git commit -m "Initial Java software admin service"
git branch -M main
git remote add origin git@github.com:<your-name>/<repo-name>.git
git push -u origin main
```

Use HTTPS remote if SSH is not configured.

# 信创软件商店后台管理系统

这是一个基于 Java / Spring Boot 实现的软件商店后台管理系统，面向企业内测和研发阶段的软件分发场景。系统围绕“软件上传 -> 元数据维护 -> 版本/安装包管理 -> 审核 -> 上下架 -> 操作审计”的后台业务闭环设计，重点体现管理后台的权限鉴权、状态流、事务写入、缓存和可测试性。

## 项目定位

本项目定位为企业软件商店的 Java 后台管理服务。它适合用于说明一个研发阶段后台系统如何从业务流程拆解到数据模型、接口设计和工程实现。

当前主线采用模块化单体设计：一个 `admin-service` 承载认证、软件、版本、安装包、分类、标签、审核和操作日志能力。这样比过早拆成多服务更利于本地运行、联调和面试讲解。

## 核心功能

已实现：

- 管理员登录和 HMAC Token 鉴权。
- RBAC 权限模型：管理员、角色、权限点、用户角色、角色权限，并支持账号管理、角色授权和接口注解拦截。
- 分类管理：新增、查询、树形结构、启停、删除校验。
- 标签管理：新增、查询、热门标记、删除校验。
- 软件管理：列表、详情、上传、编辑、上架、下架。
- 版本管理：新增版本、版本列表、最新版本标记。
- 安装包管理：按系统、CPU 架构和包格式追加安装包变体，并支持安装包安全状态管理。
- 大文件上传：安装包分片上传、断点续传、幂等分片覆盖、完成合并和上传会话消费。
- 上传治理：过期上传会话定时清理，失败时清理已落盘正式包，自动删除临时分片目录。
- 发布安全拦截：签名失败、未标记安全、处理失败或安全状态有风险的安装包不能审核通过或上架。
- 审核流程：提交审核、分配审核人、通过、驳回、审核历史。
- 操作审计：软件、版本、安装包、审核动作在事务提交后写入操作日志。
- 上传安全：限制单文件大小、总请求大小、安装包格式、文件后缀一致性、SHA256 完整性校验和可选 RSA 签名验签。
- 登录安全：数据库账号使用 BCrypt 密码哈希，Token 带会话版本，账号禁用或密码重置后旧 Token 失效。
- OpenAPI / Swagger UI：自动生成接口文档和在线调试页面。
- MySQL 独立表结构和事务写入。
- Redis 缓存集成：分类、标签、软件详情缓存，并在事务提交后统一失效。
- 静态管理页面：用于本地验证主要接口。
- Docker 后端镜像和 Compose profile：支持 MySQL、Redis、后端一键启动。
- 单元测试和 Controller 绑定测试。

暂未作为当前主线实现：

- 客户端 SDK 下载接口。
- 生产级监控、告警和发布流水线。

## 技术栈

- Java 17
- Spring Boot 3.2
- Spring MVC
- MyBatis XML
- MySQL 8
- Redis
- Bean Validation
- springdoc-openapi / Swagger UI
- JUnit 5
- 静态 HTML / CSS / JavaScript 管理页面

## 目录结构

```text
java-software-admin-service/
  admin-service/          # 主 Spring Boot 后台服务
  admin-ui/               # 本地验证用静态管理页面
  database/mysql/         # MySQL 初始化脚本
  docs/                   # 项目说明、接口和面试材料
  .env.example            # 本地运行配置模板
  docker-compose.yml      # 本地 MySQL + Redis + 可选后端服务
  Makefile                # 常用命令
```

操作日志能力已经内置到 `admin-service`，便于一个服务跑通完整后台闭环。

## 快速启动

首次克隆后，先生成本地配置：

```bash
cp .env.example .env
make doctor
```

默认配置会把 Docker MySQL 映射到 `3308`，Redis 映射到 `6381`，避免和电脑上已有的 `3306` / `6379` 冲突。

启动 MySQL 和 Redis：

```bash
make docker-up
```

初始化数据库：

```bash
make init-db
```

如果你本机已经有旧的 Docker MySQL 数据卷，也建议执行一次 `make init-db`，用于补齐新版本的 RBAC 表和默认权限数据。

运行测试：

```bash
make test
```

启动后端：

```bash
make run
```

如果想直接用 Docker 启动 MySQL、Redis 和后端：

```bash
make docker-app-up
```

打开本地管理页面：

```text
http://127.0.0.1:8090/admin/index.html
```

打开接口文档：

```text
http://127.0.0.1:8090/swagger-ui.html
```

获取 OpenAPI JSON：

```text
http://127.0.0.1:8090/v3/api-docs
```

默认账号：

```text
admin / admin123456
```

服务启动后可以执行本地冒烟验收：

```bash
make smoke
```

`make smoke` 会检查健康接口、登录鉴权、OpenAPI 文档、RBAC、分类、标签、软件列表、审核任务和操作日志接口。

## 安全和上传限制

当前项目定位为研发阶段后台系统，但已经具备后台服务必须解释清楚的基础控制：

- 所有后台业务接口都需要 `Authorization: Bearer <token>`。
- Token 使用 HMAC 签名，并带过期时间、会话版本和 `jti`。
- 核心接口使用 `@RequirePermission` 声明权限点，例如 `software:publish`、`review:approve`。
- 初始化 SQL 会创建默认 RBAC 数据，`admin` 用户默认绑定 `super_admin` 角色。
- 权限管理页面支持新增管理员、重置密码、启停账号、分配角色、新增角色和分配权限点。
- 新建和重置管理员密码使用 BCrypt，历史 SHA-256 哈希仅用于兼容升级。
- 上传接口限制单个文件大小，默认 `500MB`。
- Multipart 总请求大小默认 `600MB`，避免请求体过大。
- 安装包格式只允许当前业务支持的 `deb`、`rpm`、`appimage`。
- 上传时校验文件后缀必须和 `packageFormat` 一致，例如 `packageFormat=deb` 时文件名必须是 `.deb`。
- 保存文件名前会做净化处理，并限制在配置的包目录下。
- 保存后计算 SHA256，便于做完整性校验和发布前安全状态校验。
- 大文件可以先创建上传会话，再按分片上传；断网后通过已上传分片列表补传缺失分片。
- 分片合并后会再次校验文件大小和 SHA256，校验通过后才能绑定到软件、版本或安装包。
- 支持 `sha256` 摘要校验和 `sha256-rsa` 非对称签名验签。
- 后台会定时清理过期的上传中/合并中分片会话，避免 `.chunks` 临时目录长期堆积。
- 上传合并、验签或数据库绑定失败时会尽量清理已落盘文件，降低正式包孤儿文件风险。
- 审核通过和上架前会检查安装包安全状态；签名失败、未标记安全、处理失败或安全状态有风险时拒绝发布。
- 安全状态接口可将安装包标记为通过、有风险或处理失败，便于内部研发测试阶段跑通发布闭环。
- 软件、版本、安装包、审核动作会在事务提交后写入操作日志。

常用安全配置：

```bash
ADMIN_SECURITY_TOKEN_SECRET=<long-random-secret> \
ADMIN_UPLOAD_MAX_FILE_SIZE=500MB \
ADMIN_UPLOAD_MAX_REQUEST_SIZE=600MB \
ADMIN_UPLOAD_MAX_PACKAGE_SIZE=500MB \
ADMIN_UPLOAD_SIGNATURE_PUBLIC_KEY_PEM='<public-key-pem>' \
make run
```

如果未来支持一次上传多个安装包，要同时做三层限制：

1. Web 容器层：`spring.servlet.multipart.max-request-size` 拦截整个 HTTP 请求的总大小。
2. 单文件层：`spring.servlet.multipart.max-file-size` 和业务层 `ADMIN_UPLOAD_MAX_PACKAGE_SIZE` 限制每个文件。
3. 业务层：遍历多个 `MultipartFile` 的 `getSize()` 求和，超过批次总上限直接拒绝，避免多个小文件绕过单文件限制。

当前接口一次只接收一个 `packageFile`，所以默认把“单文件上限”和“业务安装包上限”作为主要控制点，总请求大小用于保护额外表单字段和未来扩展。

大文件上传和签名校验设计见：

```text
docs/design/upload-resume-and-signature.md
docs/design/security-and-upload.md
```

## 数据库

默认数据库：

```text
db_java_software_admin
```

本地 Docker 默认端口：

```text
MySQL: 127.0.0.1:3308
Redis: 127.0.0.1:6381
```

初始化和升级脚本：

```text
database/mysql/001_init_admin_schema.sql
database/mysql/002_rbac_management_upgrade.sql
```

核心表：

- `categories`
- `tags`
- `admin_users`
- `admin_roles`
- `admin_permissions`
- `admin_user_roles`
- `admin_role_permissions`
- `apps`
- `app_versions`
- `app_packages`
- `package_upload_sessions`
- `app_tags`
- `review_tasks`
- `review_histories`
- `operation_logs`

如果 MySQL 不在本机：

```bash
make init-db ADMIN_DB_HOST=<mysql-host> ADMIN_DB_PORT=3306 ADMIN_DB_USERNAME=root ADMIN_DB_PASSWORD=<password>
```

也可以通过环境变量指定后端连接：

```bash
ADMIN_DB_URL='jdbc:mysql://<mysql-host>:3306/db_java_software_admin?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true' make run
```

## 常用接口

登录后使用：

```text
Authorization: Bearer <token>
```

主要接口：

- `POST /api/v1/admin/auth/login`
- `GET /api/v1/admin/software/apps`
- `POST /api/v1/admin/software/apps`
- `PUT /api/v1/admin/software/apps/{id}`
- `POST /api/v1/admin/software/apps/{id}/publish`
- `POST /api/v1/admin/software/apps/{id}/unpublish`
- `POST /api/v1/admin/software/apps/{id}/versions`
- `POST /api/v1/admin/software/apps/{id}/versions/{versionId}/packages`
- `POST /api/v1/admin/software/package-upload-sessions`
- `POST /api/v1/admin/software/package-upload-sessions/{uploadId}/chunks`
- `GET /api/v1/admin/software/package-upload-sessions/{uploadId}`
- `POST /api/v1/admin/software/package-upload-sessions/{uploadId}/complete`
- `POST /api/v1/admin/reviews`
- `POST /api/v1/admin/reviews/{id}/approve`
- `POST /api/v1/admin/reviews/{id}/reject`
- `GET /api/v1/admin/operation-logs`

详细 curl 示例见：

```text
docs/api/admin-software-api.md
```

在线调试可以使用 Swagger UI：

```text
http://127.0.0.1:8090/swagger-ui.html
```

## 当前测试状态

当前测试通过情况以 `mvn clean test` 为准。

覆盖范围包括：

- Token 生成、校验和会话版本失效。
- BCrypt 密码哈希和历史 SHA-256 哈希兼容升级。
- 分类/标签业务规则。
- Testcontainers MySQL 集成测试验证分类软删除唯一索引，Docker 不可用时自动跳过。
- 软件上传、编辑、发布、版本和安装包追加。
- 上传文件大小、后缀和包格式校验。
- 分片上传会话、断点续传进度、分片合并、SHA256 校验和上传会话消费。
- 审核任务创建、通过、驳回状态保护。
- 操作日志写入、查询参数校验和展示内容提取。
- Multipart Controller 参数绑定。

## 文档

文档入口见 [docs/README.md](docs/README.md)。

常用入口：

- [项目设计总览](docs/overview/project-outline.md)
- [架构和核心流程图](docs/overview/architecture-and-flows.md)
- [干净环境克隆和运行指南](docs/runbook/clone-and-run.md)
- [后台接口示例](docs/api/admin-software-api.md)
- [面试讲解稿](docs/interview/interview-guide.md)

# 安全和上传限制设计

## 当前目标

当前项目定位是研发阶段的软件商店后台服务。安全设计的目标不是一次到位做成生产级 IAM，而是把后台系统里必须有的基础控制补齐，并且能清楚说明后续如何演进。

## 登录和 Token

后台登录只使用数据库管理员账号，登录成功后签发标准 JWT。初始化 SQL 会创建默认 `admin` 管理员，后续账号新增、禁用、重置密码和角色授权都走 RBAC 管理链路。

当前能力：

- JWT 使用标准三段式格式，固定采用 HS256 签名，不兼容迁移前的自定义 Token。
- 标准 Claims 包含 `iss`、`sub`、`iat`、`exp` 和 `jti`，业务 Claims 包含 `username`、`user_type` 和 `token_version`。
- JWT issuer 固定为 `xcappstore-admin`；HS256 密钥至少 32 字节，非本地环境必须通过环境变量或密钥系统注入。
- 后台业务接口统一经过 `AdminAuthInterceptor` 校验。
- 每次鉴权会重新确认数据库账号状态，账号禁用、密码重置或会话版本变化后旧 Token 会失效。
- 核心接口通过 `@RequirePermission` 声明 RBAC 权限点。
- 初始化 SQL 提供 `super_admin`、`operator`、`reviewer`、`viewer` 四类角色。
- 默认 `admin` 账号绑定 `super_admin`，拥有 `*` 全部权限。
- `admin_users.password_hash` 是管理员登录的优先来源，新建和重置密码使用 BCrypt。
- 历史 `password_sha256` 只作为兼容字段，旧账号登录成功后会自动升级为 BCrypt 哈希。
- 权限管理页面支持新增管理员、重置密码、启停账号、分配角色、新增角色和分配权限点。
- 登录和 Token 验证使用同一套数据库身份来源，避免配置文件管理员登录成功后因数据库不存在该用户而鉴权失败。

示例：

```bash
ADMIN_SECURITY_TOKEN_SECRET=<long-random-secret> \
make run
```

默认管理员密码由初始化 SQL 写入数据库；旧的 `password_sha256` 仅用于历史账号兼容升级，不再作为配置文件登录入口。

后续生产增强：

- 增加登录失败次数限制和验证码。
- 增加菜单级权限和按钮级权限隐藏。
- JWT secret 由配置中心或密钥管理系统托管，并设计密钥轮换策略。

## RBAC 权限模型

当前权限链路是：

```text
管理员 -> 角色 -> 权限点 -> Controller 注解
```

核心表：

- `admin_users`
- `admin_roles`
- `admin_permissions`
- `admin_user_roles`
- `admin_role_permissions`

示例权限：

- `software:view`
- `software:create`
- `software:publish`
- `review:approve`
- `category:manage`
- `tag:manage`
- `operation_log:view`
- `rbac:view`
- `rbac:manage`

接口示例：

```java
@RequirePermission("software:publish")
@PostMapping("/{id}/publish")
public ApiResponse<SoftwareResponse> publish(...) {
    ...
}
```

请求进入后，后端会先验证 Token，再根据当前用户 ID 查询角色权限。如果权限不足，返回 `PERMISSION_DENIED`。

## 上传限制

上传链路有三层控制。

### 1. Web 容器层

Spring Multipart 会在进入 Controller 之前限制请求体：

```yaml
spring:
  servlet:
    multipart:
      max-file-size: ${ADMIN_UPLOAD_MAX_FILE_SIZE:500MB}
      max-request-size: ${ADMIN_UPLOAD_MAX_REQUEST_SIZE:600MB}
```

含义：

- `max-file-size`：任何单个 multipart part 不能超过该值。
- `max-request-size`：整个 HTTP multipart 请求体不能超过该值。

如果请求超过限制，后端会返回“安装包文件过大”或“安装包上传请求格式错误”。

### 2. 业务层

`PackageFileStorageService` 会在保存文件之前继续校验：

- 文件不能为空。
- 文件大小不能超过 `ADMIN_UPLOAD_MAX_PACKAGE_SIZE`，默认 `500MB`。
- `packageFormat` 只接受当前业务支持的格式。
- 文件后缀必须和 `packageFormat` 一致。

当前支持：

```text
deb       -> .deb
rpm       -> .rpm
appimage  -> .appimage / .AppImage
```

### 3. 存储层

保存文件时会做：

- 文件名净化，只保留字母、数字、点、下划线和横线。
- 按日期目录保存。
- 使用 UUID 前缀降低重名风险。
- 目标路径 normalize 后必须仍在包存储根目录下。
- 保存时计算 SHA256。

## 分片上传和断点续传

当前已经支持大文件分片上传。后端把上传过程拆成四步：

1. 创建上传会话，记录文件名、包格式、完整大小、分片大小和期望 SHA256。
2. 按 `chunkIndex` 上传分片，分片落到正式包目录下的 `.chunks/<uploadId>/` 临时目录。
3. 查询会话状态，返回 `uploaded_chunks`，前端可以只补传缺失分片。
4. 所有分片到齐后合并，合并后校验文件大小和 SHA256，再写入正式安装包目录。

关键规则：

- `chunkIndex` 从 `0` 开始。
- 同一个分片可以重复上传，用于断点续传或失败重试；服务端始终原子覆盖，不能把“同大小”当作“同内容”。
- 非最后分片大小必须等于会话里的 `chunk_size`。
- 最后分片大小必须等于剩余文件大小。
- 分片写入期间锁定上传会话行，避免并发进度丢失；过期任务只有在条件标记仍过期成功后才删除目录。
- 会话完成后才能被软件上传、新增版本或追加安装包接口消费。
- 会话被消费后标记为“已使用”，避免重复绑定到多个安装包。
- 超过配置时间仍在上传中或合并中的会话会被定时清理，并删除 `.chunks/<uploadId>` 临时目录。

相关配置：

```dotenv
ADMIN_UPLOAD_CLEANUP_ENABLED=true
ADMIN_UPLOAD_CLEANUP_EXPIRE_HOURS=24
ADMIN_UPLOAD_CLEANUP_BATCH_SIZE=100
ADMIN_UPLOAD_CLEANUP_FIXED_DELAY_MS=3600000
```

详细设计见：

```text
docs/design/upload-resume-and-signature.md
```

## 完整性和签名校验

当前支持两种校验：

1. `sha256`：前端或制品平台提供完整文件 SHA256，后端合并后重新计算并比对。
2. `sha256-rsa`：发布系统对完整文件签名，后端用 `ADMIN_UPLOAD_SIGNATURE_PUBLIC_KEY_PEM` 配置的公钥验签。

安装包表会记录：

- `sha256`
- `signature_algorithm`
- `signature_value`
- `signature_status`
- `signature_verified_at`

签名状态：

- `0`：未校验。
- `1`：通过。
- `2`：失败。

当前实现里，如果 SHA256 或签名不通过，请求会直接失败，不会生成可用安装包。

## 发布前安全状态拦截

本项目暂未接真实安全扫描执行器，但发布策略已经按安全分发收紧：只有 `scan_status=1` 的安装包允许审核通过或上架，其他状态都会阻断。

- `signature_status=2`：签名校验失败。
- `scan_status=0`：未标记安全。
- `scan_status=2`：安全状态有风险。
- `scan_status=3`：安全状态处理失败。

这样可以先把安全状态流接入业务发布路径。后续接入真实扫描服务后，只需要让扫描服务更新 `scan_status` 和 `scan_report`，审核和上架拦截无需重写。

## 多文件总大小怎么处理

如果未来支持一次上传多个安装包，比如同时上传 `x86_64`、`aarch64`、`loongarch64` 三个架构包，不能只看单文件大小。

推荐处理方式：

1. 网关或 Nginx 配 `client_max_body_size`，先挡住过大的请求。
2. Spring 配 `max-request-size`，挡住整个 multipart 请求总大小。
3. Spring 配 `max-file-size`，挡住任何单个文件过大。
4. Controller 或 Service 遍历所有 `MultipartFile`，把 `getSize()` 累加。
5. 如果总和超过业务批次上限，直接返回参数错误，不保存任何文件。
6. 保存文件和写数据库仍然放在明确的事务边界里；如果文件已经落盘但数据库失败，需要有清理策略。

伪代码：

```java
long totalSize = files.stream().mapToLong(MultipartFile::getSize).sum();
if (totalSize > maxBatchBytes) {
    throw new BusinessException(ErrorCode.PARAM_FORMAT, "本次上传总大小超过限制");
}
```

当前项目每个接口只接收一个 `packageFile`，所以“总请求上限”主要由 `max-request-size` 保护。

## 安全状态设计

当前内部研发测试阶段暂不接入具体扫描引擎，而是提供安装包安全状态接口，并保留 `scan_status` 和 `scan_report` 作为后续异步扫描服务的落点。

当前本地流程：

1. 安装包上传完成并通过 SHA256 / 签名校验。
2. 写入 `scan_status=0`，表示待安全确认。
3. 管理员调用安全状态接口，必须显式提交 `result=safe/risky/failed`，写入 `scan_status=1/2/3` 和 `scan_report`。
4. 审核通过或上架前检查安全状态，只有 `scan_status=1` 允许继续。

后续生产演进：

1. 后端发送扫描任务到消息队列或任务表。
2. 扫描服务读取 `storage_path`，调用 ClamAV、商业安全网关或内部沙箱。
3. 扫描通过后写入 `scan_status=1`。
4. 扫描执行失败写入 `scan_status=3`，命中风险写入 `scan_status=2`，同时保存 `scan_report`。

这样不会让大文件上传请求长时间阻塞，也方便后续替换为真实扫描执行器。

## 仍未覆盖的生产安全点

当前还没有做：

- 文件 magic number 校验。
- 真实安全扫描和恶意包检测执行器。
- 文件 magic number、解包元数据和安装脚本风险规则。
- 后台定期孤儿文件巡检。
- 登录失败次数限制和验证码。
- 菜单级权限和按钮级权限隐藏。

这些可以作为后续优化路线，不建议在当前阶段一次性塞满。

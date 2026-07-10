# 分片上传、断点续传和签名校验设计

## 背景

软件商店后台上传的安装包可能达到数百 MB。普通 multipart 一次性上传有几个问题：

- 网络波动后需要从头重传，体验差。
- 单个请求时间过长，容易被网关、浏览器或反向代理中断。
- 后端无法精确记录已上传进度。
- 如果只依赖文件后缀和大小，无法证明安装包没有被篡改。

因此当前实现增加了“上传会话 + 分片文件 + 完成合并 + 完整性校验 + 业务消费”的链路。

## 当前实现范围

已实现：

- 创建分片上传会话。
- 按 `chunkIndex` 上传分片。
- 查询已上传分片列表，支持断点续传。
- 所有分片上传完成后合并文件。
- 合并后校验文件大小和 SHA256。
- 支持 `sha256` 和 `sha256-rsa` 两类校验方式。
- 完成后的上传会话可以被“上传软件 / 新增版本 / 追加安装包”业务消费。
- 上传会话被消费后标记为已使用，避免重复绑定。
- 过期上传中或合并中的会话会被定时清理，并删除临时分片目录。

当前边界：

- 后端分片上传接口已经实现；管理台暂未提供专门的分片上传页面，当前主要通过接口文档和 curl 示例验证。
- 暂未接真实安装包安全扫描执行器。
- 内部研发测试阶段使用本地目录存储；生产化时可扩展对象存储直传。

## 接口流程

### 1. 创建上传会话

```http
POST /api/v1/admin/software/package-upload-sessions
Content-Type: application/json
Authorization: Bearer <token>
```

请求示例：

```json
{
  "file_name": "editor.deb",
  "package_format": "deb",
  "file_size": 104857600,
  "chunk_size": 5242880,
  "expected_sha256": "<完整文件sha256>",
  "signature_algorithm": "sha256",
  "signature_value": "<完整文件sha256>"
}
```

返回核心字段：

- `upload_id`：后续上传分片和完成合并使用。
- `total_chunks`：需要上传的分片总数。
- `uploaded_chunks`：已上传分片序号。
- `status`：`0=上传中`。

### 2. 上传分片

```http
POST /api/v1/admin/software/package-upload-sessions/{uploadId}/chunks
Content-Type: multipart/form-data
Authorization: Bearer <token>
```

字段：

- `chunkIndex`：从 `0` 开始。
- `chunkFile`：当前分片内容。

设计要点：

- 同一个 `chunkIndex` 可以重复上传，后上传的分片会覆盖旧分片。
- 非最后一个分片大小必须等于会话里的 `chunk_size`。
- 最后一个分片大小必须等于 `file_size - chunkIndex * chunk_size`。
- 每次上传后后端从磁盘重新统计已存在分片，写回 `uploaded_chunks`。

这就是断点续传的基础：前端断网后重新查询会话状态，只补传缺失的分片即可。

### 3. 查询上传进度

```http
GET /api/v1/admin/software/package-upload-sessions/{uploadId}
Authorization: Bearer <token>
```

返回：

```json
{
  "upload_id": "...",
  "total_chunks": 20,
  "uploaded_chunk_count": 7,
  "uploaded_chunks": [0, 1, 2, 3, 4, 8, 9],
  "status": 0,
  "status_text": "上传中"
}
```

前端可以用 `uploaded_chunks` 和 `[0, total_chunks)` 做差集，得到待补传分片。

### 4. 完成合并

```http
POST /api/v1/admin/software/package-upload-sessions/{uploadId}/complete
Authorization: Bearer <token>
```

后端处理：

1. 检查所有分片是否存在。
2. 按 `0 -> total_chunks - 1` 顺序合并。
3. 校验合并后文件大小。
4. 计算实际 SHA256。
5. 比对 `expected_sha256`。
6. 如配置签名，执行签名校验。
7. 将最终文件移动到正式安装包目录。
8. 写入 `storage_path`、`actual_sha256`、签名状态。
9. 删除临时分片目录。

### 5. 绑定到业务对象

上传软件、新增版本、追加安装包三个业务接口都支持两种方式：

普通小文件：

```text
packageFile=@./editor.deb
```

大文件分片上传：

```text
uploadSessionId=<upload_id>
```

规则：

- `packageFile` 和 `uploadSessionId` 不能同时提交。
- 使用 `uploadSessionId` 时，会话必须是“已完成”。
- 会话创建人必须和当前管理员一致。
- 会话包格式必须和业务请求里的 `packageFormat` 一致。
- 绑定成功后会话状态变成“已使用”，避免重复消费。

## 数据模型

### package_upload_sessions

核心字段：

- `upload_id`：上传会话 ID。
- `file_name`：原始文件名。
- `package_format`：包格式。
- `file_size`：完整文件大小。
- `chunk_size`：单个分片大小。
- `total_chunks`：分片总数。
- `uploaded_chunk_count`：已上传分片数量。
- `uploaded_chunks`：已上传分片序号 JSON 数组。
- `expected_sha256`：期望 SHA256。
- `actual_sha256`：合并后实际 SHA256。
- `storage_path`：最终正式存储路径。
- `signature_algorithm`：签名算法。
- `signature_value`：签名值或期望摘要。
- `signature_status`：`0=未校验 1=通过 2=失败`。
- `status`：`0=上传中 1=已完成 2=已消费 3=失败 4=合并中`。
- `error_message`：失败原因。

### app_packages

安装包表增加：

- `signature_algorithm`
- `signature_value`
- `signature_status`
- `signature_verified_at`

这样业务侧能直接看到某个安装包是否完成完整性校验。

## 签名校验策略

当前支持两类策略。

### sha256

适合内部制品平台或前端先计算完整包摘要的场景。

- `expected_sha256` 或 `signature_value` 必须是 64 位十六进制字符串。
- 后端合并完成后重新计算 SHA256。
- 两者一致才允许进入正式安装包目录。

### sha256-rsa

适合发布系统对安装包做非对称签名的场景。

- `signature_value` 是 Base64 编码的 RSA 签名。
- `ADMIN_UPLOAD_SIGNATURE_PUBLIC_KEY_PEM` 配置验签公钥。
- 后端使用 `SHA256withRSA` 对完整文件内容验签。

本地开发默认不配置公钥，不影响普通 SHA256 校验。

## 失败处理

当前规则：

- 分片缺失：保持上传中，前端可以继续补传。
- 重复上传同一个同大小分片：按幂等成功处理，便于断点续传重试。
- 完成合并前先将会话置为“合并中”，避免同一个上传会话被并发完成。
- 文件大小不一致、SHA256 不一致、签名失败：会话标记为失败。
- 已落盘正式文件在后续校验或数据库状态变更失败时会尽量清理，降低孤儿文件风险。
- 失败会话不能继续消费，需要重新创建上传会话。

当前已经有过期上传会话清理和主要失败路径清理。后续可以增加：

- 后台定期孤儿文件巡检。
- 批次总大小限制。

## 安全状态设计

当前先不接入具体扫描引擎，原因是它会引入 ClamAV、商业扫描服务或异步沙箱，部署成本较高。项目提供安装包安全状态接口，用于把安全状态流接入审核和上架路径。

推荐演进方案：

1. 安装包合并并完成签名校验后，先写入 `scan_status=0`。
2. 内部研发测试环境调用安全状态接口，必须显式提交 `result=safe/risky/failed`，更新 `scan_status` 和 `scan_report`。
3. 安全状态通过后更新 `scan_status=1`。
4. 命中风险后更新 `scan_status=2`，处理失败后更新 `scan_status=3`。
5. 上架或审核通过时检查 `signature_status` 和 `scan_status`。

这样主链路不被慢扫描阻塞，也便于替换不同扫描服务。

## 面试讲法

可以这样讲：

> 普通上传对大安装包不可靠，所以我把上传拆成“会话、分片、进度、合并、校验、消费”几个阶段。断网后前端查询 `uploaded_chunks` 补传缺失分片；后端合并后重新计算 SHA256，并把签名状态写入安装包表。业务接口不直接关心分片细节，只消费已完成的上传会话，这样上传基础设施和软件业务是解耦的。

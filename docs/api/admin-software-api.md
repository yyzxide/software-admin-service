# 后台接口示例

基础地址：

```text
http://127.0.0.1:8090
```

默认账号：

```text
admin / admin123456
```

在线接口文档：

```text
Swagger 页面：http://127.0.0.1:8090/swagger-ui.html
OpenAPI JSON：http://127.0.0.1:8090/v3/api-docs
```

## 登录

```bash
curl -s -X POST http://127.0.0.1:8090/api/v1/admin/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123456"}'
```

后续请求使用：

```text
Authorization: Bearer <token>
```

## 软件管理

上传约束：

- 默认单个安装包最大 `500MB`。
- 默认整个 multipart 请求最大 `600MB`。
- `packageFormat` 当前支持 `deb`、`rpm`、`appimage`。
- 文件后缀必须和 `packageFormat` 一致，例如 `packageFormat=deb` 时上传文件必须是 `.deb`。
- 大文件可以先走分片上传会话，再在业务接口里传 `uploadSessionId`。
- 支持 `expectedSha256` / `expected_sha256` 做完整性校验。
- 支持 `signatureAlgorithm` / `signature_algorithm` 和 `signatureValue` / `signature_value` 做签名校验。

列表：

```bash
curl -s 'http://127.0.0.1:8090/api/v1/admin/software/apps?page=1&page_size=20' \
  -H "Authorization: Bearer <token>"
```

详情：

```bash
curl -s http://127.0.0.1:8090/api/v1/admin/software/apps/1 \
  -H "Authorization: Bearer <token>"
```

## 大文件分片上传

创建上传会话：

```bash
FILE=./sample.deb
SHA256=$(sha256sum "$FILE" | awk '{print $1}')
SIZE=$(stat -c%s "$FILE")

curl -s -X POST http://127.0.0.1:8090/api/v1/admin/software/package-upload-sessions \
  -H "Authorization: Bearer <token>" \
  -H 'Content-Type: application/json' \
  -d "{
    \"file_name\": \"sample.deb\",
    \"package_format\": \"deb\",
    \"file_size\": ${SIZE},
    \"chunk_size\": 5242880,
    \"expected_sha256\": \"${SHA256}\",
    \"signature_algorithm\": \"sha256\",
    \"signature_value\": \"${SHA256}\"
  }"
```

上传单个分片：

```bash
curl -s -X POST http://127.0.0.1:8090/api/v1/admin/software/package-upload-sessions/<upload_id>/chunks \
  -H "Authorization: Bearer <token>" \
  -F chunkIndex=0 \
  -F chunkFile=@./sample.deb.part0
```

查询进度：

```bash
curl -s http://127.0.0.1:8090/api/v1/admin/software/package-upload-sessions/<upload_id> \
  -H "Authorization: Bearer <token>"
```

完成合并和校验：

```bash
curl -s -X POST http://127.0.0.1:8090/api/v1/admin/software/package-upload-sessions/<upload_id>/complete \
  -H "Authorization: Bearer <token>"
```

合并完成后，在上传软件、新增版本或追加安装包接口里使用：

```text
-F uploadSessionId=<upload_id>
```

注意：`packageFile` 和 `uploadSessionId` 不能同时提交。

上传新软件：

```bash
curl -s -X POST http://127.0.0.1:8090/api/v1/admin/software/apps \
  -H "Authorization: Bearer <token>" \
  -F appKey=com.example.editor \
  -F name=文本编辑器 \
  -F categoryId=1 \
  -F summary=Linux文本编辑器 \
  -F description=适配国产Linux发行版的文本编辑器 \
  -F versionName=1.0.0 \
  -F versionCode=100 \
  -F osType=uos_v20 \
  -F arch=x86_64 \
  -F packageFormat=deb \
  -F tagIds=1,2 \
  -F publishNow=false \
  -F packageFile=@./sample.deb
```

使用分片上传会话创建新软件：

```bash
curl -s -X POST http://127.0.0.1:8090/api/v1/admin/software/apps \
  -H "Authorization: Bearer <token>" \
  -F appKey=com.example.editor \
  -F name=文本编辑器 \
  -F categoryId=1 \
  -F summary=Linux文本编辑器 \
  -F description=适配国产Linux发行版的文本编辑器 \
  -F versionName=1.0.0 \
  -F versionCode=100 \
  -F osType=uos_v20 \
  -F arch=x86_64 \
  -F packageFormat=deb \
  -F tagIds=1,2 \
  -F publishNow=false \
  -F uploadSessionId=<upload_id>
```

说明：`publishNow=false` 仅为兼容旧参数；如果传 `publishNow=true`，服务端会拒绝请求，软件和新版本必须先提交审核。

编辑元数据：

```bash
curl -s -X PUT http://127.0.0.1:8090/api/v1/admin/software/apps/1 \
  -H "Authorization: Bearer <token>" \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "文本编辑器",
    "category_id": 1,
    "summary": "Linux文本编辑器",
    "description": "更新后的软件描述",
    "supported_os_types": "uos_v20,kylin_v10",
    "supported_archs": "x86_64,aarch64",
    "tag_ids": "1,2",
    "is_featured": 1,
    "sort_weight": 20
  }'
```

上架/下架：

```bash
curl -s -X POST http://127.0.0.1:8090/api/v1/admin/software/apps/1/publish \
  -H "Authorization: Bearer <token>"

curl -s -X POST http://127.0.0.1:8090/api/v1/admin/software/apps/1/unpublish \
  -H "Authorization: Bearer <token>"
```

## 版本和安装包

新增版本并上传一个安装包：

```bash
curl -s -X POST http://127.0.0.1:8090/api/v1/admin/software/apps/1/versions \
  -H "Authorization: Bearer <token>" \
  -F versionName=2.0.0 \
  -F versionCode=200 \
  -F changelog='新增功能' \
  -F osType=uos_v20 \
  -F arch=x86_64 \
  -F packageFormat=deb \
  -F publishNow=false \
  -F packageFile=@./sample-2.0.0.deb
```

为已有版本追加架构变体：

```bash
curl -s -X POST http://127.0.0.1:8090/api/v1/admin/software/apps/1/versions/2/packages \
  -H "Authorization: Bearer <token>" \
  -F osType=uos_v20 \
  -F arch=aarch64 \
  -F packageFormat=deb \
  -F packageFile=@./sample-2.0.0-arm.deb
```

本地模拟扫描安装包：

```bash
curl -s -X POST http://127.0.0.1:8090/api/v1/admin/software/apps/1/packages/1/scan \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "result": "safe",
    "report": "本地模拟扫描通过"
  }'
```

`result` 支持 `safe`、`risky`、`failed`。审核通过和上架前只允许 `safe`。

查询版本和安装包：

```bash
curl -s http://127.0.0.1:8090/api/v1/admin/software/apps/1/versions \
  -H "Authorization: Bearer <token>"

curl -s http://127.0.0.1:8090/api/v1/admin/software/apps/1/packages \
  -H "Authorization: Bearer <token>"
```

## 审核流程

提交软件审核：

```bash
curl -s -X POST http://127.0.0.1:8090/api/v1/admin/reviews \
  -H "Authorization: Bearer <token>" \
  -H 'Content-Type: application/json' \
  -d '{"app_id":1,"reason":"准备上架","priority":1}'
```

提交版本审核：

```bash
curl -s -X POST http://127.0.0.1:8090/api/v1/admin/reviews \
  -H "Authorization: Bearer <token>" \
  -H 'Content-Type: application/json' \
  -d '{"app_id":1,"version_id":2,"reason":"新版本发布","priority":2}'
```

查询审核任务：

```bash
curl -s 'http://127.0.0.1:8090/api/v1/admin/reviews?page=1&page_size=20&status=0' \
  -H "Authorization: Bearer <token>"
```

分配审核人：

```bash
curl -s -X POST http://127.0.0.1:8090/api/v1/admin/reviews/1/assign \
  -H "Authorization: Bearer <token>" \
  -H 'Content-Type: application/json' \
  -d '{"reviewer_id":2}'
```

审核通过：

```bash
curl -s -X POST http://127.0.0.1:8090/api/v1/admin/reviews/1/approve \
  -H "Authorization: Bearer <token>" \
  -H 'Content-Type: application/json' \
  -d '{"comment":"安装包和元数据检查通过"}'
```

审核驳回：

```bash
curl -s -X POST http://127.0.0.1:8090/api/v1/admin/reviews/1/reject \
  -H "Authorization: Bearer <token>" \
  -H 'Content-Type: application/json' \
  -d '{"comment":"描述信息不完整"}'
```

## 操作日志

列表：

```bash
curl -s 'http://127.0.0.1:8090/api/v1/admin/operation-logs?page=1&page_size=20' \
  -H "Authorization: Bearer <token>"
```

按对象编号精确查询：

```bash
curl -s 'http://127.0.0.1:8090/api/v1/admin/operation-logs?resource_type=software&resource_id=1' \
  -H "Authorization: Bearer <token>"
```

统计：

```bash
curl -s 'http://127.0.0.1:8090/api/v1/admin/operation-logs/stats' \
  -H "Authorization: Bearer <token>"
```

选项：

```bash
curl -s 'http://127.0.0.1:8090/api/v1/admin/operation-logs/options' \
  -H "Authorization: Bearer <token>"
```

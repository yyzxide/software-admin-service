# Admin Software API Notes

Base URL:

```text
http://127.0.0.1:8090
```

Default account:

```text
admin / admin123456
```

## Login

```bash
curl -s -X POST http://127.0.0.1:8090/api/v1/admin/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123456"}'
```

Use response token:

```text
Authorization: Bearer <token>
```

## Software

List:

```bash
curl -s 'http://127.0.0.1:8090/api/v1/admin/software/apps?page=1&pageSize=20' \
  -H "Authorization: Bearer <token>"
```

Detail:

```bash
curl -s http://127.0.0.1:8090/api/v1/admin/software/apps/1 \
  -H "Authorization: Bearer <token>"
```

Upload new software:

```bash
curl -s -X POST http://127.0.0.1:8090/api/v1/admin/software/apps \
  -H "Authorization: Bearer <token>" \
  -F appKey=com.example.editor \
  -F name=TextEditor \
  -F categoryId=1 \
  -F summary=Linux text editor \
  -F description=Text editor for domestic Linux distributions \
  -F versionName=1.0.0 \
  -F versionCode=100 \
  -F osType=uos_v20 \
  -F arch=x86_64 \
  -F packageFormat=deb \
  -F tagIds=1,2 \
  -F publishNow=true \
  -F packageFile=@./sample.deb
```

Update metadata:

```bash
curl -s -X PUT http://127.0.0.1:8090/api/v1/admin/software/apps/1 \
  -H "Authorization: Bearer <token>" \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "TextEditor",
    "categoryId": 1,
    "summary": "Linux text editor",
    "description": "Updated description",
    "supportedOsTypes": "uos_v20,kylin_v10",
    "supportedArchs": "x86_64,aarch64",
    "tagIds": "1,2",
    "isFeatured": 1,
    "sortWeight": 20
  }'
```

Publish/unpublish:

```bash
curl -s -X POST http://127.0.0.1:8090/api/v1/admin/software/apps/1/publish \
  -H "Authorization: Bearer <token>"

curl -s -X POST http://127.0.0.1:8090/api/v1/admin/software/apps/1/unpublish \
  -H "Authorization: Bearer <token>"
```

## Versions And Packages

Append a new version with one package:

```bash
curl -s -X POST http://127.0.0.1:8090/api/v1/admin/software/apps/1/versions \
  -H "Authorization: Bearer <token>" \
  -F versionName=2.0.0 \
  -F versionCode=200 \
  -F changelog='New release' \
  -F osType=uos_v20 \
  -F arch=x86_64 \
  -F packageFormat=deb \
  -F publishNow=true \
  -F packageFile=@./sample-2.0.0.deb
```

Append package variant to an existing version:

```bash
curl -s -X POST http://127.0.0.1:8090/api/v1/admin/software/apps/1/versions/2/packages \
  -H "Authorization: Bearer <token>" \
  -F osType=uos_v20 \
  -F arch=aarch64 \
  -F packageFormat=deb \
  -F packageFile=@./sample-2.0.0-arm.deb
```

Query versions and packages:

```bash
curl -s http://127.0.0.1:8090/api/v1/admin/software/apps/1/versions \
  -H "Authorization: Bearer <token>"

curl -s http://127.0.0.1:8090/api/v1/admin/software/apps/1/packages \
  -H "Authorization: Bearer <token>"
```

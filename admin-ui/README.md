# Admin UI

Lightweight browser UI for verifying `admin-service` workflows.

It has no npm dependency and no build step. The backend serves it from:

```text
http://127.0.0.1:8090/admin/index.html
```

## Features

- Login.
- Software list/detail.
- Software package upload.
- Publish/unpublish.
- Category management.
- Tag management.
- Basic diagnostics.

## Run

Recommended:

```bash
make run
```

Then open:

```text
http://127.0.0.1:8090/admin/index.html
```

Standalone static mode is only for UI debugging:

```bash
make ui
```

Then open:

```text
http://127.0.0.1:5178
```

Default account:

```text
admin / admin123456
```

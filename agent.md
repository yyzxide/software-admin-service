# Agent Memory

## Working Directory

- Main project directory: `/home/sid/java-software-admin-service`.
- Do not continue feature work in `/home/sid/xcappstore-backend/java-software-admin-service`.
- This repository is now independent from `xcappstore-backend`.

## User Goal

- 用户目标是从 Linux C++ 终端安全研发转向 Java 后端研发。
- 当前最需要补的是“Java 后端工作经历可信度”，不是继续做学习型 Demo。
- 该项目用于沉淀一个可讲述的 Java 软件商店后台管理服务经历。
- 用户之前参与过软件商店包获取相关工作，这是业务连续性的支点。

## Project Positioning

- 项目名称：Java Software Admin Service / software-admin-service。
- 定位：内测阶段的软件商店后台管理侧 Java 实现。
- 参考旧软件商店项目的业务大纲，但不再依赖原项目代码、目录或表结构。
- 不主动讲“Go 改 Java”或“重写 Go 服务”。
- 推荐口径：在内部软件商店平台内测阶段，负责后台管理侧 Java 服务的独立实现与验证。

## Current Structure

```text
java-software-admin-service/
  admin-service/          # Spring Boot 主后端
  admin-ui/               # 静态后台页面
  database/mysql/         # 独立 MySQL 表结构
  docs/                   # 项目大纲、状态、Windows/WSL说明
  operation-log-service/  # 日志查询参考模块，不是主线
  docker-compose.yml
  Makefile
```

## Current Progress

- `admin-service` 已完成 Spring Boot 基础骨架。
- 已完成统一响应、错误码、全局异常处理。
- 已完成本地管理员登录和 HMAC token 校验。
- 已完成分类管理：CRUD、树查询、启停、删除校验、唯一性校验、Redis 缓存失效。
- 已完成标签管理：CRUD、热门标签、热门切换、删除校验、唯一性校验、Redis 缓存失效。
- 已完成软件后台查询：分页、详情、多条件筛选、分类名、标签名、最新版本、安装包数量、状态文案。
- 已完成软件上传基础链路：multipart 上传、本地保存、SHA256、`apps/app_versions/app_packages/app_tags` 多表事务写入。
- 已完成上架/下架基础状态流转。
- 已完成独立数据库 `db_java_software_admin`，脚本在 `database/mysql/001_init_admin_schema.sql`。
- 已完成静态 `admin-ui`，用于登录、软件列表/详情/上传、分类、标签、本地联调。
- 当前测试：`make test` 通过，20 个测试。

## Known Issues

- 当前 bug 还不少，特别是实际环境上传、UI 体验、部署联调细节。
- 暂时不要急着修所有 bug，先保持项目结构清晰、能跑测试、能 GitHub 同步。
- `operation-log-service` 是早期日志模块，业务含金量较低，保留作参考即可。

## Main Direction

- 后续主线是 `admin-service`。
- 表结构由本项目自己维护，不再兼容原项目多套历史表结构。
- 优先补强 Java 后端面试可讲内容：软件编辑、版本管理、安装包管理、操作日志写入、事务、动态 SQL、Redis、接口测试。
- 前端目标是“能演示后台闭环”，不追求生产级复杂前端。

## Roadmap

1. 稳定本地/WSL 运行环境。
2. 验证 `make docker-up -> make init-db -> make run`。
3. 修复上传链路实际环境 bug。
4. 补软件编辑。
5. 补版本追加和安装包追加。
6. 补操作日志写入。
7. 补接口文档和 curl 测试脚本。
8. 优化 `admin-ui`，让它更像真实后台。

## Commands

```bash
cd /home/sid/java-software-admin-service
make docker-up
make init-db
make test
make build
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

## Git

- Repository has been initialized.
- Initial commit exists.
- Future changes should be committed from `/home/sid/java-software-admin-service`.
- Typical flow:

```bash
git status
git add .
git commit -m "<message>"
git push
```

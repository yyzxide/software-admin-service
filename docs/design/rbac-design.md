# RBAC 权限模型设计

## 目标

后台管理系统不能只有“登录了就是管理员”。不同岗位应该有不同操作边界：

- 超级管理员：拥有全部权限。
- 运营人员：维护软件、分类、标签，提交审核。
- 审核人员：处理审核任务。
- 只读人员：查看数据和日志，不能修改。

当前实现采用 RBAC，也就是：

```text
管理员 -> 角色 -> 权限点 -> 接口拦截
```

## 数据模型

核心表：

- `admin_users`：管理员账号。
- `admin_roles`：角色。
- `admin_permissions`：权限点。
- `admin_user_roles`：管理员和角色关系。
- `admin_role_permissions`：角色和权限点关系。

默认角色：

| 角色编码 | 名称 | 说明 |
| --- | --- | --- |
| `super_admin` | 超级管理员 | 拥有 `*` 全部权限 |
| `operator` | 运营人员 | 软件资料、分类标签、提审 |
| `reviewer` | 审核人员 | 审核任务分配、通过、驳回 |
| `viewer` | 只读人员 | 查看软件、分类、标签、审核、日志 |

默认 `admin` 用户绑定 `super_admin`。

## 权限点

示例：

```text
software:view
software:create
software:update
software:publish
software:unpublish
software:version:create
software:package:create
software:package:scan
software:upload
review:view
review:submit
review:assign
review:approve
review:reject
category:view
category:manage
tag:view
tag:manage
operation_log:view
rbac:view
rbac:manage
```

支持通配：

- `*`：全部权限。
- `review:*`：审核模块全部权限。
- `software:*`：软件模块全部权限。

## 接口拦截

Controller 方法通过 `@RequirePermission` 声明权限：

```java
@RequirePermission("software:publish")
@PostMapping("/{id}/publish")
public ApiResponse<SoftwareResponse> publish(...) {
    ...
}
```

请求流程：

1. `AdminAuthInterceptor` 校验 Bearer Token。
2. 解析当前管理员 ID。
3. 读取 Controller 方法或类上的 `@RequirePermission`。
4. `AdminPermissionService` 查询当前管理员拥有的权限点。
5. 权限匹配则放行，否则返回 `PERMISSION_DENIED`。

接口权限只是第一层。涉及具体资源时，Service 还要做业务级授权：

- 分配审核任务时，目标管理员必须启用，并同时具备通过和驳回权限。
- 任务已经分配后，只允许 `reviewer_id` 对应的审核人处理。
- 即使另一个管理员也有 `review:approve`，也不能越过任务归属直接通过。

这样区分“有资格调用某类接口”和“有权处理这条具体数据”，避免 RBAC 只停留在页面按钮层。

## 当前取舍

当前已经具备 RBAC 后端模型、接口拦截和基础管理界面：

- 管理员账号：新增账号、重置密码、启用/禁用账号。
- 用户角色：给管理员分配一个或多个角色。
- 角色管理：新增角色、启用/禁用角色。
- 角色权限：给角色分配权限点。
- 登录链路：优先使用数据库 `admin_users.password_hash` 校验，新建和重置密码使用 BCrypt，历史 `password_sha256` 只做兼容升级。

当前已经使用 BCrypt 慢哈希和 Token 会话版本。生产环境还应继续补充登录失败次数限制、密码复杂度策略和操作审计脱敏。

## 面试讲法

可以这样说：

> 我没有把权限简化成 admin/user 两种身份，而是做了 RBAC：管理员绑定角色，角色绑定权限点，接口通过注解声明需要的权限。拦截器处理接口级权限，Service 再校验审核人资格和任务归属，区分“能调用审核接口”和“能处理这条任务”。后台还提供账号管理和角色授权页面，可以创建运营、审核、只读等岗位账号。

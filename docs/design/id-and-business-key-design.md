# ID 和业务编码设计

## 结论

数据库自增 ID 仍然需要保留，但它不应该成为运营人员在页面上手动输入的业务参数。

本项目按三层处理：

1. 数据库层使用数字 ID 做主键和外键，保证关联简单、查询稳定。
2. 业务层为长期稳定的对象提供业务编码，例如 `app_key`、`role_code`、`permission_code`。
3. 展示层优先展示名称、编码和状态，通过下拉、多选完成选择，提交时由前端把选择结果转换成后端需要的 ID。

## 哪些 ID 是真正需要的

需要保留的内部 ID：

- `admin_users.id`：管理员账号主键，审计日志、Token 用户标识、用户角色关系表都会用到。
- `admin_roles.id`：角色主键，`admin_user_roles` 关联用户和角色。
- `admin_permissions.id`：权限点主键，`admin_role_permissions` 关联角色和权限。
- `categories.id`：分类主键，软件表通过 `category_id` 关联分类。
- `tags.id`：标签主键，软件标签关系表通过 `tag_id` 关联标签。
- `apps.id`：软件主键，版本、安装包、审核任务、操作日志都会关联它。
- `app_versions.id`：版本主键，安装包和版本级审核任务会关联它。
- `app_packages.id`：安装包主键，用于包变体、下载统计和安全状态追踪。

这些 ID 的价值在于数据库关系和服务端状态流，不在于让人记住它们。

## 哪些字段适合作为业务编码

适合被人理解、配置和讨论的字段：

- `apps.app_key`：软件唯一业务标识，例如 `com.example.editor`。
- `admin_roles.role_code`：角色编码，例如 `operator`、`reviewer`、`security_reviewer`。
- `admin_permissions.permission_code`：权限编码，例如 `software:publish`、`review:approve`、`rbac:manage`。
- 分类名、标签名：面向运营侧的展示和选择字段。

这些字段更接近代码里的枚举或常量。比如 `software:publish` 的语义是稳定的，而 `permission_id=12` 只是某个数据库环境里的主键值。

## 页面为什么不应该裸露 ID 输入

裸露数字 ID 有几个问题：

- 使用成本高：运营或审核人员必须先查 ID，才能完成上传、提审、授权。
- 容易误操作：输入相邻 ID 可能选中完全不同的软件、版本或角色。
- 环境不稳定：测试库、开发库、生产库的自增 ID 顺序可能不同。
- 语义弱：看到 `11` 不知道是 `software:publish` 还是 `review:approve`。
- 不利于面试讲解：页面像数据库联调工具，而不是完整后台产品。

因此当前管理台已经改成：

- 上传软件时选择分类名、多选标签名。
- 查询软件时按分类下拉筛选。
- 创建审核任务时选择软件和版本。
- 新增管理员时多选角色。
- 新增角色时多选权限点。
- 分配角色和权限时使用角色编码、权限编码，而不是数据库 ID。
- 新增分类时选择父分类名称。

## 权限点列表有没有必要展示

权限点本身有必要存在，因为它是 RBAC 的最小授权单位。

例如：

```text
software:publish  -> 允许上架软件
review:approve    -> 允许审核通过
rbac:manage       -> 允许管理账号和角色
```

但权限点列表不是普通运营人员的主操作入口。它更像“系统能力字典”：

- 开发人员用它确认接口需要哪些权限。
- 超级管理员创建角色时用它选择授权范围。
- 面试讲项目时用它说明权限模型不是简单的 admin/user。
- 排查权限问题时用它确认某个角色是否缺少某项能力。

因此页面中权限点改为默认收起的“权限字典”。日常操作只需要创建角色、给角色勾选权限、给账号分配角色，不需要逐项浏览所有权限点。

## 操作日志里的对象编号是什么

操作日志表需要记录一次操作影响了哪个对象。这里的对象编号就是后端资源主键，例如：

- `resource_type=software`，对象编号是软件 ID。
- `resource_type=version`，对象编号是版本 ID。
- `resource_type=package`，对象编号是安装包 ID。
- `resource_type=review`，对象编号是审核任务 ID。
- `resource_type=category`，对象编号是分类 ID。
- `resource_type=tag`，对象编号是标签 ID。

它的意义是审计追踪：当某个软件状态异常时，可以按“对象类型 + 对象编号”精确查出谁上传、谁提交审核、谁上架或下架。

普通查询日志时通常只看动作、关键词和时间范围；对象编号属于高级筛选，不应该让它变成页面上的主要业务概念。

## 后端 API 是否可以继续使用 ID

可以。内部管理 API 使用数字 ID 是常见做法，尤其是路径参数：

```text
POST /api/v1/admin/software/apps/{id}/publish
POST /api/v1/admin/software/apps/{id}/versions/{versionId}/packages
```

这类接口通常由前端通过已选记录发起，用户不需要手写 URL。前端拿到记录 ID 后提交给后端，是合理的。

如果未来要做开放平台或第三方集成，建议增加基于业务编码的接口，例如：

```text
GET /api/v1/admin/software/apps/by-key/{appKey}
PUT /api/v1/admin/rbac/roles/{roleCode}/permissions
```

这样外部调用方就不用依赖某个数据库环境里的自增 ID。

## 和 C++ 宏或枚举的类比

数字 ID 不是宏，也不应该当成业务常量。

更合理的类比是：

```cpp
constexpr auto PERMISSION_SOFTWARE_PUBLISH = "software:publish";
constexpr auto ROLE_REVIEWER = "reviewer";
```

在本项目里，对应的就是：

```text
permission_code = software:publish
role_code = reviewer
```

数据库 ID 则更像运行时分配的对象句柄，只适合系统内部关联，不适合作为人工配置语义。

## 当前实现策略

当前没有大改数据库和后端接口，而是在管理台做了低风险改造：

- 页面加载分类、标签、软件、角色、权限等引用数据。
- 用户通过名称、编码、下拉和多选完成选择。
- 前端提交前把选择结果映射为后端已有接口需要的 ID。
- 表格里弱化数据库 ID，优先展示名称、编码、状态和业务关系。

这样既不破坏现有 API 和测试，又能让页面更接近真实后台系统。

## 后续可演进方向

- 后端 RBAC 接口支持按 `role_code`、`permission_code` 授权，减少前端映射逻辑。
- 软件查询支持按 `app_key` 定位详情。
- 审核任务创建支持 `app_key + version_code`，便于外部制品平台集成。
- 对外 API 使用 UUID、ULID 或业务编码，内部管理 API 继续保留数字 ID。
- OpenAPI 文档中明确区分“内部 ID”和“业务编码”。

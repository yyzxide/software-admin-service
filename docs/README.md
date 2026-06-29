# 文档导航

这套文档按阅读目的拆分。第一次看项目时，建议按下面顺序读，不要从零散文件里随便跳。

## 推荐阅读顺序

1. [项目设计总览](overview/project-outline.md)
2. [架构和核心流程图](overview/architecture-and-flows.md)
3. [当前进度和下一步](overview/status-and-next-steps.md)
4. [干净环境克隆和运行指南](runbook/clone-and-run.md)
5. [后台接口示例](api/admin-software-api.md)
6. [面试讲解稿](interview/interview-guide.md)
7. [简历写法和面试表达](interview/resume-bullets.md)

## 文档分组

### Overview：先看全局

- [项目设计总览](overview/project-outline.md)：项目定位、模块、数据模型和安全边界。
- [架构和核心流程图](overview/architecture-and-flows.md)：整体结构、上传审核发布、鉴权和审核并发控制流程图。
- [当前进度和下一步](overview/status-and-next-steps.md)：已完成能力、已知不足和短期路线。
- [缺陷和优化计划](overview/defect-and-optimization-plan.md)：已处理问题、剩余优化和优先级。

### Design：讲清设计

- [RBAC 权限模型设计](design/rbac-design.md)：管理员、角色、权限点和接口拦截。
- [安全和上传限制设计](design/security-and-upload.md)：Token、密码、上传限制、签名和扫描策略。
- [分片上传、断点续传和签名校验设计](design/upload-resume-and-signature.md)：大文件上传链路。
- [ID 和业务编码设计](design/id-and-business-key-design.md)：数据库 ID、业务编码和页面展示边界。

### Runbook：跑起来和演示

- [干净环境克隆和运行指南](runbook/clone-and-run.md)：从克隆到启动。
- [Windows / WSL 运行说明](runbook/windows-wsl-setup.md)：Windows 环境准备。
- [本地演示和验收清单](runbook/demo-checklist.md)：演示路径和验收点。

### API：联调接口

- [后台接口示例](api/admin-software-api.md)：核心 curl 示例。

### Interview：面试材料

- [面试讲解稿](interview/interview-guide.md)：按问题组织的项目讲法。
- [简历写法和面试表达](interview/resume-bullets.md)：简历 bullet 和边界话术。

## 维护规则

- 项目能力变化，优先更新 `overview/status-and-next-steps.md`。
- 安全、上传、审核、权限等设计变化，更新 `design/` 下对应文件。
- 启动命令、环境变量、端口变化，更新 `runbook/` 和根 README。
- 面试话术只放在 `interview/`，不要混进设计文档。

SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS db_java_software_admin
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE db_java_software_admin;

CREATE TABLE IF NOT EXISTS categories (
  id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name         VARCHAR(50) NOT NULL COMMENT '分类名称',
  description  VARCHAR(255) DEFAULT NULL COMMENT '分类描述',
  icon         VARCHAR(512) DEFAULT NULL COMMENT '分类图标',
  parent_id    BIGINT UNSIGNED DEFAULT NULL COMMENT '父分类ID',
  sort_order   INT NOT NULL DEFAULT 0 COMMENT '排序',
  status       TINYINT NOT NULL DEFAULT 1 COMMENT '1=启用 0=禁用',
  is_builtin   TINYINT NOT NULL DEFAULT 0 COMMENT '1=系统内置',
  created_at   DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at   DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  deleted_at   DATETIME(3) DEFAULT NULL,
  alive_name   VARCHAR(50) GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN name ELSE NULL END) STORED,
  UNIQUE KEY uk_category_alive_name (alive_name),
  KEY idx_parent_id (parent_id),
  KEY idx_status (status),
  KEY idx_deleted_at (deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Java后台软件分类表';

CREATE TABLE IF NOT EXISTS tags (
  id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name         VARCHAR(50) NOT NULL COMMENT '标签名称',
  description  VARCHAR(255) DEFAULT NULL COMMENT '标签描述',
  is_hot       TINYINT NOT NULL DEFAULT 0 COMMENT '1=热门',
  is_builtin   TINYINT NOT NULL DEFAULT 0 COMMENT '1=系统内置',
  created_at   DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_tag_name (name),
  KEY idx_hot (is_hot)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Java后台软件标签表';

CREATE TABLE IF NOT EXISTS admin_users (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  username      VARCHAR(64) NOT NULL COMMENT '管理员账号',
  display_name  VARCHAR(128) NOT NULL DEFAULT '' COMMENT '显示名称',
  password_hash VARCHAR(128) NOT NULL DEFAULT '{sha256}ac0e7d037817094e9e0b4441f9bae3209d67b02fa484917065f71b16109a1a78' COMMENT '登录密码哈希',
  password_sha256 VARCHAR(64) DEFAULT NULL COMMENT '历史SHA256密码哈希，仅用于兼容迁移',
  status        TINYINT NOT NULL DEFAULT 1 COMMENT '1=启用 0=禁用',
  token_version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Token会话版本，变更后旧Token失效',
  created_at    DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at    DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_admin_username (username),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Java后台管理员账号表';

CREATE TABLE IF NOT EXISTS admin_roles (
  id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  role_code    VARCHAR(64) NOT NULL COMMENT '角色编码',
  role_name    VARCHAR(128) NOT NULL COMMENT '角色名称',
  description  VARCHAR(255) DEFAULT NULL COMMENT '角色说明',
  status       TINYINT NOT NULL DEFAULT 1 COMMENT '1=启用 0=禁用',
  created_at   DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at   DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_role_code (role_code),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Java后台角色表';

CREATE TABLE IF NOT EXISTS admin_permissions (
  id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  permission_code  VARCHAR(128) NOT NULL COMMENT '权限编码',
  permission_name  VARCHAR(128) NOT NULL COMMENT '权限名称',
  module           VARCHAR(64) NOT NULL DEFAULT '' COMMENT '所属模块',
  description      VARCHAR(255) DEFAULT NULL COMMENT '权限说明',
  status           TINYINT NOT NULL DEFAULT 1 COMMENT '1=启用 0=禁用',
  created_at       DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_permission_code (permission_code),
  KEY idx_module (module),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Java后台权限点表';

CREATE TABLE IF NOT EXISTS admin_user_roles (
  user_id     BIGINT UNSIGNED NOT NULL COMMENT '管理员ID',
  role_id     BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
  created_at  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (user_id, role_id),
  KEY idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Java后台管理员角色关联表';

CREATE TABLE IF NOT EXISTS admin_role_permissions (
  role_id        BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
  permission_id  BIGINT UNSIGNED NOT NULL COMMENT '权限ID',
  created_at     DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (role_id, permission_id),
  KEY idx_permission_id (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Java后台角色权限关联表';

CREATE TABLE IF NOT EXISTS apps (
  id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  app_key             VARCHAR(128) NOT NULL COMMENT '全局唯一标识',
  name                VARCHAR(128) NOT NULL COMMENT '软件名称',
  developer_id        BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建者/开发者ID',
  submit_source       VARCHAR(16) NOT NULL DEFAULT 'admin' COMMENT 'admin/developer',
  category_id         BIGINT UNSIGNED NOT NULL COMMENT '分类ID',
  icon_url            VARCHAR(512) NOT NULL DEFAULT '' COMMENT '图标URL',
  summary             VARCHAR(512) NOT NULL COMMENT '摘要',
  description         TEXT NOT NULL COMMENT '描述',
  supported_os_types  VARCHAR(255) DEFAULT NULL COMMENT '支持系统，逗号分隔',
  supported_archs     VARCHAR(255) DEFAULT NULL COMMENT '支持架构，逗号分隔',
  screenshots         JSON NOT NULL COMMENT '截图URL数组',
  status              TINYINT NOT NULL DEFAULT 0 COMMENT '0=草稿 1=审核中 2=已上架 3=已下架 4=审核驳回',
  is_official         TINYINT NOT NULL DEFAULT 0 COMMENT '1=官方',
  is_featured         TINYINT NOT NULL DEFAULT 0 COMMENT '1=推荐',
  sort_weight         INT NOT NULL DEFAULT 0 COMMENT '排序权重',
  download_count      BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '下载量',
  rating_score        DECIMAL(3,2) NOT NULL DEFAULT 0.00 COMMENT '评分',
  rating_count        INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '评分数',
  published_at        DATETIME(3) DEFAULT NULL COMMENT '上架时间',
  rejected_at         DATETIME(3) DEFAULT NULL COMMENT '驳回时间',
  created_at          DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at          DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  deleted_at          DATETIME(3) DEFAULT NULL,
  created_by          BIGINT UNSIGNED NOT NULL DEFAULT 0,
  updated_by          BIGINT UNSIGNED NOT NULL DEFAULT 0,
  UNIQUE KEY uk_app_key (app_key),
  KEY idx_category_status (category_id, status),
  KEY idx_status (status),
  KEY idx_deleted_at (deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Java后台软件主表';

CREATE TABLE IF NOT EXISTS app_versions (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  app_id         BIGINT UNSIGNED NOT NULL COMMENT '软件ID',
  version_name   VARCHAR(64) NOT NULL COMMENT '版本名称',
  version_code   BIGINT UNSIGNED NOT NULL COMMENT '版本数字',
  changelog      TEXT DEFAULT NULL COMMENT '更新日志',
  submit_source  VARCHAR(16) NOT NULL DEFAULT 'admin',
  status         TINYINT NOT NULL DEFAULT 0 COMMENT '0=草稿 1=审核中 2=通过 3=驳回 4=已下架',
  is_latest      TINYINT NOT NULL DEFAULT 0 COMMENT '1=最新版本',
  submitted_at   DATETIME(3) DEFAULT NULL COMMENT '提交时间',
  reviewed_at    DATETIME(3) DEFAULT NULL COMMENT '审核时间',
  published_at   DATETIME(3) DEFAULT NULL COMMENT '发布时间',
  created_at     DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at     DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  deleted_at     DATETIME(3) DEFAULT NULL,
  created_by     BIGINT UNSIGNED NOT NULL DEFAULT 0,
  updated_by     BIGINT UNSIGNED NOT NULL DEFAULT 0,
  KEY idx_app_status (app_id, status),
  KEY idx_app_latest (app_id, is_latest),
  KEY idx_deleted_at (deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Java后台软件版本表';

CREATE TABLE IF NOT EXISTS app_packages (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  app_id          BIGINT UNSIGNED NOT NULL COMMENT '软件ID',
  version_id      BIGINT UNSIGNED NOT NULL COMMENT '版本ID',
  os_type         VARCHAR(32) NOT NULL COMMENT '操作系统',
  arch            VARCHAR(32) NOT NULL COMMENT 'CPU架构',
  package_format  VARCHAR(16) NOT NULL COMMENT 'deb/rpm/appimage',
  file_name       VARCHAR(256) NOT NULL COMMENT '文件名',
  file_size       BIGINT UNSIGNED NOT NULL COMMENT '文件大小',
  storage_path    VARCHAR(512) NOT NULL COMMENT '本地或对象存储路径',
  cdn_url         VARCHAR(512) DEFAULT NULL COMMENT '下载加速地址',
  sha256          VARCHAR(128) NOT NULL COMMENT 'SHA256',
  signature_algorithm VARCHAR(64) DEFAULT NULL COMMENT '签名算法，如 sha256 或 sha256-rsa',
  signature_value VARCHAR(2048) DEFAULT NULL COMMENT '签名值或期望摘要',
  signature_status TINYINT NOT NULL DEFAULT 0 COMMENT '0=未校验 1=通过 2=失败',
  signature_verified_at DATETIME(3) DEFAULT NULL COMMENT '签名校验时间',
  status          TINYINT NOT NULL DEFAULT 1 COMMENT '0=上传中 1=可用 2=已删除',
  download_count  BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '下载量',
  scan_status     TINYINT NOT NULL DEFAULT 0 COMMENT '0=未扫描 1=安全 2=有风险 3=扫描失败',
  scan_report     TEXT DEFAULT NULL COMMENT '扫描报告',
  created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  deleted_at      DATETIME(3) DEFAULT NULL,
  created_by      BIGINT UNSIGNED NOT NULL DEFAULT 0,
  updated_by      BIGINT UNSIGNED NOT NULL DEFAULT 0,
  UNIQUE KEY uk_version_os_arch (version_id, os_type, arch),
  KEY idx_app_id (app_id),
  KEY idx_os_arch (os_type, arch),
  KEY idx_deleted_at (deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Java后台安装包表';

CREATE TABLE IF NOT EXISTS package_upload_sessions (
  upload_id             VARCHAR(64) NOT NULL PRIMARY KEY COMMENT '上传会话ID',
  file_name             VARCHAR(256) NOT NULL COMMENT '原始文件名',
  package_format        VARCHAR(16) NOT NULL COMMENT 'deb/rpm/appimage',
  file_size             BIGINT UNSIGNED NOT NULL COMMENT '完整文件大小',
  chunk_size            BIGINT UNSIGNED NOT NULL COMMENT '分片大小',
  total_chunks          INT UNSIGNED NOT NULL COMMENT '分片总数',
  uploaded_chunk_count  INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '已上传分片数量',
  uploaded_chunks       JSON NOT NULL COMMENT '已上传分片序号数组',
  expected_sha256       VARCHAR(128) DEFAULT NULL COMMENT '前端或制品平台提供的期望SHA256',
  actual_sha256         VARCHAR(128) DEFAULT NULL COMMENT '合并后实际SHA256',
  storage_path          VARCHAR(512) DEFAULT NULL COMMENT '合并成功后的安装包存储路径',
  signature_algorithm   VARCHAR(64) DEFAULT NULL COMMENT '签名算法',
  signature_value       VARCHAR(2048) DEFAULT NULL COMMENT '签名值或期望摘要',
  signature_status      TINYINT NOT NULL DEFAULT 0 COMMENT '0=未校验 1=通过 2=失败',
  signature_verified_at DATETIME(3) DEFAULT NULL COMMENT '签名校验时间',
  status                TINYINT NOT NULL DEFAULT 0 COMMENT '0=上传中 1=已完成 2=已消费 3=失败',
  error_message         VARCHAR(512) DEFAULT NULL COMMENT '失败原因',
  created_by            BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人',
  created_at            DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at            DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  completed_at          DATETIME(3) DEFAULT NULL,
  KEY idx_created_by_status (created_by, status),
  KEY idx_updated_at (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Java后台安装包分片上传会话表';

CREATE TABLE IF NOT EXISTS app_tags (
  app_id      BIGINT UNSIGNED NOT NULL COMMENT '软件ID',
  tag_id      BIGINT UNSIGNED NOT NULL COMMENT '标签ID',
  created_at  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (app_id, tag_id),
  KEY idx_tag_id (tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Java后台软件标签关联表';


CREATE TABLE IF NOT EXISTS review_tasks (
  id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  app_id           BIGINT UNSIGNED NOT NULL COMMENT '软件ID',
  version_id       BIGINT UNSIGNED DEFAULT NULL COMMENT '版本ID，空表示软件级审核',
  target_type      VARCHAR(32) NOT NULL DEFAULT 'software' COMMENT 'software/version',
  title            VARCHAR(180) NOT NULL COMMENT '审核标题',
  status           TINYINT NOT NULL DEFAULT 0 COMMENT '0=待审核 1=审核中 2=通过 3=驳回 4=取消',
  priority         TINYINT NOT NULL DEFAULT 1 COMMENT '0=低 1=普通 2=高',
  submit_reason    VARCHAR(512) DEFAULT NULL COMMENT '提交原因',
  review_comment   VARCHAR(512) DEFAULT NULL COMMENT '审核意见',
  reviewer_id      BIGINT UNSIGNED DEFAULT NULL COMMENT '审核人ID',
  submitted_by     BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '提交人ID',
  submitted_at     DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  reviewed_at      DATETIME(3) DEFAULT NULL,
  created_at       DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at       DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  KEY idx_app_status (app_id, status),
  KEY idx_version_status (version_id, status),
  KEY idx_reviewer_status (reviewer_id, status),
  KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Java后台审核任务表';

CREATE TABLE IF NOT EXISTS review_histories (
  id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  task_id      BIGINT UNSIGNED NOT NULL COMMENT '审核任务ID',
  action       VARCHAR(32) NOT NULL COMMENT 'submit/assign/approve/reject',
  from_status  TINYINT DEFAULT NULL COMMENT '变更前状态',
  to_status    TINYINT NOT NULL COMMENT '变更后状态',
  operator_id  BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '操作人ID',
  comment      VARCHAR(512) DEFAULT NULL COMMENT '备注',
  created_at   DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  KEY idx_task_id (task_id),
  KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Java后台审核历史表';

CREATE TABLE IF NOT EXISTS operation_logs (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id        BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '操作人ID',
  user_type      VARCHAR(32) NOT NULL DEFAULT 'admin' COMMENT '操作人类型',
  username       VARCHAR(128) NOT NULL DEFAULT '' COMMENT '操作人名称',
  action         VARCHAR(64) NOT NULL COMMENT '操作类型',
  resource_type  VARCHAR(64) NOT NULL COMMENT '资源类型',
  resource_id    BIGINT UNSIGNED DEFAULT NULL COMMENT '资源ID',
  resource_name  VARCHAR(128) NOT NULL DEFAULT '' COMMENT '资源名称',
  detail         JSON NOT NULL COMMENT '操作详情',
  ip             VARCHAR(64) NOT NULL DEFAULT '' COMMENT '客户端IP',
  user_agent     VARCHAR(512) NOT NULL DEFAULT '' COMMENT 'User-Agent',
  created_at     DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  KEY idx_user_created (user_id, created_at),
  KEY idx_action_created (action, created_at),
  KEY idx_resource (resource_type, resource_id),
  KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Java后台操作审计日志表';

INSERT IGNORE INTO categories (id, name, icon, parent_id, sort_order, status, description, is_builtin) VALUES
(1, '办公软件', '/icons/office.png', NULL, 10, 1, '办公文档、笔记、PDF等办公相关软件', 1),
(2, '图形图像', '/icons/graphics.png', NULL, 20, 1, '图片编辑、设计、绘图等图像处理软件', 1),
(3, '影音娱乐', '/icons/media.png', NULL, 30, 1, '视频播放、音频处理、多媒体软件', 1),
(4, '系统工具', '/icons/system.png', NULL, 40, 1, '系统优化、文件管理、终端工具', 1),
(5, '网络工具', '/icons/network.png', NULL, 50, 1, '浏览器、下载、远程连接等网络工具', 1),
(6, '编程开发', '/icons/develop.png', NULL, 60, 1, 'IDE、编辑器、版本控制等开发工具', 1);

INSERT IGNORE INTO tags (id, name, is_hot, description, is_builtin) VALUES
(1, '免费', 1, '完全免费使用的软件', 1),
(2, '开源', 1, '开放源代码的软件', 1),
(3, '官方', 0, '官方出品的软件', 1),
(4, '绿色', 0, '无需复杂安装的便携软件', 1),
(5, '多语言', 0, '支持多种语言的软件', 1),
(6, '轻量', 1, '体积小、资源占用低的软件', 1);

INSERT IGNORE INTO admin_users (id, username, display_name, status) VALUES
(1, 'admin', '超级管理员', 1);

INSERT IGNORE INTO admin_roles (id, role_code, role_name, description, status) VALUES
(1, 'super_admin', '超级管理员', '拥有后台全部权限', 1),
(2, 'operator', '运营人员', '负责软件资料、分类标签和提审', 1),
(3, 'reviewer', '审核人员', '负责审核任务分配、通过和驳回', 1),
(4, 'viewer', '只读人员', '只能查看后台数据和日志', 1);

INSERT IGNORE INTO admin_permissions (permission_code, permission_name, module, description) VALUES
('*', '全部权限', 'system', '超级管理员通配权限'),
('software:view', '查看软件', 'software', '查看软件列表、详情、版本和安装包'),
('software:create', '上传软件', 'software', '创建软件和初始版本'),
('software:update', '编辑软件', 'software', '编辑软件元数据'),
('software:publish', '上架软件', 'software', '上架已通过审核的软件'),
('software:unpublish', '下架软件', 'software', '直接下架软件'),
('software:version:create', '新增版本', 'software', '为软件新增版本'),
('software:package:create', '追加安装包', 'software', '为版本追加安装包变体'),
('software:package:scan', '模拟扫描安装包', 'software', '更新安装包本地模拟扫描结果'),
('software:upload', '分片上传', 'software', '创建和管理安装包上传会话'),
('review:view', '查看审核', 'review', '查看审核任务和历史'),
('review:submit', '提交审核', 'review', '提交软件或版本审核'),
('review:assign', '分配审核', 'review', '分配审核任务'),
('review:approve', '审核通过', 'review', '通过审核任务'),
('review:reject', '审核驳回', 'review', '驳回审核任务'),
('category:view', '查看分类', 'category', '查看分类列表和树'),
('category:manage', '管理分类', 'category', '新增、编辑、启停和删除分类'),
('tag:view', '查看标签', 'tag', '查看标签列表和热门标签'),
('tag:manage', '管理标签', 'tag', '新增、编辑、热门标记和删除标签'),
('operation_log:view', '查看操作日志', 'operation_log', '查看操作日志、统计和选项'),
('rbac:view', '查看权限配置', 'rbac', '查看管理员、角色和权限点'),
('rbac:manage', '管理权限配置', 'rbac', '新增管理员、维护角色和分配权限');

INSERT IGNORE INTO admin_user_roles (user_id, role_id)
SELECT 1, id FROM admin_roles WHERE role_code = 'super_admin';

INSERT IGNORE INTO admin_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM admin_roles r
INNER JOIN admin_permissions p ON p.permission_code = '*'
WHERE r.role_code = 'super_admin';

INSERT IGNORE INTO admin_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM admin_roles r
INNER JOIN admin_permissions p ON p.permission_code IN (
  'software:view', 'software:create', 'software:update', 'software:version:create',
  'software:package:create', 'software:package:scan', 'software:upload', 'review:view', 'review:submit',
  'category:view', 'category:manage', 'tag:view', 'tag:manage', 'rbac:view'
)
WHERE r.role_code = 'operator';

INSERT IGNORE INTO admin_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM admin_roles r
INNER JOIN admin_permissions p ON p.permission_code IN (
  'software:view', 'review:view', 'review:assign', 'review:approve',
  'review:reject', 'operation_log:view', 'rbac:view'
)
WHERE r.role_code = 'reviewer';

INSERT IGNORE INTO admin_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM admin_roles r
INNER JOIN admin_permissions p ON p.permission_code IN (
  'software:view', 'review:view', 'category:view', 'tag:view', 'operation_log:view', 'rbac:view'
)
WHERE r.role_code = 'viewer';

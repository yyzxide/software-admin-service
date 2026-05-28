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
  UNIQUE KEY uk_category_name_alive (name, deleted_at),
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
  status          TINYINT NOT NULL DEFAULT 1 COMMENT '0=上传中 1=可用 2=已删除',
  download_count  BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '下载量',
  scan_status     TINYINT NOT NULL DEFAULT 0 COMMENT '0=未扫描 1=安全 2=有风险',
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

CREATE TABLE IF NOT EXISTS app_tags (
  app_id      BIGINT UNSIGNED NOT NULL COMMENT '软件ID',
  tag_id      BIGINT UNSIGNED NOT NULL COMMENT '标签ID',
  created_at  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (app_id, tag_id),
  KEY idx_tag_id (tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Java后台软件标签关联表';

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

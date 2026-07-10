SET NAMES utf8mb4;
USE db_java_software_admin;

SET @ddl = (
  SELECT IF(
    COUNT(*) > 0,
    'ALTER TABLE app_packages DROP INDEX uk_version_os_arch',
    'SELECT 1'
  )
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'app_packages'
    AND INDEX_NAME = 'uk_version_os_arch'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE app_packages
  MODIFY scan_status TINYINT NOT NULL DEFAULT 0 COMMENT '0=未标记安全 1=安全 2=有风险 3=处理失败',
  MODIFY scan_report TEXT DEFAULT NULL COMMENT '安全状态说明';

ALTER TABLE package_upload_sessions
  MODIFY status TINYINT NOT NULL DEFAULT 0 COMMENT '0=上传中 1=已完成 2=已消费 3=失败 4=合并中';

SET @ddl = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE app_packages ADD UNIQUE KEY uk_version_os_arch_format (version_id, os_type, arch, package_format)',
    'SELECT 1'
  )
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'app_packages'
    AND INDEX_NAME = 'uk_version_os_arch_format'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE app_versions ADD UNIQUE KEY uk_app_version_code (app_id, version_code)',
    'SELECT 1'
  )
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'app_versions'
    AND INDEX_NAME = 'uk_app_version_code'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE app_packages ADD KEY idx_app_scan_status (app_id, scan_status)',
    'SELECT 1'
  )
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'app_packages'
    AND INDEX_NAME = 'idx_app_scan_status'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE package_upload_sessions ADD KEY idx_status_updated_at (status, updated_at)',
    'SELECT 1'
  )
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'package_upload_sessions'
    AND INDEX_NAME = 'idx_status_updated_at'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE apps ADD KEY idx_status_updated_at (status, updated_at)',
    'SELECT 1'
  )
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'apps'
    AND INDEX_NAME = 'idx_status_updated_at'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

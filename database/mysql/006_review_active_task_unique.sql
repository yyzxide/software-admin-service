SET NAMES utf8mb4;

USE db_java_software_admin;

SET @has_active_review_key := (
  SELECT COUNT(1)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'review_tasks'
    AND COLUMN_NAME = 'active_review_key'
);

SET @add_active_review_key_sql := IF(
  @has_active_review_key = 0,
  'ALTER TABLE review_tasks ADD COLUMN active_review_key BIGINT UNSIGNED GENERATED ALWAYS AS (CASE WHEN status IN (0, 1) THEN COALESCE(version_id, 0) ELSE NULL END) STORED COMMENT ''活动审核唯一键，0表示软件级审核'' AFTER updated_at',
  'SELECT 1'
);

PREPARE add_active_review_key_stmt FROM @add_active_review_key_sql;
EXECUTE add_active_review_key_stmt;
DEALLOCATE PREPARE add_active_review_key_stmt;

SET @has_active_review_index := (
  SELECT COUNT(1)
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'review_tasks'
    AND INDEX_NAME = 'uk_review_active_target'
);

SET @add_active_review_index_sql := IF(
  @has_active_review_index = 0,
  'ALTER TABLE review_tasks ADD UNIQUE KEY uk_review_active_target (app_id, active_review_key)',
  'SELECT 1'
);

PREPARE add_active_review_index_stmt FROM @add_active_review_index_sql;
EXECUTE add_active_review_index_stmt;
DEALLOCATE PREPARE add_active_review_index_stmt;

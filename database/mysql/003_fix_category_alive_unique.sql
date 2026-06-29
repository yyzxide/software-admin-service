SET NAMES utf8mb4;

USE db_java_software_admin;

SET @has_old_index := (
  SELECT COUNT(1)
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'categories'
    AND INDEX_NAME = 'uk_category_name_alive'
);

SET @drop_old_index_sql := IF(
  @has_old_index > 0,
  'ALTER TABLE categories DROP INDEX uk_category_name_alive',
  'SELECT 1'
);

PREPARE drop_old_index_stmt FROM @drop_old_index_sql;
EXECUTE drop_old_index_stmt;
DEALLOCATE PREPARE drop_old_index_stmt;

SET @has_alive_name := (
  SELECT COUNT(1)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'categories'
    AND COLUMN_NAME = 'alive_name'
);

SET @add_alive_name_sql := IF(
  @has_alive_name = 0,
  'ALTER TABLE categories ADD COLUMN alive_name VARCHAR(50) GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN name ELSE NULL END) STORED AFTER deleted_at',
  'SELECT 1'
);

PREPARE add_alive_name_stmt FROM @add_alive_name_sql;
EXECUTE add_alive_name_stmt;
DEALLOCATE PREPARE add_alive_name_stmt;

SET @has_alive_index := (
  SELECT COUNT(1)
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'categories'
    AND INDEX_NAME = 'uk_category_alive_name'
);

SET @add_alive_index_sql := IF(
  @has_alive_index = 0,
  'ALTER TABLE categories ADD UNIQUE KEY uk_category_alive_name (alive_name)',
  'SELECT 1'
);

PREPARE add_alive_index_stmt FROM @add_alive_index_sql;
EXECUTE add_alive_index_stmt;
DEALLOCATE PREPARE add_alive_index_stmt;

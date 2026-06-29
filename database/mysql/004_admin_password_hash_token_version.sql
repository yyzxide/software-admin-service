SET NAMES utf8mb4;

USE db_java_software_admin;

SET @has_password_hash := (
  SELECT COUNT(1)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'admin_users'
    AND COLUMN_NAME = 'password_hash'
);

SET @add_password_hash_sql := IF(
  @has_password_hash = 0,
  'ALTER TABLE admin_users ADD COLUMN password_hash VARCHAR(128) NOT NULL DEFAULT '''' COMMENT ''登录密码哈希'' AFTER display_name',
  'SELECT 1'
);

PREPARE add_password_hash_stmt FROM @add_password_hash_sql;
EXECUTE add_password_hash_stmt;
DEALLOCATE PREPARE add_password_hash_stmt;

SET @has_token_version := (
  SELECT COUNT(1)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'admin_users'
    AND COLUMN_NAME = 'token_version'
);

SET @add_token_version_sql := IF(
  @has_token_version = 0,
  'ALTER TABLE admin_users ADD COLUMN token_version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT ''Token会话版本，变更后旧Token失效'' AFTER status',
  'SELECT 1'
);

PREPARE add_token_version_stmt FROM @add_token_version_sql;
EXECUTE add_token_version_stmt;
DEALLOCATE PREPARE add_token_version_stmt;

UPDATE admin_users
SET password_hash = CONCAT('{sha256}', password_sha256)
WHERE (password_hash IS NULL OR password_hash = '')
  AND password_sha256 IS NOT NULL
  AND password_sha256 <> '';

UPDATE admin_users
SET password_hash = '{sha256}ac0e7d037817094e9e0b4441f9bae3209d67b02fa484917065f71b16109a1a78'
WHERE username = 'admin'
  AND (password_hash IS NULL OR password_hash = '');

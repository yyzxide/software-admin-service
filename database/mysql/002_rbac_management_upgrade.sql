SET NAMES utf8mb4;

USE db_java_software_admin;

SET @has_password_sha256 := (
  SELECT COUNT(1)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'admin_users'
    AND COLUMN_NAME = 'password_sha256'
);

SET @add_password_sha256_sql := IF(
  @has_password_sha256 = 0,
  'ALTER TABLE admin_users ADD COLUMN password_sha256 VARCHAR(64) NOT NULL DEFAULT ''ac0e7d037817094e9e0b4441f9bae3209d67b02fa484917065f71b16109a1a78'' COMMENT ''登录密码SHA256'' AFTER display_name',
  'SELECT 1'
);

PREPARE add_password_sha256_stmt FROM @add_password_sha256_sql;
EXECUTE add_password_sha256_stmt;
DEALLOCATE PREPARE add_password_sha256_stmt;

UPDATE admin_users
SET password_sha256 = 'ac0e7d037817094e9e0b4441f9bae3209d67b02fa484917065f71b16109a1a78'
WHERE username = 'admin'
  AND (password_sha256 IS NULL OR password_sha256 = '');

INSERT IGNORE INTO admin_permissions (permission_code, permission_name, module, description) VALUES
('rbac:view', '查看权限配置', 'rbac', '查看管理员、角色和权限点'),
('rbac:manage', '管理权限配置', 'rbac', '新增管理员、维护角色和分配权限');

INSERT IGNORE INTO admin_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM admin_roles r
INNER JOIN admin_permissions p ON p.permission_code = 'rbac:view'
WHERE r.role_code IN ('operator', 'reviewer', 'viewer');

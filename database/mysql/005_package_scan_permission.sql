SET NAMES utf8mb4;

USE db_java_software_admin;

INSERT IGNORE INTO admin_permissions (permission_code, permission_name, module, description) VALUES
('software:package:scan', '安装包安全状态', 'software', '更新安装包安全状态结果');

UPDATE admin_permissions
SET permission_name = '安装包安全状态',
    description = '更新安装包安全状态结果'
WHERE permission_code = 'software:package:scan';

INSERT IGNORE INTO admin_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM admin_roles r
INNER JOIN admin_permissions p ON p.permission_code = 'software:package:scan'
WHERE r.role_code = 'operator';

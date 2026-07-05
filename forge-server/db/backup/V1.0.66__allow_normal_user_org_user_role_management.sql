-- Allow normal users with assigned permissions to access org, user and role management.
-- Platform-only resources such as menu, config, client and data-scope configuration remain restricted by V1.0.65.

UPDATE sys_resource
SET min_user_type = 2
WHERE resource_name IN ('系统管理', '组织管理', '用户管理', '角色管理')
   OR path IN ('/system/org', '/system/user', '/system/role')
   OR component IN ('system/org', 'system/user', 'system/role')
   OR perms LIKE 'system:org:%'
   OR perms LIKE 'system:user:%'
   OR perms LIKE 'system:role:%'
   OR api_url = '/system/org'
   OR api_url LIKE '/system/org/%'
   OR api_url = '/system/user'
   OR api_url LIKE '/system/user/%'
   OR api_url = '/system/role'
   OR api_url LIKE '/system/role/%';

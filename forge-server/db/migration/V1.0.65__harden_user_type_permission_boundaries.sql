-- Harden permission boundaries between user type, role data scope and assignable resources.

SET @col_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_resource'
    AND COLUMN_NAME = 'min_user_type'
);
SET @sql = IF(@col_exists = 0,
  'ALTER TABLE sys_resource ADD COLUMN min_user_type INT NOT NULL DEFAULT 2 COMMENT ''最低可访问用户类型（0-系统管理员，1-租户管理员，2-普通用户）'' AFTER client_code',
  'SELECT ''Column min_user_type already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE sys_resource
SET min_user_type = 2
WHERE min_user_type IS NULL;

-- Tenant management resources: visible/assignable only to tenant admins and system admins.
UPDATE sys_resource
SET min_user_type = 1
WHERE resource_name IN ('系统管理', '用户管理', '角色管理', '组织管理', '岗位管理')
   OR path IN ('/system/user', '/system/role', '/system/org', '/system/post')
   OR component IN ('system/user', 'system/role', 'system/org', 'system/post')
   OR perms LIKE 'system:user:%'
   OR perms LIKE 'system:role:%'
   OR perms LIKE 'system:org:%'
   OR perms LIKE 'system:post:%'
   OR api_url = '/system/user'
   OR api_url LIKE '/system/user/%'
   OR api_url = '/system/role'
   OR api_url LIKE '/system/role/%'
   OR api_url = '/system/org'
   OR api_url LIKE '/system/org/%'
   OR api_url = '/system/post'
   OR api_url LIKE '/system/post/%';

-- Platform configuration resources: visible/assignable only to system admins.
UPDATE sys_resource
SET min_user_type = 0
WHERE resource_name IN (
        '数据权限配置', '客户端配置', '系统参数配置', '系统配置', '菜单管理',
        '资源管理', '租户管理', '字典管理', '字典类型', '字典数据',
        '缓存管理', '系统监控', '在线用户', '操作日志', '登录日志',
        '文件存储配置', 'Excel导出配置', 'Excel列配置'
      )
   OR path IN (
        '/system/dataScopeConfig', '/system/client', '/system/config',
        '/system/menu', '/system/resource', '/system/tenant',
        '/system/dictType', '/system/dictData', '/system/cache',
        '/system/monitor', '/system/online', '/system/operation-log',
        '/system/login-log', '/system/fileStorageConfig',
        '/system/excelExportConfig', '/system/excelColumnConfig'
      )
   OR component IN (
        'system/dataScopeConfig', 'system/client', 'system/config',
        'system/menu', 'system/tenant', 'system/dictType',
        'system/dictData', 'system/cache', 'system/monitor',
        'system/online', 'system/operation-log', 'system/login-log',
        'system/fileStorageConfig', 'system/excelExportConfig',
        'system/excelColumnConfig'
      )
   OR perms LIKE 'system:dataScopeConfig:%'
   OR perms LIKE 'system:client:%'
   OR perms LIKE 'system:config:%'
   OR perms LIKE 'system:resource:%'
   OR perms LIKE 'system:menu:%'
   OR perms LIKE 'system:tenant:%'
   OR perms LIKE 'system:dict:%'
   OR perms LIKE 'system:cache:%'
   OR perms LIKE 'system:monitor:%'
   OR perms LIKE 'system:online:%'
   OR perms LIKE 'system:operationLog:%'
   OR perms LIKE 'system:loginLog:%'
   OR api_url = '/system/dataScopeConfig'
   OR api_url LIKE '/system/dataScopeConfig/%'
   OR api_url = '/datascope'
   OR api_url LIKE '/datascope/%'
   OR api_url = '/system/client'
   OR api_url LIKE '/system/client/%'
   OR api_url = '/system/config'
   OR api_url LIKE '/system/config/%'
   OR api_url = '/system/resource'
   OR api_url LIKE '/system/resource/%'
   OR api_url = '/system/tenant'
   OR api_url LIKE '/system/tenant/%'
   OR api_url = '/system/dict'
   OR api_url LIKE '/system/dict/%'
   OR api_url = '/system/cache'
   OR api_url LIKE '/system/cache/%'
   OR api_url = '/system/monitor'
   OR api_url LIKE '/system/monitor/%'
   OR api_url = '/system/online'
   OR api_url LIKE '/system/online/%'
   OR api_url = '/system/operation-log'
   OR api_url LIKE '/system/operation-log/%'
   OR api_url = '/system/login-log'
   OR api_url LIKE '/system/login-log/%';

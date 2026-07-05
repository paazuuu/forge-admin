-- Add a visible debug page for tenant business datasource routing.

SET @app_center_root_id := (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND client_code = 'pc'
    AND resource_type = 1
    AND parent_id = 0
    AND resource_name = '应用中心'
  ORDER BY id
  LIMIT 1
);

SET @app_datasource_id := (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND client_code = 'pc'
    AND resource_type = 2
    AND path = '/generator/datasource'
  ORDER BY id
  LIMIT 1
);

SET @developer_root_id := (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND client_code = 'pc'
    AND resource_type = 1
    AND parent_id = 0
    AND resource_name = '开发者工具'
  ORDER BY id
  LIMIT 1
);

SET @debug_parent_id := COALESCE(@app_center_root_id, @developer_root_id, 0);

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external,
                          sso_enabled, sso_target_client, open_target, is_public, menu_status, visible, perms, icon,
                          api_method, api_url, keep_alive, always_show, redirect, remark, create_by, create_time,
                          update_by, update_time, create_dept, client_code)
SELECT 1, '业务数据源调试', @debug_parent_id, 2, 6, '/business/datasource-debug', 'business/datasource-debug', 0,
       0, NULL, '_self', 0, 1, 1, 'business:datasourceDebug:view', 'ionicons5:GitCompareOutline',
       NULL, NULL, 0, 0, NULL, '租户业务数据源路由调试页面', 1, NOW(),
       1, NOW(), 1, 'pc'
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_resource
  WHERE tenant_id = 1
    AND client_code = 'pc'
    AND resource_type = 2
    AND path = '/business/datasource-debug'
);

UPDATE sys_resource
SET parent_id = @debug_parent_id,
    resource_name = '业务数据源调试',
    component = 'business/datasource-debug',
    perms = 'business:datasourceDebug:view',
    icon = 'ionicons5:GitCompareOutline',
    sort = 6,
    visible = 1,
    menu_status = 1,
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND client_code = 'pc'
  AND resource_type = 2
  AND path = '/business/datasource-debug';

SET @debug_menu_id := (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND client_code = 'pc'
    AND resource_type = 2
    AND path = '/business/datasource-debug'
  ORDER BY id
  LIMIT 1
);

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external,
                          sso_enabled, sso_target_client, open_target, is_public, menu_status, visible, perms, icon,
                          api_method, api_url, keep_alive, always_show, redirect, remark, create_by, create_time,
                          update_by, update_time, create_dept, client_code)
SELECT seed.tenant_id, seed.resource_name, @debug_menu_id, 4, seed.sort, NULL, NULL, 0,
       0, NULL, '_self', 0, 1, 0, seed.perms, NULL,
       seed.api_method, seed.api_url, 0, 0, NULL, seed.remark, 1, NOW(), 1, NOW(), 1, 'pc'
FROM (
  SELECT 1 tenant_id, '业务数据源调试-当前路由' resource_name, 1 sort,
         'business:datasourceDebug:current' perms, 'GET' api_method,
         '/business/datasource-demo/current' api_url, '查询当前租户业务数据源路由' remark
  UNION ALL
  SELECT 1, '业务数据源调试-写入记录', 2,
         'business:datasourceDebug:prepare', 'POST',
         '/business/datasource-demo/prepare', '写入业务数据源调试记录'
  UNION ALL
  SELECT 1, '业务数据源调试-记录列表', 3,
         'business:datasourceDebug:list', 'GET',
         '/business/datasource-demo/list', '查询业务数据源调试记录'
) seed
WHERE @debug_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM sys_resource r
    WHERE r.tenant_id = seed.tenant_id
      AND r.client_code = 'pc'
      AND r.resource_type = 4
      AND r.api_method = seed.api_method
      AND r.api_url = seed.api_url
  );

SET @admin_role_id := (
  SELECT id
  FROM sys_role
  WHERE tenant_id = 1
    AND role_key = 'admin'
  ORDER BY id
  LIMIT 1
);

INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT 1, @admin_role_id, target.id, NOW()
FROM sys_resource target
WHERE @admin_role_id IS NOT NULL
  AND target.tenant_id = 1
  AND target.client_code = 'pc'
  AND (
    target.path = '/business/datasource-debug'
    OR target.api_url IN (
      '/business/datasource-demo/current',
      '/business/datasource-demo/prepare',
      '/business/datasource-demo/list'
    )
  )
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_resource exists_rr
    WHERE exists_rr.tenant_id = 1
      AND exists_rr.role_id = @admin_role_id
      AND exists_rr.resource_id = target.id
  );

INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT DISTINCT source.tenant_id, source.role_id, target.id, NOW()
FROM (
  SELECT DISTINCT rr.tenant_id, rr.role_id
  FROM sys_role_resource rr
  INNER JOIN sys_resource owned ON owned.id = rr.resource_id
  WHERE owned.tenant_id = 1
    AND owned.client_code = 'pc'
    AND (
      owned.id = @app_datasource_id
      OR owned.path = '/generator/datasource'
      OR owned.perms IN ('ai:datasource:list', 'gen:datasource:list')
    )
) source
INNER JOIN sys_resource target ON target.tenant_id = 1
WHERE target.client_code = 'pc'
  AND (
    target.path = '/business/datasource-debug'
    OR target.api_url IN (
      '/business/datasource-demo/current',
      '/business/datasource-demo/prepare',
      '/business/datasource-demo/list'
    )
  )
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_resource exists_rr
    WHERE exists_rr.tenant_id = source.tenant_id
      AND exists_rr.role_id = source.role_id
      AND exists_rr.resource_id = target.id
  );

-- Restore application-center runtime route permissions.
-- The frontend permission guard uses the current user's menu resources as the route allowlist.
-- Some application-center actions navigate to manual or hidden routes that were never registered
-- or were not granted to roles that already own the application center.

SET @app_root_id := (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND client_code = 'pc'
    AND resource_type = 1
    AND resource_name = '应用中心'
  ORDER BY id
  LIMIT 1
);

SET @app_overview_id := (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND client_code = 'pc'
    AND resource_type = 2
    AND path = '/app-center'
    AND component = 'app-center/index'
  ORDER BY id
  LIMIT 1
);

SET @app_engine_id := (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND client_code = 'pc'
    AND resource_type = 2
    AND path = '/app-center/engines'
  ORDER BY id
  LIMIT 1
);

SET @message_root_id := (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND client_code = 'pc'
    AND resource_type = 1
    AND resource_name = '消息中心'
  ORDER BY id
  LIMIT 1
);

SET @app_hidden_parent_id := COALESCE(@app_engine_id, @app_overview_id, @app_root_id, 0);
SET @app_permission_parent_id := COALESCE(@app_overview_id, @app_engine_id, @app_root_id, 0);

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external,
                          sso_enabled, sso_target_client, open_target, is_public, menu_status, visible, perms, icon,
                          api_method, api_url, keep_alive, always_show, redirect, remark, create_by, create_time,
                          update_by, update_time, create_dept, client_code)
SELECT seed.tenant_id, seed.resource_name, seed.parent_id, 2, seed.sort, seed.path, seed.component, 0,
       0, NULL, '_self', 0, 1, 0, seed.perms, seed.icon,
       NULL, NULL, 0, 0, NULL, seed.remark, 1, NOW(), 1, NOW(), 1, 'pc'
FROM (
  SELECT 1 tenant_id, '业务报表看板' resource_name, @app_hidden_parent_id parent_id, 31 sort,
         '/app-center/stats' path, 'app-center/stats-dashboard' component,
         'ai:businessStats:view' perms, 'ionicons5:BarChartOutline' icon, '应用中心报表看板隐藏路由' remark
  UNION ALL
  SELECT 1, '触发器配置', @app_hidden_parent_id, 32,
         '/app-center/trigger', 'app-center/trigger',
         'ai:businessTrigger:list', 'ionicons5:FlashOutline', '应用中心触发器配置隐藏路由'
  UNION ALL
  SELECT 1, '业务运行态页面', @app_permission_parent_id, 33,
         '/ai/crud-page/:configKey', 'ai/crud-page',
         'ai:businessRuntime:route', 'ionicons5:PlayCircleOutline', '应用中心通用运行态隐藏路由'
  UNION ALL
  SELECT 1, '访问入口跳转', @app_permission_parent_id, 34,
         '/app-center/app/:appId', 'app-center/app-entry',
         'ai:businessApp:entryRoute', 'ionicons5:OpenOutline', '应用中心访问入口跳转隐藏路由'
  UNION ALL
  SELECT 1, '消息模板配置', COALESCE(@message_root_id, @app_hidden_parent_id), 35,
         '/message/template', 'message/template-list',
         'message:template:route', 'ionicons5:MailOutline', '引擎中心消息模板隐藏路由'
) seed
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_resource r
  WHERE r.tenant_id = seed.tenant_id
    AND r.client_code = 'pc'
    AND r.resource_type = 2
    AND r.path = seed.path
);

UPDATE sys_resource
SET parent_id = CASE path
                  WHEN '/app-center/stats' THEN @app_hidden_parent_id
                  WHEN '/app-center/trigger' THEN @app_hidden_parent_id
                  WHEN '/ai/crud-page/:configKey' THEN @app_permission_parent_id
                  WHEN '/app-center/app/:appId' THEN @app_permission_parent_id
                  WHEN '/message/template' THEN COALESCE(@message_root_id, @app_hidden_parent_id)
                  ELSE parent_id
                END,
    resource_name = CASE path
                      WHEN '/app-center/stats' THEN '业务报表看板'
                      WHEN '/app-center/trigger' THEN '触发器配置'
                      WHEN '/ai/crud-page/:configKey' THEN '业务运行态页面'
                      WHEN '/app-center/app/:appId' THEN '访问入口跳转'
                      WHEN '/message/template' THEN '消息模板配置'
                      ELSE resource_name
                    END,
    component = CASE path
                  WHEN '/app-center/stats' THEN 'app-center/stats-dashboard'
                  WHEN '/app-center/trigger' THEN 'app-center/trigger'
                  WHEN '/ai/crud-page/:configKey' THEN 'ai/crud-page'
                  WHEN '/app-center/app/:appId' THEN 'app-center/app-entry'
                  WHEN '/message/template' THEN 'message/template-list'
                  ELSE component
                END,
    menu_status = 1,
    visible = 0,
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND client_code = 'pc'
  AND resource_type = 2
  AND path IN (
    '/app-center/stats',
    '/app-center/trigger',
    '/ai/crud-page/:configKey',
    '/app-center/app/:appId',
    '/message/template'
  );

UPDATE sys_resource
SET parent_id = @app_overview_id,
    menu_status = 1,
    visible = 0,
    component = CASE path
                  WHEN '/app-center/suite/:suiteCode' THEN 'app-center/suite.[suiteCode]'
                  WHEN '/app-center/object/:objectCode' THEN 'app-center/object.[objectCode]'
                  WHEN '/app-center/object/:objectCode/designer' THEN 'app-center/object-designer.[objectCode]'
                  ELSE component
                END,
    update_by = 1,
    update_time = NOW()
WHERE @app_overview_id IS NOT NULL
  AND tenant_id = 1
  AND client_code = 'pc'
  AND resource_type = 2
  AND path IN (
    '/app-center/suite/:suiteCode',
    '/app-center/object/:objectCode',
    '/app-center/object/:objectCode/designer'
  );

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external,
                          sso_enabled, sso_target_client, open_target, is_public, menu_status, visible, perms, icon,
                          api_method, api_url, keep_alive, always_show, redirect, remark, create_by, create_time,
                          update_by, update_time, create_dept, client_code)
SELECT 1, seed.resource_name, @app_permission_parent_id, 3, seed.sort, NULL, NULL, 0,
       0, NULL, '_self', 0, 1, 1, seed.perms, NULL,
       NULL, NULL, 0, 0, NULL, seed.remark, 1, NOW(), 1, NOW(), 1, 'pc'
FROM (
  SELECT '查看业务就绪度' resource_name, 36 sort, 'ai:businessReadiness:view' perms, '查看业务单元就绪度权限' remark
  UNION ALL SELECT '查看关系运行入口', 37, 'ai:businessRelation:runtime', '查看业务对象关系运行入口权限'
  UNION ALL SELECT '查看业务域验收', 38, 'ai:businessAcceptance:view', '查看业务域验收检查权限'
  UNION ALL SELECT '查看引擎运行状态', 39, 'ai:businessEngine:runtime', '查看引擎运行状态汇总权限'
) seed
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_resource r
  WHERE r.tenant_id = 1
    AND r.resource_type = 3
    AND r.perms = seed.perms
);

UPDATE sys_resource
SET parent_id = @app_permission_parent_id,
    menu_status = 1,
    visible = 1,
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND resource_type = 3
  AND perms IN (
    'ai:businessApp:list',
    'ai:businessSuite:list',
    'ai:businessObject:list',
    'ai:businessObject:relation',
    'ai:businessBinding:config',
    'ai:businessApp:open',
    'ai:businessStats:view',
    'ai:businessTrigger:list',
    'ai:businessReadiness:view',
    'ai:businessRelation:runtime',
    'ai:businessAcceptance:view',
    'ai:businessEngine:runtime'
  );

SET @app_suite_detail_id := (
  SELECT id FROM sys_resource
  WHERE tenant_id = 1 AND client_code = 'pc' AND resource_type = 2 AND path = '/app-center/suite/:suiteCode'
  ORDER BY id LIMIT 1
);

SET @app_object_detail_id := (
  SELECT id FROM sys_resource
  WHERE tenant_id = 1 AND client_code = 'pc' AND resource_type = 2 AND path = '/app-center/object/:objectCode'
  ORDER BY id LIMIT 1
);

SET @app_object_designer_id := (
  SELECT id FROM sys_resource
  WHERE tenant_id = 1 AND client_code = 'pc' AND resource_type = 2 AND path = '/app-center/object/:objectCode/designer'
  ORDER BY id LIMIT 1
);

SET @app_stats_route_id := (
  SELECT id FROM sys_resource
  WHERE tenant_id = 1 AND client_code = 'pc' AND resource_type = 2 AND path = '/app-center/stats'
  ORDER BY id LIMIT 1
);

SET @app_trigger_route_id := (
  SELECT id FROM sys_resource
  WHERE tenant_id = 1 AND client_code = 'pc' AND resource_type = 2 AND path = '/app-center/trigger'
  ORDER BY id LIMIT 1
);

SET @runtime_route_id := (
  SELECT id FROM sys_resource
  WHERE tenant_id = 1 AND client_code = 'pc' AND resource_type = 2 AND path = '/ai/crud-page/:configKey'
  ORDER BY id LIMIT 1
);

SET @app_entry_route_id := (
  SELECT id FROM sys_resource
  WHERE tenant_id = 1 AND client_code = 'pc' AND resource_type = 2 AND path = '/app-center/app/:appId'
  ORDER BY id LIMIT 1
);

SET @message_template_route_id := (
  SELECT id FROM sys_resource
  WHERE tenant_id = 1 AND client_code = 'pc' AND resource_type = 2 AND path = '/message/template'
  ORDER BY id LIMIT 1
);

INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT DISTINCT source.tenant_id, source.role_id, target.id, NOW()
FROM (
  SELECT DISTINCT rr.tenant_id, rr.role_id
  FROM sys_role_resource rr
  INNER JOIN sys_resource src ON src.id = rr.resource_id
  WHERE src.tenant_id = 1
    AND (
      src.id IN (@app_root_id, @app_overview_id, @app_engine_id)
      OR src.path LIKE '/app-center%'
      OR src.path LIKE '/ai/crud-page/%'
      OR src.perms IN (
        'ai:businessApp:list',
        'ai:businessSuite:list',
        'ai:businessObject:list',
        'ai:businessEngine:list',
        'ai:businessApp:open',
        'ai:businessBinding:config'
      )
      OR src.perms LIKE 'ai:business:suite-menu:%'
    )
) source
INNER JOIN sys_resource target ON target.tenant_id = 1
WHERE (
    target.id IN (
      @app_root_id,
      @app_overview_id,
      @app_engine_id,
      @app_suite_detail_id,
      @app_object_detail_id,
      @app_object_designer_id,
      @app_stats_route_id,
      @app_trigger_route_id,
      @runtime_route_id,
      @app_entry_route_id,
      @message_root_id,
      @message_template_route_id
    )
    OR (
      target.resource_type = 3
      AND target.perms IN (
        'ai:businessApp:list',
        'ai:businessSuite:list',
        'ai:businessObject:list',
        'ai:businessObject:relation',
        'ai:businessBinding:config',
        'ai:businessApp:open',
        'ai:businessStats:view',
        'ai:businessTrigger:list',
        'ai:businessReadiness:view',
        'ai:businessRelation:runtime',
        'ai:businessAcceptance:view',
        'ai:businessEngine:runtime'
      )
    )
  )
  AND target.id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_resource exists_rr
    WHERE exists_rr.tenant_id = source.tenant_id
      AND exists_rr.role_id = source.role_id
      AND exists_rr.resource_id = target.id
  );

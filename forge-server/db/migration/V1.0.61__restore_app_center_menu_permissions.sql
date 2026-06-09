-- Restore application center menu visibility and role grants after PC menu normalization.
-- V1.0.59 moved application resources under a new parent but only granted the parent
-- to roles that already owned child resources. Admin roles without those child grants
-- could no longer see /app-center.

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

SET @app_mobile_id := (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND client_code = 'pc'
    AND resource_type = 2
    AND path = '/app-center/mobile'
  ORDER BY id
  LIMIT 1
);

SET @app_integration_id := (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND client_code = 'pc'
    AND resource_type = 2
    AND path = '/app-center/integration'
  ORDER BY id
  LIMIT 1
);

SET @app_suite_detail_id := (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND client_code = 'pc'
    AND resource_type = 2
    AND path = '/app-center/suite/:suiteCode'
  ORDER BY id
  LIMIT 1
);

SET @app_object_detail_id := (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND client_code = 'pc'
    AND resource_type = 2
    AND path = '/app-center/object/:objectCode'
  ORDER BY id
  LIMIT 1
);

SET @app_object_designer_id := (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND client_code = 'pc'
    AND resource_type = 2
    AND path = '/app-center/object/:objectCode/designer'
  ORDER BY id
  LIMIT 1
);

SET @admin_role_id := (
  SELECT id
  FROM sys_role
  WHERE tenant_id = 1
    AND role_key = 'admin'
  ORDER BY id
  LIMIT 1
);

UPDATE sys_resource
SET parent_id = 0,
    resource_type = 1,
    resource_name = '应用中心',
    path = '/app-center',
    component = NULL,
    sort = 2,
    visible = 1,
    menu_status = 1,
    update_time = NOW()
WHERE id = @app_center_root_id;

UPDATE sys_resource
SET parent_id = @app_center_root_id,
    resource_type = 2,
    resource_name = '应用总览',
    path = '/app-center',
    component = 'app-center/index',
    sort = 1,
    visible = 1,
    menu_status = 1,
    update_time = NOW()
WHERE @app_center_root_id IS NOT NULL
  AND id = @app_overview_id;

UPDATE sys_resource
SET parent_id = @app_center_root_id,
    sort = CASE id
             WHEN @app_datasource_id THEN 2
             WHEN @app_engine_id THEN 3
             WHEN @app_mobile_id THEN 4
             WHEN @app_integration_id THEN 5
             ELSE sort
           END,
    visible = 1,
    menu_status = 1,
    update_time = NOW()
WHERE @app_center_root_id IS NOT NULL
  AND id IN (@app_datasource_id, @app_engine_id, @app_mobile_id, @app_integration_id);

UPDATE sys_resource
SET parent_id = @app_overview_id,
    visible = 0,
    menu_status = 1,
    update_time = NOW()
WHERE @app_overview_id IS NOT NULL
  AND id IN (@app_suite_detail_id, @app_object_detail_id, @app_object_designer_id);

-- Super administrator should always own the application center entry and its runtime routes.
INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT 1, @admin_role_id, target.id, NOW()
FROM sys_resource target
WHERE @admin_role_id IS NOT NULL
  AND target.id IN (
    @app_center_root_id,
    @app_overview_id,
    @app_datasource_id,
    @app_engine_id,
    @app_mobile_id,
    @app_integration_id,
    @app_suite_detail_id,
    @app_object_detail_id,
    @app_object_designer_id
  )
  AND target.id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_resource exists_rr
    WHERE exists_rr.tenant_id = 1
      AND exists_rr.role_id = @admin_role_id
      AND exists_rr.resource_id = target.id
  );

-- Roles that already own any application-center resource should also own the parent and overview page.
INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT DISTINCT source.tenant_id, source.role_id, target.id, NOW()
FROM (
  SELECT rr.tenant_id, rr.role_id
  FROM sys_role_resource rr
  INNER JOIN sys_resource owned ON owned.id = rr.resource_id
  WHERE owned.tenant_id = 1
    AND owned.client_code = 'pc'
    AND (
      owned.id IN (
        @app_center_root_id,
        @app_overview_id,
        @app_datasource_id,
        @app_engine_id,
        @app_mobile_id,
        @app_integration_id,
        @app_suite_detail_id,
        @app_object_detail_id,
        @app_object_designer_id
      )
      OR owned.path LIKE '/app-center%'
      OR owned.perms LIKE 'ai:business%'
    )
) source
INNER JOIN sys_resource target ON target.id IN (@app_center_root_id, @app_overview_id)
WHERE target.id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_resource exists_rr
    WHERE exists_rr.tenant_id = source.tenant_id
      AND exists_rr.role_id = source.role_id
      AND exists_rr.resource_id = target.id
  );

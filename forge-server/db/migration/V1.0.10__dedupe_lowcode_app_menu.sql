-- 合并低代码应用菜单入口。
-- 将阶段 1 的业务领域入口作为统一“低代码应用”菜单，删除旧版重复入口和临时领域工作台子菜单。

SET @ai_root_id := (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND resource_type = 1
    AND path = '/ai'
  LIMIT 1
);

SET @lowcode_app_menu_id := (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND resource_type = 2
    AND perms = 'ai:lowcode:domain:list'
  LIMIT 1
);

SET @legacy_lowcode_menu_id := (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND resource_type = 2
    AND perms = 'ai:lowcode:list'
  LIMIT 1
);

SET @workspace_menu_id := (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND resource_type = 2
    AND perms = 'ai:lowcode:domain:workspace'
  LIMIT 1
);

UPDATE sys_resource
SET resource_name = '低代码应用',
    parent_id = COALESCE(@ai_root_id, parent_id),
    sort = 12,
    path = '/ai/lowcode-apps',
    component = 'ai/lowcode-apps',
    menu_status = 1,
    visible = 1,
    icon = 'ionicons5:ConstructOutline',
    redirect = NULL,
    remark = '低代码应用与业务领域管理',
    update_by = 1,
    update_time = NOW()
WHERE @lowcode_app_menu_id IS NOT NULL
  AND id = @lowcode_app_menu_id;

UPDATE sys_resource
SET parent_id = @lowcode_app_menu_id,
    update_by = 1,
    update_time = NOW()
WHERE @lowcode_app_menu_id IS NOT NULL
  AND tenant_id = 1
  AND resource_type = 3
  AND perms IN ('ai:lowcode:publish', 'ai:lowcode:rollback', 'ai:lowcode:deploy-ddl');

INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT rr.tenant_id, rr.role_id, @lowcode_app_menu_id, NOW()
FROM sys_role_resource rr
WHERE @lowcode_app_menu_id IS NOT NULL
  AND @legacy_lowcode_menu_id IS NOT NULL
  AND rr.resource_id = @legacy_lowcode_menu_id
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_resource exists_rr
    WHERE exists_rr.tenant_id = rr.tenant_id
      AND exists_rr.role_id = rr.role_id
      AND exists_rr.resource_id = @lowcode_app_menu_id
  );

INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT rr.tenant_id, rr.role_id, @lowcode_app_menu_id, NOW()
FROM sys_role_resource rr
WHERE @lowcode_app_menu_id IS NOT NULL
  AND @workspace_menu_id IS NOT NULL
  AND rr.resource_id = @workspace_menu_id
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_resource exists_rr
    WHERE exists_rr.tenant_id = rr.tenant_id
      AND exists_rr.role_id = rr.role_id
      AND exists_rr.resource_id = @lowcode_app_menu_id
  );

DELETE FROM sys_role_resource
WHERE (@legacy_lowcode_menu_id IS NOT NULL AND resource_id = @legacy_lowcode_menu_id)
   OR (@workspace_menu_id IS NOT NULL AND resource_id = @workspace_menu_id);

DELETE FROM sys_resource
WHERE (@legacy_lowcode_menu_id IS NOT NULL AND id = @legacy_lowcode_menu_id)
   OR (@workspace_menu_id IS NOT NULL AND id = @workspace_menu_id);

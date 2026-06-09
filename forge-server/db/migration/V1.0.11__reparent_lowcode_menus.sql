-- 调整低代码菜单层级：统一挂到原“AI代码生成”目录，并重命名为“AI低代码”。
-- 最终可见结构：
-- AI低代码
--   - 模型设计
--   - 应用开发

SET @lowcode_root_id := (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND resource_type = 1
    AND (path = '/generator' OR resource_name = 'AI代码生成' OR resource_name = 'AI低代码')
  ORDER BY CASE
             WHEN path = '/generator' THEN 0
             WHEN resource_name = 'AI低代码' THEN 1
             ELSE 2
           END
  LIMIT 1
);

SET @app_menu_id := (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND resource_type = 2
    AND perms = 'ai:lowcode:domain:list'
  LIMIT 1
);

SET @model_menu_id := (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND resource_type = 2
    AND perms = 'ai:lowcode:model:list'
  LIMIT 1
);

SET @builder_menu_id := (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND resource_type = 2
    AND perms = 'ai:lowcode:edit'
  LIMIT 1
);

UPDATE sys_resource
SET resource_name = 'AI低代码',
    menu_status = 1,
    visible = 1,
    always_show = 1,
    remark = 'AI低代码目录',
    update_by = 1,
    update_time = NOW()
WHERE @lowcode_root_id IS NOT NULL
  AND id = @lowcode_root_id;

UPDATE sys_resource
SET resource_name = '模型设计',
    parent_id = @lowcode_root_id,
    sort = 1,
    path = '/ai/lowcode-models',
    component = 'ai/lowcode-models',
    menu_status = 1,
    visible = 1,
    icon = 'ionicons5:GitBranchOutline',
    redirect = NULL,
    remark = '低代码数据模型设计',
    update_by = 1,
    update_time = NOW()
WHERE @lowcode_root_id IS NOT NULL
  AND @model_menu_id IS NOT NULL
  AND id = @model_menu_id;

UPDATE sys_resource
SET resource_name = '应用开发',
    parent_id = @lowcode_root_id,
    sort = 2,
    path = '/ai/lowcode-apps',
    component = 'ai/lowcode-apps',
    menu_status = 1,
    visible = 1,
    icon = 'ionicons5:ConstructOutline',
    redirect = NULL,
    remark = '低代码应用开发与业务领域管理',
    update_by = 1,
    update_time = NOW()
WHERE @lowcode_root_id IS NOT NULL
  AND @app_menu_id IS NOT NULL
  AND id = @app_menu_id;

UPDATE sys_resource
SET resource_name = '应用搭建器',
    parent_id = @app_menu_id,
    sort = 99,
    path = '/ai/lowcode-builder',
    component = 'ai/lowcode-builder',
    menu_status = 0,
    visible = 0,
    redirect = NULL,
    remark = '低代码应用搭建器隐藏入口',
    update_by = 1,
    update_time = NOW()
WHERE @app_menu_id IS NOT NULL
  AND @builder_menu_id IS NOT NULL
  AND id = @builder_menu_id;

UPDATE sys_resource
SET parent_id = @app_menu_id,
    update_by = 1,
    update_time = NOW()
WHERE @app_menu_id IS NOT NULL
  AND tenant_id = 1
  AND resource_type = 3
  AND perms IN (
    'ai:lowcode:domain:add',
    'ai:lowcode:domain:edit',
    'ai:lowcode:domain:status',
    'ai:lowcode:domain:move',
    'ai:lowcode:publish',
    'ai:lowcode:rollback',
    'ai:lowcode:deploy-ddl'
  );

UPDATE sys_resource
SET parent_id = @model_menu_id,
    update_by = 1,
    update_time = NOW()
WHERE @model_menu_id IS NOT NULL
  AND tenant_id = 1
  AND resource_type = 3
  AND perms IN (
    'ai:lowcode:model:add',
    'ai:lowcode:model:edit',
    'ai:lowcode:model:status'
  );

INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT DISTINCT rr.tenant_id, rr.role_id, @lowcode_root_id, NOW()
FROM sys_role_resource rr
JOIN sys_resource r ON r.id = rr.resource_id
WHERE @lowcode_root_id IS NOT NULL
  AND rr.tenant_id = 1
  AND r.tenant_id = 1
  AND r.perms LIKE 'ai:lowcode:%'
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_resource exists_rr
    WHERE exists_rr.tenant_id = rr.tenant_id
      AND exists_rr.role_id = rr.role_id
      AND exists_rr.resource_id = @lowcode_root_id
  );

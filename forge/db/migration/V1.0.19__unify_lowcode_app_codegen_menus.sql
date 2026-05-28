-- 收敛低代码应用、模型设计与代码生成入口。
-- 保留应用开发、模型设计、数据源管理；隐藏旧表模型、纯 JSON 配置、AI 表单生成和模板管理菜单。

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

UPDATE sys_resource
SET resource_name = '应用开发',
    path = '/ai/lowcode-apps',
    component = 'ai/lowcode-apps',
    menu_status = 1,
    visible = 1,
    remark = '低代码应用设计、AI创建、发布运行和代码输出主入口',
    update_by = 1,
    update_time = NOW()
WHERE @app_menu_id IS NOT NULL
  AND id = @app_menu_id;

UPDATE sys_resource
SET resource_name = '模型设计',
    path = '/ai/lowcode-models',
    component = 'ai/lowcode-models',
    menu_status = 1,
    visible = 1,
    remark = '低代码数据模型资产设计入口',
    update_by = 1,
    update_time = NOW()
WHERE @model_menu_id IS NOT NULL
  AND id = @model_menu_id;

UPDATE sys_resource
SET menu_status = 0,
    visible = 0,
    remark = CONCAT(COALESCE(remark, ''), '；统一低代码代码生成后隐藏旧入口'),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND resource_type = 2
  AND (
    path IN ('/generator/table', '/generator/template', '/ai/crud-config', '/ai/crud-generator', '/ai/page-template')
    OR component IN ('generator/table', 'generator/template', 'ai/crud-config', 'ai/crud-generator', 'ai/page-template')
    OR perms IN ('ai:crud-config:list', 'ai:crud-gen:list', 'ai:crud-generator:use', 'ai:page-template:list')
  );

UPDATE sys_resource
SET menu_status = 1,
    visible = 1,
    remark = COALESCE(remark, '代码生成数据源管理'),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND resource_type = 2
  AND (path = '/generator/datasource' OR component = 'generator/datasource');

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external,
                          sso_enabled, sso_target_client, open_target, is_public, menu_status, visible, perms, icon,
                          api_method, api_url, keep_alive, always_show, redirect, remark, create_by, create_time,
                          update_by, update_time, create_dept, client_code)
SELECT 1, 'AI创建应用', @app_menu_id, 3, 11, NULL, NULL, 0,
       0, NULL, '_self', 0, 1, 1, 'ai:lowcode:ai-generate', NULL,
       NULL, NULL, 0, 0, NULL, 'AI生成低代码模型和应用草稿按钮权限', 1, NOW(), 1, NOW(), 1, 'pc'
WHERE @app_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM sys_resource
    WHERE tenant_id = 1
      AND resource_type = 3
      AND perms = 'ai:lowcode:ai-generate'
  );

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external,
                          sso_enabled, sso_target_client, open_target, is_public, menu_status, visible, perms, icon,
                          api_method, api_url, keep_alive, always_show, redirect, remark, create_by, create_time,
                          update_by, update_time, create_dept, client_code)
SELECT 1, '代码预览', @app_menu_id, 3, 12, NULL, NULL, 0,
       0, NULL, '_self', 0, 1, 1, 'ai:lowcode:code-preview', NULL,
       NULL, NULL, 0, 0, NULL, '低代码应用代码预览按钮权限', 1, NOW(), 1, NOW(), 1, 'pc'
WHERE @app_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM sys_resource
    WHERE tenant_id = 1
      AND resource_type = 3
      AND perms = 'ai:lowcode:code-preview'
  );

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external,
                          sso_enabled, sso_target_client, open_target, is_public, menu_status, visible, perms, icon,
                          api_method, api_url, keep_alive, always_show, redirect, remark, create_by, create_time,
                          update_by, update_time, create_dept, client_code)
SELECT 1, '下载代码', @app_menu_id, 3, 13, NULL, NULL, 0,
       0, NULL, '_self', 0, 1, 1, 'ai:lowcode:code-download', NULL,
       NULL, NULL, 0, 0, NULL, '低代码应用代码下载按钮权限', 1, NOW(), 1, NOW(), 1, 'pc'
WHERE @app_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM sys_resource
    WHERE tenant_id = 1
      AND resource_type = 3
      AND perms = 'ai:lowcode:code-download'
  );

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external,
                          sso_enabled, sso_target_client, open_target, is_public, menu_status, visible, perms, icon,
                          api_method, api_url, keep_alive, always_show, redirect, remark, create_by, create_time,
                          update_by, update_time, create_dept, client_code)
SELECT 1, '导入数据源表', @model_menu_id, 3, 11, NULL, NULL, 0,
       0, NULL, '_self', 0, 1, 1, 'ai:lowcode:model:import-table', NULL,
       NULL, NULL, 0, 0, NULL, '从数据源表导入低代码模型按钮权限', 1, NOW(), 1, NOW(), 1, 'pc'
WHERE @model_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM sys_resource
    WHERE tenant_id = 1
      AND resource_type = 3
      AND perms = 'ai:lowcode:model:import-table'
  );

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external,
                          sso_enabled, sso_target_client, open_target, is_public, menu_status, visible, perms, icon,
                          api_method, api_url, keep_alive, always_show, redirect, remark, create_by, create_time,
                          update_by, update_time, create_dept, client_code)
SELECT 1, 'AI生成模型', @model_menu_id, 3, 12, NULL, NULL, 0,
       0, NULL, '_self', 0, 1, 1, 'ai:lowcode:model:ai-generate', NULL,
       NULL, NULL, 0, 0, NULL, 'AI生成低代码模型草稿按钮权限', 1, NOW(), 1, NOW(), 1, 'pc'
WHERE @model_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM sys_resource
    WHERE tenant_id = 1
      AND resource_type = 3
      AND perms = 'ai:lowcode:model:ai-generate'
  );

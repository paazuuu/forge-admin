-- 应用管理收敛代码下载模式、权限和旧配置入口。

INSERT INTO sys_dict_type (tenant_id, dict_name, dict_type, dict_status, remark, create_by, create_time, update_by, update_time, create_dept)
SELECT 1, '访问入口使用模式', 'ai_business_app_mode', 1, '访问入口在线运行或下载代码模式', 1, NOW(), 1, NOW(), 1
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_dict_type
  WHERE tenant_id = 1
    AND dict_type = 'ai_business_app_mode'
);

INSERT INTO sys_dict_data (tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default,
                           dict_status, remark, create_by, create_time, update_by, update_time, create_dept)
SELECT seed.tenant_id, seed.dict_sort, seed.dict_label, seed.dict_value, seed.dict_type, NULL, seed.list_class,
       seed.is_default, 1, seed.remark, 1, NOW(), 1, NOW(), 1
FROM (
  SELECT 1 tenant_id, 1 dict_sort, '在线运行' dict_label, 'DYNAMIC_RENDER' dict_value,
         'ai_business_app_mode' dict_type, 'success' list_class, 'Y' is_default,
         '在线搭建并由平台动态运行' remark
  UNION ALL
  SELECT 1, 2, '下载代码', 'CODE_DOWNLOAD',
         'ai_business_app_mode', 'primary', 'N',
         '下载完整功能代码后导入本地工程二次开发'
) seed
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_dict_data d
  WHERE d.tenant_id = seed.tenant_id
    AND d.dict_type = seed.dict_type
    AND d.dict_value = seed.dict_value
);

UPDATE sys_dict_type
SET dict_name = CASE dict_type
                  WHEN 'ai_business_suite' THEN '业务域'
                  WHEN 'ai_business_object_type' THEN '业务单元类型'
                  WHEN 'ai_business_relation_type' THEN '业务单元关系类型'
                  WHEN 'ai_business_app_entry_mode' THEN '访问入口打开方式'
                  ELSE dict_name
                END,
    remark = CASE dict_type
               WHEN 'ai_business_suite' THEN '企业应用装配平台业务域'
               WHEN 'ai_business_object_type' THEN '业务单元/实体类型'
               WHEN 'ai_business_relation_type' THEN '业务单元之间的引用、明细和关联列表关系'
               WHEN 'ai_business_app_entry_mode' THEN '访问入口打开方式'
               ELSE remark
             END,
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND dict_type IN (
    'ai_business_suite',
    'ai_business_object_type',
    'ai_business_relation_type',
    'ai_business_app_entry_mode'
  );

UPDATE sys_dict_data
SET dict_label = CASE
                   WHEN dict_type = 'ai_business_app_entry_mode' AND dict_value = 'RUNTIME' THEN '业务页面'
                   WHEN dict_type = 'ai_business_app_entry_mode' AND dict_value = 'H5' THEN 'H5 入口'
                   ELSE dict_label
                 END,
    remark = CASE
               WHEN dict_type = 'ai_business_app_entry_mode' AND dict_value = 'RUNTIME' THEN '打开平台托管的业务页面'
               WHEN dict_type = 'ai_business_app_entry_mode' AND dict_value = 'API' THEN '登记接口或集成资源'
               ELSE remark
             END,
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND dict_type = 'ai_business_app_entry_mode'
  AND dict_value IN ('RUNTIME', 'H5', 'API');

SET @app_center_menu_id := (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND resource_type = 2
    AND perms = 'ai:businessApp:list'
  ORDER BY id
  LIMIT 1
);

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external,
                          sso_enabled, sso_target_client, open_target, is_public, menu_status, visible, perms, icon,
                          api_method, api_url, keep_alive, always_show, redirect, remark, create_by, create_time,
                          update_by, update_time, create_dept, client_code)
SELECT seed.tenant_id, seed.resource_name, @app_center_menu_id, 3, seed.sort, NULL, NULL, 0,
       0, NULL, '_self', 0, 1, 1, seed.perms, NULL,
       NULL, NULL, 0, 0, NULL, seed.remark, 1, NOW(), 1, NOW(), 1, 'pc'
FROM (
  SELECT 1 tenant_id, '设置功能代码' resource_name, 46 sort, 'ai:businessApp:code' perms, '维护访问入口代码包设置' remark
  UNION ALL SELECT 1, '预览功能代码', 47, 'ai:businessApp:codePreview', '预览访问入口完整功能代码'
  UNION ALL SELECT 1, '下载功能代码', 48, 'ai:businessApp:codeDownload', '下载访问入口完整功能代码'
) seed
WHERE @app_center_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM sys_resource r
    WHERE r.tenant_id = seed.tenant_id
      AND r.resource_type = 3
      AND r.perms = seed.perms
  );

UPDATE sys_resource
SET resource_name = CASE perms
                      WHEN 'ai:businessApp:list' THEN '应用总览'
                      WHEN 'ai:businessSuite:list' THEN '查看业务域'
                      WHEN 'ai:businessSuite:edit' THEN '维护业务域'
                      WHEN 'ai:businessObject:list' THEN '查看业务单元'
                      WHEN 'ai:businessObject:add' THEN '新增业务单元'
                      WHEN 'ai:businessObject:edit' THEN '编辑业务单元'
                      WHEN 'ai:businessObject:delete' THEN '删除业务单元'
                      WHEN 'ai:businessObject:relation' THEN '配置业务单元关系'
                      WHEN 'ai:businessApp:add' THEN '新增访问入口'
                      WHEN 'ai:businessApp:edit' THEN '编辑访问入口'
                      WHEN 'ai:businessApp:delete' THEN '删除访问入口'
                      WHEN 'ai:businessApp:status' THEN '启停访问入口'
                      WHEN 'ai:businessApp:open' THEN '打开访问入口'
                      ELSE resource_name
                    END,
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND perms IN (
    'ai:businessApp:list',
    'ai:businessSuite:list',
    'ai:businessSuite:edit',
    'ai:businessObject:list',
    'ai:businessObject:add',
    'ai:businessObject:edit',
    'ai:businessObject:delete',
    'ai:businessObject:relation',
    'ai:businessApp:add',
    'ai:businessApp:edit',
    'ai:businessApp:delete',
    'ai:businessApp:status',
    'ai:businessApp:open'
  );

UPDATE sys_resource
SET menu_status = 0,
    visible = 0,
    update_by = 1,
    update_time = NOW()
WHERE resource_type = 2
  AND tenant_id IN (0, 1)
  AND (
    path IN ('/ai/crud-config', '/ai/crud-generator', '/ai/lowcode-builder', '/ai/lowcode-models', '/ai/page-template', '/generator/table', '/generator/template')
    OR component IN ('ai/crud-config', 'ai/crud-generator', 'ai/lowcode-builder', 'ai/lowcode-models', 'ai/page-template', 'generator/table', 'generator/template')
    OR perms IN ('ai:crud-config:list', 'ai:crud-gen:list', 'ai:crud-generator:use', 'ai:page-template:list')
  );

INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT DISTINCT rr.tenant_id, rr.role_id, target.id, NOW()
FROM sys_role_resource rr
JOIN sys_resource src ON src.id = rr.resource_id
JOIN sys_resource target ON target.tenant_id = 1
WHERE src.tenant_id = 1
  AND src.perms IN ('ai:businessApp:list', 'ai:businessApp:edit', 'ai:businessApp:open')
  AND target.perms IN ('ai:businessApp:code', 'ai:businessApp:codePreview', 'ai:businessApp:codeDownload')
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_resource exists_rr
    WHERE exists_rr.tenant_id = rr.tenant_id
      AND exists_rr.role_id = rr.role_id
      AND exists_rr.resource_id = target.id
  );

-- 通用数量台账只读查询权限。写入动作权限已由 V1.0.88 提供。

SET @app_center_id := (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND client_code = 'pc'
    AND resource_type = 2
    AND path = '/app-center'
  ORDER BY id
  LIMIT 1
);

SET @quantity_parent_id := COALESCE(@app_center_id, 0);

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external,
                          sso_enabled, sso_target_client, open_target, is_public, menu_status, visible, perms, icon,
                          api_method, api_url, keep_alive, always_show, redirect, remark, create_by, create_time,
                          update_by, update_time, create_dept, client_code)
SELECT 1, '查看数量台账', @quantity_parent_id, 3, 213, NULL, NULL, 0,
       0, NULL, '_self', 0, 1, 1, 'ai:businessQuantity:view', NULL,
       NULL, NULL, 0, 0, NULL, '通用数量台账只读查询权限', 1, NOW(), 1, NOW(), 1, 'pc'
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_resource r
  WHERE r.tenant_id = 1
    AND r.client_code = 'pc'
    AND r.perms = 'ai:businessQuantity:view'
);

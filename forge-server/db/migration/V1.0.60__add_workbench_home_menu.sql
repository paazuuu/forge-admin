-- Add the PC home menu under workbench. This is separated from V1.0.59 because
-- V1.0.59 has already been applied in existing environments.

INSERT INTO sys_resource (
    tenant_id, resource_name, parent_id, resource_type, sort, path, component,
    is_external, sso_enabled, open_target, is_public, menu_status, visible,
    perms, icon, keep_alive, always_show, client_code, create_by, create_time,
    update_by, update_time
)
SELECT
    1, '首页', 43, 2, 0, '/home', 'home/index',
    0, 0, '_self', 0, 1, 1,
    NULL, 'ionicons5:HomeOutline', 0, 0, 'pc', NULL, NOW(),
    NULL, NOW()
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_resource
    WHERE tenant_id = 1 AND client_code = 'pc' AND path = '/home'
);

UPDATE sys_resource
SET resource_name = '首页',
    parent_id = 43,
    resource_type = 2,
    sort = 0,
    component = 'home/index',
    icon = 'ionicons5:HomeOutline',
    visible = 1,
    menu_status = 1,
    update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND path = '/home';

INSERT IGNORE INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT DISTINCT rr.tenant_id, rr.role_id, 43, NOW()
FROM sys_role_resource rr
INNER JOIN sys_resource owned ON owned.id = rr.resource_id
WHERE owned.client_code = 'pc';

INSERT IGNORE INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT DISTINCT rr.tenant_id, rr.role_id, home.id, NOW()
FROM sys_role_resource rr
INNER JOIN sys_resource owned ON owned.id = rr.resource_id
INNER JOIN sys_resource home ON home.tenant_id = 1
    AND home.client_code = 'pc'
    AND home.path = '/home'
WHERE owned.client_code = 'pc';

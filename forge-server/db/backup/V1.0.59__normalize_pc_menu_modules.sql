-- Normalize PC-side sys_resource modules into clearer product boundaries.
-- Keep existing resource ids so role permissions and historical links remain stable.

-- Top-level modules.
UPDATE sys_resource
SET resource_name = '工作台',
    parent_id = 0,
    sort = 1,
    path = '/workbench',
    icon = 'ionicons5:HomeOutline',
    visible = 1,
    menu_status = 1,
    update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 43;

UPDATE sys_resource
SET resource_name = '应用中心',
    parent_id = 0,
    sort = 2,
    path = '/app-center',
    icon = 'ionicons5:GridOutline',
    visible = 1,
    menu_status = 1,
    update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 9194;

UPDATE sys_resource
SET resource_name = '流程中心',
    parent_id = 0,
    sort = 3,
    path = '/flow',
    icon = 'ionicons5:GitNetworkOutline',
    visible = 1,
    menu_status = 1,
    update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 79;

UPDATE sys_resource
SET resource_name = '数据与报表',
    parent_id = 0,
    sort = 4,
    path = '/data-report',
    icon = 'ionicons5:StatsChartOutline',
    visible = 1,
    menu_status = 1,
    update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 9100;

UPDATE sys_resource
SET resource_name = 'AI能力',
    parent_id = 0,
    sort = 5,
    path = '/ai',
    icon = 'ionicons5:SparklesOutline',
    visible = 1,
    menu_status = 1,
    update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 9067;

UPDATE sys_resource
SET resource_name = '平台管理',
    parent_id = 0,
    sort = 6,
    path = '/platform',
    icon = 'ionicons5:SettingsOutline',
    visible = 1,
    menu_status = 1,
    update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 1;

-- Archive old top-level containers. Their useful children are moved below.
UPDATE sys_resource
SET resource_name = '旧低代码配置',
    sort = 900,
    visible = 0,
    menu_status = 0,
    update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 39;

UPDATE sys_resource
SET resource_name = '个人中心(已归档)',
    sort = 910,
    visible = 0,
    menu_status = 0,
    update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 9045;

UPDATE sys_resource
SET resource_name = '报表管理(已归档)',
    sort = 920,
    visible = 0,
    menu_status = 0,
    update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 9096;

UPDATE sys_resource
SET resource_name = '开发者工具(已归档)',
    sort = 930,
    visible = 0,
    menu_status = 0,
    update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 9218;

-- Workbench: personal process work and office notices.
UPDATE sys_resource
SET parent_id = 43, sort = 1, visible = 1, menu_status = 1, update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 80;

UPDATE sys_resource
SET parent_id = 43, sort = 2, visible = 1, menu_status = 1, update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 81;

UPDATE sys_resource
SET parent_id = 43, sort = 3, visible = 1, menu_status = 1, update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 82;

UPDATE sys_resource
SET parent_id = 43, sort = 4, visible = 1, menu_status = 1, update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 83;

UPDATE sys_resource
SET parent_id = 43, sort = 10, visible = 1, menu_status = 1, update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 30;

UPDATE sys_resource
SET parent_id = 43, sort = 90, visible = 1, menu_status = 1, update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 900;

-- Application center: keep one visible application management entry and move low-code datasource here.
UPDATE sys_resource
SET resource_name = '应用总览',
    parent_id = 9194,
    sort = 1,
    visible = 1,
    menu_status = 1,
    update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 9217;

UPDATE sys_resource
SET resource_name = '应用数据源',
    parent_id = 9194,
    sort = 2,
    visible = 1,
    menu_status = 1,
    update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 40;

UPDATE sys_resource
SET parent_id = 9194, sort = 3, visible = 1, menu_status = 1, update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 9197;

UPDATE sys_resource
SET parent_id = 9194, sort = 4, visible = 0, menu_status = 0, update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 9198;

UPDATE sys_resource
SET parent_id = 9194, sort = 5, visible = 0, menu_status = 0, update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 9199;

-- Application detail and designer pages are hidden routes under application overview.
UPDATE sys_resource
SET parent_id = 9217, sort = 1, visible = 0, menu_status = 0, update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 9195;

UPDATE sys_resource
SET parent_id = 9217, sort = 2, visible = 0, menu_status = 0, update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 9196;

UPDATE sys_resource
SET parent_id = 9217, sort = 6, visible = 0, menu_status = 0, update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 9221;

-- Old JSON/legacy low-code pages are no longer shown in the menu.
UPDATE sys_resource
SET visible = 0,
    menu_status = 0,
    sort = 900,
    update_time = NOW()
WHERE tenant_id = 1
  AND client_code = 'pc'
  AND id IN (
      41, 9080, 9081, 9085, 9088,
      9174, 9180, 9169,
      9184, 9185, 9186, 9187, 9193,
      9220, 9229, 9230, 9244, 9270
  );

UPDATE sys_resource
SET visible = 0,
    menu_status = 0,
    sort = 910,
    update_time = NOW()
WHERE tenant_id = 1
  AND client_code = 'pc'
  AND id IN (9239, 9243);

-- Flow center: keep modeling/configuration here; personal tasks moved to workbench.
UPDATE sys_resource
SET parent_id = 79, sort = 1, visible = 1, menu_status = 1, update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 85;

UPDATE sys_resource
SET parent_id = 79, sort = 2, visible = 1, menu_status = 1, update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 86;

UPDATE sys_resource
SET parent_id = 79, sort = 3, visible = 1, menu_status = 1, update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 9094;

UPDATE sys_resource
SET parent_id = 79, sort = 4, visible = 1, menu_status = 1, update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 9046;

UPDATE sys_resource
SET parent_id = 79, sort = 90, visible = 0, menu_status = 1, update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 101;

UPDATE sys_resource
SET parent_id = 79, sort = 91, visible = 0, menu_status = 1, update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 9095;

-- Data and report center.
UPDATE sys_resource
SET parent_id = 9100, sort = 1, visible = 1, menu_status = 1, update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 9101;

UPDATE sys_resource
SET parent_id = 9100, sort = 2, visible = 1, menu_status = 1, update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 9102;

UPDATE sys_resource
SET parent_id = 9100, sort = 3, visible = 1, menu_status = 1, update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 9136;

UPDATE sys_resource
SET parent_id = 9100, sort = 4, visible = 1, menu_status = 1, update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 9147;

UPDATE sys_resource
SET parent_id = 9100, sort = 5, visible = 1, menu_status = 1, update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 9162;

UPDATE sys_resource
SET parent_id = 9100, sort = 10, visible = 1, menu_status = 1, update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 9113;

-- AI capability center.
UPDATE sys_resource
SET parent_id = 9067, sort = 1, visible = 1, menu_status = 1, update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 9068;

UPDATE sys_resource
SET parent_id = 9067, sort = 2, visible = 1, menu_status = 1, update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 9160;

UPDATE sys_resource
SET parent_id = 9067, sort = 3, visible = 1, menu_status = 1, update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 9077;

UPDATE sys_resource
SET parent_id = 9067, sort = 4, visible = 1, menu_status = 1, update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 9167;

UPDATE sys_resource
SET parent_id = 9067, sort = 5, visible = 1, menu_status = 1, update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 9163;

-- Platform management: system identity menus stay directly under platform; configuration/message/ops become groups.
UPDATE sys_resource
SET parent_id = 1, sort = 1, visible = 1, menu_status = 1, update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 2;

UPDATE sys_resource
SET parent_id = 1, sort = 2, visible = 1, menu_status = 1, update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 9;

UPDATE sys_resource
SET parent_id = 1, sort = 3, visible = 1, menu_status = 1, update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 14;

UPDATE sys_resource
SET parent_id = 1, sort = 4, visible = 1, menu_status = 1, update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 17;

UPDATE sys_resource
SET parent_id = 1, sort = 5, visible = 1, menu_status = 1, update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 24;

UPDATE sys_resource
SET parent_id = 1, sort = 6, visible = 1, menu_status = 1, update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 26;

UPDATE sys_resource
SET parent_id = 1, sort = 7, visible = 1, menu_status = 1, update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 9051;

UPDATE sys_resource
SET parent_id = 1, sort = 8, visible = 1, menu_status = 1, update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 31;

UPDATE sys_resource
SET resource_name = '基础配置',
    parent_id = 1,
    sort = 50,
    path = '/platform/config',
    icon = 'ionicons5:OptionsOutline',
    visible = 1,
    menu_status = 1,
    update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 44;

UPDATE sys_resource
SET resource_name = '消息中心',
    parent_id = 1,
    sort = 60,
    path = '/platform/message',
    icon = 'ionicons5:ChatboxEllipsesOutline',
    visible = 1,
    menu_status = 1,
    update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 48;

UPDATE sys_resource
SET resource_name = '运维监控',
    parent_id = 1,
    sort = 70,
    path = '/platform/monitor',
    icon = 'ionicons5:PulseOutline',
    visible = 1,
    menu_status = 1,
    update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 54;

-- Basic configuration group.
UPDATE sys_resource
SET parent_id = 44, sort = 1, visible = 1, menu_status = 1, update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 27;

UPDATE sys_resource
SET parent_id = 44, sort = 2, visible = 0, menu_status = 0, update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 28;

UPDATE sys_resource
SET parent_id = 44, sort = 3, visible = 1, menu_status = 1, update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 29;

UPDATE sys_resource
SET parent_id = 44, sort = 4, visible = 1, menu_status = 1, update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 77;

UPDATE sys_resource
SET parent_id = 44, sort = 5, visible = 1, menu_status = 1, update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 9091;

UPDATE sys_resource
SET parent_id = 44, sort = 6, visible = 1, menu_status = 1, update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 9047;

UPDATE sys_resource
SET parent_id = 44, sort = 20, visible = 1, menu_status = 1, update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 45;

UPDATE sys_resource
SET parent_id = 44, sort = 21, visible = 1, menu_status = 1, update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 46;

UPDATE sys_resource
SET parent_id = 44, sort = 22, visible = 1, menu_status = 1, update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 9097;

UPDATE sys_resource
SET parent_id = 44, sort = 23, visible = 1, menu_status = 1, update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 47;

UPDATE sys_resource
SET parent_id = 44, sort = 24, visible = 1, menu_status = 1, update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 68;

-- Message center group.
UPDATE sys_resource
SET resource_name = '模板配置', parent_id = 48, sort = 1, visible = 1, menu_status = 1, update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 49;

UPDATE sys_resource
SET parent_id = 48, sort = 2, visible = 1, menu_status = 1, update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 50;

UPDATE sys_resource
SET parent_id = 48, sort = 3, visible = 1, menu_status = 1, update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 9048;

UPDATE sys_resource
SET parent_id = 48, sort = 4, visible = 1, menu_status = 1, update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 9050;

UPDATE sys_resource
SET parent_id = 48, sort = 5, visible = 1, menu_status = 1, update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 9049;

-- Operations group.
UPDATE sys_resource
SET parent_id = 54, sort = 1, visible = 1, menu_status = 1, update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 78;

UPDATE sys_resource
SET parent_id = 54, sort = 2, visible = 1, menu_status = 1, update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 58;

UPDATE sys_resource
SET parent_id = 54, sort = 3, visible = 1, menu_status = 1, update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 67;

UPDATE sys_resource
SET parent_id = 54, sort = 4, visible = 1, menu_status = 1, update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 56;

UPDATE sys_resource
SET parent_id = 54, sort = 5, visible = 1, menu_status = 1, update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 57;

UPDATE sys_resource
SET parent_id = 54, sort = 6, visible = 1, menu_status = 1, update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 55;

-- Remove stray test menu from visible navigation.
UPDATE sys_resource
SET visible = 0,
    menu_status = 0,
    update_time = NOW()
WHERE tenant_id = 1 AND client_code = 'pc' AND id = 53;

-- Ensure roles that own child menus also own the new visible parent groups.
INSERT IGNORE INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT DISTINCT rr.tenant_id, rr.role_id, 43, NOW()
FROM sys_role_resource rr
WHERE rr.resource_id IN (30, 80, 81, 82, 83, 900, 901, 902);

INSERT IGNORE INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT DISTINCT rr.tenant_id, rr.role_id, 9194, NOW()
FROM sys_role_resource rr
WHERE rr.resource_id IN (40, 9197, 9198, 9199, 9217, 9239, 9243);

INSERT IGNORE INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT DISTINCT rr.tenant_id, rr.role_id, 79, NOW()
FROM sys_role_resource rr
WHERE rr.resource_id IN (85, 86, 101, 9046, 9094, 9095);

INSERT IGNORE INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT DISTINCT rr.tenant_id, rr.role_id, 9100, NOW()
FROM sys_role_resource rr
WHERE rr.resource_id IN (9101, 9102, 9113, 9136, 9147, 9162);

INSERT IGNORE INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT DISTINCT rr.tenant_id, rr.role_id, 9067, NOW()
FROM sys_role_resource rr
WHERE rr.resource_id IN (9068, 9077, 9160, 9163, 9167);

INSERT IGNORE INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT DISTINCT rr.tenant_id, rr.role_id, 1, NOW()
FROM sys_role_resource rr
WHERE rr.resource_id IN (
    2, 9, 14, 17, 24, 26, 31, 44, 45, 46, 47, 48, 49, 50, 54, 55, 56, 57,
    58, 67, 68, 77, 9047, 9048, 9049, 9050, 9051, 9091, 9097
);

INSERT IGNORE INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT DISTINCT rr.tenant_id, rr.role_id, 44, NOW()
FROM sys_role_resource rr
WHERE rr.resource_id IN (27, 28, 29, 45, 46, 47, 68, 77, 9047, 9091, 9097);

INSERT IGNORE INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT DISTINCT rr.tenant_id, rr.role_id, 48, NOW()
FROM sys_role_resource rr
WHERE rr.resource_id IN (49, 50, 9048, 9049, 9050);

INSERT IGNORE INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT DISTINCT rr.tenant_id, rr.role_id, 54, NOW()
FROM sys_role_resource rr
WHERE rr.resource_id IN (55, 56, 57, 58, 67, 78);

INSERT IGNORE INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT DISTINCT rr.tenant_id, rr.role_id, 900, NOW()
FROM sys_role_resource rr
WHERE rr.resource_id IN (901, 902);

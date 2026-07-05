-- 修正阶段 1 领域工作台菜单指向不存在前端路由的问题。
-- 第一版领域工作台前端尚未单独实现，先复用现有低代码应用入口，避免菜单点击 404。

UPDATE sys_resource
SET path = '/ai/lowcode-apps',
    component = 'ai/lowcode-apps',
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND resource_type = 2
  AND perms = 'ai:lowcode:domain:workspace'
  AND path = '/ai/lowcode-domain-workspace';

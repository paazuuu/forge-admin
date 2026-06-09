-- Correct flow entry IA: entry configuration belongs to flow model, not standalone menus.

DELETE FROM sys_role_resource
WHERE resource_id IN (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND (
      (resource_type = 2 AND (perms IN ('flow:entry:list', 'flow:fillBatch:list') OR path IN ('/flow/entry', '/flow/fill-batch')))
      OR (resource_type = 3 AND perms IN ('flow:fillBatch:edit', 'flow:fillBatch:publish'))
    )
);

DELETE FROM sys_resource
WHERE tenant_id = 1
  AND (
    (resource_type = 2 AND (perms IN ('flow:entry:list', 'flow:fillBatch:list') OR path IN ('/flow/entry', '/flow/fill-batch')))
    OR (resource_type = 3 AND perms IN ('flow:fillBatch:edit', 'flow:fillBatch:publish'))
  );

UPDATE sys_resource
SET resource_name = '维护模型入口配置',
    remark = '在流程模型内新增或编辑入口配置',
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND resource_type = 3
  AND perms = 'flow:entry:edit';

UPDATE sys_resource
SET resource_name = '打开模型入口',
    remark = '按模型入口填报并发起流程',
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND resource_type = 3
  AND perms = 'flow:entry:open';

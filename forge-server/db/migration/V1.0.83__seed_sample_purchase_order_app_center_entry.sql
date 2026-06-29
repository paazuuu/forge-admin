-- Seed the sample purchase order code-business entry into App Center.
-- The menu route already exists in V1.0.81; this script makes it visible as
-- an App Center business unit and app entry.

INSERT INTO ai_business_suite (id, tenant_id, parent_id, suite_code, suite_name, icon, description, status,
                               sort_order, options, create_by, create_time, create_dept, update_by, update_time)
SELECT 1910000000000008301, 1, NULL, 'PURCHASE', '采购', 'ionicons5:CartOutline',
       '采购申请、采购审批和供应商协同测试业务域', 1, 40,
       '{"sample":true,"codeApp":true,"scenario":"sample_purchase_order_flow"}',
       1, NOW(), 1, 1, NOW()
WHERE NOT EXISTS (
  SELECT 1
  FROM ai_business_suite
  WHERE tenant_id = 1
    AND suite_code = 'PURCHASE'
);

SET @purchase_suite_id := (
  SELECT id
  FROM ai_business_suite
  WHERE tenant_id = 1
    AND suite_code = 'PURCHASE'
  LIMIT 1
);

INSERT INTO ai_business_object (id, tenant_id, suite_code, object_code, object_name, object_type, model_id,
                                model_code, display_field, icon, description, status, sort_order, options,
                                design_status, config_key, last_publish_time, last_publish_version,
                                designer_options, create_by, create_time, create_dept, update_by, update_time)
SELECT 1910000000000008302, 1, 'PURCHASE', 'sample_purchase_order', '采购申请', 'TRANSACTION', NULL,
       NULL, 'title', 'ionicons5:CartOutline',
       '代码实现的采购单审批测试业务，业务流程配置维护节点表单和字段权限', 1, 10,
       '{"sample":true,"codeApp":true,"businessType":"sample_purchase_order","flowModelKey":"sample_purchase_order_approval"}',
       'PUBLISHED', NULL, NOW(), 1,
       '{"codeApp":true,"documentManaged":false,"defaultPanel":"flow-app"}',
       1, NOW(), 1, 1, NOW()
WHERE @purchase_suite_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM ai_business_object
    WHERE tenant_id = 1
      AND suite_code = 'PURCHASE'
      AND object_code = 'sample_purchase_order'
  );

SET @sample_purchase_order_object_id := (
  SELECT id
  FROM ai_business_object
  WHERE tenant_id = 1
    AND suite_code = 'PURCHASE'
    AND object_code = 'sample_purchase_order'
  LIMIT 1
);

UPDATE ai_business_binding
SET target_id = @sample_purchase_order_object_id,
    update_by = 1,
    update_time = NOW()
WHERE @sample_purchase_order_object_id IS NOT NULL
  AND tenant_id = 1
  AND target_type = 'OBJECT'
  AND target_code = 'sample_purchase_order'
  AND binding_type = 'FLOW'
  AND binding_key = 'sample_purchase_order_approval'
  AND target_id IS NULL;

INSERT INTO ai_business_app (id, tenant_id, app_code, app_name, app_type, suite_code, object_code, entry_mode,
                             entry_url, config_key, icon, description, status, sort_order, options, create_by,
                             create_time, create_dept, update_by, update_time)
SELECT 1910000000000008303, 1, 'PURCHASE_ORDER_APPROVAL_TEST', '采购单审批测试', 'BUSINESS',
       'PURCHASE', 'sample_purchase_order', 'ROUTE',
       '/business/purchase-order-test', NULL, 'ionicons5:ClipboardOutline',
       '打开采购单审批测试页面，创建采购单并验证流程节点表单权限', 1, 10,
       '{"sample":true,"codeApp":true,"flowConfigUrl":"/app-center/object-designer/sample_purchase_order?panel=flow-app&codeApp=1&name=采购申请"}',
       1, NOW(), 1, 1, NOW()
WHERE @sample_purchase_order_object_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM ai_business_app
    WHERE tenant_id = 1
      AND app_code = 'PURCHASE_ORDER_APPROVAL_TEST'
  );

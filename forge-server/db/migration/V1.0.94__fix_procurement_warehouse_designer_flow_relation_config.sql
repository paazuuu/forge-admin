-- Fix procurement/warehouse designer flow keys, relation visibility and child internal fields.
-- Keep existing migrations immutable; this script patches persisted low-code assets in place.

DROP TEMPORARY TABLE IF EXISTS tmp_pw_flow_fix;
CREATE TEMPORARY TABLE tmp_pw_flow_fix (
  object_code VARCHAR(64) NOT NULL PRIMARY KEY,
  flow_key VARCHAR(128) NOT NULL,
  flow_name VARCHAR(128) NOT NULL,
  binding_name VARCHAR(128) NOT NULL,
  title_template VARCHAR(128) NOT NULL
);

INSERT INTO tmp_pw_flow_fix (object_code, flow_key, flow_name, binding_name, title_template)
VALUES
  ('PW_PURCHASE_ORDER', 'pw_purchase_approval', '采购单审批流程', '采购单审批流程', '采购单审批'),
  ('PW_OUTBOUND_ORDER', 'pw_outbound_approval', '出库单审批流程', '出库单审批流程', '出库单审批'),
  ('PW_TRANSFER_ORDER', 'pw_transfer_approval', '调拨单审批流程', '调拨单审批流程', '调拨单审批');

DROP TEMPORARY TABLE IF EXISTS tmp_pw_existing_flow_binding;
CREATE TEMPORARY TABLE tmp_pw_existing_flow_binding AS
SELECT id, tenant_id, target_type, target_code, binding_type, binding_key
FROM ai_business_binding
WHERE tenant_id = 1
  AND target_type = 'OBJECT'
  AND binding_type = 'FLOW';

UPDATE ai_business_document_config c
JOIN tmp_pw_flow_fix f ON f.object_code = c.object_code
SET c.default_flow_key = f.flow_key,
    c.options = JSON_SET(
      COALESCE(CAST(c.options AS JSON), JSON_OBJECT()),
      '$.flowModelKey', f.flow_key,
      '$.flowModelName', f.flow_name,
      '$.showStartFlowAction', false,
      '$.hideStartFlowAction', true
    ),
    c.update_by = 1,
    c.update_time = NOW()
WHERE c.tenant_id = 1
  AND c.suite_code = 'PROCUREMENT_WAREHOUSE';

UPDATE ai_business_binding b
JOIN tmp_pw_flow_fix f ON f.object_code = b.target_code
LEFT JOIN tmp_pw_existing_flow_binding exists_binding
  ON exists_binding.tenant_id = b.tenant_id
  AND exists_binding.target_type = b.target_type
  AND exists_binding.target_code = b.target_code
  AND exists_binding.binding_type = b.binding_type
  AND exists_binding.binding_key = f.flow_key
  AND exists_binding.id <> b.id
SET b.binding_key = f.flow_key,
    b.binding_name = f.binding_name,
    b.binding_config = JSON_SET(
      COALESCE(CAST(b.binding_config AS JSON), JSON_OBJECT()),
      '$.flowModelKey', f.flow_key,
      '$.flowModelName', f.flow_name,
      '$.titleTemplate', f.title_template,
      '$.startMode', 'ACTION_ONLY',
      '$.options.lowcodeApp', true,
      '$.options.scenario', 'procurement_warehouse'
    ),
    b.description = CONCAT(f.binding_name, '，绑定采购仓储业务流程模型'),
    b.status = 1,
    b.update_by = 1,
    b.update_time = NOW()
WHERE b.tenant_id = 1
  AND b.target_type = 'OBJECT'
  AND b.binding_type = 'FLOW'
  AND b.binding_key = 'leave_multi'
  AND exists_binding.id IS NULL;

UPDATE ai_business_binding b
JOIN tmp_pw_flow_fix f ON f.object_code = b.target_code AND f.flow_key = b.binding_key
SET b.binding_name = f.binding_name,
    b.binding_config = JSON_SET(
      COALESCE(CAST(b.binding_config AS JSON), JSON_OBJECT()),
      '$.flowModelKey', f.flow_key,
      '$.flowModelName', f.flow_name,
      '$.titleTemplate', f.title_template,
      '$.startMode', 'ACTION_ONLY',
      '$.options.lowcodeApp', true,
      '$.options.scenario', 'procurement_warehouse'
    ),
    b.description = CONCAT(f.binding_name, '，绑定采购仓储业务流程模型'),
    b.status = 1,
    b.update_by = 1,
    b.update_time = NOW()
WHERE b.tenant_id = 1
  AND b.target_type = 'OBJECT'
  AND b.binding_type = 'FLOW';

UPDATE ai_business_binding b
JOIN tmp_pw_flow_fix f ON f.object_code = b.target_code
SET b.status = 0,
    b.description = '已替换为采购仓储业务流程模型绑定',
    b.update_by = 1,
    b.update_time = NOW()
WHERE b.tenant_id = 1
  AND b.target_type = 'OBJECT'
  AND b.binding_type = 'FLOW'
  AND b.binding_key = 'leave_multi';

UPDATE ai_business_object o
JOIN tmp_pw_flow_fix f ON f.object_code = o.object_code
SET o.designer_options = JSON_SET(
      COALESCE(CAST(o.designer_options AS JSON), JSON_OBJECT()),
      JSON_UNQUOTE(JSON_SEARCH(CAST(o.designer_options AS JSON), 'one', 'leave_multi', NULL, '$.actions[*].actionConfig.steps[*].stepConfig.flowModelKey')),
      f.flow_key
    ),
    o.update_by = 1,
    o.update_time = NOW()
WHERE o.tenant_id = 1
  AND o.suite_code = 'PROCUREMENT_WAREHOUSE'
  AND JSON_SEARCH(CAST(o.designer_options AS JSON), 'one', 'leave_multi', NULL, '$.actions[*].actionConfig.steps[*].stepConfig.flowModelKey') IS NOT NULL;

UPDATE ai_business_object o
SET o.designer_options = JSON_SET(
      COALESCE(CAST(o.designer_options AS JSON), JSON_OBJECT()),
      '$.relationConfigVisible', true,
      '$.relationSummaryVisible', true
    ),
    o.update_by = 1,
    o.update_time = NOW()
WHERE o.tenant_id = 1
  AND o.suite_code = 'PROCUREMENT_WAREHOUSE';

SET @pw_child_field := 'id';

UPDATE ai_crud_config
SET options = JSON_SET(
      CAST(options AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(options AS JSON), 'one', @pw_child_field, NULL, '$.masterDetailConfig.children[*].fields[*].field')), '.field', '.formVisible'), false,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(options AS JSON), 'one', @pw_child_field, NULL, '$.masterDetailConfig.children[*].fields[*].field')), '.field', '.required'), false,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(options AS JSON), 'one', @pw_child_field, NULL, '$.masterDetailConfig.children[*].fields[*].field')), '.field', '.hidden'), true,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(options AS JSON), 'one', @pw_child_field, NULL, '$.masterDetailConfig.children[*].fields[*].field')), '.field', '.showInChildEditor'), false
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND domain_code = 'PROCUREMENT_WAREHOUSE'
  AND JSON_SEARCH(CAST(options AS JSON), 'one', @pw_child_field, NULL, '$.masterDetailConfig.children[*].fields[*].field') IS NOT NULL;

UPDATE ai_crud_config
SET page_schema = JSON_SET(
      CAST(page_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(page_schema AS JSON), 'one', @pw_child_field, NULL, '$.modelRefs[*].fields[*].field')), '.field', '.formVisible'), false,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(page_schema AS JSON), 'one', @pw_child_field, NULL, '$.modelRefs[*].fields[*].field')), '.field', '.required'), false,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(page_schema AS JSON), 'one', @pw_child_field, NULL, '$.modelRefs[*].fields[*].field')), '.field', '.hidden'), true,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(page_schema AS JSON), 'one', @pw_child_field, NULL, '$.modelRefs[*].fields[*].field')), '.field', '.showInChildEditor'), false
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND domain_code = 'PROCUREMENT_WAREHOUSE'
  AND JSON_SEARCH(CAST(page_schema AS JSON), 'one', @pw_child_field, NULL, '$.modelRefs[*].fields[*].field') IS NOT NULL;

UPDATE ai_lowcode_model
SET model_schema = JSON_SET(
      CAST(model_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', @pw_child_field, NULL, '$.fields[*].field')), '.field', '.formVisible'), false,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', @pw_child_field, NULL, '$.fields[*].field')), '.field', '.required'), false,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', @pw_child_field, NULL, '$.fields[*].field')), '.field', '.hidden'), true,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', @pw_child_field, NULL, '$.fields[*].field')), '.field', '.showInChildEditor'), false
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND domain_code = 'PROCUREMENT_WAREHOUSE'
  AND model_code IN ('pw_supplier_material', 'pw_purchase_order_item', 'pw_outbound_order_item', 'pw_transfer_order_item')
  AND JSON_SEARCH(CAST(model_schema AS JSON), 'one', @pw_child_field, NULL, '$.fields[*].field') IS NOT NULL;

SET @pw_child_field := 'supplierId';

UPDATE ai_crud_config
SET options = JSON_SET(
      CAST(options AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(options AS JSON), 'one', @pw_child_field, NULL, '$.masterDetailConfig.children[*].fields[*].field')), '.field', '.formVisible'), false,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(options AS JSON), 'one', @pw_child_field, NULL, '$.masterDetailConfig.children[*].fields[*].field')), '.field', '.required'), false,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(options AS JSON), 'one', @pw_child_field, NULL, '$.masterDetailConfig.children[*].fields[*].field')), '.field', '.hidden'), true,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(options AS JSON), 'one', @pw_child_field, NULL, '$.masterDetailConfig.children[*].fields[*].field')), '.field', '.showInChildEditor'), false
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND domain_code = 'PROCUREMENT_WAREHOUSE'
  AND JSON_SEARCH(CAST(options AS JSON), 'one', @pw_child_field, NULL, '$.masterDetailConfig.children[*].fields[*].field') IS NOT NULL;

UPDATE ai_crud_config
SET page_schema = JSON_SET(
      CAST(page_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(page_schema AS JSON), 'one', @pw_child_field, NULL, '$.modelRefs[*].fields[*].field')), '.field', '.formVisible'), false,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(page_schema AS JSON), 'one', @pw_child_field, NULL, '$.modelRefs[*].fields[*].field')), '.field', '.required'), false,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(page_schema AS JSON), 'one', @pw_child_field, NULL, '$.modelRefs[*].fields[*].field')), '.field', '.hidden'), true,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(page_schema AS JSON), 'one', @pw_child_field, NULL, '$.modelRefs[*].fields[*].field')), '.field', '.showInChildEditor'), false
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND domain_code = 'PROCUREMENT_WAREHOUSE'
  AND JSON_SEARCH(CAST(page_schema AS JSON), 'one', @pw_child_field, NULL, '$.modelRefs[*].fields[*].field') IS NOT NULL;

UPDATE ai_lowcode_model
SET model_schema = JSON_SET(
      CAST(model_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', @pw_child_field, NULL, '$.fields[*].field')), '.field', '.formVisible'), false,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', @pw_child_field, NULL, '$.fields[*].field')), '.field', '.required'), false,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', @pw_child_field, NULL, '$.fields[*].field')), '.field', '.hidden'), true,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', @pw_child_field, NULL, '$.fields[*].field')), '.field', '.showInChildEditor'), false
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND domain_code = 'PROCUREMENT_WAREHOUSE'
  AND model_code IN ('pw_supplier_material', 'pw_purchase_order_item', 'pw_outbound_order_item', 'pw_transfer_order_item')
  AND JSON_SEARCH(CAST(model_schema AS JSON), 'one', @pw_child_field, NULL, '$.fields[*].field') IS NOT NULL;

SET @pw_child_field := 'materialId';

UPDATE ai_crud_config
SET options = JSON_SET(
      CAST(options AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(options AS JSON), 'one', @pw_child_field, NULL, '$.masterDetailConfig.children[*].fields[*].field')), '.field', '.formVisible'), false,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(options AS JSON), 'one', @pw_child_field, NULL, '$.masterDetailConfig.children[*].fields[*].field')), '.field', '.required'), false,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(options AS JSON), 'one', @pw_child_field, NULL, '$.masterDetailConfig.children[*].fields[*].field')), '.field', '.hidden'), true,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(options AS JSON), 'one', @pw_child_field, NULL, '$.masterDetailConfig.children[*].fields[*].field')), '.field', '.showInChildEditor'), false
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND domain_code = 'PROCUREMENT_WAREHOUSE'
  AND JSON_SEARCH(CAST(options AS JSON), 'one', @pw_child_field, NULL, '$.masterDetailConfig.children[*].fields[*].field') IS NOT NULL;

UPDATE ai_crud_config
SET page_schema = JSON_SET(
      CAST(page_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(page_schema AS JSON), 'one', @pw_child_field, NULL, '$.modelRefs[*].fields[*].field')), '.field', '.formVisible'), false,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(page_schema AS JSON), 'one', @pw_child_field, NULL, '$.modelRefs[*].fields[*].field')), '.field', '.required'), false,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(page_schema AS JSON), 'one', @pw_child_field, NULL, '$.modelRefs[*].fields[*].field')), '.field', '.hidden'), true,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(page_schema AS JSON), 'one', @pw_child_field, NULL, '$.modelRefs[*].fields[*].field')), '.field', '.showInChildEditor'), false
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND domain_code = 'PROCUREMENT_WAREHOUSE'
  AND JSON_SEARCH(CAST(page_schema AS JSON), 'one', @pw_child_field, NULL, '$.modelRefs[*].fields[*].field') IS NOT NULL;

UPDATE ai_lowcode_model
SET model_schema = JSON_SET(
      CAST(model_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', @pw_child_field, NULL, '$.fields[*].field')), '.field', '.formVisible'), false,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', @pw_child_field, NULL, '$.fields[*].field')), '.field', '.required'), false,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', @pw_child_field, NULL, '$.fields[*].field')), '.field', '.hidden'), true,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', @pw_child_field, NULL, '$.fields[*].field')), '.field', '.showInChildEditor'), false
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND domain_code = 'PROCUREMENT_WAREHOUSE'
  AND model_code IN ('pw_supplier_material', 'pw_purchase_order_item', 'pw_outbound_order_item', 'pw_transfer_order_item')
  AND JSON_SEARCH(CAST(model_schema AS JSON), 'one', @pw_child_field, NULL, '$.fields[*].field') IS NOT NULL;

SET @pw_child_field := 'warehouseId';

UPDATE ai_crud_config
SET options = JSON_SET(
      CAST(options AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(options AS JSON), 'one', @pw_child_field, NULL, '$.masterDetailConfig.children[*].fields[*].field')), '.field', '.formVisible'), false,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(options AS JSON), 'one', @pw_child_field, NULL, '$.masterDetailConfig.children[*].fields[*].field')), '.field', '.required'), false,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(options AS JSON), 'one', @pw_child_field, NULL, '$.masterDetailConfig.children[*].fields[*].field')), '.field', '.hidden'), true,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(options AS JSON), 'one', @pw_child_field, NULL, '$.masterDetailConfig.children[*].fields[*].field')), '.field', '.showInChildEditor'), false
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND domain_code = 'PROCUREMENT_WAREHOUSE'
  AND JSON_SEARCH(CAST(options AS JSON), 'one', @pw_child_field, NULL, '$.masterDetailConfig.children[*].fields[*].field') IS NOT NULL;

UPDATE ai_crud_config
SET page_schema = JSON_SET(
      CAST(page_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(page_schema AS JSON), 'one', @pw_child_field, NULL, '$.modelRefs[*].fields[*].field')), '.field', '.formVisible'), false,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(page_schema AS JSON), 'one', @pw_child_field, NULL, '$.modelRefs[*].fields[*].field')), '.field', '.required'), false,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(page_schema AS JSON), 'one', @pw_child_field, NULL, '$.modelRefs[*].fields[*].field')), '.field', '.hidden'), true,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(page_schema AS JSON), 'one', @pw_child_field, NULL, '$.modelRefs[*].fields[*].field')), '.field', '.showInChildEditor'), false
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND domain_code = 'PROCUREMENT_WAREHOUSE'
  AND JSON_SEARCH(CAST(page_schema AS JSON), 'one', @pw_child_field, NULL, '$.modelRefs[*].fields[*].field') IS NOT NULL;

UPDATE ai_lowcode_model
SET model_schema = JSON_SET(
      CAST(model_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', @pw_child_field, NULL, '$.fields[*].field')), '.field', '.formVisible'), false,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', @pw_child_field, NULL, '$.fields[*].field')), '.field', '.required'), false,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', @pw_child_field, NULL, '$.fields[*].field')), '.field', '.hidden'), true,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', @pw_child_field, NULL, '$.fields[*].field')), '.field', '.showInChildEditor'), false
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND domain_code = 'PROCUREMENT_WAREHOUSE'
  AND model_code IN ('pw_supplier_material', 'pw_purchase_order_item', 'pw_outbound_order_item', 'pw_transfer_order_item')
  AND JSON_SEARCH(CAST(model_schema AS JSON), 'one', @pw_child_field, NULL, '$.fields[*].field') IS NOT NULL;

SET @pw_child_field := 'purchaseId';

UPDATE ai_crud_config
SET page_schema = JSON_SET(
      CAST(page_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(page_schema AS JSON), 'one', @pw_child_field, NULL, '$.modelRefs[*].fields[*].field')), '.field', '.formVisible'), false,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(page_schema AS JSON), 'one', @pw_child_field, NULL, '$.modelRefs[*].fields[*].field')), '.field', '.required'), false,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(page_schema AS JSON), 'one', @pw_child_field, NULL, '$.modelRefs[*].fields[*].field')), '.field', '.hidden'), true,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(page_schema AS JSON), 'one', @pw_child_field, NULL, '$.modelRefs[*].fields[*].field')), '.field', '.showInChildEditor'), false
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND domain_code = 'PROCUREMENT_WAREHOUSE'
  AND JSON_SEARCH(CAST(page_schema AS JSON), 'one', @pw_child_field, NULL, '$.modelRefs[*].fields[*].field') IS NOT NULL;

UPDATE ai_lowcode_model
SET model_schema = JSON_SET(
      CAST(model_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', @pw_child_field, NULL, '$.fields[*].field')), '.field', '.formVisible'), false,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', @pw_child_field, NULL, '$.fields[*].field')), '.field', '.required'), false,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', @pw_child_field, NULL, '$.fields[*].field')), '.field', '.hidden'), true,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', @pw_child_field, NULL, '$.fields[*].field')), '.field', '.showInChildEditor'), false
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND domain_code = 'PROCUREMENT_WAREHOUSE'
  AND model_code IN ('pw_purchase_order_item')
  AND JSON_SEARCH(CAST(model_schema AS JSON), 'one', @pw_child_field, NULL, '$.fields[*].field') IS NOT NULL;

SET @pw_child_field := 'outboundId';

UPDATE ai_crud_config
SET page_schema = JSON_SET(
      CAST(page_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(page_schema AS JSON), 'one', @pw_child_field, NULL, '$.modelRefs[*].fields[*].field')), '.field', '.formVisible'), false,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(page_schema AS JSON), 'one', @pw_child_field, NULL, '$.modelRefs[*].fields[*].field')), '.field', '.required'), false,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(page_schema AS JSON), 'one', @pw_child_field, NULL, '$.modelRefs[*].fields[*].field')), '.field', '.hidden'), true,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(page_schema AS JSON), 'one', @pw_child_field, NULL, '$.modelRefs[*].fields[*].field')), '.field', '.showInChildEditor'), false
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND domain_code = 'PROCUREMENT_WAREHOUSE'
  AND JSON_SEARCH(CAST(page_schema AS JSON), 'one', @pw_child_field, NULL, '$.modelRefs[*].fields[*].field') IS NOT NULL;

UPDATE ai_lowcode_model
SET model_schema = JSON_SET(
      CAST(model_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', @pw_child_field, NULL, '$.fields[*].field')), '.field', '.formVisible'), false,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', @pw_child_field, NULL, '$.fields[*].field')), '.field', '.required'), false,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', @pw_child_field, NULL, '$.fields[*].field')), '.field', '.hidden'), true,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', @pw_child_field, NULL, '$.fields[*].field')), '.field', '.showInChildEditor'), false
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND domain_code = 'PROCUREMENT_WAREHOUSE'
  AND model_code IN ('pw_outbound_order_item')
  AND JSON_SEARCH(CAST(model_schema AS JSON), 'one', @pw_child_field, NULL, '$.fields[*].field') IS NOT NULL;

SET @pw_child_field := 'transferId';

UPDATE ai_crud_config
SET page_schema = JSON_SET(
      CAST(page_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(page_schema AS JSON), 'one', @pw_child_field, NULL, '$.modelRefs[*].fields[*].field')), '.field', '.formVisible'), false,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(page_schema AS JSON), 'one', @pw_child_field, NULL, '$.modelRefs[*].fields[*].field')), '.field', '.required'), false,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(page_schema AS JSON), 'one', @pw_child_field, NULL, '$.modelRefs[*].fields[*].field')), '.field', '.hidden'), true,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(page_schema AS JSON), 'one', @pw_child_field, NULL, '$.modelRefs[*].fields[*].field')), '.field', '.showInChildEditor'), false
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND domain_code = 'PROCUREMENT_WAREHOUSE'
  AND JSON_SEARCH(CAST(page_schema AS JSON), 'one', @pw_child_field, NULL, '$.modelRefs[*].fields[*].field') IS NOT NULL;

UPDATE ai_lowcode_model
SET model_schema = JSON_SET(
      CAST(model_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', @pw_child_field, NULL, '$.fields[*].field')), '.field', '.formVisible'), false,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', @pw_child_field, NULL, '$.fields[*].field')), '.field', '.required'), false,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', @pw_child_field, NULL, '$.fields[*].field')), '.field', '.hidden'), true,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', @pw_child_field, NULL, '$.fields[*].field')), '.field', '.showInChildEditor'), false
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND domain_code = 'PROCUREMENT_WAREHOUSE'
  AND model_code IN ('pw_transfer_order_item')
  AND JSON_SEARCH(CAST(model_schema AS JSON), 'one', @pw_child_field, NULL, '$.fields[*].field') IS NOT NULL;

UPDATE ai_crud_config
SET edit_schema = JSON_SET(
      CAST(edit_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'orderStatus', NULL, '$[*].field')), '.field', '.type'), 'select',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'orderStatus', NULL, '$[*].field')), '.field', '.componentType'), 'select',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'orderStatus', NULL, '$[*].field')), '.field', '.dictType'), 'pw_order_status',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'orderStatus', NULL, '$[*].field')), '.field', '.props'), JSON_OBJECT('dictType', 'pw_order_status')
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND domain_code = 'PROCUREMENT_WAREHOUSE'
  AND config_key IN ('pw_purchase_order', 'pw_outbound_order', 'pw_transfer_order')
  AND JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'orderStatus', NULL, '$[*].field') IS NOT NULL;

UPDATE ai_lowcode_model
SET model_schema = JSON_SET(
      CAST(model_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'orderStatus', NULL, '$.fields[*].field')), '.field', '.fieldType'), 'DICT',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'orderStatus', NULL, '$.fields[*].field')), '.field', '.businessFieldType'), 'DICT',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'orderStatus', NULL, '$.fields[*].field')), '.field', '.componentType'), 'select',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'orderStatus', NULL, '$.fields[*].field')), '.field', '.dictType'), 'pw_order_status',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'orderStatus', NULL, '$.fields[*].field')), '.field', '.basicProps'), JSON_OBJECT('dictType', 'pw_order_status')
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND domain_code = 'PROCUREMENT_WAREHOUSE'
  AND model_code IN ('pw_purchase_order', 'pw_outbound_order', 'pw_transfer_order')
  AND JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'orderStatus', NULL, '$.fields[*].field') IS NOT NULL;

DROP TEMPORARY TABLE IF EXISTS tmp_pw_existing_flow_binding;
DROP TEMPORARY TABLE IF EXISTS tmp_pw_flow_fix;

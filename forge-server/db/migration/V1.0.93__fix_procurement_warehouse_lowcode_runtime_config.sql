-- Fix procurement/warehouse low-code runtime config.
-- Business-specific values stay in seed data; runtime behavior remains platform-generic.

SET @gen_material := JSON_OBJECT('enabled', true, 'mode', 'CODE_RULE', 'ruleCode', 'material_code', 'trigger', 'ON_CREATE', 'fillPolicy', 'EMPTY_ONLY');
SET @gen_supplier := JSON_OBJECT('enabled', true, 'mode', 'CODE_RULE', 'ruleCode', 'supplier_code', 'trigger', 'ON_CREATE', 'fillPolicy', 'EMPTY_ONLY');
SET @gen_warehouse := JSON_OBJECT('enabled', true, 'mode', 'CODE_RULE', 'ruleCode', 'warehouse_code', 'trigger', 'ON_CREATE', 'fillPolicy', 'EMPTY_ONLY');
SET @gen_purchase := JSON_OBJECT('enabled', true, 'mode', 'CODE_RULE', 'ruleCode', 'purchase_no', 'trigger', 'ON_CREATE', 'fillPolicy', 'EMPTY_ONLY');
SET @gen_outbound := JSON_OBJECT('enabled', true, 'mode', 'CODE_RULE', 'ruleCode', 'outbound_no', 'trigger', 'ON_CREATE', 'fillPolicy', 'EMPTY_ONLY');
SET @gen_transfer := JSON_OBJECT('enabled', true, 'mode', 'CODE_RULE', 'ruleCode', 'transfer_no', 'trigger', 'ON_CREATE', 'fillPolicy', 'EMPTY_ONLY');
SET @phone_validation := JSON_OBJECT('preset', 'PHONE', 'message', '请输入正确的手机号');

INSERT INTO sys_dict_type (tenant_id, dict_name, dict_type, dict_status, remark, create_by, create_time, update_by, update_time, create_dept)
SELECT seed.tenant_id, seed.dict_name, seed.dict_type, 1, seed.remark, 1, NOW(), 1, NOW(), 1
FROM (
  SELECT 1 tenant_id, '采购仓储通用状态' dict_name, 'pw_common_status' dict_type, '采购仓储主数据启停状态' remark
  UNION ALL SELECT 1, '采购仓储仓库类型', 'pw_warehouse_type', '采购仓储仓库类型'
  UNION ALL SELECT 1, '采购仓储单据状态', 'pw_order_status', '采购、出库、调拨单据状态'
) seed
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_dict_type t
  WHERE t.tenant_id = seed.tenant_id
    AND t.dict_type = seed.dict_type
);

INSERT INTO sys_dict_data (tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default,
                           dict_status, remark, create_by, create_time, update_by, update_time, create_dept)
SELECT seed.tenant_id, seed.dict_sort, seed.dict_label, seed.dict_value, seed.dict_type, NULL, seed.list_class,
       seed.is_default, 1, seed.remark, 1, NOW(), 1, NOW(), 1
FROM (
  SELECT 1 tenant_id, 1 dict_sort, '启用' dict_label, 'ENABLED' dict_value, 'pw_common_status' dict_type, 'success' list_class, 'Y' is_default, '默认启用' remark
  UNION ALL SELECT 1, 2, '停用', 'DISABLED', 'pw_common_status', 'error', 'N', '停用状态'
  UNION ALL SELECT 1, 1, '中心仓库', 'CENTER', 'pw_warehouse_type', 'success', 'Y', '中心仓库'
  UNION ALL SELECT 1, 2, '项目现场仓', 'SITE', 'pw_warehouse_type', 'info', 'N', '项目现场仓'
  UNION ALL SELECT 1, 3, '临时仓', 'TEMP', 'pw_warehouse_type', 'warning', 'N', '临时仓'
  UNION ALL SELECT 1, 1, '草稿', 'DRAFT', 'pw_order_status', 'default', 'Y', '尚未提交审批'
  UNION ALL SELECT 1, 2, '审批中', 'SUBMITTED', 'pw_order_status', 'warning', 'N', '已提交审批'
  UNION ALL SELECT 1, 3, '流转中', 'IN_PROCESS', 'pw_order_status', 'info', 'N', '流程流转中'
  UNION ALL SELECT 1, 4, '已通过', 'APPROVED', 'pw_order_status', 'success', 'N', '审批通过'
  UNION ALL SELECT 1, 5, '已驳回', 'REJECTED', 'pw_order_status', 'error', 'N', '审批驳回'
  UNION ALL SELECT 1, 6, '已取消', 'CANCELED', 'pw_order_status', 'default', 'N', '单据取消'
  UNION ALL SELECT 1, 7, '已关闭', 'CLOSED', 'pw_order_status', 'default', 'N', '单据关闭'
) seed
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_dict_data d
  WHERE d.tenant_id = seed.tenant_id
    AND d.dict_type = seed.dict_type
    AND d.dict_value = seed.dict_value
);

INSERT INTO ai_code_rule (id, tenant_id, rule_code, rule_name, scene, template, reset_policy, seq_length,
                          status, builtin, remark, options, create_by, create_time, create_dept, update_by, update_time)
SELECT seed.id, 1, seed.rule_code, seed.rule_name, seed.scene, seed.template, seed.reset_policy, seed.seq_length,
       1, 1, seed.remark, NULL, 1, NOW(), 1, 1, NOW()
FROM (
  SELECT 1910000000000009301 id, 'supplier_code' rule_code, '供应商编号' rule_name, 'PROCUREMENT_WAREHOUSE' scene,
         CONCAT('SUP', '$', '{seq:5}') template, 'NONE' reset_policy, 5 seq_length, '供应商主数据编号' remark
  UNION ALL
  SELECT 1910000000000009302, 'warehouse_code', '仓库编号', 'PROCUREMENT_WAREHOUSE',
         CONCAT('WH', '$', '{seq:4}'), 'NONE', 4, '仓库主数据编号'
  UNION ALL
  SELECT 1910000000000009303, 'purchase_no', '采购单号', 'PROCUREMENT_WAREHOUSE',
         CONCAT('CG', '$', '{yyyyMMdd}', '$', '{seq:4}'), 'DAY', 4, '采购单日流水'
  UNION ALL
  SELECT 1910000000000009304, 'outbound_no', '出库单号', 'PROCUREMENT_WAREHOUSE',
         CONCAT('CK', '$', '{yyyyMMdd}', '$', '{seq:4}'), 'DAY', 4, '出库单日流水'
  UNION ALL
  SELECT 1910000000000009305, 'transfer_no', '调拨单号', 'PROCUREMENT_WAREHOUSE',
         CONCAT('DB', '$', '{yyyyMMdd}', '$', '{seq:4}'), 'DAY', 4, '调拨单日流水'
) seed
WHERE NOT EXISTS (
  SELECT 1
  FROM ai_code_rule r
  WHERE r.tenant_id = 1
    AND r.rule_code = seed.rule_code
);

UPDATE pw_material SET status = 'ENABLED' WHERE status IS NULL OR status = '';
UPDATE pw_supplier SET status = 'ENABLED' WHERE status IS NULL OR status = '';
UPDATE pw_supplier_material SET status = 'ENABLED' WHERE status IS NULL OR status = '';
UPDATE pw_warehouse SET status = 'ENABLED' WHERE status IS NULL OR status = '';
UPDATE pw_purchase_order SET order_status = 'DRAFT' WHERE order_status IS NULL OR order_status = '';
UPDATE pw_outbound_order SET order_status = 'DRAFT' WHERE order_status IS NULL OR order_status = '';
UPDATE pw_transfer_order SET order_status = 'DRAFT' WHERE order_status IS NULL OR order_status = '';

UPDATE ai_crud_config
SET options = JSON_SET(
      COALESCE(CAST(options AS JSON), JSON_OBJECT()),
      '$.businessObjectCode', object_code,
      '$.objectCode', object_code
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND domain_code = 'PROCUREMENT_WAREHOUSE'
  AND object_code IS NOT NULL
  AND object_code <> '';

UPDATE ai_business_document_config
SET options = JSON_SET(
      COALESCE(CAST(options AS JSON), JSON_OBJECT()),
      '$.showStartFlowAction', false,
      '$.hideStartFlowAction', true
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND suite_code = 'PROCUREMENT_WAREHOUSE'
  AND object_code IN ('PW_PURCHASE_ORDER', 'PW_OUTBOUND_ORDER', 'PW_TRANSFER_ORDER');

UPDATE ai_crud_config
SET edit_schema = JSON_SET(
      CAST(edit_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'materialCode', NULL, '$[*].field')), '.field', '.required'), false,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'materialCode', NULL, '$[*].field')), '.field', '.readonly'), true,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'materialCode', NULL, '$[*].field')), '.field', '.disabled'), true,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'materialCode', NULL, '$[*].field')), '.field', '.generation'), @gen_material
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND config_key = 'pw_material'
  AND JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'materialCode', NULL, '$[*].field') IS NOT NULL;

UPDATE ai_crud_config
SET edit_schema = JSON_SET(
      CAST(edit_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'supplierCode', NULL, '$[*].field')), '.field', '.required'), false,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'supplierCode', NULL, '$[*].field')), '.field', '.readonly'), true,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'supplierCode', NULL, '$[*].field')), '.field', '.disabled'), true,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'supplierCode', NULL, '$[*].field')), '.field', '.generation'), @gen_supplier
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND config_key = 'pw_supplier'
  AND JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'supplierCode', NULL, '$[*].field') IS NOT NULL;

UPDATE ai_crud_config
SET edit_schema = JSON_SET(
      CAST(edit_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'warehouseCode', NULL, '$[*].field')), '.field', '.required'), false,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'warehouseCode', NULL, '$[*].field')), '.field', '.readonly'), true,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'warehouseCode', NULL, '$[*].field')), '.field', '.disabled'), true,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'warehouseCode', NULL, '$[*].field')), '.field', '.generation'), @gen_warehouse
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND config_key = 'pw_warehouse'
  AND JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'warehouseCode', NULL, '$[*].field') IS NOT NULL;

UPDATE ai_crud_config
SET edit_schema = JSON_SET(
      CAST(edit_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'purchaseNo', NULL, '$[*].field')), '.field', '.required'), false,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'purchaseNo', NULL, '$[*].field')), '.field', '.readonly'), true,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'purchaseNo', NULL, '$[*].field')), '.field', '.disabled'), true,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'purchaseNo', NULL, '$[*].field')), '.field', '.generation'), @gen_purchase
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND config_key = 'pw_purchase_order'
  AND JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'purchaseNo', NULL, '$[*].field') IS NOT NULL;

UPDATE ai_crud_config
SET edit_schema = JSON_SET(
      CAST(edit_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'outboundNo', NULL, '$[*].field')), '.field', '.required'), false,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'outboundNo', NULL, '$[*].field')), '.field', '.readonly'), true,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'outboundNo', NULL, '$[*].field')), '.field', '.disabled'), true,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'outboundNo', NULL, '$[*].field')), '.field', '.generation'), @gen_outbound
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND config_key = 'pw_outbound_order'
  AND JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'outboundNo', NULL, '$[*].field') IS NOT NULL;

UPDATE ai_crud_config
SET edit_schema = JSON_SET(
      CAST(edit_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'transferNo', NULL, '$[*].field')), '.field', '.required'), false,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'transferNo', NULL, '$[*].field')), '.field', '.readonly'), true,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'transferNo', NULL, '$[*].field')), '.field', '.disabled'), true,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'transferNo', NULL, '$[*].field')), '.field', '.generation'), @gen_transfer
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND config_key = 'pw_transfer_order'
  AND JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'transferNo', NULL, '$[*].field') IS NOT NULL;

UPDATE ai_crud_config
SET edit_schema = JSON_SET(
      CAST(edit_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'contactPhone', NULL, '$[*].field')), '.field', '.validation'), @phone_validation,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'contactPhone', NULL, '$[*].field')), '.field', '.maxlength'), 32,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'contactPhone', NULL, '$[*].field')), '.field', '.showCount'), true
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND config_key = 'pw_supplier'
  AND JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'contactPhone', NULL, '$[*].field') IS NOT NULL;

UPDATE ai_lowcode_model
SET model_schema = JSON_SET(
      CAST(model_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'materialCode', NULL, '$.fields[*].field')), '.field', '.required'), false,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'materialCode', NULL, '$.fields[*].field')), '.field', '.basicProps'), JSON_OBJECT('generation', @gen_material)
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND domain_code = 'PROCUREMENT_WAREHOUSE'
  AND model_code = 'pw_material'
  AND JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'materialCode', NULL, '$.fields[*].field') IS NOT NULL;

UPDATE ai_lowcode_model
SET model_schema = JSON_SET(
      CAST(model_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'supplierCode', NULL, '$.fields[*].field')), '.field', '.required'), false,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'supplierCode', NULL, '$.fields[*].field')), '.field', '.basicProps'), JSON_OBJECT('generation', @gen_supplier)
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND domain_code = 'PROCUREMENT_WAREHOUSE'
  AND model_code = 'pw_supplier'
  AND JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'supplierCode', NULL, '$.fields[*].field') IS NOT NULL;

UPDATE ai_lowcode_model
SET model_schema = JSON_SET(
      CAST(model_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'warehouseCode', NULL, '$.fields[*].field')), '.field', '.required'), false,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'warehouseCode', NULL, '$.fields[*].field')), '.field', '.basicProps'), JSON_OBJECT('generation', @gen_warehouse)
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND domain_code = 'PROCUREMENT_WAREHOUSE'
  AND model_code = 'pw_warehouse'
  AND JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'warehouseCode', NULL, '$.fields[*].field') IS NOT NULL;

UPDATE ai_lowcode_model
SET model_schema = JSON_SET(
      CAST(model_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'purchaseNo', NULL, '$.fields[*].field')), '.field', '.required'), false,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'purchaseNo', NULL, '$.fields[*].field')), '.field', '.basicProps'), JSON_OBJECT('generation', @gen_purchase)
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND domain_code = 'PROCUREMENT_WAREHOUSE'
  AND model_code = 'pw_purchase_order'
  AND JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'purchaseNo', NULL, '$.fields[*].field') IS NOT NULL;

UPDATE ai_lowcode_model
SET model_schema = JSON_SET(
      CAST(model_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'outboundNo', NULL, '$.fields[*].field')), '.field', '.required'), false,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'outboundNo', NULL, '$.fields[*].field')), '.field', '.basicProps'), JSON_OBJECT('generation', @gen_outbound)
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND domain_code = 'PROCUREMENT_WAREHOUSE'
  AND model_code = 'pw_outbound_order'
  AND JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'outboundNo', NULL, '$.fields[*].field') IS NOT NULL;

UPDATE ai_lowcode_model
SET model_schema = JSON_SET(
      CAST(model_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'transferNo', NULL, '$.fields[*].field')), '.field', '.required'), false,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'transferNo', NULL, '$.fields[*].field')), '.field', '.basicProps'), JSON_OBJECT('generation', @gen_transfer)
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND domain_code = 'PROCUREMENT_WAREHOUSE'
  AND model_code = 'pw_transfer_order'
  AND JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'transferNo', NULL, '$.fields[*].field') IS NOT NULL;

UPDATE ai_lowcode_model
SET model_schema = JSON_SET(
      CAST(model_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'contactPhone', NULL, '$.fields[*].field')), '.field', '.basicProps'), JSON_OBJECT('validation', @phone_validation)
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND domain_code = 'PROCUREMENT_WAREHOUSE'
  AND model_code = 'pw_supplier'
  AND JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'contactPhone', NULL, '$.fields[*].field') IS NOT NULL;

UPDATE ai_crud_config
SET search_schema = JSON_SET(
      CAST(search_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(search_schema AS JSON), 'one', 'status', NULL, '$[*].field')), '.field', '.type'), 'select',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(search_schema AS JSON), 'one', 'status', NULL, '$[*].field')), '.field', '.dictType'), 'pw_common_status'
    ),
    columns_schema = JSON_SET(
      CAST(columns_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(columns_schema AS JSON), 'one', 'status', NULL, '$[*].prop')), '.prop', '.render'), JSON_OBJECT('type', 'dictTag', 'dictType', 'pw_common_status')
    ),
    edit_schema = JSON_SET(
      CAST(edit_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'status', NULL, '$[*].field')), '.field', '.type'), 'select',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'status', NULL, '$[*].field')), '.field', '.dictType'), 'pw_common_status'
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND config_key IN ('pw_material', 'pw_supplier', 'pw_supplier_material')
  AND JSON_SEARCH(CAST(search_schema AS JSON), 'one', 'status', NULL, '$[*].field') IS NOT NULL
  AND JSON_SEARCH(CAST(columns_schema AS JSON), 'one', 'status', NULL, '$[*].prop') IS NOT NULL
  AND JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'status', NULL, '$[*].field') IS NOT NULL;

UPDATE ai_crud_config
SET search_schema = JSON_SET(
      CAST(search_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(search_schema AS JSON), 'one', 'warehouseType', NULL, '$[*].field')), '.field', '.type'), 'select',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(search_schema AS JSON), 'one', 'warehouseType', NULL, '$[*].field')), '.field', '.dictType'), 'pw_warehouse_type'
    ),
    columns_schema = JSON_SET(
      CAST(columns_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(columns_schema AS JSON), 'one', 'warehouseType', NULL, '$[*].prop')), '.prop', '.render'), JSON_OBJECT('type', 'dictTag', 'dictType', 'pw_warehouse_type')
    ),
    edit_schema = JSON_SET(
      CAST(edit_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'warehouseType', NULL, '$[*].field')), '.field', '.type'), 'select',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'warehouseType', NULL, '$[*].field')), '.field', '.dictType'), 'pw_warehouse_type'
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND config_key = 'pw_warehouse'
  AND JSON_SEARCH(CAST(search_schema AS JSON), 'one', 'warehouseType', NULL, '$[*].field') IS NOT NULL
  AND JSON_SEARCH(CAST(columns_schema AS JSON), 'one', 'warehouseType', NULL, '$[*].prop') IS NOT NULL
  AND JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'warehouseType', NULL, '$[*].field') IS NOT NULL;

UPDATE ai_crud_config
SET edit_schema = JSON_SET(
      CAST(edit_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'status', NULL, '$[*].field')), '.field', '.type'), 'select',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'status', NULL, '$[*].field')), '.field', '.dictType'), 'pw_common_status'
    ),
    columns_schema = JSON_SET(
      CAST(columns_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(columns_schema AS JSON), 'one', 'status', NULL, '$[*].prop')), '.prop', '.render'), JSON_OBJECT('type', 'dictTag', 'dictType', 'pw_common_status')
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND config_key = 'pw_warehouse'
  AND JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'status', NULL, '$[*].field') IS NOT NULL
  AND JSON_SEARCH(CAST(columns_schema AS JSON), 'one', 'status', NULL, '$[*].prop') IS NOT NULL;

UPDATE ai_crud_config
SET search_schema = JSON_SET(
      CAST(search_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(search_schema AS JSON), 'one', 'orderStatus', NULL, '$[*].field')), '.field', '.type'), 'select',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(search_schema AS JSON), 'one', 'orderStatus', NULL, '$[*].field')), '.field', '.dictType'), 'pw_order_status'
    ),
    columns_schema = JSON_SET(
      CAST(columns_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(columns_schema AS JSON), 'one', 'orderStatus', NULL, '$[*].prop')), '.prop', '.render'), JSON_OBJECT('type', 'dictTag', 'dictType', 'pw_order_status')
    ),
    edit_schema = JSON_SET(
      CAST(edit_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'orderStatus', NULL, '$[*].field')), '.field', '.type'), 'select',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'orderStatus', NULL, '$[*].field')), '.field', '.dictType'), 'pw_order_status'
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND config_key IN ('pw_purchase_order', 'pw_outbound_order', 'pw_transfer_order')
  AND JSON_SEARCH(CAST(search_schema AS JSON), 'one', 'orderStatus', NULL, '$[*].field') IS NOT NULL
  AND JSON_SEARCH(CAST(columns_schema AS JSON), 'one', 'orderStatus', NULL, '$[*].prop') IS NOT NULL
  AND JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'orderStatus', NULL, '$[*].field') IS NOT NULL;

UPDATE ai_lowcode_model
SET model_schema = JSON_SET(
      CAST(model_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'status', NULL, '$.fields[*].field')), '.field', '.dictType'), 'pw_common_status'
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND domain_code = 'PROCUREMENT_WAREHOUSE'
  AND model_code IN ('pw_material', 'pw_supplier', 'pw_supplier_material')
  AND JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'status', NULL, '$.fields[*].field') IS NOT NULL;

UPDATE ai_lowcode_model
SET model_schema = JSON_SET(
      CAST(model_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'status', NULL, '$.fields[*].field')), '.field', '.dictType'), 'pw_common_status',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'warehouseType', NULL, '$.fields[*].field')), '.field', '.dictType'), 'pw_warehouse_type'
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND domain_code = 'PROCUREMENT_WAREHOUSE'
  AND model_code = 'pw_warehouse'
  AND JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'status', NULL, '$.fields[*].field') IS NOT NULL
  AND JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'warehouseType', NULL, '$.fields[*].field') IS NOT NULL;

UPDATE ai_lowcode_model
SET model_schema = JSON_SET(
      CAST(model_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'orderStatus', NULL, '$.fields[*].field')), '.field', '.dictType'), 'pw_order_status'
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND domain_code = 'PROCUREMENT_WAREHOUSE'
  AND model_code IN ('pw_purchase_order', 'pw_outbound_order', 'pw_transfer_order')
  AND JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'orderStatus', NULL, '$.fields[*].field') IS NOT NULL;

UPDATE ai_crud_config
SET options = JSON_SET(
      CAST(options AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(options AS JSON), 'one', 'materialId', NULL, '$.masterDetailConfig.children[0].fields[*].field')), '.field', '.formVisible'), false,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(options AS JSON), 'one', 'materialId', NULL, '$.masterDetailConfig.children[0].fields[*].field')), '.field', '.required'), false,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(options AS JSON), 'one', 'materialId', NULL, '$.masterDetailConfig.children[0].fields[*].field')), '.field', '.hidden'), true
    ),
    page_schema = JSON_SET(
      CAST(page_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'materialId', NULL, '$.modelRefs[1].fields[*].field')), '.field', '.formVisible'), false,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'materialId', NULL, '$.modelRefs[1].fields[*].field')), '.field', '.required'), false,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'materialId', NULL, '$.modelRefs[1].fields[*].field')), '.field', '.hidden'), true
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND config_key IN ('pw_supplier', 'pw_purchase_order', 'pw_outbound_order', 'pw_transfer_order')
  AND JSON_SEARCH(CAST(options AS JSON), 'one', 'materialId', NULL, '$.masterDetailConfig.children[0].fields[*].field') IS NOT NULL
  AND JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'materialId', NULL, '$.modelRefs[1].fields[*].field') IS NOT NULL;

UPDATE ai_crud_config
SET options = JSON_SET(
      CAST(options AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(options AS JSON), 'one', 'warehouseId', NULL, '$.masterDetailConfig.children[0].fields[*].field')), '.field', '.formVisible'), false,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(options AS JSON), 'one', 'warehouseId', NULL, '$.masterDetailConfig.children[0].fields[*].field')), '.field', '.required'), false,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(options AS JSON), 'one', 'warehouseId', NULL, '$.masterDetailConfig.children[0].fields[*].field')), '.field', '.hidden'), true
    ),
    page_schema = JSON_SET(
      CAST(page_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'warehouseId', NULL, '$.modelRefs[1].fields[*].field')), '.field', '.formVisible'), false,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'warehouseId', NULL, '$.modelRefs[1].fields[*].field')), '.field', '.required'), false,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'warehouseId', NULL, '$.modelRefs[1].fields[*].field')), '.field', '.hidden'), true
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND config_key = 'pw_outbound_order'
  AND JSON_SEARCH(CAST(options AS JSON), 'one', 'warehouseId', NULL, '$.masterDetailConfig.children[0].fields[*].field') IS NOT NULL
  AND JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'warehouseId', NULL, '$.modelRefs[1].fields[*].field') IS NOT NULL;

UPDATE ai_crud_config
SET options = JSON_SET(
      CAST(options AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(options AS JSON), 'one', 'status', NULL, '$.masterDetailConfig.children[0].fields[*].field')), '.field', '.type'), 'select',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(options AS JSON), 'one', 'status', NULL, '$.masterDetailConfig.children[0].fields[*].field')), '.field', '.componentType'), 'select',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(options AS JSON), 'one', 'status', NULL, '$.masterDetailConfig.children[0].fields[*].field')), '.field', '.dictType'), 'pw_common_status'
    ),
    page_schema = JSON_SET(
      CAST(page_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'status', NULL, '$.modelRefs[1].fields[*].field')), '.field', '.type'), 'select',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'status', NULL, '$.modelRefs[1].fields[*].field')), '.field', '.componentType'), 'select',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'status', NULL, '$.modelRefs[1].fields[*].field')), '.field', '.dictType'), 'pw_common_status'
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND config_key = 'pw_supplier'
  AND JSON_SEARCH(CAST(options AS JSON), 'one', 'status', NULL, '$.masterDetailConfig.children[0].fields[*].field') IS NOT NULL
  AND JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'status', NULL, '$.modelRefs[1].fields[*].field') IS NOT NULL;

-- Harden procurement/warehouse low-code designer and runtime config.
-- Keep platform behavior generic; this script only repairs persisted sample metadata.

SET @sql := (
  SELECT IF(COUNT(*) > 0,
    'ALTER TABLE pw_supplier_material MODIFY COLUMN material_id bigint DEFAULT NULL COMMENT ''物料ID''',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'pw_supplier_material'
    AND column_name = 'material_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE ai_crud_config
SET search_schema = JSON_SET(
      CAST(search_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(search_schema AS JSON), 'one', 'status', NULL, '$[*].field')), '.field', '.type'), 'select',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(search_schema AS JSON), 'one', 'status', NULL, '$[*].field')), '.field', '.componentType'), 'select',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(search_schema AS JSON), 'one', 'status', NULL, '$[*].field')), '.field', '.dictType'), 'pw_common_status',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(search_schema AS JSON), 'one', 'status', NULL, '$[*].field')), '.field', '.props.dictType'), 'pw_common_status'
    ),
    edit_schema = JSON_SET(
      CAST(edit_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'status', NULL, '$[*].field')), '.field', '.type'), 'select',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'status', NULL, '$[*].field')), '.field', '.componentType'), 'select',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'status', NULL, '$[*].field')), '.field', '.dictType'), 'pw_common_status',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'status', NULL, '$[*].field')), '.field', '.props.dictType'), 'pw_common_status',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'status', NULL, '$[*].field')), '.field', '.defaultValue'), 'ENABLED'
    ),
    columns_schema = JSON_SET(
      CAST(columns_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(columns_schema AS JSON), 'one', 'status', NULL, '$[*].prop')), '.prop', '.render'), JSON_OBJECT('type', 'dictTag', 'dictType', 'pw_common_status')
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND domain_code = 'PROCUREMENT_WAREHOUSE'
  AND config_key IN ('pw_material', 'pw_supplier', 'pw_supplier_material', 'pw_warehouse')
  AND JSON_SEARCH(CAST(search_schema AS JSON), 'one', 'status', NULL, '$[*].field') IS NOT NULL
  AND JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'status', NULL, '$[*].field') IS NOT NULL
  AND JSON_SEARCH(CAST(columns_schema AS JSON), 'one', 'status', NULL, '$[*].prop') IS NOT NULL;

UPDATE ai_crud_config
SET search_schema = JSON_SET(
      CAST(search_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(search_schema AS JSON), 'one', 'warehouseType', NULL, '$[*].field')), '.field', '.type'), 'select',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(search_schema AS JSON), 'one', 'warehouseType', NULL, '$[*].field')), '.field', '.componentType'), 'select',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(search_schema AS JSON), 'one', 'warehouseType', NULL, '$[*].field')), '.field', '.dictType'), 'pw_warehouse_type',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(search_schema AS JSON), 'one', 'warehouseType', NULL, '$[*].field')), '.field', '.props.dictType'), 'pw_warehouse_type'
    ),
    edit_schema = JSON_SET(
      CAST(edit_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'warehouseType', NULL, '$[*].field')), '.field', '.type'), 'select',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'warehouseType', NULL, '$[*].field')), '.field', '.componentType'), 'select',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'warehouseType', NULL, '$[*].field')), '.field', '.dictType'), 'pw_warehouse_type',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'warehouseType', NULL, '$[*].field')), '.field', '.props.dictType'), 'pw_warehouse_type'
    ),
    columns_schema = JSON_SET(
      CAST(columns_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(columns_schema AS JSON), 'one', 'warehouseType', NULL, '$[*].prop')), '.prop', '.render'), JSON_OBJECT('type', 'dictTag', 'dictType', 'pw_warehouse_type')
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND domain_code = 'PROCUREMENT_WAREHOUSE'
  AND config_key = 'pw_warehouse'
  AND JSON_SEARCH(CAST(search_schema AS JSON), 'one', 'warehouseType', NULL, '$[*].field') IS NOT NULL
  AND JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'warehouseType', NULL, '$[*].field') IS NOT NULL
  AND JSON_SEARCH(CAST(columns_schema AS JSON), 'one', 'warehouseType', NULL, '$[*].prop') IS NOT NULL;

UPDATE ai_crud_config
SET search_schema = JSON_SET(
      CAST(search_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(search_schema AS JSON), 'one', 'orderStatus', NULL, '$[*].field')), '.field', '.type'), 'select',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(search_schema AS JSON), 'one', 'orderStatus', NULL, '$[*].field')), '.field', '.componentType'), 'select',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(search_schema AS JSON), 'one', 'orderStatus', NULL, '$[*].field')), '.field', '.dictType'), 'pw_order_status',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(search_schema AS JSON), 'one', 'orderStatus', NULL, '$[*].field')), '.field', '.props.dictType'), 'pw_order_status'
    ),
    edit_schema = JSON_SET(
      CAST(edit_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'orderStatus', NULL, '$[*].field')), '.field', '.type'), 'select',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'orderStatus', NULL, '$[*].field')), '.field', '.componentType'), 'select',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'orderStatus', NULL, '$[*].field')), '.field', '.dictType'), 'pw_order_status',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'orderStatus', NULL, '$[*].field')), '.field', '.props.dictType'), 'pw_order_status',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'orderStatus', NULL, '$[*].field')), '.field', '.defaultValue'), 'DRAFT'
    ),
    columns_schema = JSON_SET(
      CAST(columns_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(columns_schema AS JSON), 'one', 'orderStatus', NULL, '$[*].prop')), '.prop', '.render'), JSON_OBJECT('type', 'dictTag', 'dictType', 'pw_order_status')
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND domain_code = 'PROCUREMENT_WAREHOUSE'
  AND config_key IN ('pw_purchase_order', 'pw_outbound_order', 'pw_transfer_order')
  AND JSON_SEARCH(CAST(search_schema AS JSON), 'one', 'orderStatus', NULL, '$[*].field') IS NOT NULL
  AND JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'orderStatus', NULL, '$[*].field') IS NOT NULL
  AND JSON_SEARCH(CAST(columns_schema AS JSON), 'one', 'orderStatus', NULL, '$[*].prop') IS NOT NULL;

UPDATE ai_lowcode_model
SET model_schema = JSON_SET(
      CAST(model_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'status', NULL, '$.fields[*].field')), '.field', '.businessFieldType'), 'DICT',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'status', NULL, '$.fields[*].field')), '.field', '.componentType'), 'select',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'status', NULL, '$.fields[*].field')), '.field', '.dictType'), 'pw_common_status',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'status', NULL, '$.fields[*].field')), '.field', '.basicProps.dictType'), 'pw_common_status',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'status', NULL, '$.fields[*].field')), '.field', '.advancedProps.dictType'), 'pw_common_status'
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND domain_code = 'PROCUREMENT_WAREHOUSE'
  AND model_code IN ('pw_material', 'pw_supplier', 'pw_supplier_material', 'pw_warehouse')
  AND JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'status', NULL, '$.fields[*].field') IS NOT NULL;

UPDATE ai_lowcode_model
SET model_schema = JSON_SET(
      CAST(model_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'warehouseType', NULL, '$.fields[*].field')), '.field', '.businessFieldType'), 'DICT',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'warehouseType', NULL, '$.fields[*].field')), '.field', '.componentType'), 'select',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'warehouseType', NULL, '$.fields[*].field')), '.field', '.dictType'), 'pw_warehouse_type',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'warehouseType', NULL, '$.fields[*].field')), '.field', '.basicProps.dictType'), 'pw_warehouse_type',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'warehouseType', NULL, '$.fields[*].field')), '.field', '.advancedProps.dictType'), 'pw_warehouse_type'
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND domain_code = 'PROCUREMENT_WAREHOUSE'
  AND model_code = 'pw_warehouse'
  AND JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'warehouseType', NULL, '$.fields[*].field') IS NOT NULL;

UPDATE ai_lowcode_model
SET model_schema = JSON_SET(
      CAST(model_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'orderStatus', NULL, '$.fields[*].field')), '.field', '.businessFieldType'), 'DICT',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'orderStatus', NULL, '$.fields[*].field')), '.field', '.componentType'), 'select',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'orderStatus', NULL, '$.fields[*].field')), '.field', '.dictType'), 'pw_order_status',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'orderStatus', NULL, '$.fields[*].field')), '.field', '.basicProps.dictType'), 'pw_order_status',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'orderStatus', NULL, '$.fields[*].field')), '.field', '.advancedProps.dictType'), 'pw_order_status'
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND domain_code = 'PROCUREMENT_WAREHOUSE'
  AND model_code IN ('pw_purchase_order', 'pw_outbound_order', 'pw_transfer_order')
  AND JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'orderStatus', NULL, '$.fields[*].field') IS NOT NULL;

UPDATE ai_crud_config
SET options = JSON_SET(
      COALESCE(CAST(options AS JSON), JSON_OBJECT()),
      '$.masterDetailConfig.children[0].recordSelector.objectCode', 'PW_MATERIAL',
      '$.masterDetailConfig.children[0].recordSelector.businessObjectCode', 'PW_MATERIAL',
      '$.masterDetailConfig.children[0].recordSelector.targetObjectCode', 'PW_MATERIAL'
    ),
    page_schema = JSON_SET(
      COALESCE(CAST(page_schema AS JSON), JSON_OBJECT()),
      '$.modelRefs[1].props.recordSelector.objectCode', 'PW_MATERIAL',
      '$.modelRefs[1].props.recordSelector.businessObjectCode', 'PW_MATERIAL',
      '$.modelRefs[1].props.recordSelector.targetObjectCode', 'PW_MATERIAL'
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND config_key = 'pw_supplier';

UPDATE ai_crud_config
SET options = JSON_SET(
      CAST(options AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(options AS JSON), 'one', 'status', NULL, '$.masterDetailConfig.children[*].fields[*].field')), '.field', '.type'), 'select',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(options AS JSON), 'one', 'status', NULL, '$.masterDetailConfig.children[*].fields[*].field')), '.field', '.componentType'), 'select',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(options AS JSON), 'one', 'status', NULL, '$.masterDetailConfig.children[*].fields[*].field')), '.field', '.dictType'), 'pw_common_status',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(options AS JSON), 'one', 'status', NULL, '$.masterDetailConfig.children[*].fields[*].field')), '.field', '.props.dictType'), 'pw_common_status',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(options AS JSON), 'one', 'status', NULL, '$.masterDetailConfig.children[*].fields[*].field')), '.field', '.defaultValue'), 'ENABLED'
    ),
    page_schema = JSON_SET(
      CAST(page_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'status', NULL, '$.modelRefs[*].fields[*].field')), '.field', '.type'), 'select',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'status', NULL, '$.modelRefs[*].fields[*].field')), '.field', '.componentType'), 'select',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'status', NULL, '$.modelRefs[*].fields[*].field')), '.field', '.dictType'), 'pw_common_status',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'status', NULL, '$.modelRefs[*].fields[*].field')), '.field', '.props.dictType'), 'pw_common_status',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'status', NULL, '$.modelRefs[*].fields[*].field')), '.field', '.defaultValue'), 'ENABLED'
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND config_key = 'pw_supplier'
  AND JSON_SEARCH(CAST(options AS JSON), 'one', 'status', NULL, '$.masterDetailConfig.children[*].fields[*].field') IS NOT NULL
  AND JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'status', NULL, '$.modelRefs[*].fields[*].field') IS NOT NULL;

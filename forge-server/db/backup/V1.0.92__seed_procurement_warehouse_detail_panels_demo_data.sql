-- Procurement/Warehouse low-code detail panels and demo data.
-- This migration keeps procurement-specific behavior in low-code seed data.

UPDATE ai_crud_config
SET search_schema = JSON_ARRAY_APPEND(
      COALESCE(CAST(search_schema AS JSON), JSON_ARRAY()),
      '$',
      JSON_OBJECT('field', 'warehouseId', 'label', '仓库ID', 'type', 'number', 'queryType', 'eq', 'hidden', true, 'visible', false, 'show', false)
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND domain_code = 'PROCUREMENT_WAREHOUSE'
  AND config_key = 'pw_purchase_order'
  AND JSON_SEARCH(COALESCE(CAST(search_schema AS JSON), JSON_ARRAY()), 'one', 'warehouseId', NULL, '$[*].field') IS NULL;

UPDATE ai_crud_config
SET search_schema = JSON_ARRAY_APPEND(
      COALESCE(CAST(search_schema AS JSON), JSON_ARRAY()),
      '$',
      JSON_OBJECT('field', 'warehouseId', 'label', '仓库ID', 'type', 'number', 'queryType', 'eq', 'hidden', true, 'visible', false, 'show', false)
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND domain_code = 'PROCUREMENT_WAREHOUSE'
  AND config_key = 'pw_outbound_order'
  AND JSON_SEARCH(COALESCE(CAST(search_schema AS JSON), JSON_ARRAY()), 'one', 'warehouseId', NULL, '$[*].field') IS NULL;

UPDATE ai_crud_config
SET search_schema = JSON_ARRAY_APPEND(
      COALESCE(CAST(search_schema AS JSON), JSON_ARRAY()),
      '$',
      JSON_OBJECT('field', 'fromWarehouseId', 'label', '调出仓库ID', 'type', 'number', 'queryType', 'eq', 'hidden', true, 'visible', false, 'show', false)
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND domain_code = 'PROCUREMENT_WAREHOUSE'
  AND config_key = 'pw_transfer_order'
  AND JSON_SEARCH(COALESCE(CAST(search_schema AS JSON), JSON_ARRAY()), 'one', 'fromWarehouseId', NULL, '$[*].field') IS NULL;

UPDATE ai_crud_config
SET search_schema = JSON_ARRAY_APPEND(
      COALESCE(CAST(search_schema AS JSON), JSON_ARRAY()),
      '$',
      JSON_OBJECT('field', 'toWarehouseId', 'label', '调入仓库ID', 'type', 'number', 'queryType', 'eq', 'hidden', true, 'visible', false, 'show', false)
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND domain_code = 'PROCUREMENT_WAREHOUSE'
  AND config_key = 'pw_transfer_order'
  AND JSON_SEARCH(COALESCE(CAST(search_schema AS JSON), JSON_ARRAY()), 'one', 'toWarehouseId', NULL, '$[*].field') IS NULL;

UPDATE ai_crud_config
SET search_schema = JSON_ARRAY_APPEND(
      COALESCE(CAST(search_schema AS JSON), JSON_ARRAY()),
      '$',
      JSON_OBJECT('field', 'materialId', 'label', '物料ID', 'type', 'number', 'queryType', 'eq', 'hidden', true, 'visible', false, 'show', false)
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND domain_code = 'PROCUREMENT_WAREHOUSE'
  AND config_key = 'pw_supplier_material'
  AND JSON_SEARCH(COALESCE(CAST(search_schema AS JSON), JSON_ARRAY()), 'one', 'materialId', NULL, '$[*].field') IS NULL;

UPDATE ai_crud_config c
JOIN (
  SELECT 'pw_warehouse' AS config_key,
         JSON_ARRAY(
           JSON_OBJECT(
             'key', 'warehouse_balance',
             'type', 'quantity-balance',
             'title', '库存余额',
             'dataSource', JSON_OBJECT('type', 'quantity', 'queryType', 'quantity-balance', 'paramsMap', JSON_OBJECT('accountCode', CONCAT('$', '{row.id}')), 'pageSize', 20),
             'quantity', JSON_OBJECT('queryType', 'quantity-balance', 'paramsMap', JSON_OBJECT('accountCode', CONCAT('$', '{row.id}')), 'pageNum', 1, 'pageSize', 20)
           ),
           JSON_OBJECT(
             'key', 'warehouse_ledger',
             'type', 'quantity-ledger',
             'title', '数量流水',
             'dataSource', JSON_OBJECT('type', 'quantity', 'queryType', 'quantity-ledger', 'paramsMap', JSON_OBJECT('accountCode', CONCAT('$', '{row.id}')), 'pageSize', 20),
             'quantity', JSON_OBJECT('queryType', 'quantity-ledger', 'paramsMap', JSON_OBJECT('accountCode', CONCAT('$', '{row.id}')), 'pageNum', 1, 'pageSize', 20)
           ),
           JSON_OBJECT(
             'key', 'warehouse_locks',
             'type', 'quantity-lock',
             'title', '数量锁定',
             'dataSource', JSON_OBJECT('type', 'quantity', 'queryType', 'quantity-lock', 'paramsMap', JSON_OBJECT('accountCode', CONCAT('$', '{row.id}')), 'pageSize', 20),
             'quantity', JSON_OBJECT('queryType', 'quantity-lock', 'paramsMap', JSON_OBJECT('accountCode', CONCAT('$', '{row.id}')), 'pageNum', 1, 'pageSize', 20)
           ),
           JSON_OBJECT(
             'key', 'warehouse_purchase_orders',
             'type', 'table',
             'title', '采购记录',
             'dataSource', JSON_OBJECT('type', 'api', 'api', 'get@/ai/crud/pw_purchase_order/page', 'paramsMap', JSON_OBJECT('warehouseId', CONCAT('$', '{row.id}')), 'dataField', 'records'),
             'table', JSON_OBJECT(
               'rowKey', 'id', 'pagination', false, 'maxHeight', 260,
               'columns', JSON_ARRAY(
                 JSON_OBJECT('prop', 'purchaseNo', 'label', '采购单号', 'width', 160),
                 JSON_OBJECT('prop', 'supplierName', 'label', '供应商', 'width', 180),
                 JSON_OBJECT('prop', 'purchaseAmountCent', 'label', '采购金额(分)', 'width', 130),
                 JSON_OBJECT('prop', 'orderStatus', 'label', '状态', 'width', 100)
               )
             )
           ),
           JSON_OBJECT(
             'key', 'warehouse_outbound_orders',
             'type', 'table',
             'title', '出库记录',
             'dataSource', JSON_OBJECT('type', 'api', 'api', 'get@/ai/crud/pw_outbound_order/page', 'paramsMap', JSON_OBJECT('warehouseId', CONCAT('$', '{row.id}')), 'dataField', 'records'),
             'table', JSON_OBJECT(
               'rowKey', 'id', 'pagination', false, 'maxHeight', 260,
               'columns', JSON_ARRAY(
                 JSON_OBJECT('prop', 'outboundNo', 'label', '出库单号', 'width', 160),
                 JSON_OBJECT('prop', 'applicantName', 'label', '申请人', 'width', 110),
                 JSON_OBJECT('prop', 'outboundDate', 'label', '出库日期', 'width', 120),
                 JSON_OBJECT('prop', 'orderStatus', 'label', '状态', 'width', 100)
               )
             )
           ),
           JSON_OBJECT(
             'key', 'warehouse_transfer_out',
             'type', 'table',
             'title', '调出记录',
             'dataSource', JSON_OBJECT('type', 'api', 'api', 'get@/ai/crud/pw_transfer_order/page', 'paramsMap', JSON_OBJECT('fromWarehouseId', CONCAT('$', '{row.id}')), 'dataField', 'records'),
             'table', JSON_OBJECT(
               'rowKey', 'id', 'pagination', false, 'maxHeight', 260,
               'columns', JSON_ARRAY(
                 JSON_OBJECT('prop', 'transferNo', 'label', '调拨单号', 'width', 160),
                 JSON_OBJECT('prop', 'toWarehouseName', 'label', '调入仓库', 'width', 180),
                 JSON_OBJECT('prop', 'transferDate', 'label', '调拨日期', 'width', 120),
                 JSON_OBJECT('prop', 'orderStatus', 'label', '状态', 'width', 100)
               )
             )
           ),
           JSON_OBJECT(
             'key', 'warehouse_transfer_in',
             'type', 'table',
             'title', '调入记录',
             'dataSource', JSON_OBJECT('type', 'api', 'api', 'get@/ai/crud/pw_transfer_order/page', 'paramsMap', JSON_OBJECT('toWarehouseId', CONCAT('$', '{row.id}')), 'dataField', 'records'),
             'table', JSON_OBJECT(
               'rowKey', 'id', 'pagination', false, 'maxHeight', 260,
               'columns', JSON_ARRAY(
                 JSON_OBJECT('prop', 'transferNo', 'label', '调拨单号', 'width', 160),
                 JSON_OBJECT('prop', 'fromWarehouseName', 'label', '调出仓库', 'width', 180),
                 JSON_OBJECT('prop', 'transferDate', 'label', '调拨日期', 'width', 120),
                 JSON_OBJECT('prop', 'orderStatus', 'label', '状态', 'width', 100)
               )
             )
           )
         ) AS detail_panels
  UNION ALL
  SELECT 'pw_material',
         JSON_ARRAY(
           JSON_OBJECT(
             'key', 'material_supplier_quotes',
             'type', 'table',
             'title', '供应商报价',
             'dataSource', JSON_OBJECT('type', 'api', 'api', 'get@/ai/crud/pw_supplier_material/page', 'paramsMap', JSON_OBJECT('materialId', CONCAT('$', '{row.id}')), 'dataField', 'records'),
             'table', JSON_OBJECT(
               'rowKey', 'id', 'pagination', false, 'maxHeight', 260,
               'columns', JSON_ARRAY(
                 JSON_OBJECT('prop', 'supplierName', 'label', '供应商', 'width', 180),
                 JSON_OBJECT('prop', 'quotePriceCent', 'label', '报价(分)', 'width', 120),
                 JSON_OBJECT('prop', 'lastPriceCent', 'label', '上次报价(分)', 'width', 130),
                 JSON_OBJECT('prop', 'effectiveDate', 'label', '有效期', 'width', 120),
                 JSON_OBJECT('prop', 'status', 'label', '状态', 'width', 100)
               )
             )
           ),
           JSON_OBJECT(
             'key', 'material_balance',
             'type', 'quantity-balance',
             'title', '库存信息',
             'dataSource', JSON_OBJECT('type', 'quantity', 'queryType', 'quantity-balance', 'paramsMap', JSON_OBJECT('itemCode', CONCAT('$', '{row.id}')), 'pageSize', 20),
             'quantity', JSON_OBJECT('queryType', 'quantity-balance', 'paramsMap', JSON_OBJECT('itemCode', CONCAT('$', '{row.id}')), 'pageNum', 1, 'pageSize', 20)
           ),
           JSON_OBJECT(
             'key', 'material_recent_ledger',
             'type', 'quantity-ledger',
             'title', '近 3 次出入库',
             'dataSource', JSON_OBJECT('type', 'quantity', 'queryType', 'quantity-ledger', 'paramsMap', JSON_OBJECT('itemCode', CONCAT('$', '{row.id}')), 'pageSize', 3),
             'quantity', JSON_OBJECT('queryType', 'quantity-ledger', 'paramsMap', JSON_OBJECT('itemCode', CONCAT('$', '{row.id}')), 'pageNum', 1, 'pageSize', 3)
           )
         )
  UNION ALL
  SELECT 'pw_purchase_order',
         JSON_ARRAY(
           JSON_OBJECT(
             'key', 'purchase_supplier_info',
             'type', 'descriptions',
             'title', '供应商信息',
             'descriptions', JSON_OBJECT(
               'columns', 3,
               'fields', JSON_ARRAY(
                 JSON_OBJECT('field', 'supplierName', 'label', '供应商'),
                 JSON_OBJECT('field', 'supplierContact', 'label', '联系人'),
                 JSON_OBJECT('field', 'supplierPhone', 'label', '联系电话'),
                 JSON_OBJECT('field', 'warehouseName', 'label', '目标仓库'),
                 JSON_OBJECT('field', 'purchaseAmountCent', 'label', '采购金额(分)'),
                 JSON_OBJECT('field', 'attachmentIds', 'label', '附件ID')
               )
             )
           )
         )
) seed ON seed.config_key = c.config_key
SET c.options = JSON_SET(
      COALESCE(CAST(c.options AS JSON), JSON_OBJECT()),
      '$.detailPanels', seed.detail_panels,
      '$.enableDetail', true,
      '$.modalWidth', '1120px'
    ),
    c.publish_time = NOW(),
    c.update_by = 1,
    c.update_time = NOW()
WHERE c.tenant_id = 1
  AND c.domain_code = 'PROCUREMENT_WAREHOUSE';

INSERT INTO pw_material (id, tenant_id, material_code, material_name, spec_model, unit, category,
                         reference_price_cent, warning_quantity, status, remark,
                         create_by, create_time, create_dept, update_by, update_time, del_flag)
SELECT seed.id, 1, seed.material_code, seed.material_name, seed.spec_model, seed.unit, seed.category,
       seed.reference_price_cent, seed.warning_quantity, 'ENABLED', seed.remark,
       1, NOW(), 1, 1, NOW(), '0'
FROM (
  SELECT 1950000000000009201 AS id, 'MAT-001' AS material_code, 'P.O 42.5 水泥' AS material_name,
         '袋装 50kg' AS spec_model, '吨' AS unit, '基础建材' AS category, 38000 AS reference_price_cent,
         30 AS warning_quantity, '主仓库常备水泥' AS remark
  UNION ALL
  SELECT 1950000000000009202, 'MAT-002', 'HRB400 螺纹钢', 'Φ16', '吨', '钢材', 420000, 5, '主体结构钢材'
  UNION ALL
  SELECT 1950000000000009203, 'MAT-003', 'YJV 电力电缆', '3*16mm²', '米', '机电材料', 2800, 500, '现场机电安装电缆'
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM pw_material m
  WHERE m.tenant_id = 1 AND m.material_code = seed.material_code
);

INSERT INTO pw_supplier (id, tenant_id, supplier_code, supplier_name, contact_name, contact_phone,
                         status, remark, create_by, create_time, create_dept, update_by, update_time, del_flag)
SELECT seed.id, 1, seed.supplier_code, seed.supplier_name, seed.contact_name, seed.contact_phone,
       'ENABLED', seed.remark, 1, NOW(), 1, 1, NOW(), '0'
FROM (
  SELECT 1950000000000009211 AS id, 'SUP-001' AS supplier_code, '华北建材供应有限公司' AS supplier_name,
         '李经理' AS contact_name, '13800000001' AS contact_phone, '水泥、钢材长期合作供应商' AS remark
  UNION ALL
  SELECT 1950000000000009212, 'SUP-002', '城建物资贸易有限公司', '王经理', '13800000002', '机电材料供应商'
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM pw_supplier s
  WHERE s.tenant_id = 1 AND s.supplier_code = seed.supplier_code
);

INSERT INTO pw_warehouse (id, tenant_id, warehouse_code, warehouse_name, warehouse_type, project_name, location,
                          related_contract_no, status, remark, create_by, create_time, create_dept, update_by, update_time, del_flag)
SELECT seed.id, 1, seed.warehouse_code, seed.warehouse_name, seed.warehouse_type, seed.project_name, seed.location,
       seed.related_contract_no, 'ENABLED', seed.remark, 1, NOW(), 1, 1, NOW(), '0'
FROM (
  SELECT 1950000000000009221 AS id, 'WH-001' AS warehouse_code, '主仓库' AS warehouse_name,
         'CENTER' AS warehouse_type, '集团集中采购' AS project_name, '园区北侧 1 号库' AS location,
         'HT-2026-CG-001' AS related_contract_no, '集中采购和周转物资主仓' AS remark
  UNION ALL
  SELECT 1950000000000009222, 'WH-002', '城东项目现场仓', 'SITE', '城东安置房项目',
         '城东安置房项目施工现场', 'HT-2026-XM-018', '项目现场领用仓'
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM pw_warehouse w
  WHERE w.tenant_id = 1 AND w.warehouse_code = seed.warehouse_code
);

SET @pw_mat_cement_id := (SELECT id FROM pw_material WHERE tenant_id = 1 AND material_code = 'MAT-001' LIMIT 1);
SET @pw_mat_steel_id := (SELECT id FROM pw_material WHERE tenant_id = 1 AND material_code = 'MAT-002' LIMIT 1);
SET @pw_mat_cable_id := (SELECT id FROM pw_material WHERE tenant_id = 1 AND material_code = 'MAT-003' LIMIT 1);
SET @pw_sup_build_id := (SELECT id FROM pw_supplier WHERE tenant_id = 1 AND supplier_code = 'SUP-001' LIMIT 1);
SET @pw_sup_trade_id := (SELECT id FROM pw_supplier WHERE tenant_id = 1 AND supplier_code = 'SUP-002' LIMIT 1);
SET @pw_wh_main_id := (SELECT id FROM pw_warehouse WHERE tenant_id = 1 AND warehouse_code = 'WH-001' LIMIT 1);
SET @pw_wh_site_id := (SELECT id FROM pw_warehouse WHERE tenant_id = 1 AND warehouse_code = 'WH-002' LIMIT 1);

INSERT INTO pw_supplier_material (id, tenant_id, supplier_id, supplier_name, material_id, material_code, material_name,
                                  spec_model, unit, quote_price_cent, last_price_cent, effective_date, status, remark,
                                  create_by, create_time, create_dept, update_by, update_time, del_flag)
SELECT seed.id, 1, seed.supplier_id, seed.supplier_name, seed.material_id, seed.material_code, seed.material_name,
       seed.spec_model, seed.unit, seed.quote_price_cent, seed.last_price_cent, seed.effective_date, 'ENABLED', seed.remark,
       1, NOW(), 1, 1, NOW(), '0'
FROM (
  SELECT 1950000000000009231 AS id, @pw_sup_build_id AS supplier_id, '华北建材供应有限公司' AS supplier_name,
         @pw_mat_cement_id AS material_id, 'MAT-001' AS material_code, 'P.O 42.5 水泥' AS material_name,
         '袋装 50kg' AS spec_model, '吨' AS unit, 36500 AS quote_price_cent, 37200 AS last_price_cent,
         DATE_ADD(CURRENT_DATE, INTERVAL 60 DAY) AS effective_date, '主力水泥报价' AS remark
  UNION ALL
  SELECT 1950000000000009232, @pw_sup_build_id, '华北建材供应有限公司',
         @pw_mat_steel_id, 'MAT-002', 'HRB400 螺纹钢', 'Φ16', '吨', 418000, 421000,
         DATE_ADD(CURRENT_DATE, INTERVAL 45 DAY), '钢材月度报价'
  UNION ALL
  SELECT 1950000000000009233, @pw_sup_trade_id, '城建物资贸易有限公司',
         @pw_mat_cable_id, 'MAT-003', 'YJV 电力电缆', '3*16mm²', '米', 2750, 2820,
         DATE_ADD(CURRENT_DATE, INTERVAL 90 DAY), '机电材料报价'
) seed
WHERE seed.supplier_id IS NOT NULL
  AND seed.material_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM pw_supplier_material sm
    WHERE sm.tenant_id = 1
      AND sm.supplier_id = seed.supplier_id
      AND sm.material_id = seed.material_id
  );

INSERT INTO pw_purchase_order (id, tenant_id, purchase_no, project_name, delivery_type, warehouse_id, warehouse_name,
                               purchaser_name, purchase_date, supplier_id, supplier_name, supplier_contact, supplier_phone,
                               purchase_amount_cent, order_status, attachment_ids, remark,
                               create_by, create_time, create_dept, update_by, update_time, del_flag)
SELECT seed.id, 1, seed.purchase_no, seed.project_name, seed.delivery_type, seed.warehouse_id, seed.warehouse_name,
       seed.purchaser_name, seed.purchase_date, seed.supplier_id, seed.supplier_name, seed.supplier_contact, seed.supplier_phone,
       seed.purchase_amount_cent, seed.order_status, seed.attachment_ids, seed.remark,
       1, NOW(), 1, 1, NOW(), '0'
FROM (
  SELECT 1950000000000009241 AS id, 'PO-20260703-0001' AS purchase_no, '城东安置房项目' AS project_name,
         'WAREHOUSE' AS delivery_type, @pw_wh_main_id AS warehouse_id, '主仓库' AS warehouse_name,
         '赵采购' AS purchaser_name, CURRENT_DATE AS purchase_date, @pw_sup_build_id AS supplier_id,
         '华北建材供应有限公司' AS supplier_name, '李经理' AS supplier_contact, '13800000001' AS supplier_phone,
         1570000 AS purchase_amount_cent, 'APPROVED' AS order_status, NULL AS attachment_ids,
         '原型验收采购单，审批通过后可执行入库动作' AS remark
  UNION ALL
  SELECT 1950000000000009242, 'PO-20260703-0002', '城东安置房项目', 'WAREHOUSE', @pw_wh_site_id, '城东项目现场仓',
         '赵采购', CURRENT_DATE, @pw_sup_trade_id, '城建物资贸易有限公司', '王经理', '13800000002',
         550000, 'DRAFT', NULL, '待提交采购申请样例'
) seed
WHERE seed.warehouse_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM pw_purchase_order p
    WHERE p.tenant_id = 1 AND p.purchase_no = seed.purchase_no
  );

INSERT INTO pw_purchase_order_item (id, tenant_id, purchase_id, material_id, material_code, material_name, spec_model, unit,
                                    quantity, cost_price_cent, deal_price_cent, amount_cent, remark,
                                    create_by, create_time, create_dept, update_by, update_time, del_flag)
SELECT seed.id, 1, seed.purchase_id, seed.material_id, seed.material_code, seed.material_name, seed.spec_model, seed.unit,
       seed.quantity, seed.cost_price_cent, seed.deal_price_cent, seed.amount_cent, seed.remark,
       1, NOW(), 1, 1, NOW(), '0'
FROM (
  SELECT 1950000000000009251 AS id, 1950000000000009241 AS purchase_id, @pw_mat_cement_id AS material_id,
         'MAT-001' AS material_code, 'P.O 42.5 水泥' AS material_name, '袋装 50kg' AS spec_model, '吨' AS unit,
         20.000 AS quantity, 37200 AS cost_price_cent, 36500 AS deal_price_cent, 730000 AS amount_cent, '主仓补货' AS remark
  UNION ALL
  SELECT 1950000000000009252, 1950000000000009241, @pw_mat_steel_id,
         'MAT-002', 'HRB400 螺纹钢', 'Φ16', '吨', 2.000, 421000, 420000, 840000, '主体结构用钢'
  UNION ALL
  SELECT 1950000000000009253, 1950000000000009242, @pw_mat_cable_id,
         'MAT-003', 'YJV 电力电缆', '3*16mm²', '米', 200.000, 2820, 2750, 550000, '现场机电材料'
) seed
WHERE seed.material_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM pw_purchase_order_item i
    WHERE i.tenant_id = 1 AND i.id = seed.id
  );

INSERT INTO pw_outbound_order (id, tenant_id, outbound_no, warehouse_id, warehouse_name, applicant_name, outbound_date,
                               outbound_reason, order_status, remark,
                               create_by, create_time, create_dept, update_by, update_time, del_flag)
SELECT seed.id, 1, seed.outbound_no, seed.warehouse_id, seed.warehouse_name, seed.applicant_name, seed.outbound_date,
       seed.outbound_reason, seed.order_status, seed.remark,
       1, NOW(), 1, 1, NOW(), '0'
FROM (
  SELECT 1950000000000009261 AS id, 'OUT-20260703-0001' AS outbound_no, @pw_wh_main_id AS warehouse_id,
         '主仓库' AS warehouse_name, '陈施工' AS applicant_name, CURRENT_DATE AS outbound_date,
         '城东项目现场领用' AS outbound_reason, 'APPROVED' AS order_status, '审批通过出库样例' AS remark
  UNION ALL
  SELECT 1950000000000009262, 'OUT-20260703-0002', @pw_wh_main_id, '主仓库', '陈施工',
         CURRENT_DATE, '钢筋班组领用', 'SUBMITTED', '提交审批并锁定库存样例'
) seed
WHERE seed.warehouse_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM pw_outbound_order o
    WHERE o.tenant_id = 1 AND o.outbound_no = seed.outbound_no
  );

INSERT INTO pw_outbound_order_item (id, tenant_id, outbound_id, warehouse_id, material_id, material_code, material_name,
                                    spec_model, unit, stock_quantity, outbound_quantity, lock_code, remark,
                                    create_by, create_time, create_dept, update_by, update_time, del_flag)
SELECT seed.id, 1, seed.outbound_id, seed.warehouse_id, seed.material_id, seed.material_code, seed.material_name,
       seed.spec_model, seed.unit, seed.stock_quantity, seed.outbound_quantity, seed.lock_code, seed.remark,
       1, NOW(), 1, 1, NOW(), '0'
FROM (
  SELECT 1950000000000009271 AS id, 1950000000000009261 AS outbound_id, @pw_wh_main_id AS warehouse_id,
         @pw_mat_cement_id AS material_id, 'MAT-001' AS material_code, 'P.O 42.5 水泥' AS material_name,
         '袋装 50kg' AS spec_model, '吨' AS unit, 150.000 AS stock_quantity, 5.000 AS outbound_quantity,
         NULL AS lock_code, '现场领用水泥' AS remark
  UNION ALL
  SELECT 1950000000000009272, 1950000000000009261, @pw_wh_main_id, @pw_mat_cable_id,
         'MAT-003', 'YJV 电力电缆', '3*16mm²', '米', 3000.000, 200.000, NULL, '现场机电安装领用'
  UNION ALL
  SELECT 1950000000000009273, 1950000000000009262, @pw_wh_main_id, @pw_mat_cement_id,
         'MAT-001', 'P.O 42.5 水泥', '袋装 50kg', '吨', 145.000, 12.000, '1950000000000009273', '审批中锁定水泥'
) seed
WHERE seed.material_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM pw_outbound_order_item i
    WHERE i.tenant_id = 1 AND i.id = seed.id
  );

INSERT INTO pw_transfer_order (id, tenant_id, transfer_no, from_warehouse_id, from_warehouse_name, to_warehouse_id,
                               to_warehouse_name, transfer_person_name, transfer_date, transfer_reason, order_status, remark,
                               create_by, create_time, create_dept, update_by, update_time, del_flag)
SELECT seed.id, 1, seed.transfer_no, seed.from_warehouse_id, seed.from_warehouse_name, seed.to_warehouse_id,
       seed.to_warehouse_name, seed.transfer_person_name, seed.transfer_date, seed.transfer_reason, seed.order_status, seed.remark,
       1, NOW(), 1, 1, NOW(), '0'
FROM (
  SELECT 1950000000000009281 AS id, 'TRF-20260703-0001' AS transfer_no,
         @pw_wh_main_id AS from_warehouse_id, '主仓库' AS from_warehouse_name,
         @pw_wh_site_id AS to_warehouse_id, '城东项目现场仓' AS to_warehouse_name,
         '周库管' AS transfer_person_name, CURRENT_DATE AS transfer_date,
         '项目现场补仓' AS transfer_reason, 'APPROVED' AS order_status, '审批通过调拨样例' AS remark
) seed
WHERE seed.from_warehouse_id IS NOT NULL
  AND seed.to_warehouse_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM pw_transfer_order t
    WHERE t.tenant_id = 1 AND t.transfer_no = seed.transfer_no
  );

INSERT INTO pw_transfer_order_item (id, tenant_id, transfer_id, material_id, material_code, material_name, spec_model,
                                    unit, current_stock_quantity, transfer_quantity, remark,
                                    create_by, create_time, create_dept, update_by, update_time, del_flag)
SELECT seed.id, 1, seed.transfer_id, seed.material_id, seed.material_code, seed.material_name, seed.spec_model,
       seed.unit, seed.current_stock_quantity, seed.transfer_quantity, seed.remark,
       1, NOW(), 1, 1, NOW(), '0'
FROM (
  SELECT 1950000000000009291 AS id, 1950000000000009281 AS transfer_id, @pw_mat_cement_id AS material_id,
         'MAT-001' AS material_code, 'P.O 42.5 水泥' AS material_name, '袋装 50kg' AS spec_model,
         '吨' AS unit, 145.000 AS current_stock_quantity, 8.000 AS transfer_quantity, '主仓调拨至现场仓' AS remark
) seed
WHERE seed.material_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM pw_transfer_order_item i
    WHERE i.tenant_id = 1 AND i.id = seed.id
  );

INSERT INTO ai_business_quantity_balance (id, tenant_id, account_code, item_code, dimension_key, quantity,
                                          locked_quantity, status, remark, create_by, create_time, create_dept,
                                          update_by, update_time)
SELECT seed.id, 1, seed.account_code, seed.item_code, '', seed.quantity, seed.locked_quantity, 1, seed.remark,
       1, NOW(), 1, 1, NOW()
FROM (
  SELECT 1950000000000009301 AS id, CAST(@pw_wh_main_id AS CHAR) AS account_code, CAST(@pw_mat_cement_id AS CHAR) AS item_code,
         150 AS quantity, 12 AS locked_quantity, '主仓水泥演示余额' AS remark
  UNION ALL
  SELECT 1950000000000009302, CAST(@pw_wh_main_id AS CHAR), CAST(@pw_mat_steel_id AS CHAR), 10, 0, '主仓钢材演示余额'
  UNION ALL
  SELECT 1950000000000009303, CAST(@pw_wh_main_id AS CHAR), CAST(@pw_mat_cable_id AS CHAR), 3000, 0, '主仓电缆演示余额'
  UNION ALL
  SELECT 1950000000000009304, CAST(@pw_wh_site_id AS CHAR), CAST(@pw_mat_cement_id AS CHAR), 38, 0, '现场仓水泥演示余额'
  UNION ALL
  SELECT 1950000000000009305, CAST(@pw_wh_site_id AS CHAR), CAST(@pw_mat_cable_id AS CHAR), 600, 0, '现场仓电缆演示余额'
) seed
WHERE seed.account_code IS NOT NULL
  AND seed.item_code IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM ai_business_quantity_balance b
    WHERE b.tenant_id = 1
      AND b.account_code = seed.account_code
      AND b.item_code = seed.item_code
      AND b.dimension_key = ''
  );

INSERT INTO ai_business_quantity_ledger (id, tenant_id, operation_type, account_code, item_code, dimension_key,
                                         quantity_delta, balance_quantity, locked_quantity, target_account_code,
                                         target_item_code, target_dimension_key, source_object_code, source_record_id,
                                         source_detail_id, lock_id, correlation_id, idempotency_key, remark, extra_data,
                                         create_by, create_time, create_dept, update_by, update_time)
SELECT seed.id, 1, seed.operation_type, seed.account_code, seed.item_code, '', seed.quantity_delta,
       seed.balance_quantity, seed.locked_quantity, seed.target_account_code, seed.target_item_code,
       seed.target_dimension_key, seed.source_object_code, seed.source_record_id, seed.source_detail_id,
       seed.lock_id, seed.correlation_id, seed.idempotency_key, seed.remark, JSON_OBJECT('demo', true),
       1, seed.create_time, 1, 1, seed.create_time
FROM (
  SELECT 1950000000000009311 AS id, 'INBOUND' AS operation_type, CAST(@pw_wh_main_id AS CHAR) AS account_code,
         CAST(@pw_mat_cement_id AS CHAR) AS item_code, 20 AS quantity_delta, 150 AS balance_quantity, 0 AS locked_quantity,
         NULL AS target_account_code, NULL AS target_item_code, NULL AS target_dimension_key,
         'PW_PURCHASE_ORDER' AS source_object_code, '1950000000000009241' AS source_record_id,
         '1950000000000009251' AS source_detail_id, NULL AS lock_id, 'pw-demo-inbound-001' AS correlation_id,
         'pw-demo-ledger-inbound-001' AS idempotency_key, '采购入库演示流水' AS remark,
         DATE_SUB(NOW(), INTERVAL 3 DAY) AS create_time
  UNION ALL
  SELECT 1950000000000009312, 'COMMIT', CAST(@pw_wh_main_id AS CHAR), CAST(@pw_mat_cement_id AS CHAR),
         -5, 145, 0, NULL, NULL, NULL, 'PW_OUTBOUND_ORDER', '1950000000000009261',
         '1950000000000009271', NULL, 'pw-demo-outbound-001', 'pw-demo-ledger-outbound-001',
         '出库扣减演示流水', DATE_SUB(NOW(), INTERVAL 2 DAY)
  UNION ALL
  SELECT 1950000000000009313, 'TRANSFER', CAST(@pw_wh_main_id AS CHAR), CAST(@pw_mat_cement_id AS CHAR),
         -8, 137, 0, CAST(@pw_wh_site_id AS CHAR), CAST(@pw_mat_cement_id AS CHAR), '',
         'PW_TRANSFER_ORDER', '1950000000000009281', '1950000000000009291', NULL,
         'pw-demo-transfer-001', 'pw-demo-ledger-transfer-001', '调拨转出演示流水', DATE_SUB(NOW(), INTERVAL 1 DAY)
  UNION ALL
  SELECT 1950000000000009314, 'LOCK', CAST(@pw_wh_main_id AS CHAR), CAST(@pw_mat_cement_id AS CHAR),
         12, 150, 12, NULL, NULL, NULL, 'PW_OUTBOUND_ORDER', '1950000000000009262',
         '1950000000000009273', 1950000000000009321, 'pw-demo-lock-001', 'pw-demo-ledger-lock-001',
         '出库审批中锁定演示流水', NOW()
) seed
WHERE seed.account_code IS NOT NULL
  AND seed.item_code IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM ai_business_quantity_ledger l
    WHERE l.tenant_id = 1 AND l.idempotency_key = seed.idempotency_key
  );

INSERT INTO ai_business_quantity_lock (id, tenant_id, lock_code, account_code, item_code, dimension_key,
                                       lock_quantity, released_quantity, committed_quantity, lock_status,
                                       source_object_code, source_record_id, source_detail_id, correlation_id,
                                       idempotency_key, remark, create_by, create_time, create_dept, update_by, update_time)
SELECT 1950000000000009321, 1, '1950000000000009273', CAST(@pw_wh_main_id AS CHAR), CAST(@pw_mat_cement_id AS CHAR), '',
       12, 0, 0, 'LOCKED', 'PW_OUTBOUND_ORDER', '1950000000000009262', '1950000000000009273',
       'pw-demo-lock-001', 'pw-demo-lock-001', '出库审批中锁定演示数据', 1, NOW(), 1, 1, NOW()
WHERE @pw_wh_main_id IS NOT NULL
  AND @pw_mat_cement_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM ai_business_quantity_lock l
    WHERE l.tenant_id = 1 AND l.idempotency_key = 'pw-demo-lock-001'
  );

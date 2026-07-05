-- Fix the sample purchase order App Center designer route without rewriting V1.0.83.

UPDATE ai_business_app
SET options = JSON_SET(
    COALESCE(options, JSON_OBJECT()),
    '$.flowConfigUrl',
    '/app-center/object/sample_purchase_order/designer?panel=flow-app&codeApp=1&name=采购申请'
  ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND app_code = 'PURCHASE_ORDER_APPROVAL_TEST'
  AND (
    JSON_UNQUOTE(JSON_EXTRACT(options, '$.flowConfigUrl')) IS NULL
    OR JSON_UNQUOTE(JSON_EXTRACT(options, '$.flowConfigUrl')) <> '/app-center/object/sample_purchase_order/designer?panel=flow-app&codeApp=1&name=采购申请'
  );

-- Configure procurement quantity action item fallback fields without hard-coding business fields in Java.

SET @purchase_action_path := (
    SELECT JSON_UNQUOTE(JSON_SEARCH(CAST(o.designer_options AS JSON), 'one', 'inbound_purchase_stock', NULL, '$.actions[*].actionCode'))
    FROM ai_business_object o
    WHERE o.tenant_id = 1
      AND o.object_code = 'PW_PURCHASE_ORDER'
      AND JSON_VALID(o.designer_options)
    LIMIT 1
);

SET @purchase_action_index := CASE
    WHEN @purchase_action_path IS NULL THEN NULL
    ELSE SUBSTRING_INDEX(SUBSTRING_INDEX(@purchase_action_path, '[', -1), ']', 1)
END;

SET @purchase_item_fallback_path := CASE
    WHEN @purchase_action_index IS NULL THEN NULL
    ELSE CONCAT('$.actions[', @purchase_action_index, '].actionConfig.steps[0].stepConfig.steps[0].stepConfig.itemCodeFallbackFields')
END;

UPDATE ai_business_object o
SET o.designer_options = JSON_SET(
        CAST(o.designer_options AS JSON),
        @purchase_item_fallback_path,
        JSON_ARRAY('item.materialId', 'item.materialCode')
    )
WHERE o.tenant_id = 1
  AND o.object_code = 'PW_PURCHASE_ORDER'
  AND JSON_VALID(o.designer_options)
  AND @purchase_item_fallback_path IS NOT NULL;

package com.mdframe.forge.plugin.generator.service.lowcode;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageModelRef;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageZone;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeRelationSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeRuntimeConfig;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 将低代码业务协议转换为 AiCrudPage 运行时配置。
 */
@Service
@RequiredArgsConstructor
public class LowcodeRuntimeConfigBuilder {

    private static final String MASTER_DETAIL_LAYOUT = "master-detail-crud";
    private static final Set<String> SYSTEM_FIELD_NAMES = Set.of(
            "id", "tenantId", "createBy", "createTime", "createDept", "updateBy", "updateTime", "delFlag"
    );
    private static final Set<String> SYSTEM_COLUMN_NAMES = Set.of(
            "id", "tenant_id", "create_by", "create_time", "create_dept", "update_by", "update_time", "del_flag"
    );

    private final ObjectMapper objectMapper;
    private final LowcodeSchemaValidator schemaValidator;

    public LowcodeRuntimeConfig buildRuntimeConfig(String configKey,
                                                   LowcodeModelSchema modelSchema,
                                                   LowcodePageSchema pageSchema) {
        if (StringUtils.isBlank(configKey)) {
            throw new BusinessException("configKey不能为空");
        }
        schemaValidator.validatePage(pageSchema, modelSchema);

        LowcodeRuntimeConfig runtimeConfig = new LowcodeRuntimeConfig();
        runtimeConfig.setConfigKey(configKey);
        runtimeConfig.setTableName(modelSchema.getTableName());
        runtimeConfig.setTableComment(modelSchema.getBusinessName());
        runtimeConfig.setLayoutType(StringUtils.defaultIfBlank(pageSchema.getLayoutType(), "simple-crud"));

        try {
            runtimeConfig.setSearchSchema(objectMapper.writeValueAsString(buildSearchSchema(configKey, modelSchema, pageSchema)));
            runtimeConfig.setColumnsSchema(objectMapper.writeValueAsString(buildColumnsSchema(modelSchema, pageSchema)));
            runtimeConfig.setEditSchema(objectMapper.writeValueAsString(buildEditSchema(configKey, modelSchema, pageSchema)));
            runtimeConfig.setApiConfig(objectMapper.writeValueAsString(buildApiConfig(configKey, modelSchema, pageSchema)));
            runtimeConfig.setOptions(objectMapper.writeValueAsString(buildOptions(modelSchema, pageSchema)));
            runtimeConfig.setDictConfig(objectMapper.writeValueAsString(buildDictConfig(modelSchema)));
            runtimeConfig.setDesensitizeConfig(objectMapper.writeValueAsString(buildDesensitizeConfig(modelSchema)));
            runtimeConfig.setEncryptConfig(objectMapper.writeValueAsString(buildEncryptConfig(modelSchema)));
            runtimeConfig.setTransConfig(objectMapper.writeValueAsString(buildTransConfig(modelSchema)));
            return runtimeConfig;
        } catch (Exception e) {
            throw new BusinessException("低代码运行时配置生成失败: " + e.getMessage());
        }
    }

    private List<Map<String, Object>> buildSearchSchema(String configKey, LowcodeModelSchema modelSchema, LowcodePageSchema pageSchema) {
        List<Map<String, Object>> fields = resolveFields(modelSchema, pageSchema, "search", field -> Boolean.TRUE.equals(field.getSearchable()))
                .stream()
                .map(field -> buildSearchField(field, resolveFieldSetting(pageSchema, "search", field.getField())))
                .collect(Collectors.toCollection(ArrayList::new));
        appendTreeRuntimeField(fields, modelSchema, pageSchema, "search");
        decorateTreeRuntimeFields(fields, configKey, modelSchema, pageSchema);
        return fields;
    }

    private List<Map<String, Object>> buildColumnsSchema(LowcodeModelSchema modelSchema, LowcodePageSchema pageSchema) {
        List<Map<String, Object>> columns = resolveFields(modelSchema, pageSchema, "table",
                field -> field.getListVisible() == null || Boolean.TRUE.equals(field.getListVisible()))
                .stream()
                .map(field -> buildTableColumn(field, resolveFieldSetting(pageSchema, "table", field.getField())))
                .collect(Collectors.toCollection(ArrayList::new));

        Map<String, Object> actions = new LinkedHashMap<>();
        actions.put("key", "actions");
        actions.put("title", "操作");
        actions.put("dataIndex", "actions");
        List<Map<String, Object>> rowActions = buildRowActions(pageSchema);
        actions.put("width", Math.max(180, rowActions.size() * 58));
        actions.put("fixed", "right");
        actions.put("actions", rowActions);
        actions.put("maxActionButtons", 3);
        columns.add(actions);
        return columns;
    }

    private List<Map<String, Object>> buildEditSchema(String configKey, LowcodeModelSchema modelSchema, LowcodePageSchema pageSchema) {
        Set<String> childFieldRefs = isMasterDetailRuntime(pageSchema) ? buildChildFieldRefs(pageSchema) : Set.of();
        List<LowcodeFieldSchema> orderedFields = sortByCanvasOrder(
                resolveFields(modelSchema, pageSchema, "edit",
                        field -> field.getFormVisible() == null || Boolean.TRUE.equals(field.getFormVisible())),
                pageSchema,
                "edit"
        );
        List<Map<String, Object>> fields = orderedFields
                .stream()
                .filter(field -> !childFieldRefs.contains(field.getField()))
                .map(field -> buildEditField(field, resolveEditFieldSetting(pageSchema, field.getField())))
                .collect(Collectors.toCollection(ArrayList::new));
        appendTreeRuntimeField(fields, modelSchema, pageSchema, "edit");
        decorateTreeRuntimeFields(fields, configKey, modelSchema, pageSchema);
        return fields;
    }

    private Map<String, String> buildApiConfig(String configKey, LowcodeModelSchema modelSchema, LowcodePageSchema pageSchema) {
        Map<String, String> apiConfig = new LinkedHashMap<>();
        apiConfig.put("list", "get@/ai/crud/" + configKey + "/page");
        if (isTreeRuntime(modelSchema, pageSchema)) {
            apiConfig.put("tree", "get@/ai/crud/" + configKey + "/tree");
        }
        apiConfig.put("detail", "get@/ai/crud/" + configKey + "/:id");
        apiConfig.put("create", "post@/ai/crud/" + configKey);
        apiConfig.put("update", "put@/ai/crud/" + configKey);
        apiConfig.put("delete", "delete@/ai/crud/" + configKey + "/:id");
        apiConfig.put("import", "post@/ai/crud/" + configKey + "/import");
        apiConfig.put("export", "post@/ai/crud/" + configKey + "/export");
        apiConfig.put("importTemplate", "get@/ai/crud/" + configKey + "/import-template");
        return apiConfig;
    }

    private Map<String, Object> buildOptions(LowcodeModelSchema modelSchema, LowcodePageSchema pageSchema) {
        Map<String, Object> options = new LinkedHashMap<>();
        options.put("modalType", "drawer");
        options.put("modalWidth", "800px");
        options.put("searchGridCols", 4);
        options.put("editGridCols", resolveEditGridCols(pageSchema));

        LowcodePageZone tableZone = findZone(pageSchema, "table");
        if (tableZone != null && tableZone.getProps() != null) {
            copyOption(tableZone.getProps(), options, "showImport");
            copyOption(tableZone.getProps(), options, "showExport");
            copyOption(tableZone.getProps(), options, "hideBatchDelete");
            copyOption(tableZone.getProps(), options, "enableCustomQuery");
        }
        options.put("toolbarActions", resolveCustomActions(pageSchema, "toolbar"));
        options.put("rowActions", resolveCustomActions(pageSchema, "row"));
        options.put("joinConfig", buildJoinConfig(modelSchema, pageSchema));
        if (isMasterDetailRuntime(pageSchema)) {
            options.put("modalWidth", "1080px");
            options.put("masterDetailConfig", buildMasterDetailConfig(modelSchema, pageSchema));
        }
        if (isTreeRuntime(modelSchema, pageSchema)) {
            options.put("treeConfig", buildTreeConfig(modelSchema, pageSchema, extractTreeConfigOverrides(pageSchema)));
        }
        return options;
    }

    private int resolveEditGridCols(LowcodePageSchema pageSchema) {
        int cols = 1;
        LowcodePageZone editZone = findZone(pageSchema, "edit");
        if (editZone != null && editZone.getProps() != null) {
            Integer configuredCols = integerValue(editZone.getProps().get("editGridCols"));
            if (configuredCols != null && configuredCols > 0) {
                return Math.max(1, Math.min(3, configuredCols));
            }
            cols = Math.max(cols, resolveCanvasGridCols(editZone));
        }
        for (Map<String, Object> rule : extractFormRules(pageSchema)) {
            Object col = rule.get("col");
            if (!(col instanceof Map<?, ?> colMap)) {
                continue;
            }
            Integer span = integerValue(colMap.get("span"));
            if (span == null || span <= 0 || span >= 24) {
                continue;
            }
            cols = Math.max(cols, Math.min(3, Math.max(1, (int) Math.ceil(24.0 / span))));
        }
        return cols;
    }

    @SuppressWarnings("unchecked")
    private int resolveCanvasGridCols(LowcodePageZone editZone) {
        List<Map<String, Object>> items = extractCanvasItems(editZone);
        if (items.isEmpty()) {
            return 1;
        }
        List<Integer> columns = new ArrayList<>();
        items.stream()
                .filter(item -> StringUtils.isNotBlank(text(item.get("fieldRef"))))
                .sorted(Comparator.comparingInt(item -> intValue(item.get("x"), 0)))
                .forEach(item -> {
                    int x = intValue(item.get("x"), 0);
                    boolean exists = columns.stream().anyMatch(columnX -> Math.abs(columnX - x) < 80);
                    if (!exists) {
                        columns.add(x);
                    }
                });
        return Math.max(1, Math.min(3, columns.isEmpty() ? 1 : columns.size()));
    }

    private List<LowcodeFieldSchema> sortByCanvasOrder(List<LowcodeFieldSchema> fields,
                                                       LowcodePageSchema pageSchema,
                                                       String zoneKey) {
        if (fields == null || fields.size() <= 1) {
            return fields == null ? List.of() : fields;
        }
        LowcodePageZone zone = findZone(pageSchema, zoneKey);
        List<Map<String, Object>> items = extractCanvasItems(zone);
        if (items.isEmpty()) {
            return fields;
        }
        Map<String, LowcodeFieldSchema> fieldMap = fields.stream()
                .collect(Collectors.toMap(
                        LowcodeFieldSchema::getField,
                        field -> field,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        List<LowcodeFieldSchema> ordered = new ArrayList<>();
        items.stream()
                .sorted(this::compareCanvasItemPosition)
                .map(item -> text(item.get("fieldRef")))
                .filter(StringUtils::isNotBlank)
                .distinct()
                .forEach(fieldRef -> {
                    LowcodeFieldSchema field = fieldMap.remove(fieldRef);
                    if (field != null) {
                        ordered.add(field);
                    }
                });
        ordered.addAll(fieldMap.values());
        return ordered;
    }

    private int compareCanvasItemPosition(Map<String, Object> left, Map<String, Object> right) {
        int leftRow = Math.round(intValue(left.get("y"), 0) / 16.0f);
        int rightRow = Math.round(intValue(right.get("y"), 0) / 16.0f);
        if (leftRow != rightRow) {
            return Integer.compare(leftRow, rightRow);
        }
        int xCompare = Integer.compare(intValue(left.get("x"), 0), intValue(right.get("x"), 0));
        if (xCompare != 0) {
            return xCompare;
        }
        return Integer.compare(intValue(left.get("zIndex"), 0), intValue(right.get("zIndex"), 0));
    }

    private List<Map<String, Object>> buildJoinConfig(LowcodeModelSchema modelSchema, LowcodePageSchema pageSchema) {
        if (pageSchema == null || pageSchema.getModelRefs() == null || pageSchema.getModelRefs().size() <= 1) {
            return List.of();
        }
        LowcodePageModelRef primaryRef = pageSchema.getModelRefs().stream()
                .filter(ref -> Boolean.TRUE.equals(ref.getPrimary()))
                .findFirst()
                .orElse(null);
        String fallbackPrimaryCode = primaryRef == null
                ? modelSchema.getObject() == null ? null : modelSchema.getObject().getCode()
                : primaryRef.getModelCode();
        String primaryModelCode = StringUtils.defaultIfBlank(pageSchema.getPrimaryModelCode(), fallbackPrimaryCode);
        if (StringUtils.isBlank(primaryModelCode)) {
            return List.of();
        }
        List<LowcodeRelationSchema> primaryRelations = primaryRef != null && primaryRef.getRelations() != null
                ? primaryRef.getRelations()
                : modelSchema.getRelations();
        List<Map<String, Object>> result = new ArrayList<>();
        for (LowcodePageModelRef ref : pageSchema.getModelRefs()) {
            if (ref == null || Boolean.TRUE.equals(ref.getPrimary()) || StringUtils.isBlank(ref.getModelCode())) {
                continue;
            }
            LowcodeRelationSchema relation = resolveRuntimeRelation(primaryModelCode, ref, primaryRelations);
            if (relation == null) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("modelCode", ref.getModelCode());
            item.put("modelName", ref.getModelName());
            item.put("tableName", ref.getTableName());
            item.put("sourceField", relation.getSourceField());
            item.put("targetField", relation.getTargetField());
            item.put("targetObjectCode", relation.getTargetObjectCode());
            item.put("relationType", StringUtils.defaultIfBlank(relation.getRelationType(), "REFERENCE"));
            result.add(item);
        }
        return result;
    }

    private Map<String, Object> buildMasterDetailConfig(LowcodeModelSchema modelSchema, LowcodePageSchema pageSchema) {
        Map<String, Object> config = new LinkedHashMap<>();
        LowcodePageModelRef primaryRef = resolvePrimaryRef(modelSchema, pageSchema);
        String primaryModelCode = primaryRef == null
                ? modelSchema.getObject() == null ? null : modelSchema.getObject().getCode()
                : primaryRef.getModelCode();
        primaryModelCode = StringUtils.defaultIfBlank(pageSchema.getPrimaryModelCode(), primaryModelCode);

        Map<String, Object> primary = new LinkedHashMap<>();
        primary.put("modelCode", primaryModelCode);
        primary.put("modelName", primaryRef == null ? modelSchema.getBusinessName() : primaryRef.getModelName());
        primary.put("tableName", modelSchema.getTableName());
        primary.put("keyField", "id");
        config.put("primary", primary);

        List<Map<String, Object>> children = new ArrayList<>();
        if (StringUtils.isBlank(primaryModelCode) || pageSchema.getModelRefs() == null) {
            config.put("children", children);
            return config;
        }

        List<LowcodeRelationSchema> primaryRelations = primaryRef != null && primaryRef.getRelations() != null
                ? primaryRef.getRelations()
                : modelSchema.getRelations();
        Set<String> selectedEditRefs = resolveSelectedEditRefs(pageSchema);
        for (LowcodePageModelRef ref : pageSchema.getModelRefs()) {
            if (ref == null || Boolean.TRUE.equals(ref.getPrimary()) || StringUtils.isBlank(ref.getModelCode())) {
                continue;
            }
            LowcodeRelationSchema relation = resolveRuntimeRelation(primaryModelCode, ref, primaryRelations);
            if (relation == null) {
                continue;
            }
            String childFkField = resolveChildRelationField(primaryModelCode, relation);
            List<Map<String, Object>> childFields = buildMasterDetailChildFields(ref, selectedEditRefs, childFkField);
            if (childFields.isEmpty()) {
                continue;
            }
            Map<String, Object> child = new LinkedHashMap<>();
            child.put("key", ref.getModelCode());
            child.put("modelCode", ref.getModelCode());
            child.put("modelName", ref.getModelName());
            child.put("tableName", ref.getTableName());
            child.put("relationType", StringUtils.defaultIfBlank(relation.getRelationType(), "ONE_TO_MANY"));
            child.put("sourceField", childFkField);
            child.put("targetField", resolveMainRelationField(primaryModelCode, relation));
            child.put("fields", childFields);
            children.add(child);
        }
        config.put("children", children);
        return config;
    }

    private LowcodePageModelRef resolvePrimaryRef(LowcodeModelSchema modelSchema, LowcodePageSchema pageSchema) {
        if (pageSchema == null || pageSchema.getModelRefs() == null || pageSchema.getModelRefs().isEmpty()) {
            return null;
        }
        return pageSchema.getModelRefs().stream()
                .filter(ref -> Boolean.TRUE.equals(ref.getPrimary()))
                .findFirst()
                .orElseGet(() -> {
                    String modelCode = modelSchema.getObject() == null ? null : modelSchema.getObject().getCode();
                    return pageSchema.getModelRefs().stream()
                            .filter(ref -> StringUtils.equals(ref.getModelCode(), pageSchema.getPrimaryModelCode())
                                    || StringUtils.equals(ref.getModelCode(), modelCode))
                            .findFirst()
                            .orElse(pageSchema.getModelRefs().get(0));
                });
    }

    private List<Map<String, Object>> buildMasterDetailChildFields(LowcodePageModelRef ref,
                                                                   Set<String> selectedEditRefs,
                                                                   String childFkField) {
        if (ref.getFields() == null) {
            return List.of();
        }
        List<Map<String, Object>> fields = new ArrayList<>();
        for (Map<String, Object> source : ref.getFields()) {
            LowcodeFieldSchema field = buildMasterDetailChildField(ref, source);
            if (field == null) {
                continue;
            }
            String fieldRef = StringUtils.defaultIfBlank(text(source.get("fieldRef")),
                    safeKey(ref.getModelCode()) + "__" + field.getField());
            if (!selectedEditRefs.isEmpty() && !selectedEditRefs.contains(fieldRef)) {
                continue;
            }
            if (!isChildEditFieldAllowed(field, childFkField)) {
                continue;
            }
            Map<String, Object> item = buildEditField(field);
            item.put("sourceField", field.getField());
            item.put("fieldRef", fieldRef);
            item.put("columnName", field.getColumnName());
            item.put("modelCode", ref.getModelCode());
            item.put("modelName", ref.getModelName());
            fields.add(item);
        }
        return fields;
    }

    private LowcodeFieldSchema buildMasterDetailChildField(LowcodePageModelRef ref, Map<String, Object> source) {
        LowcodeFieldSchema field = buildPageRefField(ref, source);
        if (field == null) {
            return null;
        }
        String sourceField = StringUtils.defaultIfBlank(text(source.get("sourceField")), text(source.get("field")));
        field.setField(sourceField);
        field.setLabel(StringUtils.defaultIfBlank(text(source.get("rawLabel")),
                StringUtils.defaultIfBlank(text(source.get("label")), sourceField)));
        return field;
    }

    private boolean isChildEditFieldAllowed(LowcodeFieldSchema field, String childFkField) {
        if (field == null) {
            return false;
        }
        String fieldName = field.getField();
        String columnName = field.getColumnName();
        if (StringUtils.equals(fieldName, childFkField) || StringUtils.equals(columnName, childFkField)
                || StringUtils.equals(columnName, camelToSnake(childFkField))) {
            return false;
        }
        return !isSystemField(field)
                && !SYSTEM_FIELD_NAMES.contains(fieldName)
                && !SYSTEM_COLUMN_NAMES.contains(columnName)
                && !Boolean.TRUE.equals(field.getReadonly())
                && !Boolean.TRUE.equals(field.getPrimaryKey())
                && (field.getFormVisible() == null || Boolean.TRUE.equals(field.getFormVisible()));
    }

    private Set<String> resolveSelectedEditRefs(LowcodePageSchema pageSchema) {
        LowcodePageZone editZone = findZone(pageSchema, "edit");
        if (editZone == null || Boolean.FALSE.equals(editZone.getEnabled()) || editZone.getFieldRefs() == null) {
            return Set.of();
        }
        return editZone.getFieldRefs().stream()
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String resolveChildRelationField(String primaryModelCode, LowcodeRelationSchema relation) {
        if (relation == null) {
            return null;
        }
        return primaryModelCode.equals(relation.getTargetObjectCode())
                ? relation.getSourceField()
                : relation.getTargetField();
    }

    private String resolveMainRelationField(String primaryModelCode, LowcodeRelationSchema relation) {
        if (relation == null) {
            return null;
        }
        return primaryModelCode.equals(relation.getTargetObjectCode())
                ? relation.getTargetField()
                : relation.getSourceField();
    }

    private LowcodeRelationSchema resolveRuntimeRelation(String primaryModelCode,
                                                         LowcodePageModelRef ref,
                                                         List<LowcodeRelationSchema> primaryRelations) {
        if (primaryRelations != null) {
            for (LowcodeRelationSchema relation : primaryRelations) {
                if (relation != null && ref.getModelCode().equals(relation.getTargetObjectCode())) {
                    return relation;
                }
            }
        }
        if (ref.getRelations() != null) {
            for (LowcodeRelationSchema relation : ref.getRelations()) {
                if (relation != null && primaryModelCode.equals(relation.getTargetObjectCode())) {
                    return relation;
                }
            }
        }
        return inferRuntimeRelation(primaryModelCode, ref);
    }

    private LowcodeRelationSchema inferRuntimeRelation(String primaryModelCode, LowcodePageModelRef ref) {
        if (StringUtils.isBlank(primaryModelCode) || ref == null || ref.getFields() == null) {
            return null;
        }
        String expectedCamel = snakeToCamel(primaryModelCode) + "Id";
        String expectedSnake = camelToSnake(primaryModelCode) + "_id";
        for (Map<String, Object> field : ref.getFields()) {
            String sourceField = StringUtils.defaultIfBlank(text(field.get("sourceField")), text(field.get("field")));
            String columnName = StringUtils.defaultIfBlank(text(field.get("columnName")), sourceField);
            if (expectedCamel.equals(sourceField) || expectedSnake.equals(columnName)) {
                LowcodeRelationSchema relation = new LowcodeRelationSchema();
                relation.setRelationType("ONE_TO_MANY");
                relation.setSourceField(sourceField);
                relation.setTargetObjectCode(primaryModelCode);
                relation.setTargetField("id");
                return relation;
            }
        }
        return null;
    }

    private boolean isTreeRuntime(LowcodeModelSchema modelSchema, LowcodePageSchema pageSchema) {
        String appType = StringUtils.defaultIfBlank(modelSchema.getAppType(), "SINGLE").toUpperCase(Locale.ROOT);
        return "TREE".equals(appType) || "tree-crud".equals(pageSchema.getLayoutType());
    }

    private boolean isMasterDetailRuntime(LowcodePageSchema pageSchema) {
        return pageSchema != null && MASTER_DETAIL_LAYOUT.equals(pageSchema.getLayoutType());
    }

    private Set<String> buildChildFieldRefs(LowcodePageSchema pageSchema) {
        if (pageSchema == null || pageSchema.getModelRefs() == null) {
            return Set.of();
        }
        Set<String> refs = new LinkedHashSet<>();
        for (LowcodePageModelRef ref : pageSchema.getModelRefs()) {
            if (ref == null || Boolean.TRUE.equals(ref.getPrimary()) || ref.getFields() == null) {
                continue;
            }
            for (Map<String, Object> field : ref.getFields()) {
                String sourceField = StringUtils.defaultIfBlank(text(field.get("sourceField")), text(field.get("field")));
                String fieldRef = StringUtils.defaultIfBlank(text(field.get("fieldRef")),
                        safeKey(ref.getModelCode()) + "__" + sourceField);
                if (StringUtils.isNotBlank(fieldRef)) {
                    refs.add(fieldRef);
                }
            }
        }
        return refs;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildTreeConfig(LowcodeModelSchema modelSchema,
                                                LowcodePageSchema pageSchema,
                                                Object overrides) {
        Map<String, Object> treeConfig = new LinkedHashMap<>();
        if (modelSchema.getTreeConfig() != null) {
            putIfNotBlank(treeConfig, "sourceModelCode", modelSchema.getTreeConfig().getSourceModelCode());
            putIfNotBlank(treeConfig, "sourceModelName", modelSchema.getTreeConfig().getSourceModelName());
            putIfNotBlank(treeConfig, "sourceTableName", modelSchema.getTreeConfig().getSourceTableName());
            putIfNotBlank(treeConfig, "keyField", modelSchema.getTreeConfig().getKeyField());
            putIfNotBlank(treeConfig, "parentField", modelSchema.getTreeConfig().getParentField());
            putIfNotBlank(treeConfig, "labelField", modelSchema.getTreeConfig().getLabelField());
            putIfNotBlank(treeConfig, "filterField", modelSchema.getTreeConfig().getFilterField());
            putIfNotBlank(treeConfig, "targetField", modelSchema.getTreeConfig().getTargetField());
            putIfNotBlank(treeConfig, "childrenField", modelSchema.getTreeConfig().getChildrenField());
            putIfNotBlank(treeConfig, "treeTitle", modelSchema.getTreeConfig().getTreeTitle());
        }
        if (overrides instanceof Map<?, ?> map) {
            putIfNotBlank(treeConfig, "sourceModelCode", text(map.get("sourceModelCode")));
            putIfNotBlank(treeConfig, "sourceModelName", text(map.get("sourceModelName")));
            putIfNotBlank(treeConfig, "sourceTableName", text(map.get("sourceTableName")));
            putIfNotBlank(treeConfig, "keyField", text(map.get("keyField")));
            putIfNotBlank(treeConfig, "parentField", text(map.get("parentField")));
            putIfNotBlank(treeConfig, "labelField", text(map.get("labelField")));
            putIfNotBlank(treeConfig, "filterField", text(map.get("filterField")));
            putIfNotBlank(treeConfig, "targetField", text(map.get("targetField")));
            putIfNotBlank(treeConfig, "childrenField", text(map.get("childrenField")));
            putIfNotBlank(treeConfig, "treeTitle", text(map.get("treeTitle")));
        }
        LowcodePageModelRef sourceRef = resolveTreeSourceRef(pageSchema, text(treeConfig.get("sourceModelCode")));
        if (sourceRef != null) {
            putIfNotBlank(treeConfig, "sourceModelCode", sourceRef.getModelCode());
            putIfNotBlank(treeConfig, "sourceModelName", sourceRef.getModelName());
            putIfNotBlank(treeConfig, "sourceTableName", sourceRef.getTableName());
            normalizeTreeSourceField(treeConfig, "keyField", sourceRef);
            normalizeTreeSourceField(treeConfig, "parentField", sourceRef);
            normalizeTreeSourceField(treeConfig, "labelField", sourceRef);
            normalizeTreeSourceField(treeConfig, "targetField", sourceRef);
        }
        treeConfig.putIfAbsent("keyField", "id");
        treeConfig.putIfAbsent("parentField", inferTreeParentField(modelSchema, sourceRef));
        treeConfig.putIfAbsent("labelField", inferTreeLabelField(modelSchema, sourceRef));
        LowcodeRelationSchema relation = sourceRef == null || Boolean.TRUE.equals(sourceRef.getPrimary())
                ? null
                : resolveRuntimeRelation(resolveTreePrimaryModelCode(modelSchema, pageSchema),
                sourceRef,
                resolvePrimaryTreeRelations(modelSchema, pageSchema));
        treeConfig.putIfAbsent("filterField", relation == null
                ? text(treeConfig.get("parentField"))
                : relation.getSourceField());
        treeConfig.putIfAbsent("targetField", relation == null
                ? text(treeConfig.get("keyField"))
                : relation.getTargetField());
        normalizePrimaryTreeField(treeConfig, "filterField", modelSchema);
        treeConfig.putIfAbsent("childrenField", "children");
        String defaultTreeTitle = StringUtils.defaultIfBlank(text(treeConfig.get("sourceModelName")), modelSchema.getBusinessName());
        treeConfig.putIfAbsent("treeTitle", StringUtils.isBlank(defaultTreeTitle) ? "树形导航" : defaultTreeTitle + "树");
        return treeConfig;
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String inferTreeParentField(LowcodeModelSchema modelSchema, LowcodePageModelRef sourceRef) {
        return treeSourceFields(modelSchema, sourceRef).stream()
                .filter(field -> "parentId".equals(field) || "pid".equals(field) || "parentCode".equals(field))
                .findFirst()
                .orElse("parentId");
    }

    private String inferTreeLabelField(LowcodeModelSchema modelSchema, LowcodePageModelRef sourceRef) {
        List<String> fields = treeSourceFields(modelSchema, sourceRef);
        return fields.stream()
                .filter(field -> "name".equals(field) || "title".equals(field) || "label".equals(field))
                .findFirst()
                .orElseGet(() -> fields.isEmpty() ? "name" : fields.get(0));
    }

    private LowcodePageModelRef resolveTreeSourceRef(LowcodePageSchema pageSchema, String sourceModelCode) {
        if (pageSchema == null || pageSchema.getModelRefs() == null || pageSchema.getModelRefs().isEmpty()) {
            return null;
        }
        if (StringUtils.isNotBlank(sourceModelCode)) {
            for (LowcodePageModelRef ref : pageSchema.getModelRefs()) {
                if (ref != null && sourceModelCode.equals(ref.getModelCode())) {
                    return ref;
                }
            }
        }
        return pageSchema.getModelRefs().stream()
                .filter(ref -> ref != null && !Boolean.TRUE.equals(ref.getPrimary()))
                .findFirst()
                .orElseGet(() -> pageSchema.getModelRefs().stream()
                        .filter(ref -> ref != null && Boolean.TRUE.equals(ref.getPrimary()))
                        .findFirst()
                        .orElse(pageSchema.getModelRefs().get(0)));
    }

    private List<String> treeSourceFields(LowcodeModelSchema modelSchema, LowcodePageModelRef sourceRef) {
        if (sourceRef != null && sourceRef.getFields() != null && !sourceRef.getFields().isEmpty()) {
            return sourceRef.getFields().stream()
                    .map(field -> StringUtils.defaultIfBlank(text(field.get("sourceField")), text(field.get("field"))))
                    .filter(StringUtils::isNotBlank)
                    .toList();
        }
        return modelSchema.getFields().stream()
                .map(LowcodeFieldSchema::getField)
                .filter(StringUtils::isNotBlank)
                .toList();
    }

    private void normalizeTreeSourceField(Map<String, Object> treeConfig, String key, LowcodePageModelRef sourceRef) {
        String value = text(treeConfig.get(key));
        if (StringUtils.isBlank(value) || sourceRef == null || sourceRef.getFields() == null) {
            return;
        }
        String normalized = resolveRefSourceField(sourceRef, value);
        if (StringUtils.isNotBlank(normalized)) {
            treeConfig.put(key, normalized);
        }
    }

    private void normalizePrimaryTreeField(Map<String, Object> treeConfig, String key, LowcodeModelSchema modelSchema) {
        String value = text(treeConfig.get(key));
        if (StringUtils.isBlank(value)) {
            return;
        }
        for (LowcodeFieldSchema field : modelSchema.getFields()) {
            if (value.equals(field.getField()) || value.equals(field.getColumnName())) {
                treeConfig.put(key, field.getField());
                return;
            }
        }
    }

    private String resolveRefSourceField(LowcodePageModelRef ref, String value) {
        for (Map<String, Object> field : ref.getFields()) {
            String sourceField = StringUtils.defaultIfBlank(text(field.get("sourceField")), text(field.get("field")));
            String fieldRef = StringUtils.defaultIfBlank(text(field.get("fieldRef")), sourceField);
            String columnName = text(field.get("columnName"));
            if (value.equals(sourceField)
                    || value.equals(fieldRef)
                    || value.equals(columnName)
                    || value.equals(ref.getModelCode() + "__" + sourceField)
                    || value.equals(ref.getModelCode() + "." + sourceField)) {
                return sourceField;
            }
        }
        return value;
    }

    private String resolveTreePrimaryModelCode(LowcodeModelSchema modelSchema, LowcodePageSchema pageSchema) {
        String code = pageSchema == null ? null : pageSchema.getPrimaryModelCode();
        if (StringUtils.isNotBlank(code)) {
            return code;
        }
        LowcodePageModelRef primaryRef = resolvePrimaryRef(modelSchema, pageSchema);
        if (primaryRef != null && StringUtils.isNotBlank(primaryRef.getModelCode())) {
            return primaryRef.getModelCode();
        }
        return modelSchema.getObject() == null ? null : modelSchema.getObject().getCode();
    }

    private List<LowcodeRelationSchema> resolvePrimaryTreeRelations(LowcodeModelSchema modelSchema, LowcodePageSchema pageSchema) {
        LowcodePageModelRef primaryRef = resolvePrimaryRef(modelSchema, pageSchema);
        if (primaryRef != null && primaryRef.getRelations() != null && !primaryRef.getRelations().isEmpty()) {
            return primaryRef.getRelations();
        }
        return modelSchema.getRelations();
    }

    private void putIfNotBlank(Map<String, Object> target, String key, String value) {
        if (StringUtils.isNotBlank(value)) {
            target.put(key, value);
        }
    }

    private void appendTreeRuntimeField(List<Map<String, Object>> fields,
                                        LowcodeModelSchema modelSchema,
                                        LowcodePageSchema pageSchema,
                                        String zoneKey) {
        if (!isTreeRuntime(modelSchema, pageSchema)) {
            return;
        }
        String filterField = String.valueOf(buildTreeConfig(modelSchema, pageSchema, extractTreeConfigOverrides(pageSchema)).get("filterField"));
        boolean exists = fields.stream().anyMatch(item -> filterField.equals(item.get("field"))
                || filterField.equals(item.get("prop"))
                || filterField.equals(item.get("dataIndex"))
                || filterField.equals(item.get("key")));
        if (exists) {
            return;
        }
        LowcodeFieldSchema fieldSchema = findField(modelSchema, filterField);
        if (fieldSchema == null) {
            return;
        }
        Map<String, Object> hiddenField = "edit".equals(zoneKey) ? buildEditField(fieldSchema) : buildSearchField(fieldSchema);
        hiddenField.put("hidden", true);
        hiddenField.put("queryType", "eq");
        hiddenField.put("required", false);
        fields.add(hiddenField);
    }

    @SuppressWarnings("unchecked")
    private void decorateTreeRuntimeFields(List<Map<String, Object>> fields,
                                           String configKey,
                                           LowcodeModelSchema modelSchema,
                                           LowcodePageSchema pageSchema) {
        if (!isTreeRuntime(modelSchema, pageSchema) || StringUtils.isBlank(configKey)) {
            return;
        }
        Map<String, Object> treeConfig = buildTreeConfig(modelSchema, pageSchema, extractTreeConfigOverrides(pageSchema));
        String filterField = text(treeConfig.get("filterField"));
        if (StringUtils.isBlank(filterField)) {
            return;
        }
        for (Map<String, Object> item : fields) {
            if (!filterField.equals(text(item.get("field")))) {
                continue;
            }
            String label = StringUtils.defaultIfBlank(text(item.get("label")), filterField);
            item.put("type", "treeSelect");
            item.put("queryType", "eq");
            Map<String, Object> props = new LinkedHashMap<>();
            Object sourceProps = item.get("props");
            if (sourceProps instanceof Map<?, ?> sourcePropsMap) {
                props.putAll((Map<String, Object>) sourcePropsMap);
            }
            props.putIfAbsent("placeholder", "请选择" + label);
            props.putIfAbsent("clearable", true);
            props.putIfAbsent("filterable", true);
            props.put("optionSource", buildTreeOptionSource(configKey, treeConfig));
            item.put("props", props);
        }
    }

    private Map<String, Object> buildTreeOptionSource(String configKey, Map<String, Object> treeConfig) {
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("type", "tree");
        source.put("api", "get@/ai/crud/" + configKey + "/tree");
        source.put("keyField", "key");
        source.put("valueField", "targetValue");
        source.put("labelField", "label");
        source.put("childrenField", StringUtils.defaultIfBlank(text(treeConfig.get("childrenField")), "children"));
        return source;
    }

    private Object extractTreeConfigOverrides(LowcodePageSchema pageSchema) {
        LowcodePageZone tableZone = findZone(pageSchema, "table");
        if (tableZone != null && tableZone.getProps() != null && tableZone.getProps().get("treeConfig") != null) {
            return tableZone.getProps().get("treeConfig");
        }
        return extractGridTreeConfigOverrides(pageSchema);
    }

    private Object extractGridTreeConfigOverrides(LowcodePageSchema pageSchema) {
        if (pageSchema == null || pageSchema.getListGridLayout() == null) {
            return null;
        }
        Object items = pageSchema.getListGridLayout().get("items");
        if (!(items instanceof List<?> itemList)) {
            return null;
        }
        for (Object item : itemList) {
            if (!(item instanceof Map<?, ?> block)) {
                continue;
            }
            if (!"tree-panel".equals(String.valueOf(block.get("blockType")))) {
                continue;
            }
            Object props = block.get("props");
            if (props instanceof Map<?, ?>) {
                return props;
            }
        }
        return null;
    }

    private LowcodeFieldSchema findField(LowcodeModelSchema modelSchema, String fieldName) {
        if (modelSchema == null || modelSchema.getFields() == null || StringUtils.isBlank(fieldName)) {
            return null;
        }
        return modelSchema.getFields().stream()
                .filter(field -> fieldName.equals(field.getField()))
                .findFirst()
                .orElse(null);
    }

    private List<Map<String, Object>> buildDictConfig(LowcodeModelSchema modelSchema) {
        Map<String, String> dictMap = new LinkedHashMap<>();
        for (LowcodeFieldSchema field : modelSchema.getFields()) {
            if (StringUtils.isNotBlank(field.getDictType())) {
                dictMap.putIfAbsent(field.getDictType(), StringUtils.defaultIfBlank(field.getLabel(), field.getDictType()));
            }
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, String> entry : dictMap.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("dictType", entry.getKey());
            item.put("dictName", entry.getValue());
            item.put("isNew", false);
            item.put("items", List.of());
            result.add(item);
        }
        return result;
    }

    private Map<String, Object> buildDesensitizeConfig(LowcodeModelSchema modelSchema) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (LowcodeFieldSchema field : modelSchema.getFields()) {
            String sensitiveType = StringUtils.defaultIfBlank(field.getSensitiveType(), "NONE").toUpperCase(Locale.ROOT);
            if ("NONE".equals(sensitiveType)) {
                continue;
            }
            Map<String, Object> rule = new LinkedHashMap<>();
            rule.put("type", sensitiveType);
            rule.put("label", StringUtils.defaultIfBlank(field.getLabel(), field.getField()));
            result.put(field.getField(), rule);
        }
        return result;
    }

    private Map<String, Object> buildEncryptConfig(LowcodeModelSchema modelSchema) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (LowcodeFieldSchema field : modelSchema.getFields()) {
            if (StringUtils.isBlank(field.getEncryptAlgorithm())) {
                continue;
            }
            Map<String, Object> rule = new LinkedHashMap<>();
            rule.put("algorithm", field.getEncryptAlgorithm());
            result.put(field.getField(), rule);
        }
        return result;
    }

    private Map<String, Object> buildTransConfig(LowcodeModelSchema modelSchema) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (LowcodeFieldSchema field : modelSchema.getFields()) {
            String componentType = StringUtils.defaultIfBlank(field.getComponentType(), "input");
            if (StringUtils.isNotBlank(field.getDictType())) {
                Map<String, Object> rule = new LinkedHashMap<>();
                rule.put("dictType", field.getDictType());
                rule.put("targetField", field.getField() + "Name");
                result.put(field.getField(), rule);
            } else if ("orgTreeSelect".equals(componentType)) {
                Map<String, Object> rule = new LinkedHashMap<>();
                rule.put("type", "orgName");
                rule.put("targetField", field.getField() + "Name");
                result.put(field.getField(), rule);
            } else if ("userSelect".equals(componentType)) {
                Map<String, Object> rule = new LinkedHashMap<>();
                rule.put("type", "userName");
                rule.put("targetField", field.getField() + "Name");
                result.put(field.getField(), rule);
            } else if ("regionTreeSelect".equals(componentType)) {
                Map<String, Object> rule = new LinkedHashMap<>();
                rule.put("type", "regionName");
                rule.put("targetField", field.getField() + "Name");
                result.put(field.getField(), rule);
            } else if ("fileUpload".equals(componentType) || "imageUpload".equals(componentType)) {
                Map<String, Object> rule = new LinkedHashMap<>();
                rule.put("type", componentType);
                rule.put("targetField", field.getField() + "Name");
                result.put(field.getField(), rule);
            }
        }
        return result;
    }

    private Map<String, Object> buildSearchField(LowcodeFieldSchema field) {
        return buildSearchField(field, Map.of());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildSearchField(LowcodeFieldSchema field, Map<String, Object> pageSetting) {
        Map<String, Object> item = new LinkedHashMap<>();
        String queryType = StringUtils.defaultIfBlank(text(pageSetting.get("queryType")),
                StringUtils.defaultIfBlank(field.getQueryType(), "eq")).toLowerCase(Locale.ROOT);
        item.put("field", field.getField());
        item.put("label", StringUtils.defaultIfBlank(field.getLabel(), field.getField()));
        String componentType = resolveSearchComponentType(field, queryType, pageSetting);
        item.put("type", componentType);
        item.put("queryType", queryType);
        if ("daterange".equals(componentType) || "datetimerange".equals(componentType) || "timerange".equals(componentType)) {
            item.put("startPlaceholder", "开始" + StringUtils.defaultIfBlank(field.getLabel(), field.getField()));
            item.put("endPlaceholder", "结束" + StringUtils.defaultIfBlank(field.getLabel(), field.getField()));
        }
        if (StringUtils.isNotBlank(field.getDictType())) {
            item.put("dictType", field.getDictType());
        }
        Object designerProps = pageSetting.get("props");
        if (designerProps instanceof Map<?, ?> designerPropsMap) {
            Map<String, Object> props = new LinkedHashMap<>();
            props.putAll((Map<String, Object>) designerPropsMap);
            item.put("props", props);
        }
        return item;
    }

    private Map<String, Object> buildTableColumn(LowcodeFieldSchema field) {
        return buildTableColumn(field, Map.of());
    }

    private Map<String, Object> buildTableColumn(LowcodeFieldSchema field, Map<String, Object> pageSetting) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("key", field.getField());
        item.put("title", StringUtils.defaultIfBlank(field.getLabel(), field.getField()));
        item.put("dataIndex", field.getField());
        if (field.getWidth() != null && field.getWidth() > 0) {
            item.put("width", field.getWidth());
        }
        Object sortable = pageSetting.get("sortable");
        if (Boolean.TRUE.equals(sortable) || Boolean.TRUE.equals(field.getSortable())) {
            item.put("sorter", true);
        }
        String componentType = StringUtils.defaultIfBlank(field.getComponentType(), "input");
        if (StringUtils.isNotBlank(field.getDictType())) {
            Map<String, Object> render = new LinkedHashMap<>();
            render.put("type", "dictTag");
            render.put("dictType", field.getDictType());
            item.put("render", render);
        } else if ("orgTreeSelect".equals(componentType)) {
            Map<String, Object> render = new LinkedHashMap<>();
            render.put("type", "orgName");
            render.put("targetField", field.getField() + "Name");
            item.put("render", render);
        } else if ("userSelect".equals(componentType)) {
            Map<String, Object> render = new LinkedHashMap<>();
            render.put("type", "userName");
            render.put("targetField", field.getField() + "Name");
            item.put("render", render);
        } else if ("regionTreeSelect".equals(componentType)) {
            Map<String, Object> render = new LinkedHashMap<>();
            render.put("type", "regionName");
            render.put("targetField", field.getField() + "Name");
            item.put("render", render);
        } else if ("fileUpload".equals(componentType)) {
            Map<String, Object> render = new LinkedHashMap<>();
            render.put("type", "fileUpload");
            item.put("render", render);
        } else if ("imageUpload".equals(componentType)) {
            Map<String, Object> render = new LinkedHashMap<>();
            render.put("type", "imageUpload");
            item.put("render", render);
        }
        return item;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> resolveFieldSetting(LowcodePageSchema pageSchema, String zoneKey, String fieldName) {
        LowcodePageZone zone = findZone(pageSchema, zoneKey);
        if (zone == null || zone.getProps() == null || StringUtils.isBlank(fieldName)) {
            return Map.of();
        }
        Object settings = zone.getProps().get("fieldSettings");
        if (!(settings instanceof Map<?, ?> settingsMap)) {
            return Map.of();
        }
        Object value = settingsMap.get(fieldName);
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private Map<String, Object> resolveEditFieldSetting(LowcodePageSchema pageSchema, String fieldName) {
        Map<String, Object> setting = new LinkedHashMap<>(resolveFieldSetting(pageSchema, "edit", fieldName));
        Map<String, Object> designerSetting = resolveFormRuleSetting(pageSchema, fieldName);
        setting.putAll(designerSetting);
        Map<String, Object> canvasSetting = resolveCanvasFieldSetting(pageSchema, fieldName);
        setting.putAll(canvasSetting);
        return setting;
    }

    private Map<String, Object> resolveCanvasFieldSetting(LowcodePageSchema pageSchema, String fieldName) {
        LowcodePageZone editZone = findZone(pageSchema, "edit");
        if (editZone == null || StringUtils.isBlank(fieldName)) {
            return Map.of();
        }
        int gridCols = Math.max(1, resolveEditGridCols(pageSchema));
        int canvasWidth = 1040;
        if (editZone.getProps() != null) {
            Object canvas = editZone.getProps().get("canvas");
            if (canvas instanceof Map<?, ?> canvasMap) {
                canvasWidth = intValue(canvasMap.get("width"), canvasWidth);
            }
        }
        int colWidth = Math.max(1, (canvasWidth - 64) / gridCols);
        for (Map<String, Object> item : extractCanvasItems(editZone)) {
            if (!fieldName.equals(text(item.get("fieldRef")))) {
                continue;
            }
            Map<String, Object> setting = new LinkedHashMap<>();
            int itemWidth = intValue(item.get("w"), 280);
            int span = Math.max(1, Math.min(gridCols, Math.round((float) itemWidth / colWidth)));
            setting.put("span", span);
            Object style = item.get("style");
            if (style instanceof Map<?, ?> styleMap && styleMap.get("labelWidth") != null) {
                setting.put("labelWidth", styleMap.get("labelWidth"));
            }
            return setting;
        }
        return Map.of();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> resolveFormRuleSetting(LowcodePageSchema pageSchema, String fieldName) {
        if (StringUtils.isBlank(fieldName)) {
            return Map.of();
        }
        for (Map<String, Object> rule : extractFormRules(pageSchema)) {
            if (!fieldName.equals(text(rule.get("field")))) {
                continue;
            }
            Map<String, Object> setting = new LinkedHashMap<>();
            Object props = rule.get("props");
            if (props instanceof Map<?, ?> propsMap) {
                setting.put("props", new LinkedHashMap<>((Map<String, Object>) propsMap));
            }
            Object style = rule.get("style");
            if (style != null) {
                setting.put("formItemStyle", style);
            }
            Object col = rule.get("col");
            if (col instanceof Map<?, ?> colMap) {
                int gridCols = resolveEditGridCols(pageSchema);
                Integer span = integerValue(colMap.get("span"));
                if (span != null && span > 0) {
                    int gridSpan = (int) Math.ceil(gridCols * Math.min(24, span) / 24.0);
                    setting.put("span", Math.max(1, Math.min(gridCols, gridSpan)));
                }
                Object gridStyle = colMap.get("style");
                if (gridStyle != null) {
                    setting.put("gridStyle", gridStyle);
                }
            }
            Object labelWidth = rule.get("labelWidth");
            if (labelWidth != null) {
                setting.put("labelWidth", labelWidth);
            }
            return setting;
        }
        return Map.of();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractFormRules(LowcodePageSchema pageSchema) {
        LowcodePageZone editZone = findZone(pageSchema, "edit");
        if (editZone == null || editZone.getProps() == null) {
            return List.of();
        }
        Object rules = editZone.getProps().get("formCreateRule");
        if (!(rules instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        collectFormRules(list, result);
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractCanvasItems(LowcodePageZone zone) {
        if (zone == null || zone.getProps() == null) {
            return List.of();
        }
        Object canvas = zone.getProps().get("canvas");
        if (!(canvas instanceof Map<?, ?> canvasMap)) {
            return List.of();
        }
        Object items = canvasMap.get("items");
        if (!(items instanceof List<?> itemList)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : itemList) {
            if (item instanceof Map<?, ?> itemMap) {
                result.add((Map<String, Object>) itemMap);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private void collectFormRules(List<?> rules, List<Map<String, Object>> result) {
        for (Object item : rules) {
            if (!(item instanceof Map<?, ?> rule)) {
                continue;
            }
            Map<String, Object> typedRule = (Map<String, Object>) rule;
            result.add(typedRule);
            Object children = typedRule.get("children");
            if (children instanceof List<?> childRules) {
                collectFormRules(childRules, result);
            }
        }
    }

    private Map<String, Object> buildEditField(LowcodeFieldSchema field) {
        return buildEditField(field, Map.of());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildEditField(LowcodeFieldSchema field, Map<String, Object> pageSetting) {
        Map<String, Object> item = new LinkedHashMap<>();
        String label = StringUtils.defaultIfBlank(field.getLabel(), field.getField());
        String componentType = resolveEditComponentType(field, pageSetting);
        item.put("field", field.getField());
        item.put("label", label);
        item.put("type", componentType);
        item.put("required", !isSystemField(field) && Boolean.TRUE.equals(field.getRequired()));
        if (isSystemField(field) || Boolean.TRUE.equals(field.getReadonly())) {
            item.put("disabled", true);
            item.put("readonly", true);
        }
        if (StringUtils.isNotBlank(field.getDictType())) {
            item.put("dictType", field.getDictType());
        }
        if (field.getDefaultValue() != null) {
            item.put("defaultValue", field.getDefaultValue());
        }
        Object span = pageSetting.get("span");
        if (span != null) {
            item.put("span", span);
        }
        Object formItemStyle = pageSetting.get("formItemStyle");
        if (formItemStyle != null) {
            item.put("formItemStyle", formItemStyle);
        }
        Object gridStyle = pageSetting.get("gridStyle");
        if (gridStyle != null) {
            item.put("gridStyle", gridStyle);
        }
        Object labelWidth = pageSetting.get("labelWidth");
        if (labelWidth != null) {
            item.put("labelWidth", labelWidth);
        }

        Map<String, Object> props = new LinkedHashMap<>();
        props.put("placeholder", buildPlaceholder(componentType, label));
        if (isSystemField(field) || Boolean.TRUE.equals(field.getReadonly())) {
            props.put("disabled", true);
        }
        if (field.getLength() != null && field.getLength() > 0 && isTextComponent(componentType)) {
            props.put("maxlength", field.getLength());
        }
        if (field.getPrecision() != null && field.getPrecision() >= 0 && "number".equals(componentType)) {
            props.put("precision", field.getPrecision());
        }
        Object designerProps = pageSetting.get("props");
        if (designerProps instanceof Map<?, ?> designerPropsMap) {
            props.putAll((Map<String, Object>) designerPropsMap);
        }
        if (isSystemField(field) || Boolean.TRUE.equals(field.getReadonly())) {
            props.put("disabled", true);
        }
        item.put("props", props);

        if (Boolean.TRUE.equals(field.getRequired())) {
            Map<String, Object> rule = new LinkedHashMap<>();
            rule.put("required", true);
            rule.put("message", buildPlaceholder(componentType, label));
            rule.put("trigger", List.of("blur", "change"));
            item.put("rules", List.of(rule));
        }
        return item;
    }

    private List<Map<String, Object>> buildRowActions(LowcodePageSchema pageSchema) {
        List<Map<String, Object>> actions = new ArrayList<>();
        actions.add(defaultAction("edit", "编辑", "primary"));
        actions.add(defaultAction("delete", "删除", "error"));
        actions.addAll(resolveCustomActions(pageSchema, "row"));
        return actions;
    }

    private Map<String, Object> defaultAction(String key, String label, String type) {
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("key", key);
        action.put("label", label);
        action.put("type", type);
        action.put("position", "row");
        return action;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> resolveCustomActions(LowcodePageSchema pageSchema, String position) {
        LowcodePageZone tableZone = findZone(pageSchema, "table");
        if (tableZone == null || tableZone.getProps() == null) {
            return List.of();
        }
        Object value = tableZone.getProps().get("customActions");
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> source)) {
                continue;
            }
            String actionPosition = StringUtils.defaultIfBlank(text(source.get("position")), "toolbar");
            if (!position.equals(actionPosition)) {
                continue;
            }
            String key = StringUtils.defaultIfBlank(text(source.get("key")), "custom_" + result.size());
            String label = StringUtils.defaultIfBlank(text(source.get("label")), "自定义按钮");
            Map<String, Object> action = new LinkedHashMap<>();
            action.put("key", key);
            action.put("label", label);
            action.put("type", StringUtils.defaultIfBlank(text(source.get("type")), "default"));
            action.put("position", position);
            action.put("actionType", StringUtils.defaultIfBlank(text(source.get("actionType")), "route"));
            putIfNotBlank(action, "routePath", text(source.get("routePath")));
            putIfNotBlank(action, "openTarget", StringUtils.defaultIfBlank(text(source.get("openTarget")), "_self"));
            putIfNotBlank(action, "confirmText", text(source.get("confirmText")));
            List<Map<String, Object>> params = resolveActionParams(source.get("params"));
            if (!params.isEmpty()) {
                action.put("params", params);
            }
            result.add(action);
        }
        return result;
    }

    private List<Map<String, Object>> resolveActionParams(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> source)) {
                continue;
            }
            String name = text(source.get("name"));
            if (StringUtils.isBlank(name)) {
                continue;
            }
            Map<String, Object> param = new LinkedHashMap<>();
            param.put("name", name);
            param.put("value", StringUtils.defaultString(text(source.get("value"))));
            result.add(param);
        }
        return result;
    }

    private boolean isSystemField(LowcodeFieldSchema field) {
        return field != null && Boolean.TRUE.equals(field.getSystemField());
    }

    private List<LowcodeFieldSchema> resolveFields(LowcodeModelSchema modelSchema,
                                                   LowcodePageSchema pageSchema,
                                                   String zoneKey,
                                                   Predicate<LowcodeFieldSchema> fallbackPredicate) {
        Map<String, LowcodeFieldSchema> fieldMap = buildRuntimeFieldMap(modelSchema, pageSchema);
        LowcodePageZone zone = findZone(pageSchema, zoneKey);
        if (zone == null || Boolean.FALSE.equals(zone.getEnabled()) || zone.getFieldRefs() == null || zone.getFieldRefs().isEmpty()) {
            return fieldMap.values().stream()
                    .filter(fallbackPredicate)
                    .toList();
        }

        Set<String> refs = new LinkedHashSet<>(zone.getFieldRefs());
        List<LowcodeFieldSchema> selectedFields = refs.stream()
                .map(fieldMap::get)
                .filter(field -> field != null)
                .filter(field -> isZoneFieldAllowed(field, zoneKey, fallbackPredicate))
                .toList();
        if (selectedFields.isEmpty()) {
            return fieldMap.values().stream()
                    .filter(fallbackPredicate)
                    .toList();
        }
        return selectedFields;
    }

    private Map<String, LowcodeFieldSchema> buildRuntimeFieldMap(LowcodeModelSchema modelSchema,
                                                                  LowcodePageSchema pageSchema) {
        Map<String, LowcodeFieldSchema> fieldMap = modelSchema.getFields().stream()
                .collect(Collectors.toMap(
                        LowcodeFieldSchema::getField,
                        field -> field,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        if (pageSchema == null || pageSchema.getModelRefs() == null) {
            return fieldMap;
        }
        for (LowcodePageModelRef ref : pageSchema.getModelRefs()) {
            if (ref == null || ref.getFields() == null) {
                continue;
            }
            for (Map<String, Object> source : ref.getFields()) {
                LowcodeFieldSchema field = buildPageRefField(ref, source);
                if (field != null) {
                    fieldMap.putIfAbsent(field.getField(), field);
                }
            }
        }
        return fieldMap;
    }

    private LowcodeFieldSchema buildPageRefField(LowcodePageModelRef ref, Map<String, Object> source) {
        if (source == null) {
            return null;
        }
        String sourceField = StringUtils.defaultIfBlank(text(source.get("sourceField")), text(source.get("field")));
        if (StringUtils.isBlank(sourceField)) {
            return null;
        }
        boolean primary = Boolean.TRUE.equals(ref.getPrimary());
        String fieldRef = StringUtils.defaultIfBlank(text(source.get("fieldRef")),
                primary ? sourceField : safeKey(ref.getModelCode()) + "__" + sourceField);
        if (StringUtils.isBlank(fieldRef)) {
            return null;
        }

        LowcodeFieldSchema field = new LowcodeFieldSchema();
        field.setField(fieldRef);
        field.setColumnName(StringUtils.defaultIfBlank(text(source.get("columnName")), sourceField));
        String rawLabel = StringUtils.defaultIfBlank(text(source.get("rawLabel")),
                StringUtils.defaultIfBlank(text(source.get("label")), sourceField));
        field.setLabel(rawLabel);
        field.setDataType(StringUtils.defaultIfBlank(text(source.get("dataType")), "varchar"));
        field.setLength(integerValue(source.get("length")));
        field.setPrecision(integerValue(source.get("precision")));
        field.setRequired(Boolean.TRUE.equals(booleanValue(source.get("required"))));
        field.setDefaultValue(source.get("defaultValue"));
        field.setSearchable(Boolean.TRUE.equals(booleanValue(source.get("searchable"))));
        field.setListVisible(booleanValue(source.get("listVisible")) == null || Boolean.TRUE.equals(booleanValue(source.get("listVisible"))));
        field.setFormVisible(booleanValue(source.get("formVisible")) == null || Boolean.TRUE.equals(booleanValue(source.get("formVisible"))));
        field.setComponentType(StringUtils.defaultIfBlank(text(source.get("componentType")), "input"));
        field.setQueryType(StringUtils.defaultIfBlank(text(source.get("queryType")), "eq"));
        field.setDictType(text(source.get("dictType")));
        field.setSensitiveType(StringUtils.defaultIfBlank(text(source.get("sensitiveType")), "NONE"));
        field.setEncryptAlgorithm(text(source.get("encryptAlgorithm")));
        field.setSortable(Boolean.TRUE.equals(booleanValue(source.get("sortable"))));
        field.setPrimaryKey(Boolean.TRUE.equals(booleanValue(source.get("primaryKey"))));
        field.setSystemField(Boolean.TRUE.equals(booleanValue(source.get("systemField"))));
        field.setReadonly(Boolean.TRUE.equals(booleanValue(source.get("readonly"))));
        field.setAutoIncrement(Boolean.TRUE.equals(booleanValue(source.get("autoIncrement"))));
        field.setWidth(integerValue(source.get("width")));
        field.setRemark(text(source.get("remark")));
        return field;
    }

    private Boolean booleanValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private boolean isZoneFieldAllowed(LowcodeFieldSchema field,
                                       String zoneKey,
                                       Predicate<LowcodeFieldSchema> fallbackPredicate) {
        if ("search".equals(zoneKey)) {
            return !isSystemField(field);
        }
        if ("edit".equals(zoneKey)) {
            return !isSystemField(field)
                    && !Boolean.TRUE.equals(field.getReadonly())
                    && (field.getFormVisible() == null || Boolean.TRUE.equals(field.getFormVisible()));
        }
        return fallbackPredicate.test(field);
    }

    private Integer integerValue(Object value) {
        if (value == null || StringUtils.isBlank(String.valueOf(value))) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int intValue(Object value, int defaultValue) {
        Integer parsed = integerValue(value);
        return parsed == null ? defaultValue : parsed;
    }

    private String safeKey(String value) {
        String key = StringUtils.defaultIfBlank(value, "model").replaceAll("[^A-Za-z0-9_]", "_");
        return StringUtils.defaultIfBlank(key, "model");
    }

    private String camelToSnake(String value) {
        if (StringUtils.isBlank(value)) {
            return value;
        }
        return value.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
    }

    private String snakeToCamel(String value) {
        if (StringUtils.isBlank(value)) {
            return value;
        }
        StringBuilder result = new StringBuilder();
        boolean upperNext = false;
        for (char ch : value.toCharArray()) {
            if (ch == '_') {
                upperNext = true;
                continue;
            }
            result.append(upperNext ? Character.toUpperCase(ch) : ch);
            upperNext = false;
        }
        return result.toString();
    }

    private LowcodePageZone findZone(LowcodePageSchema pageSchema, String zoneKey) {
        if (pageSchema == null || pageSchema.getZones() == null) {
            return null;
        }
        return pageSchema.getZones().stream()
                .filter(zone -> zoneKey.equals(zone.getZoneKey()))
                .findFirst()
                .orElse(null);
    }

    private void copyOption(Map<String, Object> source, Map<String, Object> target, String key) {
        if (source.containsKey(key)) {
            target.put(key, source.get(key));
        }
    }

    private String resolveSearchComponentType(LowcodeFieldSchema field, String queryType) {
        return resolveSearchComponentType(field, queryType, Map.of());
    }

    private String resolveSearchComponentType(LowcodeFieldSchema field, String queryType, Map<String, Object> pageSetting) {
        String configuredType = StringUtils.defaultIfBlank(text(pageSetting.get("componentType")), text(pageSetting.get("type")));
        if (StringUtils.isNotBlank(configuredType)) {
            return normalizeEditComponentType(configuredType);
        }
        String componentType = StringUtils.defaultIfBlank(field.getComponentType(), field.getDataType());
        componentType = StringUtils.defaultIfBlank(componentType, "input");
        if (isBusinessSelectComponent(componentType)) {
            return componentType;
        }
        if (StringUtils.isNotBlank(field.getDictType())) {
            return "select";
        }
        if ("between".equals(queryType)) {
            if ("datetime".equals(componentType)) {
                return "datetimerange";
            }
            if ("date".equals(componentType)) {
                return "daterange";
            }
            if ("time".equals(componentType)) {
                return "timerange";
            }
        }
        if ("number".equals(componentType)) {
            return "number";
        }
        if ("date".equals(componentType)) {
            return "date";
        }
        if ("datetime".equals(componentType)) {
            return "datetime";
        }
        if ("time".equals(componentType)) {
            return "time";
        }
        if ("treeSelect".equals(componentType)) {
            return "treeSelect";
        }
        if ("cascader".equals(componentType)) {
            return "cascader";
        }
        return "input";
    }

    private String resolveEditComponentType(LowcodeFieldSchema field) {
        return normalizeEditComponentType(StringUtils.defaultIfBlank(field.getComponentType(), "input"));
    }

    private String resolveEditComponentType(LowcodeFieldSchema field, Map<String, Object> pageSetting) {
        String componentType = StringUtils.defaultIfBlank(text(pageSetting.get("componentType")), text(pageSetting.get("type")));
        componentType = StringUtils.defaultIfBlank(componentType, field.getComponentType());
        return normalizeEditComponentType(StringUtils.defaultIfBlank(componentType, "input"));
    }

    private String normalizeEditComponentType(String componentType) {
        if ("inputNumber".equals(componentType)) {
            return "number";
        }
        return componentType;
    }

    private boolean isBusinessSelectComponent(String componentType) {
        return "dictSelect".equals(componentType)
                || "treeSelect".equals(componentType)
                || "orgTreeSelect".equals(componentType)
                || "userSelect".equals(componentType)
                || "regionTreeSelect".equals(componentType)
                || "cascader".equals(componentType);
    }

    private String buildPlaceholder(String componentType, String label) {
        if ("select".equals(componentType) || "radio".equals(componentType) || "checkbox".equals(componentType)
                || "date".equals(componentType) || "datetime".equals(componentType) || "time".equals(componentType)
                || "daterange".equals(componentType) || "datetimerange".equals(componentType) || "timerange".equals(componentType)
                || "dictSelect".equals(componentType) || "treeSelect".equals(componentType) || "orgTreeSelect".equals(componentType)
                || "userSelect".equals(componentType) || "regionTreeSelect".equals(componentType) || "cascader".equals(componentType)
                || "fileUpload".equals(componentType) || "imageUpload".equals(componentType) || "upload".equals(componentType)) {
            return "请选择" + label;
        }
        return "请输入" + label;
    }

    private boolean isTextComponent(String componentType) {
        return "input".equals(componentType) || "textarea".equals(componentType);
    }
}

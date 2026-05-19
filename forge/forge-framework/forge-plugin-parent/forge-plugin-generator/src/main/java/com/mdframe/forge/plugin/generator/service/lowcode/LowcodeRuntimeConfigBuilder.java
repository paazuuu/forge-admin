package com.mdframe.forge.plugin.generator.service.lowcode;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageZone;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeRuntimeConfig;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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
            runtimeConfig.setSearchSchema(objectMapper.writeValueAsString(buildSearchSchema(modelSchema, pageSchema)));
            runtimeConfig.setColumnsSchema(objectMapper.writeValueAsString(buildColumnsSchema(modelSchema, pageSchema)));
            runtimeConfig.setEditSchema(objectMapper.writeValueAsString(buildEditSchema(modelSchema, pageSchema)));
            runtimeConfig.setApiConfig(objectMapper.writeValueAsString(buildApiConfig(configKey)));
            runtimeConfig.setOptions(objectMapper.writeValueAsString(buildOptions(pageSchema)));
            runtimeConfig.setDictConfig(objectMapper.writeValueAsString(buildDictConfig(modelSchema)));
            runtimeConfig.setDesensitizeConfig(objectMapper.writeValueAsString(buildDesensitizeConfig(modelSchema)));
            runtimeConfig.setEncryptConfig(objectMapper.writeValueAsString(buildEncryptConfig(modelSchema)));
            runtimeConfig.setTransConfig(objectMapper.writeValueAsString(buildTransConfig(modelSchema)));
            return runtimeConfig;
        } catch (Exception e) {
            throw new BusinessException("低代码运行时配置生成失败: " + e.getMessage());
        }
    }

    private List<Map<String, Object>> buildSearchSchema(LowcodeModelSchema modelSchema, LowcodePageSchema pageSchema) {
        return resolveFields(modelSchema, pageSchema, "search", field -> Boolean.TRUE.equals(field.getSearchable()))
                .stream()
                .map(this::buildSearchField)
                .toList();
    }

    private List<Map<String, Object>> buildColumnsSchema(LowcodeModelSchema modelSchema, LowcodePageSchema pageSchema) {
        List<Map<String, Object>> columns = resolveFields(modelSchema, pageSchema, "table",
                field -> field.getListVisible() == null || Boolean.TRUE.equals(field.getListVisible()))
                .stream()
                .map(this::buildTableColumn)
                .collect(Collectors.toCollection(ArrayList::new));

        Map<String, Object> actions = new LinkedHashMap<>();
        actions.put("key", "actions");
        actions.put("title", "操作");
        actions.put("dataIndex", "actions");
        actions.put("width", 180);
        actions.put("fixed", "right");
        columns.add(actions);
        return columns;
    }

    private List<Map<String, Object>> buildEditSchema(LowcodeModelSchema modelSchema, LowcodePageSchema pageSchema) {
        return resolveFields(modelSchema, pageSchema, "edit",
                field -> field.getFormVisible() == null || Boolean.TRUE.equals(field.getFormVisible()))
                .stream()
                .map(this::buildEditField)
                .toList();
    }

    private Map<String, String> buildApiConfig(String configKey) {
        Map<String, String> apiConfig = new LinkedHashMap<>();
        apiConfig.put("list", "get@/ai/crud/" + configKey + "/page");
        apiConfig.put("detail", "get@/ai/crud/" + configKey + "/:id");
        apiConfig.put("create", "post@/ai/crud/" + configKey);
        apiConfig.put("update", "put@/ai/crud/" + configKey);
        apiConfig.put("delete", "delete@/ai/crud/" + configKey + "/:id");
        apiConfig.put("import", "post@/ai/crud/" + configKey + "/import");
        apiConfig.put("export", "post@/ai/crud/" + configKey + "/export");
        apiConfig.put("importTemplate", "get@/ai/crud/" + configKey + "/import-template");
        return apiConfig;
    }

    private Map<String, Object> buildOptions(LowcodePageSchema pageSchema) {
        Map<String, Object> options = new LinkedHashMap<>();
        options.put("modalType", "drawer");
        options.put("modalWidth", "800px");
        options.put("searchGridCols", 4);
        options.put("editGridCols", 1);

        LowcodePageZone tableZone = findZone(pageSchema, "table");
        if (tableZone != null && tableZone.getProps() != null) {
            copyOption(tableZone.getProps(), options, "showImport");
            copyOption(tableZone.getProps(), options, "showExport");
            copyOption(tableZone.getProps(), options, "hideBatchDelete");
            copyOption(tableZone.getProps(), options, "enableCustomQuery");
        }
        return options;
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
            if (StringUtils.isBlank(field.getDictType())) {
                continue;
            }
            Map<String, Object> rule = new LinkedHashMap<>();
            rule.put("dictType", field.getDictType());
            rule.put("targetField", field.getField() + "Name");
            result.put(field.getField(), rule);
        }
        return result;
    }

    private Map<String, Object> buildSearchField(LowcodeFieldSchema field) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("field", field.getField());
        item.put("label", StringUtils.defaultIfBlank(field.getLabel(), field.getField()));
        item.put("type", resolveSearchComponentType(field));
        item.put("queryType", StringUtils.defaultIfBlank(field.getQueryType(), "eq").toLowerCase(Locale.ROOT));
        if (StringUtils.isNotBlank(field.getDictType())) {
            item.put("dictType", field.getDictType());
        }
        return item;
    }

    private Map<String, Object> buildTableColumn(LowcodeFieldSchema field) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("key", field.getField());
        item.put("title", StringUtils.defaultIfBlank(field.getLabel(), field.getField()));
        item.put("dataIndex", field.getField());
        if (field.getWidth() != null && field.getWidth() > 0) {
            item.put("width", field.getWidth());
        }
        if (StringUtils.isNotBlank(field.getDictType())) {
            Map<String, Object> render = new LinkedHashMap<>();
            render.put("type", "dictTag");
            render.put("dictType", field.getDictType());
            item.put("render", render);
        }
        return item;
    }

    private Map<String, Object> buildEditField(LowcodeFieldSchema field) {
        Map<String, Object> item = new LinkedHashMap<>();
        String label = StringUtils.defaultIfBlank(field.getLabel(), field.getField());
        item.put("field", field.getField());
        item.put("label", label);
        item.put("type", resolveEditComponentType(field));
        item.put("required", Boolean.TRUE.equals(field.getRequired()));
        if (StringUtils.isNotBlank(field.getDictType())) {
            item.put("dictType", field.getDictType());
        }
        if (field.getDefaultValue() != null) {
            item.put("defaultValue", field.getDefaultValue());
        }

        Map<String, Object> props = new LinkedHashMap<>();
        props.put("placeholder", buildPlaceholder(field, label));
        if (field.getLength() != null && field.getLength() > 0 && isTextComponent(field)) {
            props.put("maxlength", field.getLength());
        }
        if (field.getPrecision() != null && field.getPrecision() >= 0 && "number".equals(resolveEditComponentType(field))) {
            props.put("precision", field.getPrecision());
        }
        item.put("props", props);

        if (Boolean.TRUE.equals(field.getRequired())) {
            Map<String, Object> rule = new LinkedHashMap<>();
            rule.put("required", true);
            rule.put("message", buildPlaceholder(field, label));
            rule.put("trigger", List.of("blur", "change"));
            item.put("rules", List.of(rule));
        }
        return item;
    }

    private List<LowcodeFieldSchema> resolveFields(LowcodeModelSchema modelSchema,
                                                   LowcodePageSchema pageSchema,
                                                   String zoneKey,
                                                   Predicate<LowcodeFieldSchema> fallbackPredicate) {
        Map<String, LowcodeFieldSchema> fieldMap = modelSchema.getFields().stream()
                .collect(Collectors.toMap(
                        LowcodeFieldSchema::getField,
                        field -> field,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        LowcodePageZone zone = findZone(pageSchema, zoneKey);
        if (zone == null || Boolean.FALSE.equals(zone.getEnabled()) || zone.getFieldRefs() == null || zone.getFieldRefs().isEmpty()) {
            return modelSchema.getFields().stream()
                    .filter(fallbackPredicate)
                    .toList();
        }

        Set<String> refs = new LinkedHashSet<>(zone.getFieldRefs());
        return refs.stream()
                .map(fieldMap::get)
                .filter(field -> field != null && fallbackPredicate.test(field))
                .toList();
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

    private String resolveSearchComponentType(LowcodeFieldSchema field) {
        if (StringUtils.isNotBlank(field.getDictType())) {
            return "select";
        }
        String queryType = StringUtils.defaultIfBlank(field.getQueryType(), "eq").toLowerCase(Locale.ROOT);
        String componentType = StringUtils.defaultIfBlank(field.getComponentType(), "input");
        if ("between".equals(queryType) && ("date".equals(componentType) || "datetime".equals(componentType))) {
            return "daterange";
        }
        if ("number".equals(componentType)) {
            return "number";
        }
        if ("date".equals(componentType) || "datetime".equals(componentType)) {
            return "date";
        }
        return "input";
    }

    private String resolveEditComponentType(LowcodeFieldSchema field) {
        String componentType = StringUtils.defaultIfBlank(field.getComponentType(), "input");
        if ("fileUpload".equals(componentType)) {
            return "upload";
        }
        if ("number".equals(componentType)) {
            return "number";
        }
        return componentType;
    }

    private String buildPlaceholder(LowcodeFieldSchema field, String label) {
        String componentType = resolveEditComponentType(field);
        if ("select".equals(componentType) || "radio".equals(componentType) || "checkbox".equals(componentType)
                || "date".equals(componentType) || "datetime".equals(componentType) || "time".equals(componentType)
                || "treeSelect".equals(componentType) || "cascader".equals(componentType)) {
            return "请选择" + label;
        }
        return "请输入" + label;
    }

    private boolean isTextComponent(LowcodeFieldSchema field) {
        String componentType = resolveEditComponentType(field);
        return "input".equals(componentType) || "textarea".equals(componentType);
    }
}

package com.mdframe.forge.plugin.generator.service.businessapp;

import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 动作步骤配置解析工具。
 */
final class BusinessActionStepConfigHelper {

    private BusinessActionStepConfigHelper() {
    }

    static Map<String, Object> buildData(Map<String, Object> config, BusinessActionExecutionContext context) {
        Map<String, Object> data = new LinkedHashMap<>();
        for (Object item : firstList(config, "fieldMapping", "fieldMappings", "fields", "params")) {
            Map<String, Object> mapping = asMap(item);
            if (mapping.isEmpty()) {
                continue;
            }
            String targetField = firstText(mapping, "targetField", "target", "field", "name");
            if (StringUtils.isBlank(targetField)) {
                continue;
            }
            data.put(targetField.trim(), resolveMappingValue(mapping, context));
        }
        data.putAll(asMap(firstValue(config, "staticValues", "values", "data")));
        return data;
    }

    static Object resolveTargetRecordId(Map<String, Object> config, BusinessActionExecutionContext context) {
        Object explicit = firstValue(config, "targetRecordId", "recordId");
        if (explicit != null) {
            return explicit;
        }
        String sourceField = firstText(config, "targetRecordIdField", "recordIdField");
        if (StringUtils.isNotBlank(sourceField)) {
            return resolvePath(sourceField, context);
        }
        return context.getRequest() == null ? null : context.getRequest().getRecordId();
    }

    static Object resolveMappingValue(Map<String, Object> mapping, BusinessActionExecutionContext context) {
        if (mapping.containsKey("value")) {
            return mapping.get("value");
        }
        if (mapping.containsKey("staticValue")) {
            return mapping.get("staticValue");
        }
        String sourceType = StringUtils.defaultIfBlank(firstText(mapping, "sourceType", "type"), "record")
                .trim()
                .toLowerCase(Locale.ROOT);
        String sourceField = firstText(mapping, "sourceField", "source", "formField", "field");
        if (StringUtils.isBlank(sourceField)) {
            return null;
        }
        return switch (sourceType) {
            case "form", "formdata", "form_data" -> readPath(context.getFormData(), sourceField);
            case "context" -> readPath(context.getExtraContext(), sourceField);
            case "system" -> resolveSystemValue(sourceField, context);
            case "static" -> mapping.get("value");
            default -> resolvePath(sourceField, context);
        };
    }

    static Object resolvePath(String sourceField, BusinessActionExecutionContext context) {
        String field = StringUtils.trimToEmpty(sourceField);
        if (context != null && context.getScopedVariables() != null) {
            String root = rootSegment(field);
            if (context.getScopedVariables().containsKey(root)) {
                Object scoped = context.getScopedVariables().get(root);
                String nestedPath = field.equals(root) ? "" : field.substring(root.length() + 1);
                return readPath(scoped, nestedPath);
            }
        }
        if (field.startsWith("formData.")) {
            return readPath(context.getFormData(), field.substring("formData.".length()));
        }
        if (field.startsWith("form.")) {
            return readPath(context.getFormData(), field.substring("form.".length()));
        }
        if (field.startsWith("record.")) {
            return readPath(context.getRecordData(), field.substring("record.".length()));
        }
        if (field.startsWith("row.")) {
            return readPath(context.getRecordData(), field.substring("row.".length()));
        }
        if (field.startsWith("context.")) {
            return readPath(context.getExtraContext(), field.substring("context.".length()));
        }
        Object formValue = readPath(context.getFormData(), field);
        if (formValue != null) {
            return formValue;
        }
        return readPath(context.getRecordData(), field);
    }

    static Object readPath(Object source, String path) {
        if (source == null) {
            return null;
        }
        if (StringUtils.isBlank(path)) {
            return source;
        }
        Object cursor = source;
        for (String part : path.split("\\.")) {
            if (StringUtils.isBlank(part)) {
                continue;
            }
            if (!(cursor instanceof Map<?, ?> map)) {
                return null;
            }
            cursor = map.get(part);
            if (cursor == null) {
                cursor = map.get(camelToSnake(part));
            }
            if (cursor == null) {
                cursor = map.get(snakeToCamel(part));
            }
        }
        return cursor;
    }

    static Map<String, Object> asMap(Object value) {
        if (!(value instanceof Map<?, ?> raw)) {
            return new LinkedHashMap<>();
        }
        Map<String, Object> map = new LinkedHashMap<>();
        raw.forEach((key, item) -> {
            if (key != null) {
                map.put(String.valueOf(key), item);
            }
        });
        return map;
    }

    static List<?> firstList(Map<String, Object> map, String... keys) {
        Object value = firstValue(map, keys);
        if (value instanceof List<?> list) {
            return list;
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream().toList();
        }
        return List.of();
    }

    static Object firstValue(Map<String, Object> map, String... keys) {
        if (map == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            if (map.containsKey(key)) {
                return map.get(key);
            }
        }
        return null;
    }

    static String firstText(Map<String, Object> map, String... keys) {
        Object value = firstValue(map, keys);
        return value == null ? null : StringUtils.trimToNull(String.valueOf(value));
    }

    static Object readPath(Map<String, Object> source, String path) {
        return readPath((Object) source, path);
    }

    private static String rootSegment(String path) {
        String value = StringUtils.trimToEmpty(path);
        int dotIndex = value.indexOf('.');
        return dotIndex < 0 ? value : value.substring(0, dotIndex);
    }

    private static Object resolveSystemValue(String sourceField, BusinessActionExecutionContext context) {
        return switch (StringUtils.defaultString(sourceField).toLowerCase(Locale.ROOT)) {
            case "objectcode" -> context.getBusinessObject() == null ? null : context.getBusinessObject().getObjectCode();
            case "recordid" -> context.getRequest() == null ? null : context.getRequest().getRecordId();
            case "correlationid" -> context.getCorrelationId();
            case "tenantid" -> context.getTenantId();
            default -> null;
        };
    }

    private static String camelToSnake(String value) {
        return value == null ? null : value.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
    }

    private static String snakeToCamel(String value) {
        if (value == null || !value.contains("_")) {
            return value;
        }
        StringBuilder builder = new StringBuilder();
        boolean upperNext = false;
        for (char ch : value.toCharArray()) {
            if (ch == '_') {
                upperNext = true;
                continue;
            }
            builder.append(upperNext ? Character.toUpperCase(ch) : ch);
            upperNext = false;
        }
        return builder.toString();
    }
}

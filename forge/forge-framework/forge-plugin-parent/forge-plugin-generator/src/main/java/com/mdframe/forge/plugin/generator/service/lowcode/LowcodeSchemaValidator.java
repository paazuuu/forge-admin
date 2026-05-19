package com.mdframe.forge.plugin.generator.service.lowcode;

import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageZone;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 低代码单表协议校验。
 */
@Service
public class LowcodeSchemaValidator {

    private static final Pattern TABLE_NAME_PATTERN = Pattern.compile("^[a-z][a-z0-9_]{1,63}$");
    private static final Pattern COLUMN_NAME_PATTERN = Pattern.compile("^[a-z][a-z0-9_]{1,63}$");
    private static final Pattern FIELD_NAME_PATTERN = Pattern.compile("^[a-z][A-Za-z0-9]{1,63}$");

    private static final Set<String> RESERVED_TABLE_PREFIXES = Set.of(
            "sys_", "ai_", "gen_", "flow_", "qrtz_", "data_"
    );
    private static final Set<String> BASE_FIELDS = Set.of(
            "id", "tenantId", "createBy", "createTime", "createDept", "updateBy", "updateTime", "delFlag"
    );
    private static final Set<String> BASE_COLUMNS = Set.of(
            "id", "tenant_id", "create_by", "create_time", "create_dept", "update_by", "update_time", "del_flag"
    );
    private static final Set<String> TABLE_MODES = Set.of("EXISTING", "CREATE");
    private static final Set<String> DATA_TYPES = Set.of(
            "varchar", "char", "text", "longtext", "int", "bigint", "decimal", "date", "datetime", "time", "tinyint"
    );
    private static final Set<String> COMPONENT_TYPES = Set.of(
            "input", "textarea", "select", "radio", "checkbox", "switch", "date", "datetime", "time",
            "number", "upload", "imageUpload", "fileUpload", "cascader", "treeSelect"
    );
    private static final Set<String> QUERY_TYPES = Set.of(
            "eq", "ne", "like", "left_like", "right_like", "gt", "ge", "gte", "lt", "le", "lte", "in", "between"
    );
    private static final Set<String> ZONE_KEYS = Set.of("search", "table", "edit", "detail", "toolbar");
    private static final Set<String> SENSITIVE_TYPES = Set.of(
            "NONE", "PHONE", "ID_CARD", "EMAIL", "BANK_CARD", "NAME", "ADDRESS", "PASSWORD", "CUSTOM"
    );

    public void validateModel(LowcodeModelSchema modelSchema) {
        if (modelSchema == null) {
            throw new BusinessException("数据模型不能为空");
        }
        String tableMode = StringUtils.defaultIfBlank(modelSchema.getTableMode(), "EXISTING").toUpperCase(Locale.ROOT);
        if (!TABLE_MODES.contains(tableMode)) {
            throw new BusinessException("不支持的数据表模式: " + modelSchema.getTableMode());
        }
        validateTableName(modelSchema.getTableName());
        if (modelSchema.getFields() == null || modelSchema.getFields().isEmpty()) {
            throw new BusinessException("数据模型至少需要一个业务字段");
        }

        Set<String> fields = new HashSet<>();
        Set<String> columns = new HashSet<>();
        for (LowcodeFieldSchema fieldSchema : modelSchema.getFields()) {
            validateField(fieldSchema);
            String field = fieldSchema.getField();
            String column = fieldSchema.getColumnName();
            if (!fields.add(field)) {
                throw new BusinessException("字段名重复: " + field);
            }
            if (!columns.add(column)) {
                throw new BusinessException("数据库列名重复: " + column);
            }
        }
    }

    public void validatePage(LowcodePageSchema pageSchema, LowcodeModelSchema modelSchema) {
        validateModel(modelSchema);
        if (pageSchema == null) {
            throw new BusinessException("页面配置不能为空");
        }
        if (pageSchema.getZones() == null) {
            return;
        }

        Set<String> modelFields = modelSchema.getFields().stream()
                .map(LowcodeFieldSchema::getField)
                .collect(Collectors.toSet());
        for (LowcodePageZone zone : pageSchema.getZones()) {
            validateZone(zone, modelFields);
        }
    }

    private void validateTableName(String tableName) {
        if (StringUtils.isBlank(tableName) || !TABLE_NAME_PATTERN.matcher(tableName).matches()) {
            throw new BusinessException("表名格式不正确，仅允许小写字母、数字、下划线，且必须以小写字母开头");
        }
        for (String prefix : RESERVED_TABLE_PREFIXES) {
            if (tableName.startsWith(prefix)) {
                throw new BusinessException("业务表名不能使用系统保留前缀: " + prefix);
            }
        }
    }

    private void validateField(LowcodeFieldSchema fieldSchema) {
        if (fieldSchema == null) {
            throw new BusinessException("字段配置不能为空");
        }
        if (StringUtils.isBlank(fieldSchema.getField()) || !FIELD_NAME_PATTERN.matcher(fieldSchema.getField()).matches()) {
            throw new BusinessException("字段名格式不正确: " + fieldSchema.getField());
        }
        if (BASE_FIELDS.contains(fieldSchema.getField())) {
            throw new BusinessException("基础审计字段不能作为业务字段: " + fieldSchema.getField());
        }
        if (StringUtils.isBlank(fieldSchema.getColumnName()) || !COLUMN_NAME_PATTERN.matcher(fieldSchema.getColumnName()).matches()) {
            throw new BusinessException("数据库列名格式不正确: " + fieldSchema.getColumnName());
        }
        if (BASE_COLUMNS.contains(fieldSchema.getColumnName())) {
            throw new BusinessException("基础审计列不能作为业务字段: " + fieldSchema.getColumnName());
        }
        String dataType = StringUtils.defaultIfBlank(fieldSchema.getDataType(), "varchar").toLowerCase(Locale.ROOT);
        if (!DATA_TYPES.contains(dataType)) {
            throw new BusinessException("不支持的数据类型: " + fieldSchema.getDataType());
        }
        String componentType = StringUtils.defaultIfBlank(fieldSchema.getComponentType(), "input");
        if (!COMPONENT_TYPES.contains(componentType)) {
            throw new BusinessException("不支持的控件类型: " + componentType);
        }
        String queryType = StringUtils.defaultIfBlank(fieldSchema.getQueryType(), "eq").toLowerCase(Locale.ROOT);
        if (!QUERY_TYPES.contains(queryType)) {
            throw new BusinessException("不支持的查询方式: " + fieldSchema.getQueryType());
        }
        String sensitiveType = StringUtils.defaultIfBlank(fieldSchema.getSensitiveType(), "NONE").toUpperCase(Locale.ROOT);
        if (!SENSITIVE_TYPES.contains(sensitiveType)) {
            throw new BusinessException("不支持的敏感类型: " + fieldSchema.getSensitiveType());
        }
    }

    private void validateZone(LowcodePageZone zone, Set<String> modelFields) {
        if (zone == null) {
            throw new BusinessException("页面区域配置不能为空");
        }
        if (StringUtils.isBlank(zone.getZoneKey()) || !ZONE_KEYS.contains(zone.getZoneKey())) {
            throw new BusinessException("不支持的页面区域: " + zone.getZoneKey());
        }
        List<String> refs = zone.getFieldRefs();
        if (refs == null) {
            return;
        }
        for (String ref : refs) {
            if (!modelFields.contains(ref)) {
                throw new BusinessException("页面区域引用了不存在的字段: " + ref);
            }
        }
    }
}

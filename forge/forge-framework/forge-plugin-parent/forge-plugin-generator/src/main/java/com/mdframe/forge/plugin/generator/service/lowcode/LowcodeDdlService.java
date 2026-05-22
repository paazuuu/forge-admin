package com.mdframe.forge.plugin.generator.service.lowcode;

import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeIndexSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeRelationSchema;
import com.mdframe.forge.plugin.generator.service.DynamicCrudRepository;
import com.mdframe.forge.plugin.generator.vo.lowcode.LowcodeDdlPreviewVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 低代码单表受控 DDL 生成与执行。
 */
@Service
@RequiredArgsConstructor
public class LowcodeDdlService {

    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("^[a-z][a-z0-9_]{1,63}$");
    private static final Set<String> INDEXABLE_TYPES = Set.of(
            "varchar", "char", "int", "bigint", "decimal", "date", "datetime", "time", "tinyint"
    );

    private final LowcodeSchemaValidator schemaValidator;
    private final LowcodeDdlRepository ddlRepository;
    private final DynamicCrudRepository dynamicCrudRepository;

    public LowcodeDdlPreviewVO previewCreateTable(LowcodeModelSchema modelSchema) {
        schemaValidator.validateModel(modelSchema);
        LowcodeDdlPreviewVO preview = new LowcodeDdlPreviewVO();
        preview.setTableName(modelSchema.getTableName());

        boolean tableExists = ddlRepository.tableExists(modelSchema.getTableName());
        preview.setTableExists(tableExists);
        if (!tableExists) {
            preview.getDdlStatements().add(buildCreateTableSql(modelSchema, preview.getWarnings()));
        } else {
            if (!ddlRepository.hasAutoIncrementPrimaryId(modelSchema.getTableName())) {
                preview.setExecutable(false);
                preview.getWarnings().add("已有表必须包含 `id` bigint AUTO_INCREMENT PRIMARY KEY 后才能绑定为低代码模型");
                return preview;
            }
            preview.getDdlStatements().addAll(buildAddColumnSql(modelSchema, preview.getWarnings()));
        }
        preview.setExecutable(true);
        if (preview.getDdlStatements().isEmpty()) {
            preview.getWarnings().add("数据表已存在，且没有需要追加的业务字段");
        }
        return preview;
    }

    @Transactional(rollbackFor = Exception.class)
    public void executeCreateTable(LowcodeModelSchema modelSchema) {
        LowcodeDdlPreviewVO preview = previewCreateTable(modelSchema);
        if (!Boolean.TRUE.equals(preview.getExecutable())) {
            throw new BusinessException("DDL预览不可执行");
        }
        for (String ddl : preview.getDdlStatements()) {
            assertSafeDdl(ddl);
            ddlRepository.executeDdl(ddl);
        }
        dynamicCrudRepository.clearTableMetadataCache(modelSchema.getTableName());
    }

    public boolean tableExists(String tableName) {
        validateIdentifier(tableName, "表名");
        return ddlRepository.tableExists(tableName);
    }

    public boolean hasAutoIncrementPrimaryId(String tableName) {
        validateIdentifier(tableName, "表名");
        return ddlRepository.hasAutoIncrementPrimaryId(tableName);
    }

    private String buildCreateTableSql(LowcodeModelSchema modelSchema, List<String> warnings) {
        List<String> definitions = new ArrayList<>();
        definitions.add("`id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID'");
        definitions.add("`tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户编号'");
        for (LowcodeFieldSchema field : businessFields(modelSchema)) {
            definitions.add(buildColumnDefinition(field, false));
        }
        definitions.add("`del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志'");
        definitions.add("`create_by` bigint DEFAULT NULL COMMENT '创建人ID'");
        definitions.add("`create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'");
        definitions.add("`create_dept` bigint DEFAULT NULL COMMENT '创建部门ID'");
        definitions.add("`update_by` bigint DEFAULT NULL COMMENT '更新人ID'");
        definitions.add("`update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'");
        definitions.add("PRIMARY KEY (`id`)");
        definitions.add("KEY `idx_" + modelSchema.getTableName() + "_tenant` (`tenant_id`)");
        definitions.add("KEY `idx_" + modelSchema.getTableName() + "_create_time` (`create_time`)");
        appendIndexDefinitions(modelSchema, definitions, warnings);

        return "CREATE TABLE IF NOT EXISTS `" + modelSchema.getTableName() + "` (\n  "
                + String.join(",\n  ", definitions)
                + "\n) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='"
                + escapeSqlComment(StringUtils.defaultIfBlank(modelSchema.getBusinessName(), modelSchema.getTableName()))
                + "'";
    }

    private List<String> buildAddColumnSql(LowcodeModelSchema modelSchema, List<String> warnings) {
        Set<String> existingColumns = ddlRepository.listColumns(modelSchema.getTableName());
        List<String> ddlList = new ArrayList<>();
        appendMissingSystemColumns(modelSchema.getTableName(), existingColumns, ddlList);
        for (LowcodeFieldSchema field : businessFields(modelSchema)) {
            if (existingColumns.contains(field.getColumnName())) {
                continue;
            }
            ddlList.add("ALTER TABLE `" + modelSchema.getTableName() + "` ADD COLUMN "
                    + buildColumnDefinition(field, true));
        }
        appendMissingIndexes(modelSchema, ddlRepository.listIndexes(modelSchema.getTableName()), ddlList, warnings);
        warnings.add("已有表在线变更仅追加缺失系统字段和业务字段，不会删除、重命名或修改已有字段类型");
        warnings.add("为避免历史数据写入失败，已有表追加字段统一按可空列生成，表单必填仍由运行时校验");
        return ddlList;
    }

    private void appendMissingSystemColumns(String tableName, Set<String> existingColumns, List<String> ddlList) {
        appendMissingColumn(tableName, existingColumns, ddlList, "tenant_id",
                "`tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户编号'");
        appendMissingColumn(tableName, existingColumns, ddlList, "del_flag",
                "`del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志'");
        appendMissingColumn(tableName, existingColumns, ddlList, "create_by",
                "`create_by` bigint DEFAULT NULL COMMENT '创建人ID'");
        appendMissingColumn(tableName, existingColumns, ddlList, "create_time",
                "`create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'");
        appendMissingColumn(tableName, existingColumns, ddlList, "create_dept",
                "`create_dept` bigint DEFAULT NULL COMMENT '创建部门ID'");
        appendMissingColumn(tableName, existingColumns, ddlList, "update_by",
                "`update_by` bigint DEFAULT NULL COMMENT '更新人ID'");
        appendMissingColumn(tableName, existingColumns, ddlList, "update_time",
                "`update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'");
    }

    private void appendMissingColumn(String tableName, Set<String> existingColumns, List<String> ddlList,
                                     String columnName, String columnDefinition) {
        if (!existingColumns.contains(columnName)) {
            ddlList.add("ALTER TABLE `" + tableName + "` ADD COLUMN " + columnDefinition);
        }
    }

    private void appendIndexDefinitions(LowcodeModelSchema modelSchema, List<String> definitions, List<String> warnings) {
        Set<String> indexNames = new HashSet<>();
        for (LowcodeFieldSchema field : businessFields(modelSchema)) {
            String dataType = normalizeDataType(field);
            if (!Boolean.TRUE.equals(field.getSearchable()) || !INDEXABLE_TYPES.contains(dataType)) {
                continue;
            }
            if ("varchar".equals(dataType) && normalizeLength(field, 255, 1, 2048) > 191) {
                warnings.add("字段 " + field.getLabel() + " 长度超过191，建表预览不自动创建索引");
                continue;
            }
            appendIndexDefinition(definitions, warnings, indexNames, modelSchema, "idx_" + field.getColumnName(),
                    List.of(field.getField()), false);
        }
        for (LowcodeRelationSchema relation : relationList(modelSchema)) {
            if (StringUtils.isBlank(relation.getSourceField())) {
                continue;
            }
            appendIndexDefinition(definitions, warnings, indexNames, modelSchema, "idx_rel_" + relation.getSourceField(),
                    List.of(relation.getSourceField()), false);
        }
        for (LowcodeIndexSchema index : indexList(modelSchema)) {
            if (index == null || index.getFields() == null || index.getFields().isEmpty()) {
                continue;
            }
            appendIndexDefinition(definitions, warnings, indexNames, modelSchema, index.getIndexName(),
                    index.getFields(), isUniqueIndex(index));
        }
    }

    private void appendMissingIndexes(LowcodeModelSchema modelSchema, Set<String> existingIndexNames,
                                      List<String> ddlList, List<String> warnings) {
        Set<String> emitted = new HashSet<>(existingIndexNames);
        List<String> definitions = new ArrayList<>();
        appendIndexDefinitions(modelSchema, definitions, warnings);
        for (String definition : definitions) {
            String indexName = extractIndexName(definition);
            if (StringUtils.isBlank(indexName) || emitted.contains(indexName)) {
                continue;
            }
            ddlList.add("ALTER TABLE `" + modelSchema.getTableName() + "` ADD " + definition);
            emitted.add(indexName);
        }
    }

    private void appendIndexDefinition(List<String> definitions, List<String> warnings, Set<String> indexNames,
                                       LowcodeModelSchema modelSchema, String preferredName,
                                       List<String> fieldNames, boolean unique) {
        List<String> columns = resolveIndexColumns(modelSchema, fieldNames, warnings);
        if (columns.isEmpty()) {
            return;
        }
        String indexName = normalizeIndexName(StringUtils.defaultIfBlank(preferredName,
                "idx_" + String.join("_", columns)));
        if (!indexNames.add(indexName)) {
            return;
        }
        String columnSql = columns.stream()
                .map(column -> "`" + column + "`")
                .reduce((left, right) -> left + ", " + right)
                .orElse("");
        definitions.add((unique ? "UNIQUE KEY" : "KEY") + " `" + indexName + "` (" + columnSql + ")");
    }

    private List<String> resolveIndexColumns(LowcodeModelSchema modelSchema, List<String> fieldNames, List<String> warnings) {
        Map<String, LowcodeFieldSchema> fieldMap = buildFieldMap(modelSchema);
        Set<String> columns = new LinkedHashSet<>();
        for (String fieldName : fieldNames) {
            LowcodeFieldSchema field = fieldMap.get(fieldName);
            if (field == null || Boolean.TRUE.equals(field.getSystemField())) {
                continue;
            }
            String dataType = normalizeDataType(field);
            if (!INDEXABLE_TYPES.contains(dataType)) {
                warnings.add("字段 " + field.getLabel() + " 类型不适合创建索引");
                continue;
            }
            if ("varchar".equals(dataType) && normalizeLength(field, 255, 1, 2048) > 191) {
                warnings.add("字段 " + field.getLabel() + " 长度超过191，不创建索引");
                continue;
            }
            columns.add(field.getColumnName());
        }
        return new ArrayList<>(columns);
    }

    private Map<String, LowcodeFieldSchema> buildFieldMap(LowcodeModelSchema modelSchema) {
        Map<String, LowcodeFieldSchema> fieldMap = new LinkedHashMap<>();
        for (LowcodeFieldSchema field : businessFields(modelSchema)) {
            fieldMap.put(field.getField(), field);
            fieldMap.put(field.getColumnName(), field);
        }
        return fieldMap;
    }

    private boolean isUniqueIndex(LowcodeIndexSchema index) {
        return Boolean.TRUE.equals(index.getUnique())
                || "UNIQUE".equalsIgnoreCase(StringUtils.defaultString(index.getIndexType()));
    }

    private List<LowcodeRelationSchema> relationList(LowcodeModelSchema modelSchema) {
        return modelSchema.getRelations() == null ? List.of() : modelSchema.getRelations();
    }

    private List<LowcodeIndexSchema> indexList(LowcodeModelSchema modelSchema) {
        return modelSchema.getIndexes() == null ? List.of() : modelSchema.getIndexes();
    }

    private String normalizeIndexName(String indexName) {
        String normalized = StringUtils.defaultIfBlank(indexName, "idx_lowcode")
                .replaceAll("[^a-zA-Z0-9_]", "_")
                .replaceAll("_+", "_")
                .toLowerCase(Locale.ROOT);
        if (!normalized.startsWith("idx_") && !normalized.startsWith("uk_")) {
            normalized = "idx_" + normalized;
        }
        return normalized.length() > 64 ? normalized.substring(0, 64) : normalized;
    }

    private String extractIndexName(String definition) {
        int start = definition.indexOf('`');
        int end = definition.indexOf('`', start + 1);
        return start >= 0 && end > start ? definition.substring(start + 1, end) : null;
    }

    private String buildColumnDefinition(LowcodeFieldSchema field, boolean forceNullable) {
        validateIdentifier(field.getColumnName(), "字段列名");
        String dataType = normalizeDataType(field);
        String sqlType = resolveSqlType(field, dataType);
        String nullable = !forceNullable && Boolean.TRUE.equals(field.getRequired()) ? " NOT NULL" : " DEFAULT NULL";
        return "`" + field.getColumnName() + "` " + sqlType + nullable
                + " COMMENT '" + escapeSqlComment(StringUtils.defaultIfBlank(field.getLabel(), field.getColumnName())) + "'";
    }

    private String resolveSqlType(LowcodeFieldSchema field, String dataType) {
        return switch (dataType) {
            case "varchar" -> "varchar(" + normalizeLength(field, 255, 1, 2048) + ")";
            case "char" -> "char(" + normalizeLength(field, 1, 1, 255) + ")";
            case "text", "longtext", "date", "datetime", "time" -> dataType;
            case "int" -> "int";
            case "bigint" -> "bigint";
            case "tinyint" -> "tinyint";
            case "decimal" -> "decimal(" + normalizeDecimalPrecision(field) + ")";
            default -> throw new BusinessException("不支持的数据类型: " + dataType);
        };
    }

    private String normalizeDataType(LowcodeFieldSchema field) {
        return StringUtils.defaultIfBlank(field.getDataType(), "varchar").toLowerCase(Locale.ROOT);
    }

    private int normalizeLength(LowcodeFieldSchema field, int defaultLength, int min, int max) {
        Integer length = field.getLength();
        if (length == null) {
            return defaultLength;
        }
        if (length < min || length > max) {
            throw new BusinessException("字段长度超出允许范围: " + field.getLabel());
        }
        return length;
    }

    private String normalizeDecimalPrecision(LowcodeFieldSchema field) {
        int length = field.getLength() != null ? field.getLength() : 18;
        int precision = field.getPrecision() != null ? field.getPrecision() : 2;
        if (length < 1 || length > 65 || precision < 0 || precision >= length) {
            throw new BusinessException("decimal精度配置不正确: " + field.getLabel());
        }
        return length + "," + precision;
    }

    private void validateIdentifier(String identifier, String label) {
        if (StringUtils.isBlank(identifier) || !SAFE_IDENTIFIER.matcher(identifier).matches()) {
            throw new BusinessException(label + "格式不正确: " + identifier);
        }
    }

    private String escapeSqlComment(String value) {
        return StringUtils.defaultString(value).replace("'", "''");
    }

    private void assertSafeDdl(String ddl) {
        String normalized = ddl.trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith("CREATE TABLE IF NOT EXISTS")) {
            return;
        }
        if (normalized.startsWith("ALTER TABLE") && normalized.contains(" ADD COLUMN ")) {
            return;
        }
        if (normalized.startsWith("ALTER TABLE") && (normalized.contains(" ADD KEY ") || normalized.contains(" ADD UNIQUE KEY "))) {
            return;
        }
        throw new BusinessException("仅允许执行受控 CREATE TABLE 或 ALTER TABLE ADD COLUMN 语句");
    }

    private List<LowcodeFieldSchema> businessFields(LowcodeModelSchema modelSchema) {
        if (modelSchema.getFields() == null) {
            return List.of();
        }
        return modelSchema.getFields().stream()
                .filter(field -> field != null && !Boolean.TRUE.equals(field.getSystemField()))
                .filter(field -> !"id".equals(field.getColumnName())
                        && !"tenant_id".equals(field.getColumnName())
                        && !"create_by".equals(field.getColumnName())
                        && !"create_time".equals(field.getColumnName())
                        && !"create_dept".equals(field.getColumnName())
                        && !"update_by".equals(field.getColumnName())
                        && !"update_time".equals(field.getColumnName())
                        && !"del_flag".equals(field.getColumnName()))
                .toList();
    }
}

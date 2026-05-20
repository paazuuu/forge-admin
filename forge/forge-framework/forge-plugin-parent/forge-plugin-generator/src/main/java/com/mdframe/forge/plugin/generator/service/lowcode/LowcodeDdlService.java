package com.mdframe.forge.plugin.generator.service.lowcode;

import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.service.DynamicCrudRepository;
import com.mdframe.forge.plugin.generator.vo.lowcode.LowcodeDdlPreviewVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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

    private String buildCreateTableSql(LowcodeModelSchema modelSchema, List<String> warnings) {
        List<String> definitions = new ArrayList<>();
        definitions.add("`id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID'");
        definitions.add("`tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户编号'");
        for (LowcodeFieldSchema field : modelSchema.getFields()) {
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
        appendBusinessIndexes(modelSchema, definitions, warnings);

        return "CREATE TABLE IF NOT EXISTS `" + modelSchema.getTableName() + "` (\n  "
                + String.join(",\n  ", definitions)
                + "\n) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='"
                + escapeSqlComment(StringUtils.defaultIfBlank(modelSchema.getBusinessName(), modelSchema.getTableName()))
                + "'";
    }

    private List<String> buildAddColumnSql(LowcodeModelSchema modelSchema, List<String> warnings) {
        Set<String> existingColumns = ddlRepository.listColumns(modelSchema.getTableName());
        List<String> ddlList = new ArrayList<>();
        for (LowcodeFieldSchema field : modelSchema.getFields()) {
            if (existingColumns.contains(field.getColumnName())) {
                continue;
            }
            ddlList.add("ALTER TABLE `" + modelSchema.getTableName() + "` ADD COLUMN "
                    + buildColumnDefinition(field, true));
        }
        warnings.add("已有表在线变更仅追加缺失字段，不会删除、重命名或修改已有字段类型");
        warnings.add("为避免历史数据写入失败，已有表追加字段统一按可空列生成，表单必填仍由运行时校验");
        return ddlList;
    }

    private void appendBusinessIndexes(LowcodeModelSchema modelSchema, List<String> definitions, List<String> warnings) {
        for (LowcodeFieldSchema field : modelSchema.getFields()) {
            String dataType = normalizeDataType(field);
            if (!Boolean.TRUE.equals(field.getSearchable()) || !INDEXABLE_TYPES.contains(dataType)) {
                continue;
            }
            if ("varchar".equals(dataType) && normalizeLength(field, 255, 1, 2048) > 191) {
                warnings.add("字段 " + field.getLabel() + " 长度超过191，建表预览不自动创建索引");
                continue;
            }
            String indexName = "idx_" + modelSchema.getTableName() + "_" + field.getColumnName();
            if (indexName.length() > 64) {
                indexName = indexName.substring(0, 64);
            }
            definitions.add("KEY `" + indexName + "` (`" + field.getColumnName() + "`)");
        }
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
        throw new BusinessException("仅允许执行受控 CREATE TABLE 或 ALTER TABLE ADD COLUMN 语句");
    }
}

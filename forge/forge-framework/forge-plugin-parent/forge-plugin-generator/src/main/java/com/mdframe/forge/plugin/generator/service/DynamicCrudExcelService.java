package com.mdframe.forge.plugin.generator.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.dto.DynamicCrudImportResult;
import com.mdframe.forge.plugin.generator.dto.DynamicCrudQuery;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.excel.model.ExcelColumnConfig;
import com.mdframe.forge.starter.excel.spi.ExcelConfigProvider;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 动态 CRUD Excel 导入导出服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicCrudExcelService {

    private static final int MAX_EXPORT_ROWS = 10000;
    private static final int MAX_IMPORT_ROWS = 5000;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final DynamicCrudService dynamicCrudService;
    private final ObjectMapper objectMapper;
    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    private final ObjectProvider<ExcelConfigProvider> excelConfigProvider;

    public void exportExcel(String configKey, DynamicCrudQuery query, HttpServletResponse response) {
        AiCrudConfig config = dynamicCrudService.getRuntimeConfig(configKey);
        List<ExcelColumnMeta> columns = resolveExportColumns(config);
        if (columns.isEmpty()) {
            throw new BusinessException("没有可导出的字段");
        }

        List<Map<String, Object>> rows = dynamicCrudService.selectExportRows(configKey, query, MAX_EXPORT_ROWS);
        List<List<String>> headers = buildHeaders(columns);
        List<List<Object>> data = rows.stream()
                .map(row -> buildExportRow(row, columns))
                .toList();

        writeWorkbook(response, buildFileName(config, "导出数据"), "数据", headers, data);
    }

    public void downloadImportTemplate(String configKey, HttpServletResponse response) {
        AiCrudConfig config = dynamicCrudService.getRuntimeConfig(configKey);
        List<ExcelColumnMeta> columns = resolveImportColumns(config);
        if (columns.isEmpty()) {
            throw new BusinessException("没有可导入的字段");
        }
        writeWorkbook(response, buildFileName(config, "导入模板"), "导入模板", buildHeaders(columns), List.of());
    }

    @Transactional(rollbackFor = Exception.class)
    public DynamicCrudImportResult importExcel(String configKey, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("导入文件不能为空");
        }

        AiCrudConfig config = dynamicCrudService.getRuntimeConfig(configKey);
        List<ExcelColumnMeta> columns = resolveImportColumns(config);
        if (columns.isEmpty()) {
            throw new BusinessException("没有可导入的字段");
        }

        ExcelReadResult readResult = readExcel(file);
        DynamicCrudImportResult result = new DynamicCrudImportResult();
        result.setTotalRows(readResult.getRows().size());

        if (readResult.getRows().size() > MAX_IMPORT_ROWS) {
            result.addError(1, null, null, readResult.getRows().size(), "单次最多导入" + MAX_IMPORT_ROWS + "行");
            return finishFailure(result);
        }

        Map<Integer, ExcelColumnMeta> headerMapping = buildHeaderMapping(readResult, columns, result);
        if (!result.getErrors().isEmpty()) {
            return finishFailure(result);
        }

        List<Map<String, Object>> importRows = buildImportRows(readResult.getRows(), headerMapping, result);
        if (!result.getErrors().isEmpty()) {
            return finishFailure(result);
        }

        for (Map<String, Object> row : importRows) {
            dynamicCrudService.insert(configKey, row);
        }

        result.setSuccess(true);
        result.setSuccessRows(importRows.size());
        result.setFailedRows(0);
        result.setSummary("导入成功，共" + importRows.size() + "行");
        return result;
    }

    private List<Map<String, Object>> buildImportRows(List<ExcelRow> rows,
                                                      Map<Integer, ExcelColumnMeta> headerMapping,
                                                      DynamicCrudImportResult result) {
        List<Map<String, Object>> importRows = new ArrayList<>();
        for (ExcelRow excelRow : rows) {
            if (isBlankRow(excelRow.getValues())) {
                continue;
            }

            Map<String, Object> data = new LinkedHashMap<>();
            for (Map.Entry<Integer, ExcelColumnMeta> entry : headerMapping.entrySet()) {
                Object rawValue = excelRow.getValues().get(entry.getKey());
                ExcelColumnMeta meta = entry.getValue();
                Object normalizedValue = normalizeCellValue(rawValue);

                if (isEmptyValue(normalizedValue)) {
                    if (meta.isRequired()) {
                        result.addError(excelRow.getRowNum(), meta.getField(), meta.getLabel(), rawValue,
                                meta.getLabel() + "不能为空");
                    }
                    continue;
                }

                try {
                    Object convertedValue = convertImportValue(meta, normalizedValue);
                    data.put(meta.getField(), convertedValue);
                } catch (Exception e) {
                    result.addError(excelRow.getRowNum(), meta.getField(), meta.getLabel(), rawValue, e.getMessage());
                }
            }

            if (!data.isEmpty()) {
                importRows.add(data);
            }
        }
        return importRows;
    }

    private Object convertImportValue(ExcelColumnMeta meta, Object value) {
        Object convertedValue = value;
        if (StringUtils.isNotBlank(meta.getDictType())) {
            String dictValue = resolveDictValue(meta.getDictType(), toCellText(value));
            if (dictValue == null) {
                throw new BusinessException("无法识别字典值: " + toCellText(value));
            }
            convertedValue = dictValue;
        }

        return convertByDataType(meta, convertedValue);
    }

    private Object convertByDataType(ExcelColumnMeta meta, Object value) {
        String dataType = StringUtils.defaultIfBlank(meta.getDataType(), "").toLowerCase(Locale.ROOT);
        String componentType = StringUtils.defaultIfBlank(meta.getType(), "").toLowerCase(Locale.ROOT);

        if (Set.of("int", "tinyint").contains(dataType)) {
            return Integer.valueOf(toNumberText(value));
        }
        if ("bigint".equals(dataType)) {
            return Long.valueOf(toNumberText(value));
        }
        if ("decimal".equals(dataType) || "number".equals(componentType) || "inputnumber".equals(componentType)) {
            return new BigDecimal(toNumberText(value));
        }
        if ("date".equals(dataType) || "date".equals(componentType)) {
            return formatDateValue(value);
        }
        if ("datetime".equals(dataType) || "datetime".equals(componentType)) {
            return formatDateTimeValue(value);
        }
        if ("time".equals(dataType) || "time".equals(componentType)) {
            return formatTimeValue(value);
        }
        return value;
    }

    private String resolveDictValue(String dictType, String labelOrValue) {
        if (StringUtils.isBlank(dictType) || StringUtils.isBlank(labelOrValue)) {
            return null;
        }

        StringBuilder sql = new StringBuilder("""
                SELECT dict_value
                FROM sys_dict_data
                WHERE dict_type = :dictType
                  AND (dict_label = :value OR dict_value = :value)
                """);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("dictType", dictType)
                .addValue("value", labelOrValue);

        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            sql.append(" AND tenant_id = :tenantId");
            params.addValue("tenantId", tenantId);
        }
        sql.append(" ORDER BY CASE WHEN dict_label = :value THEN 0 ELSE 1 END, dict_sort ASC LIMIT 1");

        List<String> values = namedJdbcTemplate.queryForList(sql.toString(), params, String.class);
        return values.isEmpty() ? null : values.get(0);
    }

    private ExcelReadResult readExcel(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            MapRowReadListener listener = new MapRowReadListener();
            EasyExcel.read(inputStream, listener).sheet().doRead();
            ExcelReadResult result = new ExcelReadResult();
            result.setHeaders(listener.getHeaders());
            result.setRows(listener.getRows());
            return result;
        } catch (IOException e) {
            throw new BusinessException("读取导入文件失败: " + e.getMessage());
        }
    }

    private Map<Integer, ExcelColumnMeta> buildHeaderMapping(ExcelReadResult readResult,
                                                            List<ExcelColumnMeta> columns,
                                                            DynamicCrudImportResult result) {
        Map<String, ExcelColumnMeta> knownHeaders = new HashMap<>();
        for (ExcelColumnMeta column : columns) {
            knownHeaders.put(normalizeHeader(column.getLabel()), column);
            knownHeaders.put(normalizeHeader(column.getField()), column);
        }

        Map<Integer, ExcelColumnMeta> mapping = new LinkedHashMap<>();
        for (Map.Entry<Integer, String> entry : readResult.getHeaders().entrySet()) {
            ExcelColumnMeta column = knownHeaders.get(normalizeHeader(entry.getValue()));
            if (column != null) {
                mapping.put(entry.getKey(), column);
            }
        }

        if (mapping.isEmpty()) {
            result.addError(1, null, null, null, "导入模板不匹配，未识别到可导入字段");
            return mapping;
        }

        Set<String> presentFields = mapping.values().stream()
                .map(ExcelColumnMeta::getField)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        for (ExcelColumnMeta column : columns) {
            if (column.isRequired() && !presentFields.contains(column.getField())) {
                result.addError(1, column.getField(), column.getLabel(), null,
                        "导入模板缺少必填列: " + column.getLabel());
            }
        }
        return mapping;
    }

    private DynamicCrudImportResult finishFailure(DynamicCrudImportResult result) {
        result.setSuccess(false);
        result.setSuccessRows(0);
        long failedRows = result.getErrors().stream()
                .map(error -> error.getRowNum() == null ? 0 : error.getRowNum())
                .distinct()
                .count();
        result.setFailedRows((int) failedRows);
        result.setSummary("导入校验失败，共" + result.getErrors().size() + "个错误");
        return result;
    }

    private List<ExcelColumnMeta> resolveExportColumns(AiCrudConfig config) {
        JsonNode schema = readArray(config.getColumnsSchema(), "columnsSchema");
        Map<String, TransMeta> transConfig = readTransConfig(config.getTransConfig());
        Map<String, String> modelDataTypes = readModelDataTypes(config.getModelSchema());

        List<ExcelColumnMeta> columns = new ArrayList<>();
        for (JsonNode node : schema) {
            String field = getFirstText(node, "prop", "dataIndex", "key", "field");
            if (StringUtils.isBlank(field) || isActionField(field)) {
                continue;
            }
            ExcelColumnMeta meta = new ExcelColumnMeta();
            meta.setField(field);
            meta.setLabel(StringUtils.defaultIfBlank(getFirstText(node, "title", "label", "columnName"), field));
            meta.setDataType(modelDataTypes.get(field));
            meta.setDictType(resolveDictType(field, node, transConfig));
            meta.setTargetField(resolveTargetField(field, node, transConfig));
            columns.add(meta);
        }
        return applyExportColumnConfig(config, columns);
    }

    private List<ExcelColumnMeta> resolveImportColumns(AiCrudConfig config) {
        JsonNode schema = readArray(config.getEditSchema(), "editSchema");
        Map<String, TransMeta> transConfig = readTransConfig(config.getTransConfig());
        Map<String, String> modelDataTypes = readModelDataTypes(config.getModelSchema());

        List<ExcelColumnMeta> columns = new ArrayList<>();
        for (JsonNode node : schema) {
            String field = getFirstText(node, "field", "prop", "dataIndex", "key");
            if (StringUtils.isBlank(field) || isActionField(field)) {
                continue;
            }
            ExcelColumnMeta meta = new ExcelColumnMeta();
            meta.setField(field);
            meta.setLabel(StringUtils.defaultIfBlank(getFirstText(node, "label", "title", "columnName"), field));
            meta.setType(getFirstText(node, "type", "componentType"));
            meta.setDataType(StringUtils.defaultIfBlank(modelDataTypes.get(field), inferDataType(meta.getType())));
            meta.setDictType(resolveDictType(field, node, transConfig));
            meta.setRequired(resolveRequired(node));
            columns.add(meta);
        }
        return applyImportColumnConfig(config, columns);
    }

    private List<ExcelColumnMeta> applyExportColumnConfig(AiCrudConfig config, List<ExcelColumnMeta> schemaColumns) {
        List<ExcelColumnConfig> configuredColumns = loadColumnConfigs(config.getConfigKey());
        if (configuredColumns.isEmpty()) {
            return schemaColumns;
        }

        Map<String, ExcelColumnMeta> schemaColumnMap = schemaColumns.stream()
                .collect(Collectors.toMap(
                        ExcelColumnMeta::getField,
                        column -> column,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        List<ExcelColumnMeta> orderedColumns = new ArrayList<>();
        for (ExcelColumnConfig columnConfig : configuredColumns) {
            if (Boolean.FALSE.equals(columnConfig.getExport()) || StringUtils.isBlank(columnConfig.getFieldName())) {
                continue;
            }
            ExcelColumnMeta schemaColumn = schemaColumnMap.get(columnConfig.getFieldName());
            if (schemaColumn == null) {
                continue;
            }
            orderedColumns.add(mergeColumnConfig(schemaColumn, columnConfig));
        }
        return orderedColumns.isEmpty() ? schemaColumns : orderedColumns;
    }

    private List<ExcelColumnMeta> applyImportColumnConfig(AiCrudConfig config, List<ExcelColumnMeta> schemaColumns) {
        List<ExcelColumnConfig> configuredColumns = loadColumnConfigs(config.getConfigKey());
        if (configuredColumns.isEmpty()) {
            return schemaColumns;
        }

        Map<String, ExcelColumnConfig> configMap = configuredColumns.stream()
                .filter(column -> StringUtils.isNotBlank(column.getFieldName()))
                .collect(Collectors.toMap(
                        ExcelColumnConfig::getFieldName,
                        column -> column,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        List<ExcelColumnMeta> orderedColumns = new ArrayList<>();
        Set<String> appendedFields = new LinkedHashSet<>();
        for (ExcelColumnConfig columnConfig : configuredColumns) {
            if (Boolean.FALSE.equals(columnConfig.getImportable()) || StringUtils.isBlank(columnConfig.getFieldName())) {
                continue;
            }
            ExcelColumnMeta schemaColumn = findColumn(schemaColumns, columnConfig.getFieldName());
            if (schemaColumn == null) {
                continue;
            }
            orderedColumns.add(mergeColumnConfig(schemaColumn, columnConfig));
            appendedFields.add(schemaColumn.getField());
        }
        for (ExcelColumnMeta schemaColumn : schemaColumns) {
            if (appendedFields.contains(schemaColumn.getField())) {
                continue;
            }
            ExcelColumnConfig columnConfig = configMap.get(schemaColumn.getField());
            if (Boolean.FALSE.equals(columnConfig != null ? columnConfig.getImportable() : null)) {
                continue;
            }
            orderedColumns.add(columnConfig == null ? schemaColumn : mergeColumnConfig(schemaColumn, columnConfig));
        }
        return orderedColumns;
    }

    private ExcelColumnMeta findColumn(List<ExcelColumnMeta> columns, String fieldName) {
        return columns.stream()
                .filter(column -> fieldName.equals(column.getField()))
                .findFirst()
                .orElse(null);
    }

    private ExcelColumnMeta mergeColumnConfig(ExcelColumnMeta source, ExcelColumnConfig columnConfig) {
        ExcelColumnMeta target = new ExcelColumnMeta();
        target.setField(source.getField());
        target.setLabel(StringUtils.defaultIfBlank(columnConfig.getColumnName(), source.getLabel()));
        target.setType(source.getType());
        target.setDataType(source.getDataType());
        target.setDictType(StringUtils.defaultIfBlank(columnConfig.getDictType(), source.getDictType()));
        target.setTargetField(source.getTargetField());
        target.setRequired(columnConfig.getRequired() != null ? columnConfig.getRequired() : source.isRequired());
        return target;
    }

    private List<ExcelColumnConfig> loadColumnConfigs(String configKey) {
        ExcelConfigProvider provider = excelConfigProvider.getIfAvailable();
        if (provider == null || StringUtils.isBlank(configKey)) {
            return List.of();
        }
        try {
            List<ExcelColumnConfig> columns = provider.getColumnConfigs(configKey);
            if (columns == null || columns.isEmpty()) {
                return List.of();
            }
            return columns.stream()
                    .filter(Objects::nonNull)
                    .sorted((left, right) -> {
                        Integer leftOrder = left.getOrderNum() == null ? Integer.MAX_VALUE : left.getOrderNum();
                        Integer rightOrder = right.getOrderNum() == null ? Integer.MAX_VALUE : right.getOrderNum();
                        return leftOrder.compareTo(rightOrder);
                    })
                    .toList();
        } catch (Exception e) {
            log.warn("[DynamicCrudExcelService] 读取Excel列配置失败, configKey={}", configKey, e);
            return List.of();
        }
    }

    private List<List<String>> buildHeaders(List<ExcelColumnMeta> columns) {
        return columns.stream()
                .map(column -> List.of(column.getLabel()))
                .toList();
    }

    private List<Object> buildExportRow(Map<String, Object> row, List<ExcelColumnMeta> columns) {
        List<Object> values = new ArrayList<>();
        for (ExcelColumnMeta column : columns) {
            Object value = row.get(column.getField());
            if (StringUtils.isNotBlank(column.getTargetField())) {
                Object displayValue = row.get(column.getTargetField());
                if (!isEmptyValue(displayValue)) {
                    value = displayValue;
                }
            }
            values.add(value);
        }
        return values;
    }

    private void writeWorkbook(HttpServletResponse response,
                               String fileName,
                               String sheetName,
                               List<List<String>> headers,
                               List<List<Object>> rows) {
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");
            response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + encodedFileName);
            EasyExcel.write(response.getOutputStream())
                    .head(headers)
                    .sheet(sheetName)
                    .doWrite(rows);
        } catch (IOException e) {
            throw new BusinessException("写入Excel失败: " + e.getMessage());
        }
    }

    private JsonNode readArray(String json, String fieldName) {
        try {
            JsonNode node = StringUtils.isBlank(json) ? objectMapper.createArrayNode() : objectMapper.readTree(json);
            if (!node.isArray()) {
                throw new BusinessException(fieldName + "必须是数组");
            }
            return node;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(fieldName + "解析失败: " + e.getMessage());
        }
    }

    private Map<String, TransMeta> readTransConfig(String json) {
        Map<String, TransMeta> result = new HashMap<>();
        if (StringUtils.isBlank(json)) {
            return result;
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            if (!node.isObject()) {
                return result;
            }
            for (Map.Entry<String, JsonNode> entry : node.properties()) {
                JsonNode rule = entry.getValue();
                TransMeta meta = new TransMeta();
                meta.setDictType(getFirstText(rule, "dictType"));
                meta.setTargetField(StringUtils.defaultIfBlank(getFirstText(rule, "targetField"), entry.getKey() + "Name"));
                result.put(entry.getKey(), meta);
            }
        } catch (Exception e) {
            log.warn("[DynamicCrudExcelService] 解析transConfig失败", e);
        }
        return result;
    }

    private Map<String, String> readModelDataTypes(String modelSchemaJson) {
        Map<String, String> result = new HashMap<>();
        if (StringUtils.isBlank(modelSchemaJson)) {
            return result;
        }
        try {
            JsonNode fields = objectMapper.readTree(modelSchemaJson).get("fields");
            if (fields == null || !fields.isArray()) {
                return result;
            }
            for (JsonNode field : fields) {
                String fieldName = getFirstText(field, "field");
                String dataType = getFirstText(field, "dataType");
                if (StringUtils.isNotBlank(fieldName) && StringUtils.isNotBlank(dataType)) {
                    result.put(fieldName, dataType.toLowerCase(Locale.ROOT));
                }
            }
        } catch (Exception e) {
            log.warn("[DynamicCrudExcelService] 解析modelSchema字段类型失败", e);
        }
        return result;
    }

    private String resolveDictType(String field, JsonNode node, Map<String, TransMeta> transConfig) {
        String dictType = getFirstText(node, "dictType", "_dictType");
        if (StringUtils.isBlank(dictType) && node.has("render") && node.get("render").isObject()) {
            dictType = getFirstText(node.get("render"), "dictType");
        }
        if (StringUtils.isBlank(dictType) && transConfig.containsKey(field)) {
            dictType = transConfig.get(field).getDictType();
        }
        return dictType;
    }

    private String resolveTargetField(String field, JsonNode node, Map<String, TransMeta> transConfig) {
        TransMeta meta = transConfig.get(field);
        if (meta != null && StringUtils.isNotBlank(meta.getTargetField())) {
            return meta.getTargetField();
        }
        if (node != null && node.has("render") && node.get("render").isObject()) {
            JsonNode render = node.get("render");
            String targetField = getFirstText(render, "targetField");
            if (StringUtils.isNotBlank(targetField)) {
                return targetField;
            }
            String renderType = StringUtils.defaultIfBlank(getFirstText(render, "type"), "").toLowerCase(Locale.ROOT);
            if (Set.of("orgname", "username", "regionname", "fileupload", "imageupload").contains(renderType)) {
                return field + "Name";
            }
        }
        String componentType = StringUtils.defaultIfBlank(getFirstText(node, "type", "componentType"), "").toLowerCase(Locale.ROOT);
        if (Set.of("orgtreeselect", "userselect", "regiontreeselect", "treeselect").contains(componentType)) {
            return field + "Name";
        }
        return null;
    }

    private boolean resolveRequired(JsonNode node) {
        if (node.has("required") && node.get("required").asBoolean(false)) {
            return true;
        }
        JsonNode rules = node.get("rules");
        if (rules == null || !rules.isArray()) {
            return false;
        }
        for (JsonNode rule : rules) {
            if (rule.has("required") && rule.get("required").asBoolean(false)) {
                return true;
            }
        }
        return false;
    }

    private String getFirstText(JsonNode node, String... names) {
        if (node == null) {
            return null;
        }
        for (String name : names) {
            JsonNode value = node.get(name);
            if (value != null && !value.isNull() && StringUtils.isNotBlank(value.asText())) {
                return value.asText();
            }
        }
        return null;
    }

    private boolean isActionField(String field) {
        return "action".equals(field) || "actions".equals(field);
    }

    private String inferDataType(String componentType) {
        String normalized = StringUtils.defaultIfBlank(componentType, "").toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "number", "inputnumber", "input-number" -> "decimal";
            case "date" -> "date";
            case "datetime" -> "datetime";
            case "time" -> "time";
            default -> "";
        };
    }

    private boolean isBlankRow(Map<Integer, Object> values) {
        if (values == null || values.isEmpty()) {
            return true;
        }
        return values.values().stream()
                .map(this::normalizeCellValue)
                .allMatch(this::isEmptyValue);
    }

    private Object normalizeCellValue(Object value) {
        if (value instanceof String str) {
            return StringUtils.trimToNull(str);
        }
        return value;
    }

    private boolean isEmptyValue(Object value) {
        return value == null || (value instanceof String str && StringUtils.isBlank(str));
    }

    private String normalizeHeader(String value) {
        return StringUtils.defaultString(value).trim();
    }

    private String toCellText(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.stripTrailingZeros().toPlainString();
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString()).stripTrailingZeros().toPlainString();
        }
        if (value instanceof Date date) {
            return DATETIME_FORMATTER.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(date.getTime()), ZoneId.systemDefault()));
        }
        return StringUtils.trimToEmpty(String.valueOf(value));
    }

    private String toNumberText(Object value) {
        String text = toCellText(value);
        if (StringUtils.isBlank(text)) {
            throw new BusinessException("数字不能为空");
        }
        return text;
    }

    private String formatDateValue(Object value) {
        if (value instanceof Date date) {
            return DATE_FORMATTER.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(date.getTime()), ZoneId.systemDefault()));
        }
        String text = normalizeDateText(value);
        try {
            return DATE_FORMATTER.format(LocalDate.parse(text.substring(0, Math.min(10, text.length())), DATE_FORMATTER));
        } catch (DateTimeParseException e) {
            throw new BusinessException("日期格式应为 yyyy-MM-dd");
        }
    }

    private String formatDateTimeValue(Object value) {
        if (value instanceof Date date) {
            return DATETIME_FORMATTER.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(date.getTime()), ZoneId.systemDefault()));
        }
        String text = normalizeDateText(value);
        if (text.length() == 10) {
            text = text + " 00:00:00";
        }
        try {
            return DATETIME_FORMATTER.format(LocalDateTime.parse(text, DATETIME_FORMATTER));
        } catch (DateTimeParseException e) {
            throw new BusinessException("日期时间格式应为 yyyy-MM-dd HH:mm:ss");
        }
    }

    private String formatTimeValue(Object value) {
        if (value instanceof Date date) {
            return TIME_FORMATTER.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(date.getTime()), ZoneId.systemDefault()));
        }
        String text = toCellText(value);
        try {
            return TIME_FORMATTER.format(LocalTime.parse(text, TIME_FORMATTER));
        } catch (DateTimeParseException e) {
            throw new BusinessException("时间格式应为 HH:mm:ss");
        }
    }

    private String normalizeDateText(Object value) {
        String text = toCellText(value).replace("/", "-").replace("T", " ");
        if (StringUtils.isBlank(text)) {
            throw new BusinessException("日期不能为空");
        }
        return text;
    }

    private String buildFileName(AiCrudConfig config, String suffix) {
        String baseName = StringUtils.defaultIfBlank(config.getAppName(),
                StringUtils.defaultIfBlank(config.getTableComment(), config.getConfigKey()));
        String fileName = baseName + "_" + suffix + ".xlsx";
        return fileName.replaceAll("[\\\\/:*?\"<>|\\r\\n]", "_");
    }

    @Data
    private static class ExcelColumnMeta {
        private String field;
        private String label;
        private String type;
        private String dataType;
        private String dictType;
        private String targetField;
        private boolean required;
    }

    @Data
    private static class TransMeta {
        private String dictType;
        private String targetField;
    }

    @Data
    private static class ExcelRow {
        private Integer rowNum;
        private Map<Integer, Object> values = new LinkedHashMap<>();
    }

    @Data
    private static class ExcelReadResult {
        private Map<Integer, String> headers = new LinkedHashMap<>();
        private List<ExcelRow> rows = new ArrayList<>();
    }

    private static class MapRowReadListener extends AnalysisEventListener<Map<Integer, Object>> {

        private final Map<Integer, String> headers = new LinkedHashMap<>();
        private final List<ExcelRow> rows = new ArrayList<>();

        @Override
        public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
            headers.clear();
            headers.putAll(headMap);
        }

        @Override
        public void invoke(Map<Integer, Object> data, AnalysisContext context) {
            ExcelRow row = new ExcelRow();
            row.setRowNum(context.readRowHolder().getRowIndex() + 1);
            row.setValues(data.entrySet().stream()
                    .filter(entry -> entry.getKey() != null)
                    .filter(entry -> Objects.nonNull(entry.getValue()))
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (left, right) -> left,
                            LinkedHashMap::new
                    )));
            rows.add(row);
        }

        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {
            log.debug("[DynamicCrudExcelService] Excel解析完成, rows={}", rows.size());
        }

        public Map<Integer, String> getHeaders() {
            return headers;
        }

        public List<ExcelRow> getRows() {
            return rows;
        }
    }
}

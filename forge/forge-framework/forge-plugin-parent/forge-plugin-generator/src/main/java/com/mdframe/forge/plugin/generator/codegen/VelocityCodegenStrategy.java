package com.mdframe.forge.plugin.generator.codegen;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.domain.entity.AiPageTemplate;
import com.mdframe.forge.plugin.generator.domain.entity.GenTable;
import com.mdframe.forge.plugin.generator.domain.entity.GenTableColumn;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.mapper.GenTableColumnMapper;
import com.mdframe.forge.plugin.generator.util.GenUtils;
import com.mdframe.forge.plugin.generator.util.VelocityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.VelocityContext;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;

/**
 * 基于 Velocity 模板的代码生成策略（适用于简单 CRUD 等结构化组件）
 * <p>
 * 生成内容：
 * - 后端：Entity / Mapper / Mapper.xml / Service / ServiceImpl / Controller / DTO / Query
 * - SQL：menu.sql / dict.sql
 * - 前端：index.vue / api.js（使用业务路由前缀，非 /ai/crud 通用路由）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VelocityCodegenStrategy implements CodegenStrategy {

    private final GenTableColumnMapper genTableColumnMapper;
    private final ObjectMapper objectMapper;

    /** 默认包名 */
    private static final String DEFAULT_PACKAGE = "com.mdframe.forge";
    /** 默认作者 */
    private static final String DEFAULT_AUTHOR = "Forge Generator";

    @Override
    public boolean supports(String codegenType) {
        // TEMPLATE 类型，或 null/空（向前兼容，默认走模板生成）
        return codegenType == null || "TEMPLATE".equalsIgnoreCase(codegenType);
    }

    @Override
    public Map<String, String> generate(AiCrudConfig config, AiPageTemplate template) throws Exception {
        Map<String, String> files = new LinkedHashMap<>();

        // ── 1. 解析 apiConfig，提取业务路由前缀 ──────────────────────────────
        String apiBase = resolveApiBase(config);          // e.g. /order/manage
        String moduleName = StringUtils.defaultIfBlank(readOption(config, "moduleName", null),
                resolveModuleName(apiBase));   // e.g. order
        String businessPath = resolveBusinessPath(apiBase); // e.g. manage

        // ── 2. 构造 GenTable（复用已有的 VelocityUtils 体系）────────────────
        GenTable genTable = buildGenTable(config, moduleName, businessPath);

        // ── 3. 从数据库加载字段元数据 ─────────────────────────────────────────
        List<GenTableColumn> columns = loadColumns(config);
        genTable.setColumns(columns);
        genTable.setPkColumn(GenUtils.getPkColumn(columns));

        // ── 4. 解析四类安全配置，注入注解控制变量 ────────────────────────────
        Map<String, Object> annotationFlags = resolveAnnotationFlags(config, columns);

        // ── 5. 初始化 Velocity ────────────────────────────────────────────────
        VelocityUtils.initVelocity();
        VelocityContext ctx = VelocityUtils.prepareContext(genTable);

        // 注入注解控制变量
        annotationFlags.forEach(ctx::put);

        // 注入业务路由前缀（前端模板使用）
        ctx.put("apiBase", apiBase);
        ctx.put("date", LocalDate.now().toString());

        // 前端 configKey → 组件名/变量名
        String configKey = config.getConfigKey();
        ctx.put("configKey", configKey);
        ctx.put("componentName", toPascalCase(configKey));
        ctx.put("apiVarName", toPascalCase(configKey));
        ctx.put("tableComment", StringUtils.isNotBlank(config.getTableComment()) ? config.getTableComment() : configKey);

        // searchSchema / columnsSchema / editSchema / apiConfig（前端模板需要）
        Map<String, Object> transConfig = parseJsonObject(config.getTransConfig());
        ctx.put("searchSchema", parseJsonArray(config.getSearchSchema()));
        ctx.put("columnsSchema", preprocessColumnsSchema(parseJsonArray(config.getColumnsSchema()), transConfig));
        ctx.put("editSchema", parseJsonArray(config.getEditSchema()));
        ctx.put("apiConfig", parseJsonObject(config.getApiConfig()));
        ctx.put("layoutType", StringUtils.isNotBlank(config.getLayoutType()) ? config.getLayoutType() : "simple-crud");

        // ── 6. 渲染后端代码 ───────────────────────────────────────────────────
        String className = genTable.getClassName();
        String pkgPath = genTable.getPackageName().replace(".", "/") + "/" + moduleName + "/";
        String javaRoot = "backend/src/main/java/" + pkgPath;
        String resRoot = "backend/src/main/resources/";

        renderTo(files, "templates/vm/entity.java.vm",      ctx, javaRoot + "entity/" + className + ".java");
        renderTo(files, "templates/vm/mapper.java.vm",      ctx, javaRoot + "mapper/" + className + "Mapper.java");
        renderTo(files, "templates/vm/mapper.xml.vm",       ctx, resRoot  + "mapper/" + className + "Mapper.xml");
        renderTo(files, "templates/vm/service.java.vm",     ctx, javaRoot + "service/I" + className + "Service.java");
        renderTo(files, "templates/vm/serviceImpl.java.vm", ctx, javaRoot + "service/impl/" + className + "ServiceImpl.java");
        renderTo(files, "templates/vm/controller.java.vm",  ctx, javaRoot + "controller/" + className + "Controller.java");
        renderTo(files, "templates/vm/dto.java.vm",         ctx, javaRoot + "dto/" + className + "DTO.java");
        renderTo(files, "templates/vm/query.java.vm",       ctx, javaRoot + "dto/" + className + "Query.java");

        // ── 7. 渲染 SQL ───────────────────────────────────────────────────────
        boolean includeSql = readBooleanOption(config, "includeSql", true);
        boolean includeMenuSql = readBooleanOption(config, "includeMenuSql", true);
        boolean includeDictSql = readBooleanOption(config, "includeDictSql", true);
        if (includeSql && includeMenuSql) {
            renderTo(files, "templates/vm/sql/menu.sql.vm", ctx, "sql/" + config.getTableName() + "_menu.sql");
        }
        if (includeSql && includeDictSql && (boolean) annotationFlags.getOrDefault("hasDictConfig", false)) {
            renderTo(files, "templates/vm/sql/dict.sql.vm", ctx, "sql/" + config.getTableName() + "_dict.sql");
        }

        // ── 8. 渲染前端代码 ───────────────────────────────────────────────────
        String viewPath = configKey.replace("_", "/");
        String frontendBasePath = normalizeOutputBasePath(readOption(config, "frontendBasePath", "frontend/src/views"));
        renderTo(files, "templates/vm/ai-crud/index.vue.vm", ctx, frontendBasePath + "/" + viewPath + "/index.vue");
        renderTo(files, "templates/vm/ai-crud/api.js.vm",    ctx, "frontend/src/api/" + configKey + ".js");

        // ── 9. 附带原始配置 JSON ──────────────────────────────────────────────
        files.put("config/" + configKey + "-config.json", buildConfigJson(config));

        return files;
    }

    // ───────────────────────────────────────────────────────────────────────────
    // 业务路由解析
    // ───────────────────────────────────────────────────────────────────────────

    /**
     * 从 apiConfig.list 提取业务路由前缀
     * <p>
     * 规则：取 URL 中去掉 /page 之前的部分作为 BASE。
     * 例：get@/order/manage/page → /order/manage
     * 例：get@/ai/crud/{configKey}/page → 降级为 /{configKey}
     */
    private String resolveApiBase(AiCrudConfig config) {
        try {
            Map<String, Object> apiConf = parseJsonObject(config.getApiConfig());
            String listUrl = (String) apiConf.get("list");
            if (StringUtils.isBlank(listUrl)) return "/" + config.getConfigKey();

            // 去掉 "get@" 前缀
            String url = listUrl.contains("@") ? listUrl.split("@")[1] : listUrl;
            // 去掉 /page 后缀
            if (url.endsWith("/page")) {
                url = url.substring(0, url.length() - 5);
            }
            // 如果是 /ai/crud/xxx 通用路由，则降级为 configKey
            if (url.startsWith("/ai/crud/") || url.startsWith("/rest/")) {
                return "/" + config.getConfigKey().replace("_", "/");
            }
            return url;
        } catch (Exception e) {
            return "/" + config.getConfigKey().replace("_", "/");
        }
    }

    /**
     * 从 apiBase 中提取模块名（第一段）
     * /order/manage → order
     */
    private String resolveModuleName(String apiBase) {
        String[] parts = apiBase.split("/");
        for (String p : parts) {
            if (StringUtils.isNotBlank(p)) return p;
        }
        return "app";
    }

    /**
     * 从 apiBase 中提取业务路径（第二段起，用于 @RequestMapping）
     * /order/manage → manage
     * /order/manage/detail → manage/detail
     */
    private String resolveBusinessPath(String apiBase) {
        String[] parts = apiBase.split("/");
        List<String> nonEmpty = new ArrayList<>();
        for (String p : parts) {
            if (StringUtils.isNotBlank(p)) nonEmpty.add(p);
        }
        if (nonEmpty.size() <= 1) return nonEmpty.isEmpty() ? "data" : nonEmpty.get(0);
        return String.join("/", nonEmpty.subList(1, nonEmpty.size()));
    }

    // ───────────────────────────────────────────────────────────────────────────
    // GenTable 构建
    // ───────────────────────────────────────────────────────────────────────────

    private GenTable buildGenTable(AiCrudConfig config, String moduleName, String businessPath) {
        GenTable table = new GenTable();
        table.setTableName(config.getTableName());
        table.setTableComment(StringUtils.isNotBlank(config.getTableComment()) ? config.getTableComment() : config.getConfigKey());
        table.setFunctionName(table.getTableComment());

        // 类名：去掉表前缀（sys_/ai_），使用 PascalCase
        String className = toPascalCase(stripTablePrefix(config.getTableName()));
        table.setClassName(className);
        table.setBusinessName(StringUtils.uncapitalize(className));

        // 包路径：从 options 中读取，或使用默认值
        String packageName = normalizeBasePackageName(readOption(config, "packageName", DEFAULT_PACKAGE), moduleName);
        table.setPackageName(packageName);
        table.setModuleName(moduleName);
        table.setBusinessName(resolveBusinessName(businessPath));
        table.setAuthor(readOption(config, "author", DEFAULT_AUTHOR));

        return table;
    }

    /** order/manage → manage（最后一段作为业务名） */
    private String resolveBusinessName(String businessPath) {
        String[] parts = businessPath.split("/");
        return parts[parts.length - 1];
    }

    private String normalizeBasePackageName(String packageName, String moduleName) {
        String normalized = StringUtils.defaultIfBlank(packageName, DEFAULT_PACKAGE)
                .replaceAll("\\.+$", "");
        String module = StringUtils.defaultString(moduleName).trim();
        if (StringUtils.isBlank(module)) {
            return normalized;
        }
        String suffix = "." + module;
        if (normalized.endsWith(suffix)) {
            return normalized.substring(0, normalized.length() - suffix.length());
        }
        return normalized;
    }

    // ───────────────────────────────────────────────────────────────────────────
    // 字段加载与初始化
    // ───────────────────────────────────────────────────────────────────────────

    private List<GenTableColumn> loadColumns(AiCrudConfig config) {
        String tableName = config.getTableName();
        if (StringUtils.isBlank(tableName)) return new ArrayList<>();
        try {
            List<GenTableColumn> columns = genTableColumnMapper.selectDbTableColumnsByName(tableName);
            if (columns == null || columns.isEmpty()) return loadColumnsFromModelSchema(config);
            columns.forEach(GenUtils::initColumnField);
            return columns;
        } catch (Exception e) {
            log.warn("[VelocityCodegenStrategy] 加载字段失败, tableName={}", tableName, e);
            return loadColumnsFromModelSchema(config);
        }
    }

    private List<GenTableColumn> loadColumnsFromModelSchema(AiCrudConfig config) {
        if (StringUtils.isBlank(config.getModelSchema())) {
            return new ArrayList<>();
        }
        try {
            LowcodeModelSchema modelSchema = objectMapper.readValue(config.getModelSchema(), LowcodeModelSchema.class);
            List<GenTableColumn> columns = new ArrayList<>();
            int sort = 0;
            for (LowcodeFieldSchema field : modelSchema.getFields()) {
                if (field == null || StringUtils.isBlank(field.getColumnName())) {
                    continue;
                }
                GenTableColumn column = new GenTableColumn();
                column.setColumnName(field.getColumnName());
                column.setColumnComment(StringUtils.defaultIfBlank(field.getLabel(), field.getField()));
                column.setColumnType(toColumnType(field));
                column.setJavaType(toJavaType(field.getDataType()));
                column.setJavaField(StringUtils.defaultIfBlank(field.getField(), field.getColumnName()));
                column.setIsPk(Boolean.TRUE.equals(field.getPrimaryKey()) ? 1 : 0);
                column.setIsIncrement(Boolean.TRUE.equals(field.getAutoIncrement()) ? 1 : 0);
                column.setIsRequired(Boolean.TRUE.equals(field.getRequired()) ? 1 : 0);
                column.setIsInsert(Boolean.TRUE.equals(field.getFormVisible()) && !Boolean.TRUE.equals(field.getReadonly()) ? 1 : 0);
                column.setIsEdit(Boolean.TRUE.equals(field.getFormVisible()) && !Boolean.TRUE.equals(field.getReadonly()) ? 1 : 0);
                column.setIsList(field.getListVisible() == null || Boolean.TRUE.equals(field.getListVisible()) ? 1 : 0);
                column.setIsQuery(Boolean.TRUE.equals(field.getSearchable()) ? 1 : 0);
                column.setQueryType(StringUtils.defaultIfBlank(field.getQueryType(), "EQ").toUpperCase(Locale.ROOT));
                column.setHtmlType(toHtmlType(field.getComponentType(), field.getDataType()));
                column.setDictType(StringUtils.trimToNull(field.getDictType()));
                column.setDesensitizeType(StringUtils.trimToNull(field.getSensitiveType()));
                column.setSort(sort++);
                columns.add(column);
            }
            return columns;
        } catch (Exception e) {
            log.warn("[VelocityCodegenStrategy] 从低代码模型协议构建字段失败, configKey={}", config.getConfigKey(), e);
            return new ArrayList<>();
        }
    }

    private String toColumnType(LowcodeFieldSchema field) {
        String dataType = StringUtils.defaultIfBlank(field.getDataType(), "varchar").toLowerCase(Locale.ROOT);
        Integer length = field.getLength();
        Integer precision = field.getPrecision();
        return switch (dataType) {
            case "bigint" -> "bigint";
            case "int", "integer" -> "int";
            case "tinyint" -> "tinyint";
            case "decimal" -> "decimal(" + (length == null ? 18 : length) + "," + (precision == null ? 2 : precision) + ")";
            case "datetime" -> "datetime";
            case "date" -> "date";
            case "text" -> "text";
            case "char" -> "char(" + (length == null ? 1 : length) + ")";
            default -> "varchar(" + (length == null || length <= 0 ? 128 : length) + ")";
        };
    }

    private String toJavaType(String dataType) {
        String type = StringUtils.defaultIfBlank(dataType, "varchar").toLowerCase(Locale.ROOT);
        return switch (type) {
            case "bigint" -> "Long";
            case "int", "integer", "tinyint" -> "Integer";
            case "decimal" -> "BigDecimal";
            case "datetime" -> "LocalDateTime";
            case "date" -> "LocalDateTime";
            default -> "String";
        };
    }

    private String toHtmlType(String componentType, String dataType) {
        String component = StringUtils.defaultString(componentType);
        if ("textarea".equals(component)) {
            return "textarea";
        }
        if ("select".equals(component)) {
            return "select";
        }
        if ("switch".equals(component)) {
            return "radio";
        }
        if ("date".equals(component) || "datetime".equals(component)
                || "date".equals(dataType) || "datetime".equals(dataType)) {
            return "datetime";
        }
        if ("fileUpload".equals(component)) {
            return "fileUpload";
        }
        if ("imageUpload".equals(component)) {
            return "imageUpload";
        }
        return "input";
    }

    // ───────────────────────────────────────────────────────────────────────────
    // 注解控制变量解析（T3）
    // ───────────────────────────────────────────────────────────────────────────

    /**
     * 解析 encryptConfig / dictConfig / desensitizeConfig / transConfig，
     * 生成 Velocity 上下文中的注解控制布尔值和字段集合。
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> resolveAnnotationFlags(AiCrudConfig config, List<GenTableColumn> columns) {
        Map<String, Object> flags = new LinkedHashMap<>();

        // ── 字典配置 ──────────────────────────────────────────────────────────
        boolean hasDictConfig = false;
        Set<String> dictFields = new LinkedHashSet<>();
        if (StringUtils.isNotBlank(config.getDictConfig()) && StringUtils.isNotBlank(config.getColumnsSchema())) {
            try {
                // 建立 dictType -> javaField 反查 Map（从 columnsSchema + editSchema）
                Map<String, String> dictTypeToField = new LinkedHashMap<>();
                for (String schemaJson : new String[]{config.getColumnsSchema(), config.getEditSchema()}) {
                    if (StringUtils.isBlank(schemaJson)) continue;
                    List<Map<String, Object>> schemaList = objectMapper.readValue(schemaJson, new TypeReference<>() {});
                    for (Map<String, Object> col : schemaList) {
                        Object dtObj = col.get("dictType");
                        Object fObj = col.get("field") != null ? col.get("field")
                                : col.get("dataIndex") != null ? col.get("dataIndex") : col.get("key");
                        if (dtObj != null && fObj != null) {
                            dictTypeToField.putIfAbsent(String.valueOf(dtObj), String.valueOf(fObj));
                        }
                    }
                }
                // dictConfig 格式： [{dictType: "sys_status", dictName: "...", items: [...]}]
                List<Map<String, Object>> dictList = objectMapper.readValue(
                        config.getDictConfig(), new TypeReference<>() {});
                for (Map<String, Object> item : dictList) {
                    String dictType = (String) item.get("dictType");
                    if (StringUtils.isBlank(dictType)) continue;
                    String field = dictTypeToField.get(dictType);
                    if (StringUtils.isNotBlank(field)) {
                        dictFields.add(field);
                        hasDictConfig = true;
                        final String finalDictType = dictType;
                        final String finalField = field;
                        columns.stream()
                                .filter(c -> finalField.equals(c.getJavaField()))
                                .findFirst()
                                .ifPresent(c -> c.setDictType(finalDictType));
                    }
                }
            } catch (Exception e) {
                log.warn("[VelocityCodegenStrategy] 解析 dictConfig 失败", e);
            }
        }
        flags.put("hasDictConfig", hasDictConfig);
        flags.put("hasDictTrans", hasDictConfig || GenUtils.hasDictTrans(columns));
        flags.put("dictFields", dictFields);

        // ── 加解密配置 ────────────────────────────────────────────────────────
        boolean hasEncrypt = false;
        boolean enableDecrypt = false;
        boolean enableEncrypt = false;
        if (StringUtils.isNotBlank(config.getEncryptConfig())) {
            try {
                Map<String, Object> encConf = objectMapper.readValue(
                        config.getEncryptConfig(), new TypeReference<>() {});
                // 格式：{enableEncrypt: true, enableDecrypt: true, operations: [...]}
                Object encryptVal = encConf.get("enableEncrypt");
                Object decryptVal = encConf.get("enableDecrypt");
                enableEncrypt = Boolean.TRUE.equals(encryptVal) || "true".equals(String.valueOf(encryptVal));
                enableDecrypt = Boolean.TRUE.equals(decryptVal) || "true".equals(String.valueOf(decryptVal));
                hasEncrypt = enableEncrypt || enableDecrypt;
            } catch (Exception e) {
                log.warn("[VelocityCodegenStrategy] 解析 encryptConfig 失败", e);
            }
        }
        flags.put("hasEncrypt", hasEncrypt);
        flags.put("enableDecrypt", enableDecrypt);
        flags.put("enableEncrypt", enableEncrypt);

        // ── 脱敏配置 ──────────────────────────────────────────────────────────
        // 前端保存格式： {"phone": {"type": "PHONE", "label": "..."}, "idCard": {...}}
        // 直接回写到列对象的 desensitizeType 字段（与 dictType 同等模式）
        boolean hasDesensitize = false;
        if (StringUtils.isNotBlank(config.getDesensitizeConfig())) {
            try {
                Map<String, Object> desMap = objectMapper.readValue(
                        config.getDesensitizeConfig(), new TypeReference<>() {});
                for (Map.Entry<String, Object> entry : desMap.entrySet()) {
                    String field = entry.getKey();
                    Object val = entry.getValue();
                    String strategy = null;
                    if (val instanceof Map) {
                        Object typeVal = ((Map<?, ?>) val).get("type");
                        if (typeVal != null) strategy = String.valueOf(typeVal);
                    } else if (val instanceof String) {
                        strategy = (String) val;
                    }
                    if (StringUtils.isNotBlank(field) && StringUtils.isNotBlank(strategy)) {
                        hasDesensitize = true;
                        final String finalField = field;
                        final String finalStrategy = strategy;
                        columns.stream()
                                .filter(c -> finalField.equals(c.getJavaField()))
                                .findFirst()
                                .ifPresent(c -> c.setDesensitizeType(finalStrategy));
                    }
                }
            } catch (Exception e) {
                log.warn("[VelocityCodegenStrategy] 解析 desensitizeConfig 失败", e);
            }
        }
        flags.put("hasDesensitize", hasDesensitize);

        return flags;
    }

    // ───────────────────────────────────────────────────────────────────────────
    // 模板渲染
    // ───────────────────────────────────────────────────────────────────────────

    private void renderTo(Map<String, String> files, String templatePath,
                          VelocityContext ctx, String outputPath) {
        try {
            String content = VelocityUtils.renderTemplate(templatePath, ctx);
            files.put(outputPath, content);
        } catch (Exception e) {
            log.warn("[VelocityCodegenStrategy] 渲染模板失败: {}, 跳过该文件", templatePath, e);
        }
    }

    // ───────────────────────────────────────────────────────────────────────────
    // 工具方法
    // ───────────────────────────────────────────────────────────────────────────

    /** 去掉常见表前缀：sys_ / ai_ / t_ 等 */
    private String stripTablePrefix(String tableName) {
        if (tableName == null) return "unknown";
        for (String prefix : new String[]{"sys_", "ai_", "t_", "tb_"}) {
            if (tableName.startsWith(prefix)) {
                return tableName.substring(prefix.length());
            }
        }
        return tableName;
    }

    /** order_manage → OrderManage */
    private String toPascalCase(String key) {
        if (StringUtils.isBlank(key)) return key;
        String[] parts = key.split("[_\\-]");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)));
                sb.append(part.substring(1).toLowerCase());
            }
        }
        return sb.toString();
    }

    /** 从 config.options JSON 中读取指定 key，不存在时返回 defaultValue */
    @SuppressWarnings("unchecked")
    private String readOption(AiCrudConfig config, String key, String defaultValue) {
        if (StringUtils.isBlank(config.getOptions())) return defaultValue;
        try {
            Map<String, Object> opts = objectMapper.readValue(config.getOptions(), new TypeReference<>() {});
            Object val = opts.get(key);
            if (val == null && opts.get("codegen") instanceof Map<?, ?> codegen) {
                val = codegen.get(key);
                if (val == null && "packageName".equals(key)) {
                    val = codegen.get("domainPackage");
                }
            }
            return val != null ? String.valueOf(val) : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private boolean readBooleanOption(AiCrudConfig config, String key, boolean defaultValue) {
        String value = readOption(config, key, null);
        if (StringUtils.isBlank(value)) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    private String normalizeOutputBasePath(String value) {
        String normalized = StringUtils.defaultIfBlank(value, "frontend/src/views")
                .replace("\\", "/")
                .replaceAll("/+$", "");
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return StringUtils.defaultIfBlank(normalized, "frontend/src/views");
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseJsonArray(String json) {
        if (StringUtils.isBlank(json)) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * 预处理 columnsSchema：
     * 1. 过滤掉 actions 操作列
     * 2. 将嵌套的 render.dictType 提取到层 _dictType 字段
     * 3. 如果该字段在 transConfig 中有配置，注入 _transName（如 statusName）
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> preprocessColumnsSchema(List<Map<String, Object>> columns, Map<String, Object> transConfig) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> col : columns) {
            // 过滤操作列
            Object key = col.get("key");
            if ("actions".equals(key)) continue;
            Map<String, Object> newCol = new java.util.LinkedHashMap<>(col);
            // 提取 render.dictType
            Object renderObj = col.get("render");
            if (renderObj instanceof Map) {
                Map<String, Object> render = (Map<String, Object>) renderObj;
                if ("dictTag".equals(render.get("type")) && render.get("dictType") != null) {
                    newCol.put("_dictType", render.get("dictType"));
                }
            }
            // 提取 transConfig 中的 targetField 为 _transName
            // transConfig 的 key 是数据字段名，优先用 dataIndex，其次用 key
            Object dataIndex = col.get("dataIndex");
            String fieldName = dataIndex != null && !String.valueOf(dataIndex).isEmpty()
                    ? String.valueOf(dataIndex)
                    : (key != null ? String.valueOf(key) : "");
            if (!fieldName.isEmpty() && transConfig.containsKey(fieldName)) {
                Object transConf = transConfig.get(fieldName);
                String targetField = fieldName + "Name"; // 默认是数据字段名 + Name
                if (transConf instanceof Map) {
                    Object tf = ((Map<String, Object>) transConf).get("targetField");
                    if (tf != null && !String.valueOf(tf).isEmpty()) {
                        targetField = String.valueOf(tf);
                    }
                }
                newCol.put("_transName", targetField);
            }
            result.add(newCol);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonObject(String json) {
        if (StringUtils.isBlank(json)) return new LinkedHashMap<>();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    private String buildConfigJson(AiCrudConfig config) throws Exception {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("configKey", config.getConfigKey());
        map.put("tableName", config.getTableName());
        map.put("tableComment", config.getTableComment());
        map.put("layoutType", config.getLayoutType());
        map.put("searchSchema", parseJsonArray(config.getSearchSchema()));
        map.put("columnsSchema", parseJsonArray(config.getColumnsSchema()));
        map.put("editSchema", parseJsonArray(config.getEditSchema()));
        map.put("apiConfig", parseJsonObject(config.getApiConfig()));
        map.put("options", parseJsonObject(config.getOptions()));
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(map);
    }
}

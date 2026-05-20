package com.mdframe.forge.plugin.generator.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.dto.CustomQueryExecuteDTO;
import com.mdframe.forge.plugin.generator.dto.DynamicCrudQuery;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeTreeConfig;
import com.mdframe.forge.plugin.generator.util.DynamicQueryGenerator;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.crypto.crypto.Encryptor;
import com.mdframe.forge.starter.crypto.crypto.EncryptorFactory;
import com.mdframe.forge.starter.crypto.desensitize.strategy.DesensitizeStrategy;
import com.mdframe.forge.starter.crypto.desensitize.strategy.DesensitizeStrategyFactory;
import com.mdframe.forge.starter.crypto.desensitize.strategy.DesensitizeType;
import com.mdframe.forge.starter.trans.spi.DictValueProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 动态CRUD服务
 * 基于DynamicCrudRepository实现，支持配置驱动的通用CRUD操作
 * 
 * @author forge
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicCrudService {

    private static final int MAX_EXPORT_ROWS = 10000;
    private static final int MAX_TREE_ROWS = 5000;

    private final DynamicCrudRepository repository;
    private final AiCrudConfigService configService;
    private final ObjectMapper objectMapper;
    private final DictValueProvider dictValueProvider;
    private final DesensitizeStrategyFactory desensitizeStrategyFactory;
    private final EncryptorFactory encryptorFactory;

    // ==================== 查询操作 ====================

    /**
     * 分页查询
     */
    public Page<Map<String, Object>> selectPage(String configKey, PageQuery pageQuery, DynamicCrudQuery query) {
        // 1. 加载配置
        AiCrudConfig config = getConfig(configKey);
        String tableName = config.getTableName();
        
        // 2. 获取字段映射
        Map<String, String> columnMapping = repository.getColumnMapping(tableName);
        
        // 3. 解析搜索配置
        Set<String> allowedSearchFields = DynamicQueryGenerator.extractFieldNames(config.getSearchSchema(), objectMapper);
        Map<String, String> searchTypeMap = DynamicQueryGenerator.extractSearchTypeMap(config.getSearchSchema(), objectMapper);
        
        // 4. 构建搜索条件
        Map<String, Object> searchParams = (query != null) ? query.getSearchParams() : null;
        
        // 5. 构建排序
        String orderBy = DynamicQueryGenerator.buildOrderByClause(
                pageQuery.getOrderByColumn(), pageQuery.getIsAsc(), columnMapping);
        
        // 6. 执行分页查询
        Page<Map<String, Object>> page = repository.selectPage(
                tableName,
                pageQuery.getPageNum(),
                pageQuery.getPageSize(),
                searchParams,
                allowedSearchFields,
                searchTypeMap,
                columnMapping,
                orderBy
        );
        
        // 7. 转换字段名为camelCase
        List<Map<String, Object>> camelCaseRecords = DynamicQueryGenerator.convertListToCamelCase(page.getRecords());
        
        // 8. 读取链路统一先解密，再做翻译和脱敏，避免密文参与展示处理
        applyReadPipeline(camelCaseRecords, config);
        
        page.setRecords(camelCaseRecords);
        return page;
    }

    /**
     * 查询动态导出数据，复用动态 CRUD 的字段白名单、解密、字典翻译和脱敏链路。
     */
    public List<Map<String, Object>> selectExportRows(String configKey,
                                                      DynamicCrudQuery query,
                                                      Integer maxRows) {
        AiCrudConfig config = getConfig(configKey);
        String tableName = config.getTableName();
        Map<String, String> columnMapping = repository.getColumnMapping(tableName);
        Set<String> allowedSearchFields = DynamicQueryGenerator.extractFieldNames(config.getSearchSchema(), objectMapper);
        Map<String, String> searchTypeMap = DynamicQueryGenerator.extractSearchTypeMap(config.getSearchSchema(), objectMapper);
        Map<String, Object> searchParams = query != null ? query.getSearchParams() : null;
        int limit = normalizeExportLimit(maxRows);

        List<Map<String, Object>> rows = repository.selectList(
                tableName,
                searchParams,
                allowedSearchFields,
                searchTypeMap,
                columnMapping,
                "id DESC",
                limit
        );

        List<Map<String, Object>> camelCaseRows = DynamicQueryGenerator.convertListToCamelCase(rows);
        applyReadPipeline(camelCaseRows, config);
        return camelCaseRows;
    }

    /**
     * 查询树形导航数据，供树形单表模板左侧树使用。
     */
    public List<Map<String, Object>> selectTree(String configKey) {
        AiCrudConfig config = getConfig(configKey);
        String tableName = config.getTableName();
        List<Map<String, Object>> rows = repository.selectList(
                tableName,
                null,
                Collections.emptySet(),
                Collections.emptyMap(),
                repository.getColumnMapping(tableName),
                "id ASC",
                MAX_TREE_ROWS
        );

        List<Map<String, Object>> camelCaseRows = DynamicQueryGenerator.convertListToCamelCase(rows);
        applyReadPipeline(camelCaseRows, config);
        return buildTree(camelCaseRows, resolveTreeConfig(config));
    }

    /**
     * 自定义分页查询。
     */
    public Page<Map<String, Object>> selectCustomPage(String configKey, CustomQueryExecuteDTO request) {
        AiCrudConfig config = getConfig(configKey);
        String tableName = config.getTableName();
        Map<String, String> columnMapping = repository.getColumnMapping(tableName);
        Set<String> allowedFields = buildAllowedCustomFields(config);

        String orderBy = DynamicQueryGenerator.buildOrderByClause(
                request.getOrderByColumn(), request.getIsAsc(), columnMapping);

        Page<Map<String, Object>> page = repository.selectCustomPage(
                tableName,
                normalizePageNum(request.getPageNum()),
                normalizePageSize(request.getPageSize()),
                request.getFields(),
                request.getConditions(),
                allowedFields,
                columnMapping,
                orderBy
        );

        List<Map<String, Object>> camelCaseRecords = DynamicQueryGenerator.convertListToCamelCase(page.getRecords());
        applyReadPipeline(camelCaseRecords, config);
        page.setRecords(camelCaseRecords);
        return page;
    }

    /**
     * 根据ID查询
     */
    public Map<String, Object> selectById(String configKey, Long id) {
        AiCrudConfig config = getConfig(configKey);
        String tableName = config.getTableName();
        
        Map<String, Object> record = repository.selectById(tableName, id);
        if (record == null) {
            return null;
        }
        
        // 转换为camelCase
        Map<String, Object> camelCaseRecord = DynamicQueryGenerator.convertMapToCamelCase(record);
        
        // 单条读取同样遵循“解密 -> 翻译 -> 脱敏”顺序
        applyReadPipeline(Collections.singletonList(camelCaseRecord), config);
        
        return camelCaseRecord;
    }

    // ==================== 新增操作 ====================

    /**
     * 新增
     */
    public void insert(String configKey, Map<String, Object> data) {
        AiCrudConfig config = getConfig(configKey);
        String tableName = config.getTableName();
        
        // 获取字段映射
        Map<String, String> columnMapping = repository.getColumnMapping(tableName);
        
        // 获取允许写入的字段
        Set<String> allowedFields = DynamicQueryGenerator.extractFieldNames(config.getEditSchema(), objectMapper);
        
        // 过滤并转换字段名
        Map<String, Object> filteredData = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (allowedFields.contains(entry.getKey())) {
                String columnName = columnMapping.getOrDefault(entry.getKey(), DynamicQueryGenerator.camelToSnake(entry.getKey()));
                filteredData.put(columnName, entry.getValue());
            }
        }
        
        if (filteredData.isEmpty()) {
            throw new BusinessException("没有可写入的字段");
        }
        
        // 应用加密
        applyEncrypt(filteredData, config.getEncryptConfig());
        
        // 执行插入
        repository.insert(tableName, filteredData);
    }

    // ==================== 更新操作 ====================

    /**
     * 更新
     */
    public void updateById(String configKey, Map<String, Object> data) {
        AiCrudConfig config = getConfig(configKey);
        String tableName = config.getTableName();
        
        // 获取ID
        Object idValue = data.get("id");
        if (idValue == null) {
            throw new BusinessException("更新操作缺少id");
        }
        Long id = Long.valueOf(idValue.toString());
        
        // 获取字段映射
        Map<String, String> columnMapping = repository.getColumnMapping(tableName);
        
        // 获取允许写入的字段
        Set<String> allowedFields = DynamicQueryGenerator.extractFieldNames(config.getEditSchema(), objectMapper);
        
        // 过滤并转换字段名
        Map<String, Object> filteredData = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            if ("id".equals(key) || "tenantId".equals(key) || "tenant_id".equals(key)) {
                continue;
            }
            if (allowedFields.contains(key)) {
                String columnName = columnMapping.getOrDefault(key, DynamicQueryGenerator.camelToSnake(key));
                filteredData.put(columnName, entry.getValue());
            }
        }
        
        if (filteredData.isEmpty()) {
            throw new BusinessException("没有可更新的字段");
        }
        
        // 应用加密
        applyEncrypt(filteredData, config.getEncryptConfig());
        
        // 执行更新
        repository.updateById(tableName, id, filteredData);
    }

    // ==================== 删除操作 ====================

    /**
     * 删除
     */
    public void deleteById(String configKey, Long id) {
        AiCrudConfig config = getConfig(configKey);
        String tableName = config.getTableName();
        
        // 判断是否逻辑删除
        boolean logicDelete = repository.hasDelFlag(tableName);
        
        // 执行删除
        repository.deleteById(tableName, id, logicDelete);
    }

    /**
     * 暴露运行时配置给动态导入导出服务，仍统一走发布态校验。
     */
    public AiCrudConfig getRuntimeConfig(String configKey) {
        return getConfig(configKey);
    }

    private void applyReadPipeline(List<Map<String, Object>> rows, AiCrudConfig config) {
        applyDecrypt(rows, config.getEncryptConfig());
        applyDictTranslation(rows, config.getTransConfig());
        applyDesensitize(rows, config.getDesensitizeConfig());
    }

    private LowcodeTreeConfig resolveTreeConfig(AiCrudConfig config) {
        LowcodeTreeConfig treeConfig = new LowcodeTreeConfig();
        applyTreeConfigFromModel(config, treeConfig);
        applyTreeConfigFromOptions(config, treeConfig);
        if (StringUtils.isBlank(treeConfig.getKeyField())) {
            treeConfig.setKeyField("id");
        }
        if (StringUtils.isBlank(treeConfig.getParentField())) {
            treeConfig.setParentField("parentId");
        }
        if (StringUtils.isBlank(treeConfig.getLabelField())) {
            treeConfig.setLabelField("name");
        }
        if (StringUtils.isBlank(treeConfig.getChildrenField())) {
            treeConfig.setChildrenField("children");
        }
        return treeConfig;
    }

    private void applyTreeConfigFromModel(AiCrudConfig config, LowcodeTreeConfig target) {
        if (StringUtils.isBlank(config.getModelSchema())) {
            return;
        }
        try {
            JsonNode treeNode = objectMapper.readTree(config.getModelSchema()).get("treeConfig");
            applyTreeConfigNode(treeNode, target);
        } catch (Exception e) {
            log.warn("[DynamicCrudService] 解析modelSchema.treeConfig失败, configKey={}", config.getConfigKey(), e);
        }
    }

    private void applyTreeConfigFromOptions(AiCrudConfig config, LowcodeTreeConfig target) {
        if (StringUtils.isBlank(config.getOptions())) {
            return;
        }
        try {
            JsonNode treeNode = objectMapper.readTree(config.getOptions()).get("treeConfig");
            applyTreeConfigNode(treeNode, target);
        } catch (Exception e) {
            log.warn("[DynamicCrudService] 解析options.treeConfig失败, configKey={}", config.getConfigKey(), e);
        }
    }

    private void applyTreeConfigNode(JsonNode treeNode, LowcodeTreeConfig target) {
        if (treeNode == null || !treeNode.isObject()) {
            return;
        }
        if (StringUtils.isNotBlank(text(treeNode, "keyField"))) {
            target.setKeyField(text(treeNode, "keyField"));
        }
        if (StringUtils.isNotBlank(text(treeNode, "parentField"))) {
            target.setParentField(text(treeNode, "parentField"));
        }
        if (StringUtils.isNotBlank(text(treeNode, "labelField"))) {
            target.setLabelField(text(treeNode, "labelField"));
        }
        if (StringUtils.isNotBlank(text(treeNode, "childrenField"))) {
            target.setChildrenField(text(treeNode, "childrenField"));
        }
        if (StringUtils.isNotBlank(text(treeNode, "treeTitle"))) {
            target.setTreeTitle(text(treeNode, "treeTitle"));
        }
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        return value == null || value.isNull() ? null : value.asText();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> buildTree(List<Map<String, Object>> rows, LowcodeTreeConfig treeConfig) {
        Map<String, Map<String, Object>> nodeMap = new LinkedHashMap<>();
        String keyField = treeConfig.getKeyField();
        String parentField = treeConfig.getParentField();
        String childrenField = treeConfig.getChildrenField();

        for (Map<String, Object> row : rows) {
            String key = normalizeTreeKey(row.get(keyField));
            if (StringUtils.isBlank(key)) {
                continue;
            }
            Map<String, Object> node = new LinkedHashMap<>(row);
            node.put(childrenField, new ArrayList<Map<String, Object>>());
            nodeMap.put(key, node);
        }

        List<Map<String, Object>> roots = new ArrayList<>();
        for (Map<String, Object> node : nodeMap.values()) {
            String key = normalizeTreeKey(node.get(keyField));
            String parentKey = normalizeTreeKey(node.get(parentField));
            Map<String, Object> parent = nodeMap.get(parentKey);
            if (isRootParent(parentKey) || parent == null || key.equals(parentKey)) {
                roots.add(node);
                continue;
            }
            Object children = parent.get(childrenField);
            if (children instanceof List<?> childList) {
                ((List<Map<String, Object>>) childList).add(node);
            }
        }
        return roots;
    }

    private boolean isRootParent(String parentKey) {
        return StringUtils.isBlank(parentKey) || "0".equals(parentKey) || "null".equalsIgnoreCase(parentKey);
    }

    private String normalizeTreeKey(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return StringUtils.isBlank(text) ? null : text;
    }

    // ==================== 加解密处理 ====================

    /**
     * 应用加密（写入时）
     * encryptConfig格式示例：
     * {
     *   "phone": {"algorithm": "SM4"},
     *   "idCard": {"algorithm": "AES"},
     *   "email": {"algorithm": "SM4"}
     * }
     */
    private void applyEncrypt(Map<String, Object> data, String encryptConfigJson) {
        if (StringUtils.isBlank(encryptConfigJson) || data == null || data.isEmpty()) {
            return;
        }
        try {
            JsonNode configNode = objectMapper.readTree(encryptConfigJson);
            if (!configNode.isObject()) return;

            for (Map.Entry<String, JsonNode> entry : configNode.properties()) {
                String fieldName = entry.getKey(); // camelCase字段名
                JsonNode ruleNode = entry.getValue();

                // 转换为snake_case（数据库列名）
                String snakeFieldName = DynamicQueryGenerator.camelToSnake(fieldName);

                if (!data.containsKey(snakeFieldName) || data.get(snakeFieldName) == null) {
                    continue;
                }

                // 获取加密算法配置
                String algorithm = ruleNode.has("algorithm") ? ruleNode.get("algorithm").asText() : "";
                if (StringUtils.isBlank(algorithm)) {
                    continue;
                }

                // 获取加密器
                Encryptor encryptor = encryptorFactory.getEncryptor(algorithm);
                if (encryptor == null) {
                    log.warn("[DynamicCrudService] 未找到加密器, algorithm={}", algorithm);
                    continue;
                }

                // 加密字段值
                Object value = data.get(snakeFieldName);
                if (value instanceof String) {
                    String plainText = (String) value;
                    if (StringUtils.isNotBlank(plainText)) {
                        String encryptedValue = encryptor.encrypt(plainText);
                        data.put(snakeFieldName, encryptedValue);
                        log.debug("[DynamicCrudService] 加密字段: {}, algorithm: {}", fieldName, algorithm);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[DynamicCrudService] 加密处理失败", e);
        }
    }

    /**
     * 应用解密（读取时）
     * encryptConfig格式示例：
     * {
     *   "phone": {"algorithm": "SM4"},
     *   "idCard": {"algorithm": "AES"},
     *   "email": {"algorithm": "SM4"}
     * }
     */
    private void applyDecrypt(List<Map<String, Object>> rows, String encryptConfigJson) {
        if (StringUtils.isBlank(encryptConfigJson) || rows == null || rows.isEmpty()) {
            return;
        }
        try {
            JsonNode configNode = objectMapper.readTree(encryptConfigJson);
            if (!configNode.isObject()) return;

            for (Map<String, Object> row : rows) {
                for (Map.Entry<String, JsonNode> entry : configNode.properties()) {
                    String fieldName = entry.getKey(); // camelCase字段名
                    JsonNode ruleNode = entry.getValue();

                    if (!row.containsKey(fieldName) || row.get(fieldName) == null) {
                        continue;
                    }

                    // 获取加密算法配置
                    String algorithm = ruleNode.has("algorithm") ? ruleNode.get("algorithm").asText() : "";
                    if (StringUtils.isBlank(algorithm)) {
                        continue;
                    }

                    // 获取加密器
                    Encryptor encryptor = encryptorFactory.getEncryptor(algorithm);
                    if (encryptor == null) {
                        log.warn("[DynamicCrudService] 未找到加密器, algorithm={}", algorithm);
                        continue;
                    }

                    // 解密字段值
                    Object value = row.get(fieldName);
                    if (value instanceof String) {
                        String cipherText = (String) value;
                        if (StringUtils.isNotBlank(cipherText)) {
                            try {
                                String decryptedValue = encryptor.decrypt(cipherText);
                                row.put(fieldName, decryptedValue);
                                log.debug("[DynamicCrudService] 解密字段: {}, algorithm: {}", fieldName, algorithm);
                            } catch (Exception decryptException) {
                                log.warn("[DynamicCrudService] 解密字段失败: {}, 可能是明文数据", fieldName);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[DynamicCrudService] 解密处理失败", e);
        }
    }

    // ==================== 脱敏处理 ====================

    /**
     * 应用字段脱敏
     */
    private void applyDesensitize(List<Map<String, Object>> rows, String desensitizeConfigJson) {
        if (StringUtils.isBlank(desensitizeConfigJson) || rows == null || rows.isEmpty()) {
            return;
        }
        try {
            JsonNode configNode = objectMapper.readTree(desensitizeConfigJson);
            if (!configNode.isObject()) return;

            for (Map<String, Object> row : rows) {
                for (Map.Entry<String, JsonNode> entry : configNode.properties()) {
                    String fieldName = entry.getKey(); // camelCase字段名
                    JsonNode ruleNode = entry.getValue();
                    if (!row.containsKey(fieldName) || row.get(fieldName) == null) continue;

                    String typeStr = ruleNode.has("type") ? ruleNode.get("type").asText("CUSTOM") : "CUSTOM";
                    DesensitizeType type = DesensitizeType.valueOf(typeStr);
                    DesensitizeStrategy strategy = desensitizeStrategyFactory.getStrategy(type);
                    if (strategy != null) {
                        String originalValue = String.valueOf(row.get(fieldName));
                        row.put(fieldName, strategy.desensitize(originalValue));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[DynamicCrudService] 脱敏处理失败", e);
        }
    }

    // ==================== 字典翻译 ====================

    /**
     * 应用字典翻译
     */
    private void applyDictTranslation(List<Map<String, Object>> rows, String transConfigJson) {
        if (StringUtils.isBlank(transConfigJson) || rows == null || rows.isEmpty() || dictValueProvider == null) {
            return;
        }
        try {
            JsonNode configNode = objectMapper.readTree(transConfigJson);
            if (!configNode.isObject()) return;

            for (Map<String, Object> row : rows) {
                for (Map.Entry<String, JsonNode> entry : configNode.properties()) {
                    String sourceField = entry.getKey(); // camelCase字段名
                    JsonNode ruleNode = entry.getValue();
                    if (!row.containsKey(sourceField) || row.get(sourceField) == null) continue;

                    String dictType = ruleNode.has("dictType") ? ruleNode.get("dictType").asText() : "";
                    String targetField = ruleNode.has("targetField") ? ruleNode.get("targetField").asText()
                            : sourceField + "Name";
                    if (StringUtils.isBlank(dictType)) continue;

                    String key = String.valueOf(row.get(sourceField));
                    String label = dictValueProvider.getLabel(dictType, key);
                    if (label != null) {
                        row.put(targetField, label);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[DynamicCrudService] 字典翻译失败", e);
        }
    }

    // ==================== 配置加载 ====================

    /**
     * 获取配置
     */
    private AiCrudConfig getConfig(String configKey) {
        AiCrudConfig config = configService.getByConfigKey(configKey);
        if (config == null || "1".equals(config.getStatus())) {
            throw new BusinessException("CRUD配置不存在或已停用: " + configKey);
        }
        if (!"CONFIG".equals(config.getMode())) {
            throw new BusinessException("该配置不是配置驱动模式: " + configKey);
        }
        if ("LOWCODE".equals(config.getBuildMode()) && !"PUBLISHED".equals(config.getPublishStatus())) {
            throw new BusinessException("低代码应用尚未发布: " + configKey);
        }
        return config;
    }

    private Set<String> buildAllowedCustomFields(AiCrudConfig config) {
        Set<String> fields = new HashSet<>();
        fields.addAll(DynamicQueryGenerator.extractFieldNames(config.getSearchSchema(), objectMapper));
        fields.addAll(DynamicQueryGenerator.extractFieldNames(config.getColumnsSchema(), objectMapper));
        fields.addAll(DynamicQueryGenerator.extractFieldNames(config.getEditSchema(), objectMapper));
        fields.add("id");
        return fields;
    }

    private int normalizePageNum(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 10;
        }
        return Math.min(pageSize, 100);
    }

    private int normalizeExportLimit(Integer maxRows) {
        if (maxRows == null || maxRows < 1) {
            return MAX_EXPORT_ROWS;
        }
        return Math.min(maxRows, MAX_EXPORT_ROWS);
    }
}

package com.mdframe.forge.plugin.generator.service.businessapp;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.mdframe.forge.plugin.generator.constant.BusinessAppMode;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApp;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeCodegenRequest;
import com.mdframe.forge.plugin.generator.service.AiCrudCodegenService;
import com.mdframe.forge.plugin.generator.service.AiCrudConfigService;
import com.mdframe.forge.plugin.generator.service.lowcode.LowcodeCodegenService;
import com.mdframe.forge.plugin.generator.vo.lowcode.LowcodeCodePreviewVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Map;

/**
 * 业务访问入口维度的功能代码预览与下载服务。
 */
@Service
@RequiredArgsConstructor
public class BusinessAppCodegenService {

    private static final String SOURCE_DRAFT = "DRAFT";

    private final BusinessAppService appService;
    private final AiCrudConfigService crudConfigService;
    private final LowcodeCodegenService lowcodeCodegenService;
    private final AiCrudCodegenService codegenService;

    public Map<String, Object> getOptions(Long appId) {
        AiBusinessApp app = requireCodeDownloadApp(appId);
        JSONObject options = readOptions(app.getOptions());
        return buildCodegenOptions(app, options, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveOptions(Long appId, LowcodeCodegenRequest request) {
        AiBusinessApp app = requireCodeDownloadApp(appId);
        JSONObject options = readOptions(app.getOptions());
        options.put("codegen", buildCodegenOptions(app, options, request));
        app.setOptions(writeOptions(options));
        appService.updateById(app);
    }

    public LowcodeCodePreviewVO previewCode(Long appId, LowcodeCodegenRequest request) {
        AiBusinessApp app = requireCodeDownloadApp(appId);
        AiCrudConfig config = prepareBusinessCodegenConfig(app, request);
        Map<String, String> files = codegenService.generateFiles(config);
        ensureNoGenericRuntimeApi(files);

        LowcodeCodePreviewVO vo = new LowcodeCodePreviewVO();
        vo.setAppId(app.getId());
        vo.setConfigKey(config.getConfigKey());
        vo.setSourceType(resolveSourceType(request));
        vo.setVersionId(request == null ? null : request.getVersionId());
        vo.setFiles(files);
        vo.setFileCount(files.size());
        return vo;
    }

    public byte[] downloadCode(Long appId, LowcodeCodegenRequest request) {
        AiBusinessApp app = requireCodeDownloadApp(appId);
        AiCrudConfig config = prepareBusinessCodegenConfig(app, request);
        Map<String, String> files = codegenService.generateFiles(config);
        ensureNoGenericRuntimeApi(files);
        return codegenService.toZip(files);
    }

    public String resolveDownloadFilename(Long appId) {
        AiBusinessApp app = requireCodeDownloadApp(appId);
        String name = StringUtils.firstNonBlank(app.getAppCode(), app.getConfigKey(), String.valueOf(app.getId()));
        return toPathSegment(name).replace("-", "_") + "-code.zip";
    }

    private AiBusinessApp requireCodeDownloadApp(Long appId) {
        AiBusinessApp app = appService.requireEntity(appId);
        String entryMode = StringUtils.defaultIfBlank(app.getEntryMode(), "").toUpperCase(Locale.ROOT);
        if (!"RUNTIME".equals(entryMode)) {
            throw new BusinessException("只有业务页面访问入口支持功能代码下载");
        }
        JSONObject options = readOptions(app.getOptions());
        if (!BusinessAppMode.isCodeDownload(options.get("appMode"))) {
            throw new BusinessException("当前访问入口不是下载代码模式");
        }
        if (StringUtils.isBlank(app.getSuiteCode()) || StringUtils.isBlank(app.getObjectCode())) {
            throw new BusinessException("下载代码模式需要关联业务域和业务单元");
        }
        if (StringUtils.isBlank(app.getConfigKey())) {
            throw new BusinessException("访问入口尚未配置业务页面，不能生成代码");
        }
        return app;
    }

    private AiCrudConfig prepareBusinessCodegenConfig(AiBusinessApp app, LowcodeCodegenRequest request) {
        AiCrudConfig config = crudConfigService.getByConfigKey(app.getConfigKey());
        if (config == null) {
            throw new BusinessException("访问入口尚未发布，不能生成代码");
        }
        JSONObject appOptions = readOptions(app.getOptions());
        JSONObject appCodegen = buildCodegenOptions(app, appOptions, request);
        JSONObject configOptions = readOptions(config.getOptions());
        configOptions.put("codegen", appCodegen);
        mirrorCodegenOptions(configOptions, appCodegen);
        config.setOptions(writeOptions(configOptions));

        AiCrudConfig prepared = lowcodeCodegenService.prepareConfigForCodegen(config, request);
        applyBusinessApiConfig(prepared, app, appCodegen);
        return prepared;
    }

    private void applyBusinessApiConfig(AiCrudConfig config, AiBusinessApp app, JSONObject codegen) {
        String apiBase = normalizeBusinessApiBase(StringUtils.firstNonBlank(
                codegen.getString("businessApiBase"),
                defaultBusinessApiBase(app)
        ));
        config.setApiConfig(buildBusinessApiConfig(apiBase).toJSONString());

        JSONObject options = readOptions(config.getOptions());
        JSONObject mergedCodegen = readCodegen(options);
        mergedCodegen.putAll(codegen);
        mergedCodegen.put("businessApiBase", apiBase);
        options.put("codegen", mergedCodegen);
        mirrorCodegenOptions(options, mergedCodegen);
        rewriteGenericRuntimeLinks(options, apiBase, config.getConfigKey());
        config.setOptions(writeOptions(options));
    }

    private JSONObject buildCodegenOptions(AiBusinessApp app, JSONObject appOptions, LowcodeCodegenRequest request) {
        JSONObject existing = readCodegen(appOptions);
        JSONObject codegen = new JSONObject();
        putIfNotBlank(codegen, "sourceType", StringUtils.firstNonBlank(
                request == null ? null : request.getSourceType(),
                existing.getString("sourceType"),
                SOURCE_DRAFT));
        if (request != null && request.getVersionId() != null) {
            codegen.put("versionId", request.getVersionId());
        } else if (existing.get("versionId") != null) {
            codegen.put("versionId", existing.get("versionId"));
        }
        String apiBase = StringUtils.firstNonBlank(
                request == null ? null : request.getBusinessApiBase(),
                existing.getString("businessApiBase"),
                defaultBusinessApiBase(app));
        codegen.put("businessApiBase", normalizeBusinessApiBase(apiBase));

        putIfNotBlank(codegen, "groupId", StringUtils.firstNonBlank(
                request == null ? null : request.getGroupId(),
                existing.getString("groupId")));
        putIfNotBlank(codegen, "domainPackage", StringUtils.firstNonBlank(
                request == null ? null : request.getDomainPackage(),
                existing.getString("domainPackage"),
                existing.getString("packageName")));
        putIfNotBlank(codegen, "moduleName", StringUtils.firstNonBlank(
                request == null ? null : request.getModuleName(),
                existing.getString("moduleName"),
                resolveModuleName(codegen.getString("businessApiBase"))));
        putIfNotBlank(codegen, "author", StringUtils.firstNonBlank(
                request == null ? null : request.getAuthor(),
                existing.getString("author")));
        putIfNotBlank(codegen, "frontendBasePath", StringUtils.firstNonBlank(
                request == null ? null : request.getFrontendBasePath(),
                existing.getString("frontendBasePath")));
        codegen.put("includeSql", resolveBoolean(request == null ? null : request.getIncludeSql(),
                existing.get("includeSql"), true));
        codegen.put("includeMenuSql", resolveBoolean(request == null ? null : request.getIncludeMenuSql(),
                existing.get("includeMenuSql"), true));
        codegen.put("includeDictSql", resolveBoolean(request == null ? null : request.getIncludeDictSql(),
                existing.get("includeDictSql"), true));
        return codegen;
    }

    private JSONObject buildBusinessApiConfig(String apiBase) {
        JSONObject apiConfig = new JSONObject();
        apiConfig.put("list", "get@" + apiBase + "/page");
        apiConfig.put("detail", "post@" + apiBase + "/getById");
        apiConfig.put("add", "post@" + apiBase + "/add");
        apiConfig.put("create", "post@" + apiBase + "/add");
        apiConfig.put("update", "post@" + apiBase + "/edit");
        apiConfig.put("delete", "post@" + apiBase + "/remove/:id");
        apiConfig.put("importTemplate", "get@" + apiBase + "/import-template");
        apiConfig.put("import", "post@" + apiBase + "/import");
        apiConfig.put("export", "post@" + apiBase + "/export");
        return apiConfig;
    }

    private String defaultBusinessApiBase(AiBusinessApp app) {
        return "/" + toPathSegment(app.getSuiteCode()) + "/" + toPathSegment(app.getObjectCode());
    }

    private String normalizeBusinessApiBase(String value) {
        String apiBase = StringUtils.trimToEmpty(value).replace("\\", "/");
        if (StringUtils.isBlank(apiBase)) {
            throw new BusinessException("业务接口前缀不能为空");
        }
        if (!apiBase.startsWith("/")) {
            apiBase = "/" + apiBase;
        }
        apiBase = apiBase.replaceAll("/{2,}", "/").replaceAll("/+$", "");
        if (StringUtils.isBlank(apiBase) || "/".equals(apiBase)) {
            throw new BusinessException("业务接口前缀不能为空");
        }
        String lower = apiBase.toLowerCase(Locale.ROOT);
        if (lower.startsWith("/ai/crud/")
                || lower.startsWith("/ai/crud-config")
                || lower.startsWith("/ai/lowcode/")
                || lower.startsWith("/rest/")) {
            throw new BusinessException("下载代码模式不能使用平台通用运行接口");
        }
        if (lower.contains("{configkey}") || lower.contains("crud")) {
            throw new BusinessException("业务接口前缀不能包含旧配置标识");
        }
        return apiBase;
    }

    private String toPathSegment(String value) {
        String text = StringUtils.defaultString(value, "app").trim();
        text = text.replaceAll("([a-z0-9])([A-Z])", "$1-$2");
        text = text.replace('_', '-');
        text = text.replaceAll("[^A-Za-z0-9\\-]+", "-");
        text = text.replaceAll("-{2,}", "-").replaceAll("^-|-$", "");
        text = StringUtils.defaultIfBlank(text, "app");
        return text.toLowerCase(Locale.ROOT);
    }

    private String resolveModuleName(String apiBase) {
        String[] parts = StringUtils.defaultString(apiBase).split("/");
        for (String part : parts) {
            if (StringUtils.isNotBlank(part)) {
                return part.replace("-", "_");
            }
        }
        return "app";
    }

    private void mirrorCodegenOptions(JSONObject options, JSONObject codegen) {
        if (codegen == null) {
            return;
        }
        copyIfPresent(options, codegen, "domainPackage", "packageName");
        copyIfPresent(options, codegen, "moduleName", "moduleName");
        copyIfPresent(options, codegen, "author", "author");
        copyIfPresent(options, codegen, "frontendBasePath", "frontendBasePath");
        copyIfPresent(options, codegen, "includeSql", "includeSql");
        copyIfPresent(options, codegen, "includeMenuSql", "includeMenuSql");
        copyIfPresent(options, codegen, "includeDictSql", "includeDictSql");
    }

    private void copyIfPresent(JSONObject target, JSONObject source, String sourceKey, String targetKey) {
        Object value = source.get(sourceKey);
        if (value != null && !(value instanceof String text && StringUtils.isBlank(text))) {
            target.put(targetKey, value);
        }
    }

    private void rewriteGenericRuntimeLinks(Object value, String apiBase, String configKey) {
        if (value instanceof JSONObject object) {
            for (String key : object.keySet()) {
                Object child = object.get(key);
                if (child instanceof String text) {
                    object.put(key, rewriteGenericRuntimeText(text, apiBase, configKey));
                } else {
                    rewriteGenericRuntimeLinks(child, apiBase, configKey);
                }
            }
        } else if (value instanceof JSONArray array) {
            for (int i = 0; i < array.size(); i++) {
                Object child = array.get(i);
                if (child instanceof String text) {
                    array.set(i, rewriteGenericRuntimeText(text, apiBase, configKey));
                } else {
                    rewriteGenericRuntimeLinks(child, apiBase, configKey);
                }
            }
        }
    }

    private String rewriteGenericRuntimeText(String text, String apiBase, String configKey) {
        String result = text;
        if (StringUtils.isNotBlank(configKey)) {
            result = result.replace("/ai/crud/" + configKey, apiBase);
            result = result.replace("/ai/crud-page/" + configKey, apiBase);
        }
        return result.replace("/ai/crud/", apiBase + "/");
    }

    private void ensureNoGenericRuntimeApi(Map<String, String> files) {
        for (Map.Entry<String, String> entry : files.entrySet()) {
            String content = entry.getValue();
            if (StringUtils.contains(content, "/ai/crud/")) {
                throw new BusinessException("功能代码仍包含平台通用运行接口，请检查代码包设置: " + entry.getKey());
            }
        }
    }

    private JSONObject readOptions(String options) {
        if (StringUtils.isBlank(options)) {
            return new JSONObject();
        }
        try {
            return JSON.parseObject(options);
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    private JSONObject readCodegen(JSONObject options) {
        JSONObject codegen = options == null ? null : options.getJSONObject("codegen");
        return codegen == null ? new JSONObject() : codegen;
    }

    private String writeOptions(JSONObject options) {
        if (options == null || options.isEmpty()) {
            return null;
        }
        return options.toJSONString();
    }

    private void putIfNotBlank(JSONObject target, String key, String value) {
        if (StringUtils.isNotBlank(value)) {
            target.put(key, value);
        }
    }

    private boolean resolveBoolean(Boolean requestValue, Object existingValue, boolean fallback) {
        if (requestValue != null) {
            return requestValue;
        }
        if (existingValue == null) {
            return fallback;
        }
        if (existingValue instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(String.valueOf(existingValue));
    }

    private String resolveSourceType(LowcodeCodegenRequest request) {
        return StringUtils.defaultIfBlank(request == null ? null : request.getSourceType(), SOURCE_DRAFT)
                .toUpperCase(Locale.ROOT);
    }
}

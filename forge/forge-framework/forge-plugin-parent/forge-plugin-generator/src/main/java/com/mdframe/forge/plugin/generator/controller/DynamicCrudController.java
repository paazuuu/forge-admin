package com.mdframe.forge.plugin.generator.controller;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.generator.dto.DynamicCrudImportResult;
import com.mdframe.forge.plugin.generator.dto.DynamicCrudQuery;
import com.mdframe.forge.plugin.generator.service.DynamicCrudExcelService;
import com.mdframe.forge.plugin.generator.service.DynamicCrudService;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.core.domain.RespInfo;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/ai/crud/{configKey}")
@RequiredArgsConstructor
public class DynamicCrudController {

    private final DynamicCrudService dynamicCrudService;
    private final DynamicCrudExcelService dynamicCrudExcelService;

    @ApiEncrypt
    @GetMapping("/page")
    public RespInfo<Page<Map<String, Object>>> page(@PathVariable String configKey,
                                                     PageQuery pageQuery,
                                                     DynamicCrudQuery query,
                                                     @RequestParam Map<String, Object> requestParams) {
        Page<Map<String, Object>> data = dynamicCrudService.selectPage(
                configKey, pageQuery, buildQuery(query, requestParams));
        log.info("DynamicCrudController#page,返回数据:{}", JSONObject.toJSONString(data));
        return RespInfo.success(data);
    }

    @ApiEncrypt
    @GetMapping("/tree")
    public RespInfo<List<Map<String, Object>>> tree(@PathVariable String configKey,
                                                    @RequestParam(required = false) String parentValue,
                                                    @RequestParam(required = false) String parentId,
                                                    @RequestParam(required = false) String loadMode,
                                                    @RequestParam(required = false) String orderByColumn,
                                                    @RequestParam(required = false) String isAsc) {
        return RespInfo.success(dynamicCrudService.selectTree(configKey, parentValue, parentId, loadMode, orderByColumn, isAsc));
    }

    @ApiEncrypt
    @GetMapping("/{id}")
    public RespInfo<Map<String, Object>> getById(@PathVariable String configKey,
                                                  @PathVariable Long id) {
        return RespInfo.success(dynamicCrudService.selectById(configKey, id));
    }

    @ApiEncrypt
    @ApiDecrypt
    @PostMapping
    public RespInfo<Void> create(@PathVariable String configKey,
                                  @RequestBody Map<String, Object> data) {
        dynamicCrudService.insert(configKey, data);
        return RespInfo.success();
    }

    @ApiEncrypt
    @ApiDecrypt
    @PutMapping
    public RespInfo<Void> update(@PathVariable String configKey,
                                  @RequestBody Map<String, Object> data) {
        dynamicCrudService.updateById(configKey, data);
        return RespInfo.success();
    }

    @ApiEncrypt
    @DeleteMapping("/{id}")
    public RespInfo<Void> delete(@PathVariable String configKey,
                                  @PathVariable Long id) {
        dynamicCrudService.deleteById(configKey, id);
        return RespInfo.success();
    }

    @PostMapping("/import")
    public RespInfo<DynamicCrudImportResult> importExcel(@PathVariable String configKey,
                                                         @RequestParam("file") MultipartFile file) {
        return RespInfo.success(dynamicCrudExcelService.importExcel(configKey, file));
    }

    @PostMapping("/export")
    public void exportExcel(@PathVariable String configKey,
                            @RequestBody(required = false) Map<String, Object> requestBody,
                            HttpServletResponse response) {
        dynamicCrudExcelService.exportExcel(configKey, buildQueryFromBody(requestBody), response);
    }

    @GetMapping("/import-template")
    public void downloadImportTemplate(@PathVariable String configKey,
                                       HttpServletResponse response) {
        dynamicCrudExcelService.downloadImportTemplate(configKey, response);
    }

    private DynamicCrudQuery buildQuery(DynamicCrudQuery query, Map<String, Object> requestParams) {
        DynamicCrudQuery result = query != null ? query : new DynamicCrudQuery();
        if (result.getSearchParams() == null || result.getSearchParams().isEmpty()) {
            result.setSearchParams(filterSearchParams(requestParams));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private DynamicCrudQuery buildQueryFromBody(Map<String, Object> requestBody) {
        DynamicCrudQuery query = new DynamicCrudQuery();
        if (requestBody == null || requestBody.isEmpty()) {
            return query;
        }
        Object searchParams = requestBody.get("searchParams");
        if (searchParams instanceof Map<?, ?> params) {
            query.setSearchParams((Map<String, Object>) params);
        } else {
            query.setSearchParams(filterSearchParams(requestBody));
        }
        return query;
    }

    private Map<String, Object> filterSearchParams(Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return null;
        }
        Set<String> controlParams = Set.of("pageNum", "pageSize", "orderByColumn", "isAsc", "searchParams");
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            if (!controlParams.contains(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result.isEmpty() ? null : result;
    }
}

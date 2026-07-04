package com.mdframe.forge.plugin.generator.dto.businessapp;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 通用业务动作执行请求。
 */
@Data
public class BusinessActionExecuteDTO {

    private String suiteCode;

    private String objectCode;

    private String businessObjectCode;

    private String targetObjectCode;

    private String targetEntityCode;

    private String candidateObjectCode;

    private String referenceObjectCode;

    private String refObjectCode;

    private String sourceObjectCode;

    private String targetCode;

    private String recordId;

    private String actionCode;

    private Map<String, Object> formData = new LinkedHashMap<>();

    private Map<String, Object> context = new LinkedHashMap<>();

    private String idempotencyKey;
}

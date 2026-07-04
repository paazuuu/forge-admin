package com.mdframe.forge.plugin.generator.dto.businessapp;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 业务待办办理参数。
 */
@Data
public class BusinessTaskActionDTO {

    private String action;

    private String taskId;

    private String businessKey;

    private String processInstanceId;

    private String processDefKey;

    private String taskDefKey;

    private String objectCode;

    private Long recordId;

    private String formKey;

    private String userId;

    private String comment;

    private String signature;

    private Map<String, Object> variables = new LinkedHashMap<>();

    private Map<String, Object> data = new LinkedHashMap<>();
}

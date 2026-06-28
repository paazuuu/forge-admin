package com.mdframe.forge.plugin.generator.vo.businessapp;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 业务待办表单运行上下文。
 */
@Data
public class BusinessTaskFormContextVO {

    private Boolean configured = false;

    private String formType;

    private String taskId;

    private String objectCode;

    private String configKey;

    private Long recordId;

    private String businessKey;

    private String processInstanceId;

    private String processDefKey;

    private String taskDefKey;

    private String formKey;

    private String formName;

    private String providerKey;

    private String formUrl;

    private String viewKey;

    private String editMode;

    private Map<String, Object> formRef = new LinkedHashMap<>();

    private List<Map<String, Object>> fields = new ArrayList<>();

    private List<Map<String, Object>> fieldPermissions = new ArrayList<>();

    private Map<String, Object> recordData = new LinkedHashMap<>();

    private List<String> warnings = new ArrayList<>();
}

package com.mdframe.forge.plugin.generator.dto.businessapp;

import lombok.Data;

import java.util.Map;

/**
 * 编码规则预览请求。
 */
@Data
public class CodeRulePreviewDTO {

    private String ruleCode;

    private String template;

    private Integer sequence;

    private Map<String, Object> context;

    private Map<String, Object> sampleData;
}

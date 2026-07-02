package com.mdframe.forge.plugin.generator.dto.businessapp;

import lombok.Data;

import java.util.Map;

/**
 * 编码规则生成请求。主要用于调试和内部集成验证。
 */
@Data
public class CodeRuleGenerateDTO {

    private String ruleCode;

    private Map<String, Object> context;
}

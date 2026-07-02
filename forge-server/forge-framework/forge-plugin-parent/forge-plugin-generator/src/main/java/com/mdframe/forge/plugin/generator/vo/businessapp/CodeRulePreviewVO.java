package com.mdframe.forge.plugin.generator.vo.businessapp;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 编码规则预览结果。
 */
@Data
public class CodeRulePreviewVO {

    private String template;

    private String previewCode;

    private Boolean valid = true;

    private List<String> usedTokens = new ArrayList<>();

    private List<PreviewIssueVO> warnings = new ArrayList<>();

    private List<PreviewIssueVO> errors = new ArrayList<>();

    @Data
    public static class PreviewIssueVO {
        private String token;
        private String message;
        private String suggestion;
    }
}

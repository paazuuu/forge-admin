package com.mdframe.forge.plugin.generator.dto.lowcode;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 低代码业务领域扩展协议。
 */
@Data
public class LowcodeDomainSchema {

    private AiContext aiContext = new AiContext();

    private Naming naming = new Naming();

    private Defaults defaults = new Defaults();

    private List<LowcodeFieldSchema> fieldTemplates = new ArrayList<>();

    private List<DictRecommendation> dictRecommendations = new ArrayList<>();

    private List<SecurityPolicy> securityPolicies = new ArrayList<>();

    @Data
    public static class AiContext {

        private String description;

        private List<String> terms = new ArrayList<>();

        private List<String> commonObjects = new ArrayList<>();

        private String fieldNamingPreference;

        private List<String> constraints = new ArrayList<>();

        private List<String> generationNotes = new ArrayList<>();
    }

    @Data
    public static class Naming {

        private String tablePrefix;

        private String configKeyPrefix;

        private String objectCodeStyle = "lower_snake";
    }

    @Data
    public static class Defaults {

        private String appType = "SINGLE";

        private String layoutType = "simple-crud";

        private String tableMode = "CREATE";

        private Long menuParentId;
    }

    @Data
    public static class DictRecommendation {

        private String fieldPattern;

        private String dictType;
    }

    @Data
    public static class SecurityPolicy {

        private String fieldPattern;

        private String sensitiveType;

        private String encryptAlgorithm;
    }
}

package com.mdframe.forge.plugin.generator.dto.lowcode;

import lombok.Data;

/**
 * 树形单表运行时配置。
 */
@Data
public class LowcodeTreeConfig {

    private String keyField;

    private String parentField;

    private String labelField;

    private String childrenField;

    private String treeTitle;
}

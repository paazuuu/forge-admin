package com.mdframe.forge.plugin.generator.dto.lowcode;

import lombok.Data;

/**
 * 低代码应用草稿保存请求。
 */
@Data
public class LowcodeAppDraftDTO {

    private Long id;

    private String configKey;

    private String appName;

    private String menuName;

    private Long menuParentId;

    private Integer menuSort;

    private LowcodeModelSchema modelSchema;

    private LowcodePageSchema pageSchema;
}

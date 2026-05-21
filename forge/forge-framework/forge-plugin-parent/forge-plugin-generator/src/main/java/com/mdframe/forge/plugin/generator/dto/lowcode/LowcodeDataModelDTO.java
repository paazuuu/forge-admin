package com.mdframe.forge.plugin.generator.dto.lowcode;

import lombok.Data;

/**
 * 低代码数据模型保存请求。
 */
@Data
public class LowcodeDataModelDTO {

    private Long id;

    private Long domainId;

    private String modelCode;

    private String modelName;

    private String modelDesc;

    private String status;

    private Boolean tenantEnabled;

    private Boolean masterData;

    private LowcodeModelSchema modelSchema;
}

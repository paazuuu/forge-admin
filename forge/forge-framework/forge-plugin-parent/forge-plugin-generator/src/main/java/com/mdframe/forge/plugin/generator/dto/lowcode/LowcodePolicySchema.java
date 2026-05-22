package com.mdframe.forge.plugin.generator.dto.lowcode;

import lombok.Data;

/**
 * 低代码业务对象策略协议。
 */
@Data
public class LowcodePolicySchema {

    private String dataScope = "TENANT";

    private String regionField;

    private Boolean auditEnabled = true;

    /** 主键策略：低代码业务表固定使用 id 自增主键。 */
    private String primaryKeyStrategy = "AUTO_INCREMENT";

    private String primaryKeyField = "id";

    private String tenantField = "tenantId";

    private String logicDeleteField = "delFlag";
}

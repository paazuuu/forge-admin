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
}

package com.mdframe.forge.plugin.generator.dto.lowcode;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 单表低代码数据模型协议。
 */
@Data
public class LowcodeModelSchema {

    /** EXISTING-绑定已有表，CREATE-在线创建新表 */
    private String tableMode;

    private String tableName;

    private String businessName;

    private List<LowcodeFieldSchema> fields = new ArrayList<>();
}

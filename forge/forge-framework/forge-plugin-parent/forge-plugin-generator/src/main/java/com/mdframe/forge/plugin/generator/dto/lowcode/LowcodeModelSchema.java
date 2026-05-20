package com.mdframe.forge.plugin.generator.dto.lowcode;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 单表低代码数据模型协议。
 */
@Data
public class LowcodeModelSchema {

    /** SINGLE-单表，TREE-树形单表，MASTER_DETAIL-主子表协议预留 */
    private String appType;

    /** EXISTING-绑定已有表，CREATE-在线创建新表 */
    private String tableMode;

    private String tableName;

    private String businessName;

    private LowcodeTreeConfig treeConfig;

    private List<LowcodeFieldSchema> fields = new ArrayList<>();

    /** 主子表协议预留，当前运行时暂不启用 */
    private List<LowcodeModelSchema> children = new ArrayList<>();
}

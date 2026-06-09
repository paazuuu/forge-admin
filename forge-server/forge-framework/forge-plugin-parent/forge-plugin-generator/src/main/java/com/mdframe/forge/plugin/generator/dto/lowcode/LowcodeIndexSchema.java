package com.mdframe.forge.plugin.generator.dto.lowcode;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 低代码数据模型索引协议。
 */
@Data
public class LowcodeIndexSchema {

    private String indexName;

    /** NORMAL-普通索引，UNIQUE-唯一索引。 */
    private String indexType = "NORMAL";

    private List<String> fields = new ArrayList<>();

    private Boolean unique = false;

    /** true 表示系统根据关联字段自动生成。 */
    private Boolean auto = false;

    private String remark;
}

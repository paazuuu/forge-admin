package com.mdframe.forge.plugin.generator.dto.lowcode;

import lombok.Data;

/**
 * 低代码应用发布请求。
 */
@Data
public class LowcodePublishDTO {

    /** SKIP_DDL-不执行DDL，ONLINE_CREATE_TABLE-受控在线建表/追加字段 */
    private String deployMode;

    /** 在线 DDL 二次确认标记 */
    private Boolean confirmOnlineDdl;

    private String menuName;

    private Long menuParentId;

    private Integer menuSort;

    private String remark;

    /** 允许发布时传入最新草稿，未传则使用已保存草稿 */
    private LowcodeModelSchema modelSchema;

    private LowcodePageSchema pageSchema;
}

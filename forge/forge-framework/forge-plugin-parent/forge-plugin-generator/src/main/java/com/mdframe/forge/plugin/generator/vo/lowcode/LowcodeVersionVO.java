package com.mdframe.forge.plugin.generator.vo.lowcode;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 低代码应用发布版本。
 */
@Data
public class LowcodeVersionVO {

    private Long id;

    private Long configId;

    private String configKey;

    private Integer versionNo;

    private String versionType;

    private String remark;

    private LocalDateTime createTime;

    private Long createBy;
}

package com.mdframe.forge.plugin.generator.dto.lowcode;

import lombok.Data;

/**
 * 单表低代码字段协议。
 */
@Data
public class LowcodeFieldSchema {

    private String field;

    private String columnName;

    private String label;

    private String dataType;

    private Integer length;

    private Integer precision;

    private Boolean required;

    private Object defaultValue;

    private Boolean searchable;

    private Boolean listVisible;

    private Boolean formVisible;

    private String componentType;

    private String queryType;

    private String dictType;

    /** 敏感类型：NONE/PHONE/ID_CARD/EMAIL/BANK_CARD/NAME/ADDRESS/PASSWORD/CUSTOM */
    private String sensitiveType;

    private String encryptAlgorithm;

    private Boolean sortable;

    private Integer width;

    private String remark;
}

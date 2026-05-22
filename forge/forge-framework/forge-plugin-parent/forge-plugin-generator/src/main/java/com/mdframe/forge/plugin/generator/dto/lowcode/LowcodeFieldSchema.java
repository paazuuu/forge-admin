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

    /** 是否主键字段；低代码业务表固定为 id。 */
    private Boolean primaryKey;

    /** 是否系统字段，系统字段只读展示，不参与业务字段 DDL 追加。 */
    private Boolean systemField;

    /** 是否只读字段，只读字段不能由用户在运行态表单中修改。 */
    private Boolean readonly;

    /** 是否自增字段，当前仅 id 字段固定启用。 */
    private Boolean autoIncrement;

    private Integer width;

    private String remark;
}

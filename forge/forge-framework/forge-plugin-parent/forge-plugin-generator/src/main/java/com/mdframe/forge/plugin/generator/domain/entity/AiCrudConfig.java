package com.mdframe.forge.plugin.generator.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_crud_config")
public class AiCrudConfig extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    private String configKey;
    private String tableName;
    private String tableComment;
    /** 应用显示名称，面向低代码业务人员 */
    private String appName;
    private String searchSchema;
    private String columnsSchema;
    private String editSchema;
    private String apiConfig;
    private String options;
    private String mode;
    /** 构建模式：AI-智能生成，LOWCODE-可视化低代码，CODEGEN-代码生成 */
    private String buildMode;
    private String status;
    /** 发布状态：DRAFT-草稿，PUBLISHED-已发布，STOPPED-已停用 */
    private String publishStatus;
    private String menuName;
    private Long menuParentId;
    private Integer menuSort;
    private Long menuResourceId;
    private String dictConfig;
    private String desensitizeConfig;
    private String encryptConfig;
    private String transConfig;
    /** 页面模板类型，对应 ai_page_template.template_key */
    private String layoutType;
    /** 可视化数据模型协议 */
    private String modelSchema;
    /** 可视化页面搭建协议 */
    private String pageSchema;
    /** 草稿版本号 */
    private Integer draftVersion;
    /** 已发布版本号 */
    private Integer publishedVersion;
    /** 发布时间 */
    private LocalDateTime publishTime;
    /** 发布人 */
    private Long publishBy;
}

package com.mdframe.forge.starter.datascope.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 数据权限配置属性
 */
@Data
@ConfigurationProperties(prefix = "forge.datascope")
public class DataScopeProperties {
    
    /**
     * 是否启用数据权限控制
     */
    private Boolean enabled = true;
    
    /**
     * 是否打印SQL改写日志
     */
    private Boolean printSql = false;

    /**
     * 数据权限控制面元数据所在数据源。
     */
    private String metadataDatasource = "master";

    /**
     * 默认数据权限配置租户。租户未配置时回退到该租户的通用配置。
     */
    private Long defaultConfigTenantId = 1L;
}

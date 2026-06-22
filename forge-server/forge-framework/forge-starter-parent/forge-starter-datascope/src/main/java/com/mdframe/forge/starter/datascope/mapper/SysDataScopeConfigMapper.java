package com.mdframe.forge.starter.datascope.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.starter.datascope.entity.SysDataScopeConfig;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 数据权限配置Mapper
 */
@Mapper
public interface SysDataScopeConfigMapper extends BaseMapper<SysDataScopeConfig> {

    /**
     * 查询启用的数据权限配置，用于构建平台元数据快照。
     */
    List<SysDataScopeConfig> selectEnabledConfigs();
}

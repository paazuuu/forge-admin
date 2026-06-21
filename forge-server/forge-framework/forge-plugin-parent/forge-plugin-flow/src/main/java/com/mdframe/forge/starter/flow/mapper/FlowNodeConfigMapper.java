package com.mdframe.forge.starter.flow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.starter.flow.entity.FlowNodeConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 流程审批节点配置Mapper
 */
@Mapper
public interface FlowNodeConfigMapper extends BaseMapper<FlowNodeConfig> {

    /**
     * 根据模型Key和节点ID查询节点配置。
     */
    FlowNodeConfig selectByModelKeyAndNode(@Param("modelKey") String modelKey,
                                           @Param("nodeId") String nodeId);
}

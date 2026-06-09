package com.mdframe.forge.starter.flow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.starter.flow.entity.FlowBusiness;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 流程业务 Mapper
 */
@Mapper
public interface FlowBusinessMapper extends BaseMapper<FlowBusiness> {
    
    /**
     * 根据流程实例ID查询业务信息
     */
    FlowBusiness selectByProcessInstanceId(@Param("processInstanceId") String processInstanceId);
    
    /**
     * 根据业务Key查询业务信息
     */
    FlowBusiness selectByBusinessKey(@Param("businessKey") String businessKey);

    /**
     * 根据租户和业务Key查询业务信息。
     */
    FlowBusiness selectByBusinessKeyAndTenantId(@Param("tenantId") Long tenantId,
                                                @Param("businessKey") String businessKey);
}

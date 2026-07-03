package com.mdframe.forge.plugin.generator.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessQuantityLedger;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BusinessQuantityLedgerMapper extends BaseMapper<AiBusinessQuantityLedger> {

    AiBusinessQuantityLedger selectByIdempotencyKey(@Param("tenantId") Long tenantId,
                                                    @Param("idempotencyKey") String idempotencyKey);
}

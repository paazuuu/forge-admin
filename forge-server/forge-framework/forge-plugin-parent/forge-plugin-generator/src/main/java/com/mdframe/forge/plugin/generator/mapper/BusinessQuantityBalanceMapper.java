package com.mdframe.forge.plugin.generator.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessQuantityBalance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BusinessQuantityBalanceMapper extends BaseMapper<AiBusinessQuantityBalance> {

    AiBusinessQuantityBalance selectByKey(@Param("tenantId") Long tenantId,
                                          @Param("accountCode") String accountCode,
                                          @Param("itemCode") String itemCode,
                                          @Param("dimensionKey") String dimensionKey);

    int increaseQuantity(@Param("tenantId") Long tenantId,
                         @Param("accountCode") String accountCode,
                         @Param("itemCode") String itemCode,
                         @Param("dimensionKey") String dimensionKey,
                         @Param("quantity") Long quantity);

    int decreaseAvailableQuantity(@Param("tenantId") Long tenantId,
                                  @Param("accountCode") String accountCode,
                                  @Param("itemCode") String itemCode,
                                  @Param("dimensionKey") String dimensionKey,
                                  @Param("quantity") Long quantity);

    int lockQuantity(@Param("tenantId") Long tenantId,
                     @Param("accountCode") String accountCode,
                     @Param("itemCode") String itemCode,
                     @Param("dimensionKey") String dimensionKey,
                     @Param("quantity") Long quantity);

    int releaseLockedQuantity(@Param("tenantId") Long tenantId,
                              @Param("accountCode") String accountCode,
                              @Param("itemCode") String itemCode,
                              @Param("dimensionKey") String dimensionKey,
                              @Param("quantity") Long quantity);

    int commitLockedQuantity(@Param("tenantId") Long tenantId,
                             @Param("accountCode") String accountCode,
                             @Param("itemCode") String itemCode,
                             @Param("dimensionKey") String dimensionKey,
                             @Param("quantity") Long quantity);
}

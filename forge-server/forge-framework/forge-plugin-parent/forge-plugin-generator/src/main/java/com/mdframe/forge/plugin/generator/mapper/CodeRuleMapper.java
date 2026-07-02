package com.mdframe.forge.plugin.generator.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.generator.domain.entity.AiCodeRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CodeRuleMapper extends BaseMapper<AiCodeRule> {

    Page<AiCodeRule> selectRulePage(Page<AiCodeRule> page,
                                    @Param("tenantId") Long tenantId,
                                    @Param("ruleCode") String ruleCode,
                                    @Param("ruleName") String ruleName,
                                    @Param("scene") String scene,
                                    @Param("status") Integer status);

    List<AiCodeRule> selectEnabledList(@Param("tenantId") Long tenantId,
                                       @Param("scene") String scene);

    AiCodeRule selectByRuleCode(@Param("tenantId") Long tenantId,
                                @Param("ruleCode") String ruleCode);

    AiCodeRule selectByRuleId(@Param("tenantId") Long tenantId,
                              @Param("id") Long id);

    int countByRuleCode(@Param("tenantId") Long tenantId,
                        @Param("ruleCode") String ruleCode,
                        @Param("excludeId") Long excludeId);
}

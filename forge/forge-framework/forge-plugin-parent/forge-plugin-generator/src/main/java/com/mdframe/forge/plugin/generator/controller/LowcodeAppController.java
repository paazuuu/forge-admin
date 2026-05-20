package com.mdframe.forge.plugin.generator.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.generator.dto.AiCrudConfigRenderVO;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeAppDraftDTO;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePublishDTO;
import com.mdframe.forge.plugin.generator.service.lowcode.LowcodeAppService;
import com.mdframe.forge.plugin.generator.service.lowcode.LowcodePublishService;
import com.mdframe.forge.plugin.generator.vo.lowcode.LowcodeAppDetailVO;
import com.mdframe.forge.plugin.generator.vo.lowcode.LowcodeVersionVO;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import com.mdframe.forge.starter.core.domain.OperationType;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 低代码 CRUD 应用接口。
 */
@Slf4j
@RestController
@RequestMapping("/ai/lowcode/app")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
public class LowcodeAppController {

    private final LowcodeAppService appService;
    private final LowcodePublishService publishService;

    @GetMapping("/page")
    @OperationLog(module = "低代码应用", type = OperationType.QUERY, desc = "分页查询低代码应用")
    public RespInfo<Page<LowcodeAppDetailVO>> page(PageQuery pageQuery,
                                                   @RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) String publishStatus) {
        return RespInfo.success(appService.page(pageQuery, keyword, publishStatus));
    }

    @GetMapping("/{id}")
    @OperationLog(module = "低代码应用", type = OperationType.QUERY, desc = "查询低代码应用详情")
    public RespInfo<LowcodeAppDetailVO> detail(@PathVariable Long id) {
        return RespInfo.success(appService.getDetail(id));
    }

    @PostMapping("/draft")
    @OperationLog(module = "低代码应用", type = OperationType.UPDATE, desc = "保存低代码应用草稿")
    public RespInfo<Long> saveDraft(@RequestBody LowcodeAppDraftDTO dto) {
        return RespInfo.success(appService.saveDraft(dto));
    }

    @PostMapping("/{id}/preview")
    @OperationLog(module = "低代码应用", type = OperationType.QUERY, desc = "预览低代码应用草稿")
    public RespInfo<AiCrudConfigRenderVO> preview(@PathVariable Long id,
                                                  @RequestBody(required = false) LowcodeAppDraftDTO dto) {
        return RespInfo.success(appService.preview(id, dto));
    }

    @PostMapping("/{id}/publish")
    @OperationLog(module = "低代码应用", type = OperationType.UPDATE, desc = "发布低代码应用")
    public RespInfo<Long> publish(@PathVariable Long id, @RequestBody LowcodePublishDTO dto) {
        return RespInfo.success(publishService.publish(id, dto));
    }

    @GetMapping("/{id}/versions")
    @OperationLog(module = "低代码应用", type = OperationType.QUERY, desc = "查询低代码应用版本")
    public RespInfo<List<LowcodeVersionVO>> versions(@PathVariable Long id) {
        return RespInfo.success(publishService.listVersions(id));
    }

    @PostMapping("/{id}/rollback/{versionId}")
    @OperationLog(module = "低代码应用", type = OperationType.UPDATE, desc = "回滚低代码应用版本")
    public RespInfo<Void> rollback(@PathVariable Long id, @PathVariable Long versionId) {
        publishService.rollback(id, versionId);
        return RespInfo.success();
    }
}

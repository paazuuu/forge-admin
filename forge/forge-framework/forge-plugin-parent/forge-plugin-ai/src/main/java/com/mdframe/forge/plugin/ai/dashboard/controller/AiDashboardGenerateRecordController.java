package com.mdframe.forge.plugin.ai.dashboard.controller;

import com.mdframe.forge.plugin.ai.dashboard.domain.AiDashboardGenerateRecord;
import com.mdframe.forge.plugin.ai.dashboard.dto.AiDashboardGenerateRecordSaveDTO;
import com.mdframe.forge.plugin.ai.dashboard.service.AiDashboardGenerateRecordService;
import com.mdframe.forge.plugin.ai.dashboard.vo.AiDashboardDatasetImpactVO;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ai/dashboard-generate-record")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
public class AiDashboardGenerateRecordController {

    private final AiDashboardGenerateRecordService recordService;

    @PostMapping
    public RespInfo<AiDashboardGenerateRecord> save(@RequestBody AiDashboardGenerateRecordSaveDTO dto) {
        return RespInfo.success(recordService.saveGenerateRecord(dto));
    }

    @GetMapping("/recent")
    public RespInfo<List<AiDashboardGenerateRecord>> recent(
            @RequestParam(required = false) Long businessDefinitionId,
            @RequestParam(required = false) Long projectId,
            @RequestParam(defaultValue = "20") Integer limit) {
        return RespInfo.success(recordService.listRecent(businessDefinitionId, projectId, limit));
    }

    @DeleteMapping("/{id}")
    public RespInfo<Void> delete(@PathVariable Long id) {
        recordService.deleteOwn(id);
        return RespInfo.success();
    }

    @GetMapping("/impact/dataset/{datasetId}")
    public RespInfo<List<AiDashboardDatasetImpactVO>> datasetImpact(
            @PathVariable Long datasetId,
            @RequestParam(defaultValue = "50") Integer limit) {
        return RespInfo.success(recordService.listDatasetImpact(datasetId, limit));
    }
}

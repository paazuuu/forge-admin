package com.mdframe.forge.plugin.generator.controller;

import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeDdlPreviewDTO;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.service.lowcode.LowcodeDdlService;
import com.mdframe.forge.plugin.generator.service.lowcode.LowcodeSchemaValidator;
import com.mdframe.forge.plugin.generator.vo.lowcode.LowcodeDdlPreviewVO;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import com.mdframe.forge.starter.core.domain.OperationType;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 低代码数据模型接口。
 */
@Slf4j
@RestController
@RequestMapping("/ai/lowcode/model")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
public class LowcodeModelController {

    private final LowcodeSchemaValidator schemaValidator;
    private final LowcodeDdlService ddlService;

    @PostMapping("/validate")
    @OperationLog(module = "低代码模型", type = OperationType.QUERY, desc = "校验低代码数据模型")
    public RespInfo<Void> validate(@RequestBody LowcodeModelSchema modelSchema) {
        schemaValidator.validateModel(modelSchema);
        return RespInfo.success();
    }

    @PostMapping("/ddl/preview")
    @OperationLog(module = "低代码模型", type = OperationType.QUERY, desc = "预览低代码建表DDL")
    public RespInfo<LowcodeDdlPreviewVO> previewDdl(@RequestBody LowcodeDdlPreviewDTO dto) {
        return RespInfo.success(ddlService.previewCreateTable(dto.getModelSchema()));
    }
}

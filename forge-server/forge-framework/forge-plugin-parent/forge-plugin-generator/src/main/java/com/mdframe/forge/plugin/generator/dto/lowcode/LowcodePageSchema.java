package com.mdframe.forge.plugin.generator.dto.lowcode;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 单表低代码页面搭建协议。
 */
@Data
public class LowcodePageSchema {

    private String layoutType;

    private String listLayoutMode;

    private Map<String, Object> listGridLayout = new LinkedHashMap<>();

    private Long primaryModelId;

    private String primaryModelCode;

    private List<LowcodePageModelRef> modelRefs = new ArrayList<>();

    private List<LowcodePageZone> zones = new ArrayList<>();
}

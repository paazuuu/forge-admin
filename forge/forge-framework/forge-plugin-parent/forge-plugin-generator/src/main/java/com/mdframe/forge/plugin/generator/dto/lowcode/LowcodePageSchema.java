package com.mdframe.forge.plugin.generator.dto.lowcode;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 单表低代码页面搭建协议。
 */
@Data
public class LowcodePageSchema {

    private String layoutType;

    private List<LowcodePageZone> zones = new ArrayList<>();
}

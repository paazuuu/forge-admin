package com.mdframe.forge.plugin.generator.service.businessapp;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.generator.domain.entity.AiCodeRule;
import com.mdframe.forge.plugin.generator.dto.businessapp.CodeRulePreviewDTO;
import com.mdframe.forge.plugin.generator.mapper.CodeRuleMapper;
import com.mdframe.forge.plugin.generator.vo.businessapp.CodeRulePreviewVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.CodeRuleTokenVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.id.service.ISequenceService;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 通用编码规则服务。
 */
@Service
@RequiredArgsConstructor
public class CodeRuleService extends ServiceImpl<CodeRuleMapper, AiCodeRule> {

    private static final Pattern RULE_CODE_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9_\\-]{1,63}$");
    private static final Pattern TOKEN_PATTERN = Pattern.compile("\\$\\{([^}]+)}");
    private static final Pattern LEGACY_TOKEN_PATTERN = Pattern.compile("(?<!\\$)\\{([^{}]+)}");
    private static final Pattern LEGACY_SEQ_TOKEN_PATTERN = Pattern.compile("seq(\\d+)");
    private static final Set<String> RESET_POLICIES = Set.of("AUTO", "NONE", "YEAR", "MONTH", "DAY", "HOUR", "MINUTE", "SECOND");

    private final ISequenceService sequenceService;

    public Page<AiCodeRule> page(Integer pageNum,
                                 Integer pageSize,
                                 String ruleCode,
                                 String ruleName,
                                 String scene,
                                 Integer status) {
        Page<AiCodeRule> page = new Page<>(normalizePageNum(pageNum), normalizePageSize(pageSize));
        return baseMapper.selectRulePage(page, resolveTenantId(), trimToNull(ruleCode), trimToNull(ruleName),
                trimToNull(scene), status);
    }

    public List<AiCodeRule> listEnabled(String scene) {
        return baseMapper.selectEnabledList(resolveTenantId(), trimToNull(scene));
    }

    public AiCodeRule detail(Long id) {
        AiCodeRule rule = baseMapper.selectByRuleId(resolveTenantId(), id);
        if (rule == null) {
            throw new BusinessException("编码规则不存在");
        }
        return rule;
    }

    @Transactional(rollbackFor = Exception.class)
    public void create(AiCodeRule rule) {
        if (rule == null) {
            throw new BusinessException("编码规则不能为空");
        }
        normalizeAndValidate(rule, true);
        rule.setTenantId(resolveTenantId());
        rule.setBuiltin(0);
        save(rule);
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(AiCodeRule rule) {
        if (rule == null || rule.getId() == null) {
            throw new BusinessException("编码规则ID不能为空");
        }
        AiCodeRule exists = detail(rule.getId());
        normalizeAndValidate(rule, false);
        rule.setTenantId(exists.getTenantId());
        rule.setBuiltin(exists.getBuiltin());
        updateById(rule);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        AiCodeRule exists = detail(id);
        AiCodeRule update = new AiCodeRule();
        update.setId(exists.getId());
        update.setTenantId(exists.getTenantId());
        update.setStatus(Integer.valueOf(1).equals(status) ? 1 : 0);
        updateById(update);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        AiCodeRule exists = detail(id);
        if (Integer.valueOf(1).equals(exists.getBuiltin())) {
            throw new BusinessException("内置编码规则不能删除，可停用后新建自定义规则");
        }
        removeById(id);
    }

    public List<CodeRuleTokenVO> listTokens() {
        List<CodeRuleTokenVO> tokens = new ArrayList<>();
        tokens.add(token("${yyyy}", "年份", "日期时间", "当前年份，四位数字", "2026", "2026"));
        tokens.add(token("${yyyyMM}", "年月", "日期时间", "当前年月，六位数字", "202607", "202607"));
        tokens.add(token("${yyyyMMdd}", "年月日", "日期时间", "当前日期，八位数字", "20260701", "20260701"));
        tokens.add(token("${yyyyMMddHHmmss}", "年月日时分秒", "日期时间", "当前时间到秒", "20260701153021", "20260701153021"));
        tokens.add(token("${HHmmss}", "时分秒", "日期时间", "当前时间，六位数字", "153021", "153021"));
        tokens.add(token("${seq}", "流水号", "序列", "使用规则默认流水号长度", "0001", "0001"));
        tokens.add(token("${seq:4}", "四位流水号", "序列", "流水号左侧补零到指定长度", "0001", "0001"));
        tokens.add(token("${tenantId}", "租户ID", "上下文", "当前租户ID", "1", "1"));
        tokens.add(token("${userId}", "用户ID", "上下文", "当前登录用户ID", "10001", "10001"));
        tokens.add(token("${username}", "用户名", "上下文", "当前登录用户名", "zhangsan", "zhangsan"));
        tokens.add(token("${suiteCode}", "套件编码", "上下文", "当前业务套件编码", "CRM", "CRM"));
        tokens.add(token("${objectCode}", "对象编码", "上下文", "当前业务对象编码", "MATERIAL", "MATERIAL"));
        tokens.add(token("${deptCode}", "部门编码", "上下文", "当前部门或组织编码", "DEPT01", "DEPT01"));
        tokens.add(token("${orgCode}", "组织编码", "上下文", "当前组织编码", "ORG01", "ORG01"));
        tokens.add(token("${field:<fieldCode>}", "业务字段", "业务字段", "读取记录字段值，例如 ${field:materialType}", "RAW", "RAW"));
        return tokens;
    }

    public CodeRulePreviewVO preview(CodeRulePreviewDTO dto) {
        CodeRulePreviewDTO source = dto == null ? new CodeRulePreviewDTO() : dto;
        AiCodeRule rule = resolvePreviewRule(source);
        Map<String, Object> context = mergePreviewContext(source);
        long sequence = source.getSequence() == null ? 1L : Math.max(source.getSequence().longValue(), 1L);
        return render(rule, context, sequence, LocalDateTime.now(), true);
    }

    public String generate(String ruleCode, Map<String, Object> context) {
        AiCodeRule rule = requireEnabledRule(ruleCode);
        LocalDateTime now = LocalDateTime.now();
        long sequence = containsSequenceToken(rule.getTemplate())
                ? sequenceService.nextId(buildSequenceKey(rule, context, now))
                : 1L;
        CodeRulePreviewVO rendered = render(rule, context, sequence, now, false);
        if (!rendered.getErrors().isEmpty()) {
            String message = rendered.getErrors().stream()
                    .map(CodeRulePreviewVO.PreviewIssueVO::getMessage)
                    .findFirst()
                    .orElse("编码规则不正确");
            throw new BusinessException(message);
        }
        return rendered.getPreviewCode();
    }

    private void normalizeAndValidate(AiCodeRule rule, boolean creating) {
        rule.setRuleCode(StringUtils.trimToNull(rule.getRuleCode()));
        rule.setRuleName(StringUtils.trimToNull(rule.getRuleName()));
        rule.setScene(StringUtils.defaultIfBlank(StringUtils.trimToNull(rule.getScene()), "COMMON"));
        rule.setTemplate(normalizeTemplate(rule.getTemplate()));
        rule.setResetPolicy(normalizeResetPolicy(rule.getResetPolicy()));
        rule.setSeqLength(normalizeSeqLength(rule.getSeqLength()));
        rule.setStatus(Integer.valueOf(0).equals(rule.getStatus()) ? 0 : 1);
        rule.setRemark(StringUtils.trimToNull(rule.getRemark()));
        rule.setOptions(StringUtils.trimToNull(rule.getOptions()));
        if (StringUtils.isBlank(rule.getRuleCode())) {
            throw new BusinessException("规则编码不能为空");
        }
        if (!RULE_CODE_PATTERN.matcher(rule.getRuleCode()).matches()) {
            throw new BusinessException("规则编码只能以字母开头，并包含字母、数字、下划线或短横线");
        }
        if (StringUtils.isBlank(rule.getRuleName())) {
            throw new BusinessException("规则名称不能为空");
        }
        if (StringUtils.isBlank(rule.getTemplate())) {
            throw new BusinessException("编码模板不能为空");
        }
        Long excludeId = creating ? null : rule.getId();
        if (baseMapper.countByRuleCode(resolveTenantId(), rule.getRuleCode(), excludeId) > 0) {
            throw new BusinessException("规则编码已存在");
        }
        CodeRulePreviewVO preview = render(rule, Map.of("fieldCode", "SAMPLE"), 1L, LocalDateTime.now(), true);
        if (!preview.getErrors().isEmpty()) {
            String message = preview.getErrors().stream()
                    .map(CodeRulePreviewVO.PreviewIssueVO::getMessage)
                    .findFirst()
                    .orElse("编码模板不正确");
            throw new BusinessException(message);
        }
    }

    private AiCodeRule requireEnabledRule(String ruleCode) {
        String normalized = StringUtils.trimToNull(ruleCode);
        if (StringUtils.isBlank(normalized)) {
            throw new BusinessException("自动编号未选择编码规则");
        }
        AiCodeRule rule = baseMapper.selectByRuleCode(resolveTenantId(), normalized);
        if (rule == null) {
            throw new BusinessException("编码规则不存在: " + normalized);
        }
        if (!Integer.valueOf(1).equals(rule.getStatus())) {
            throw new BusinessException("编码规则已停用: " + normalized);
        }
        rule.setTemplate(normalizeTemplate(rule.getTemplate()));
        rule.setSeqLength(normalizeSeqLength(rule.getSeqLength()));
        rule.setResetPolicy(normalizeResetPolicy(rule.getResetPolicy()));
        return rule;
    }

    private AiCodeRule resolvePreviewRule(CodeRulePreviewDTO dto) {
        if (StringUtils.isNotBlank(dto.getRuleCode())) {
            AiCodeRule rule = baseMapper.selectByRuleCode(resolveTenantId(), dto.getRuleCode().trim());
            if (rule == null) {
                throw new BusinessException("编码规则不存在: " + dto.getRuleCode());
            }
            rule.setTemplate(StringUtils.defaultIfBlank(normalizeTemplate(dto.getTemplate()), normalizeTemplate(rule.getTemplate())));
            rule.setSeqLength(normalizeSeqLength(rule.getSeqLength()));
            rule.setResetPolicy(normalizeResetPolicy(rule.getResetPolicy()));
            return rule;
        }
        AiCodeRule rule = new AiCodeRule();
        rule.setRuleCode("preview");
        rule.setRuleName("预览规则");
        rule.setTemplate(StringUtils.defaultIfBlank(normalizeTemplate(dto.getTemplate()), "CODE${yyyyMMdd}${seq:4}"));
        rule.setSeqLength(4);
        rule.setResetPolicy("AUTO");
        return rule;
    }

    private CodeRulePreviewVO render(AiCodeRule rule,
                                     Map<String, Object> context,
                                     Long sequence,
                                     LocalDateTime now,
                                     boolean previewMode) {
        CodeRulePreviewVO vo = new CodeRulePreviewVO();
        String template = normalizeTemplate(rule == null ? null : rule.getTemplate());
        vo.setTemplate(template);
        if (StringUtils.isBlank(template)) {
            vo.getErrors().add(issue(null, "编码模板不能为空", "请选择编码规则或填写模板"));
            vo.setValid(false);
            return vo;
        }
        StringBuilder result = new StringBuilder();
        Matcher matcher = TOKEN_PATTERN.matcher(template);
        int lastIndex = 0;
        while (matcher.find()) {
            result.append(template, lastIndex, matcher.start());
            String token = matcher.group(1);
            vo.getUsedTokens().add("${" + token + "}");
            result.append(resolveToken(rule, token, context, sequence, now, vo, previewMode));
            lastIndex = matcher.end();
        }
        result.append(template.substring(lastIndex));
        String code = result.toString();
        vo.setPreviewCode(code);
        if (!code.matches("[A-Za-z0-9_\\-./]+")) {
            vo.getWarnings().add(issue(null, "编码包含空格或特殊字符，可能不适合作为业务编号", "建议只使用字母、数字、短横线、下划线、点和斜线"));
        }
        if (code.length() > 96) {
            vo.getWarnings().add(issue(null, "编码长度超过 96 个字符", "建议缩短固定前缀或字段变量内容"));
        }
        vo.setValid(vo.getErrors().isEmpty());
        return vo;
    }

    private String resolveToken(AiCodeRule rule,
                                String token,
                                Map<String, Object> context,
                                Long sequence,
                                LocalDateTime now,
                                CodeRulePreviewVO vo,
                                boolean previewMode) {
        return switch (token) {
            case "yyyy" -> now.format(DateTimeFormatter.ofPattern("yyyy"));
            case "yyyyMM" -> now.format(DateTimeFormatter.ofPattern("yyyyMM"));
            case "yyyyMMdd" -> now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            case "yyyyMMddHHmmss" -> now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            case "HHmmss" -> now.format(DateTimeFormatter.ofPattern("HHmmss"));
            case "seq" -> renderSequenceToken(token, sequence, normalizeSeqLength(rule == null ? null : rule.getSeqLength()), vo);
            case "tenantId" -> text(firstNonBlank(readContextValue(context, "tenantId"), resolveTenantId()));
            case "userId" -> text(firstNonBlank(readContextValue(context, "userId"), safeLoginUserId()));
            case "username" -> text(firstNonBlank(readContextValue(context, "username"), safeUsername()));
            case "starter" -> text(firstNonBlank(readContextValue(context, "starter"), safeUsername(), "starter"));
            case "suiteCode" -> text(firstNonBlank(readContextValue(context, "suiteCode"), "SUITE"));
            case "objectCode" -> text(firstNonBlank(readContextValue(context, "objectCode"), "OBJECT"));
            case "deptCode" -> text(firstNonBlank(readContextValue(context, "deptCode"), safeDeptCode(), "DEPT"));
            case "orgCode" -> text(firstNonBlank(readContextValue(context, "orgCode"), readContextValue(context, "deptCode"), safeDeptCode(), "ORG"));
            case "ruleCode" -> text(firstNonBlank(rule == null ? null : rule.getRuleCode(), "RULE"));
            case "ruleName" -> text(firstNonBlank(rule == null ? null : rule.getRuleName(), "规则"));
            default -> resolveDynamicToken(token, context, sequence, vo, previewMode);
        };
    }

    private String resolveDynamicToken(String token,
                                       Map<String, Object> context,
                                       Long sequence,
                                       CodeRulePreviewVO vo,
                                       boolean previewMode) {
        if (token.startsWith("seq:")) {
            String lengthText = token.substring("seq:".length()).trim();
            int length;
            try {
                length = Integer.parseInt(lengthText);
            } catch (Exception e) {
                vo.getErrors().add(issue("${" + token + "}", "流水号长度必须是数字", "例如 ${seq:4}"));
                return "";
            }
            return renderSequenceToken(token, sequence, length, vo);
        }
        if (token.startsWith("field:")) {
            String fieldCode = token.substring("field:".length()).trim();
            if (StringUtils.isBlank(fieldCode)) {
                vo.getErrors().add(issue("${" + token + "}", "字段变量缺少字段编码", "改为 ${field:字段编码}"));
                return "";
            }
            Object value = readContextValue(context, fieldCode);
            if (isBlankValue(value)) {
                CodeRulePreviewVO.PreviewIssueVO issue = issue("${" + token + "}", "上下文中没有字段 " + fieldCode,
                        "预览时可传入 sampleData，运行态会读取真实记录字段");
                if (previewMode) {
                    vo.getWarnings().add(issue);
                    return fieldCode;
                }
                vo.getErrors().add(issue);
                return "";
            }
            return String.valueOf(value);
        }
        vo.getErrors().add(issue("${" + token + "}", "未知编码变量: ${" + token + "}", "从内置变量列表选择，或使用 ${field:<fieldCode>} 引用业务字段"));
        return "";
    }

    private String renderSequenceToken(String token, Long sequence, Integer length, CodeRulePreviewVO vo) {
        int normalizedLength = length == null ? 4 : length;
        if (normalizedLength <= 0 || normalizedLength > 16) {
            vo.getErrors().add(issue("${" + token + "}", "流水号长度建议在 1-16 之间", "例如 ${seq:4}"));
            return "";
        }
        String seqText = String.valueOf(Math.max(sequence == null ? 1L : sequence, 1L));
        return StringUtils.leftPad(seqText, normalizedLength, '0');
    }

    private String buildSequenceKey(AiCodeRule rule, Map<String, Object> context, LocalDateTime now) {
        String tenantId = String.valueOf(resolveTenantId());
        String period = resolveSequencePeriod(rule, now);
        String ruleCode = safeSequencePart(rule == null ? "RULE" : rule.getRuleCode());
        Object scope = firstNonBlank(readContextValue(context, "sequenceScope"), readContextValue(context, "objectCode"), "global");
        return "code-rule:" + tenantId + ":" + ruleCode + ":" + safeSequencePart(text(scope)) + ":" + period;
    }

    private String resolveSequencePeriod(AiCodeRule rule, LocalDateTime now) {
        String policy = normalizeResetPolicy(rule == null ? null : rule.getResetPolicy());
        if ("AUTO".equals(policy)) {
            policy = inferResetPolicy(rule == null ? null : rule.getTemplate());
        }
        return switch (policy) {
            case "SECOND" -> now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            case "MINUTE" -> now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
            case "HOUR" -> now.format(DateTimeFormatter.ofPattern("yyyyMMddHH"));
            case "DAY" -> now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            case "MONTH" -> now.format(DateTimeFormatter.ofPattern("yyyyMM"));
            case "YEAR" -> now.format(DateTimeFormatter.ofPattern("yyyy"));
            default -> "all";
        };
    }

    private String inferResetPolicy(String template) {
        String value = StringUtils.defaultString(template);
        if (value.contains("yyyyMMddHHmmss")) {
            return "SECOND";
        }
        if (value.contains("yyyyMMdd")) {
            return "DAY";
        }
        if (value.contains("yyyyMM")) {
            return "MONTH";
        }
        if (value.contains("yyyy")) {
            return "YEAR";
        }
        return "NONE";
    }

    private boolean containsSequenceToken(String template) {
        Matcher matcher = TOKEN_PATTERN.matcher(StringUtils.defaultString(template));
        while (matcher.find()) {
            String token = matcher.group(1);
            if ("seq".equals(token) || token.startsWith("seq:")) {
                return true;
            }
        }
        return false;
    }

    private Map<String, Object> mergePreviewContext(CodeRulePreviewDTO dto) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (dto.getContext() != null) {
            result.putAll(dto.getContext());
        }
        if (dto.getSampleData() != null) {
            result.put("sampleData", dto.getSampleData());
            result.putAll(dto.getSampleData());
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Object readContextValue(Map<String, Object> context, String key) {
        if (context == null || StringUtils.isBlank(key)) {
            return null;
        }
        for (String alias : fieldAliases(key)) {
            if (context.containsKey(alias)) {
                return context.get(alias);
            }
        }
        for (String nestedKey : List.of("recordData", "record", "data", "sampleData")) {
            Object nested = context.get(nestedKey);
            if (nested instanceof Map<?, ?> map) {
                Map<String, Object> nestedMap = (Map<String, Object>) map;
                for (String alias : fieldAliases(key)) {
                    if (nestedMap.containsKey(alias)) {
                        return nestedMap.get(alias);
                    }
                }
            }
        }
        return null;
    }

    private List<String> fieldAliases(String key) {
        String value = StringUtils.defaultString(key).trim();
        if (StringUtils.isBlank(value)) {
            return List.of();
        }
        return List.of(value, snakeToCamel(value), camelToSnake(value));
    }

    private String normalizeTemplate(String template) {
        String value = StringUtils.trimToNull(template);
        if (value == null) {
            return null;
        }
        Matcher matcher = LEGACY_TOKEN_PATTERN.matcher(value);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(buffer, Matcher.quoteReplacement("${" + normalizeLegacyToken(matcher.group(1)) + "}"));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private String normalizeLegacyToken(String token) {
        String value = StringUtils.defaultString(token).trim();
        Matcher seqMatcher = LEGACY_SEQ_TOKEN_PATTERN.matcher(value);
        if (seqMatcher.matches()) {
            return "seq:" + seqMatcher.group(1);
        }
        return value;
    }

    private String normalizeResetPolicy(String value) {
        String policy = StringUtils.defaultIfBlank(value, "AUTO").trim().toUpperCase(Locale.ROOT);
        if (!RESET_POLICIES.contains(policy)) {
            throw new BusinessException("不支持的重置周期: " + value);
        }
        return policy;
    }

    private Integer normalizeSeqLength(Integer value) {
        int length = value == null ? 4 : value;
        if (length <= 0 || length > 16) {
            throw new BusinessException("流水号长度必须在 1-16 之间");
        }
        return length;
    }

    private CodeRuleTokenVO token(String insertText,
                                  String label,
                                  String groupName,
                                  String description,
                                  String example,
                                  String sampleValue) {
        CodeRuleTokenVO vo = new CodeRuleTokenVO();
        vo.setToken(insertText);
        vo.setInsertText(insertText);
        vo.setLabel(label);
        vo.setGroupName(groupName);
        vo.setDescription(description);
        vo.setExample(example);
        vo.setSampleValue(sampleValue);
        return vo;
    }

    private CodeRulePreviewVO.PreviewIssueVO issue(String token, String message, String suggestion) {
        CodeRulePreviewVO.PreviewIssueVO vo = new CodeRulePreviewVO.PreviewIssueVO();
        vo.setToken(token);
        vo.setMessage(message);
        vo.setSuggestion(suggestion);
        return vo;
    }

    private boolean isBlankValue(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String text) {
            return StringUtils.isBlank(text);
        }
        if (value instanceof Collection<?> collection) {
            return collection.isEmpty();
        }
        return false;
    }

    private Object firstNonBlank(Object... values) {
        if (values == null) {
            return null;
        }
        for (Object value : values) {
            if (!isBlankValue(value)) {
                return value;
            }
        }
        return null;
    }

    private LoginUser safeLoginUser() {
        try {
            return SessionHelper.getLoginUser();
        } catch (Exception e) {
            return null;
        }
    }

    private Long safeLoginUserId() {
        LoginUser user = safeLoginUser();
        return user == null ? null : user.getUserId();
    }

    private String safeUsername() {
        LoginUser user = safeLoginUser();
        if (user == null) {
            return null;
        }
        return StringUtils.firstNonBlank(user.getUsername(), user.getRealName(),
                user.getUserId() == null ? null : String.valueOf(user.getUserId()));
    }

    private String safeDeptCode() {
        LoginUser user = safeLoginUser();
        if (user == null) {
            return null;
        }
        return StringUtils.firstNonBlank(
                user.getMainOrgId() == null ? null : String.valueOf(user.getMainOrgId()),
                user.getDeptName());
    }

    private Long resolveTenantId() {
        Long tenantId;
        try {
            tenantId = SessionHelper.getTenantId();
        } catch (Exception e) {
            tenantId = null;
        }
        if (tenantId == null) {
            tenantId = TenantContextHolder.getTenantId();
        }
        return tenantId != null ? tenantId : 1L;
    }

    private int normalizePageNum(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 10;
        }
        return Math.min(pageSize, 200);
    }

    private String trimToNull(String value) {
        return StringUtils.trimToNull(value);
    }

    private String safeSequencePart(String value) {
        String result = StringUtils.defaultIfBlank(value, "NA").replaceAll("[^A-Za-z0-9_\\-]", "_");
        return StringUtils.defaultIfBlank(result, "NA");
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String camelToSnake(String value) {
        if (StringUtils.isBlank(value)) {
            return value;
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (Character.isUpperCase(ch) && i > 0) {
                result.append('_');
            }
            result.append(Character.toLowerCase(ch));
        }
        return result.toString();
    }

    private String snakeToCamel(String value) {
        if (StringUtils.isBlank(value)) {
            return value;
        }
        StringBuilder result = new StringBuilder();
        boolean upperNext = false;
        for (char ch : value.toCharArray()) {
            if (ch == '_' || ch == '-') {
                upperNext = true;
                continue;
            }
            result.append(upperNext ? Character.toUpperCase(ch) : ch);
            upperNext = false;
        }
        return result.toString();
    }
}

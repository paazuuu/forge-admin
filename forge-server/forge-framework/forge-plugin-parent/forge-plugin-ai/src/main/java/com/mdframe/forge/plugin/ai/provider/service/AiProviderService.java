package com.mdframe.forge.plugin.ai.provider.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.ai.constant.AiConstants;
import com.mdframe.forge.plugin.ai.provider.domain.AiProvider;
import com.mdframe.forge.plugin.ai.provider.mapper.AiProviderMapper;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AiProviderService extends ServiceImpl<AiProviderMapper, AiProvider> {

    /**
     * 获取默认供应商
     */
    public AiProvider getDefaultProvider() {
        return getOne(new LambdaQueryWrapper<AiProvider>()
                .eq(AiProvider::getIsDefault, AiConstants.IS_DEFAULT_YES)
                .eq(AiProvider::getStatus, AiConstants.STATUS_NORMAL)
                .last("LIMIT 1"));
    }

    /**
     * 测试供应商连接（发送一条简单消息验证可用性）
     *
     * @param provider 供应商配置（包括 baseUrl 和 apiKey）
     * @return true 表示连接成功
     */
    public String testConnection(AiProvider provider) {
        if (!StringUtils.hasText(provider.getApiKey())) {
            throw new BusinessException("API Key 不能为空");
        }
        if (!StringUtils.hasText(provider.getBaseUrl())) {
            throw new BusinessException("Base URL 不能为空");
        }
        try {
            OpenAiApi openAiApi = OpenAiApi.builder()
                    .baseUrl(provider.getBaseUrl())
                    .apiKey(provider.getApiKey())
                    .build();
            String model = StringUtils.hasText(provider.getDefaultModel())
                    ? provider.getDefaultModel() : "gpt-3.5-turbo";
            ChatModel chatModel = OpenAiChatModel.builder()
                    .openAiApi(openAiApi)
                    .defaultOptions(OpenAiChatOptions.builder()
                            .model(model)
                            .maxTokens(128)
                            .build())
                    .build();
            Prompt prompt = new Prompt(List.of(new UserMessage("请只回复 OK。若为推理模型，也请在最终答案中明确输出 OK。")));
            ChatResponse chatResponse = chatModel.call(prompt);
            log.info("[AI供应商测试] 连接成功, provider={},chatResponse:{}", provider.getProviderName(),chatResponse.getResult().toString());
            AssistantMessage message = chatResponse.getResult().getOutput();
            String content = message != null ? message.getText() : null;
            String reasoningContent = extractReasoningContent(message);
            return buildTestResult(model, content, reasoningContent);
        } catch (Exception e) {
            log.warn("[AI供应商测试] 连接失败, provider={}, error={}", provider.getProviderName(), e.getMessage());
            throw new BusinessException("连接失败: " + e.getMessage());
        }
    }

    private String buildTestResult(String model, String content, String reasoningContent) {
        String normalizedContent = StringUtils.hasText(content) ? content.trim() : "";
        String normalizedReasoning = StringUtils.hasText(reasoningContent) ? reasoningContent.trim() : "";

        StringBuilder result = new StringBuilder("连接成功");
        if (StringUtils.hasText(model)) {
            result.append("\n模型: ").append(model);
        }
        if (StringUtils.hasText(normalizedReasoning)) {
            result.append("\n\n思考过程:\n").append(normalizedReasoning);
        }
        if (StringUtils.hasText(normalizedContent)) {
            result.append("\n\n回复内容:\n").append(normalizedContent);
        } else if (StringUtils.hasText(normalizedReasoning)) {
            result.append("\n\n回复内容:\n")
                    .append("(模型已返回思考过程，但未输出可见最终答案。通常是推理模型在测试模式下的正常表现。)");
        } else {
            result.append("\n\n回复内容:\n")
                    .append("(模型已连通，但当前响应未返回可见文本。)");
        }
        return result.toString();
    }

    private String extractReasoningContent(AssistantMessage message) {
        if (message == null) {
            return null;
        }
        Map<String, Object> metadata = message.getMetadata();
        if (metadata == null) {
            return null;
        }
        Object reasoning = metadata.get("reasoningContent");
        if (reasoning instanceof String reasoningText) {
            return reasoningText;
        }
        reasoning = metadata.get("reasoning_content");
        if (reasoning instanceof String reasoningText) {
            return reasoningText;
        }
        reasoning = metadata.get("reasoning");
        if (reasoning instanceof String reasoningText) {
            return reasoningText;
        }
        return null;
    }

    /**
     * 删除供应商
     *
     * @param id 供应商ID
     */
    public void deleteProvider(Long id) {
        removeById(id);
        log.info("[AI供应商] 删除供应商, id={}", id);
    }

    /**
     * 设为默认供应商（先清除其他默认，再设当前）
     *
     * @param id 供应商ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long id) {
        // 清除全部默认
        update(new LambdaUpdateWrapper<AiProvider>()
                .set(AiProvider::getIsDefault, AiConstants.IS_DEFAULT_NO)
                .eq(AiProvider::getIsDefault, AiConstants.IS_DEFAULT_YES));
        // 设置当前为默认
        update(new LambdaUpdateWrapper<AiProvider>()
                .set(AiProvider::getIsDefault, AiConstants.IS_DEFAULT_YES)
                .eq(AiProvider::getId, id));
        log.info("[AI供应商] 设为默认供应商, id={}", id);
    }
}

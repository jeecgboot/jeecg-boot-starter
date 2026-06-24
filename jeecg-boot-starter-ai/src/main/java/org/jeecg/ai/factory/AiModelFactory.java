package org.jeecg.ai.factory;

import dev.langchain4j.community.model.dashscope.*;
import dev.langchain4j.community.model.qianfan.*;
import dev.langchain4j.community.model.zhipu.ZhipuAiChatModel;
import dev.langchain4j.community.model.zhipu.ZhipuAiEmbeddingModel;
import dev.langchain4j.community.model.zhipu.ZhipuAiStreamingChatModel;
import dev.langchain4j.http.client.HttpClientBuilder;
import dev.langchain4j.http.client.jdk.JdkHttpClient;
import dev.langchain4j.http.client.jdk.JdkHttpClientBuilder;
import dev.langchain4j.http.client.okhttp.OkHttpClientBuilder;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.jeecg.ai.custom.zhipu.CustomZhipuAiImageModel;
import dev.langchain4j.community.model.zhipu.chat.ChatCompletionModel;
import dev.langchain4j.community.model.zhipu.image.ImageModelName;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.anthropic.AnthropicChatModelName;
import dev.langchain4j.model.anthropic.AnthropicStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.model.openai.*;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * AI模型工厂
 *
 * @Author: chenrui
 * @Date: 2025/2/13 18:02
 */
public class AiModelFactory {


    public static final String AIMODEL_TYPE_OPENAI = "OPENAI";
    public static final String AIMODEL_TYPE_ZHIPU = "ZHIPU";
    public static final String AIMODEL_TYPE_QIANFAN = "QIANFAN";
    public static final String AIMODEL_TYPE_QWEN = "QWEN";
    public static final String AIMODEL_TYPE_DEEPSEEK = "DEEPSEEK";
    public static final String AIMODEL_TYPE_OLLAMA = "OLLAMA";
    public static final String AIMODEL_TYPE_ANTHROPIC = "ANTHROPIC";
    public static final String AIMODEL_TYPE_XINFERENCE = "XINFERENCE";
    public static final String AIMODEL_TYPE_VLLM = "VLLM";
    public static final String AIMODEL_TYPE_LMSTDIO = "LMSTDIO";
    public static final String AIMODEL_TYPE_GOOGLE = "GOOGLE";

    static {
        // ZhipuAI 等 community 模型的 builder 不支持显式传入 httpClientBuilder，走 SPI 自动发现。
        // 当 langchain4j-http-client-jdk 和 langchain4j-http-client-okhttp 同时在 classpath 时会冲突，
        // 通过系统属性指定优先使用 OkHttp（显式传入 httpClientBuilder 的模型不受此属性影响）。
        if (System.getProperty("langchain4j.http.clientBuilderFactory") == null) {
            System.setProperty("langchain4j.http.clientBuilderFactory",
                    "dev.langchain4j.http.client.okhttp.OkHttpClientBuilderFactory");
        }
    }

    /**
     * model缓存
     */
    private static final ConcurrentHashMap<String, Object> chatModelCache = new ConcurrentHashMap<>();

    private static Object getCache(String key) {
        return chatModelCache.get(key);
    }

    private static void setCache(String key, Object model) {
        chatModelCache.put(key, model);
    }

    /**
     * 创建聊天模型
     *
     * @param options
     * @return
     * @author chenrui
     * @date 2025/2/13 19:28
     */
    public static ChatModel createChatModel(AiModelOptions options) {
        assertNotEmpty("请设置模型参数", options);
        assertNotEmpty("请选择AI模型供应商", options.getProvider());
        String cacheKey = options.toString();
        Object cachedModel = getCache(cacheKey);
        if (cachedModel != null) {
            //update-begin---author:wangshuai---date:2026-03-30---for:【issues/9446】接入qwen3.5-plus出现问题---
            if (AIMODEL_TYPE_QWEN.equalsIgnoreCase(options.getProvider())) {
                Map<String, Object> ep = options.getExtraParams();
                if (ep != null && Boolean.TRUE.equals(ep.get("incremental_output"))) {
                    setQwenIncrementalOutput((QwenChatModel) cachedModel);
                }
            }
            //update-end---author:wangshuai---date:2026-03-30---for:【issues/9446】接入qwen3.5-plus出现问题---
            return (ChatModel) cachedModel;
        }
        String apiKey = options.getApiKey();
        String secretKey = options.getSecretKey();
        String baseUrl = options.getBaseUrl();
        String modelName = options.getModelName();
        double temperature = getDouble(options.getTemperature(), 0.7);
        double topP = getDouble(options.getTopP(), 1D);
        double presencePenalty = getDouble(options.getPresencePenalty(), 0D);
        double frequencyPenalty = getDouble(options.getFrequencyPenalty(), 0D);
        double repetitionPenalty = 1.0 + (presencePenalty + frequencyPenalty) / 2.0;
        int timeout = getInteger(options.getTimeout(),120);
        Integer maxTokens = options.getMaxTokens();
        ChatModel chatModel = null;
        switch (options.getProvider().toUpperCase()) {
            case AIMODEL_TYPE_OPENAI:
            case AIMODEL_TYPE_XINFERENCE:
            case AIMODEL_TYPE_VLLM:
            case AIMODEL_TYPE_LMSTDIO:
                if (AIMODEL_TYPE_OPENAI.equalsIgnoreCase(options.getProvider())) {
                    assertNotEmpty("apiKey不能为空", apiKey);
                }
                // 确保baseUrl以v1结尾
                baseUrl = ensureOpenAiUrlEnd(baseUrl);
                modelName = getString(modelName, OpenAiChatModelName.GPT_3_5_TURBO.toString());
                OpenAiChatModel.OpenAiChatModelBuilder openAIBuilder = OpenAiChatModel.builder()
                        .apiKey(apiKey)
                        .baseUrl(baseUrl)
                        .modelName(modelName)
                        // 温度 0-1 step 0.1
                        .temperature(temperature)
                        // 多样性  0-1 step 0.1
                        .topP(topP)
                        // 存在惩罚 0-1 step 0.1
                        .presencePenalty(presencePenalty)
                        // 频率惩罚 0-1 step 0.1
                        .frequencyPenalty(frequencyPenalty)
                        .timeout(Duration.ofSeconds(timeout))
                        .maxRetries(0);
                if (null != maxTokens) {
                    openAIBuilder.maxTokens(maxTokens);
                }
                openAIBuilder.httpClientBuilder(pickHttpClientBuilder(options, timeout));
                chatModel = openAIBuilder.build();
                break;
            case AIMODEL_TYPE_ZHIPU:
                assertNotEmpty("apiKey不能为空", apiKey);
                modelName = getString(modelName, ChatCompletionModel.GLM_4_FLASH.toString());
                ZhipuAiChatModel.ZhipuAiChatModelBuilder zhipuBuilder = ZhipuAiChatModel.builder()
                        .apiKey(apiKey)
                        .baseUrl(baseUrl)
                        .model(modelName)
                        // 温度 0-1 step 0.1
                        .temperature(temperature)
                        // 多样性  0-1 step 0.1
                        .topP(topP)
                        .maxRetries(0)
                        .readTimeout(Duration.ofSeconds(timeout))
                        .connectTimeout(Duration.ofSeconds(timeout));
                if (null != maxTokens) {
                    zhipuBuilder.maxToken(maxTokens);
                }
                chatModel = zhipuBuilder.build();
                break;
            case AIMODEL_TYPE_QIANFAN:
                assertNotEmpty("apiKey不能为空", apiKey);
                assertNotEmpty("secretKey不能为空", secretKey);
                modelName = getString(modelName, QianfanChatModelNameEnum.YI_34B_CHAT.getModelName());
                QianfanChatModel.QianfanChatModelBuilder qianfanBuilder = QianfanChatModel.builder()
                        .apiKey(apiKey)
                        .baseUrl(baseUrl)
                        .secretKey(secretKey)
                        .modelName(modelName)
                        // 温度 0-1 step 0.1
                        .temperature(temperature)
                        // 多样性  0-1 step 0.1
                        .topP(topP)
                        // 惩罚分数
                        .penaltyScore(repetitionPenalty)
                        .maxRetries(0);
                if (null != maxTokens) {
                    qianfanBuilder.maxOutputTokens(maxTokens);
                }
                chatModel = qianfanBuilder.build();
                break;
            case AIMODEL_TYPE_QWEN:
                assertNotEmpty("apiKey不能为空", apiKey);
                modelName = getString(modelName, QwenModelName.QWEN_PLUS);
                //update-begin---author:wangshuai---date:2026-03-30---for:【issues/9446】接入qwen3.5-plus出现问题---
                boolean hasImageUrl = options.getExtraParams() != null && options.getExtraParams().containsKey("image_url");
                boolean isMultiModal = modelName.contains("vl-") || modelName.contains("audio-") || modelName.contains("omni-") || hasImageUrl;
                //update-end---author:wangshuai---date:2026-03-30---for:【issues/9446】接入qwen3.5-plus出现问题---
                boolean enableSearch = getBool(options.getEnableSearch(), false);
                QwenChatModel.QwenChatModelBuilder qwenBuilder = QwenChatModel.builder()
                        .apiKey(apiKey)
                        .baseUrl(baseUrl)
                        .modelName(modelName)
                        // 温度 0-1 step 0.1
                        .temperature((float) temperature)
                        // 多样性  0-1 step 0.1
                        .topP(topP)
                        .enableSearch(enableSearch);
                if(!isMultiModal){
                    // 非多模态模型才支持设置重复惩罚
                    // 重复惩罚
                    qwenBuilder.repetitionPenalty((float) repetitionPenalty);
                }
                if (null != maxTokens) {
                    qwenBuilder.maxTokens(maxTokens);
                }
                Map<String, Object> extraParams = options.getExtraParams();
                // 先读取 incremental_output（在 buildQwenRequestParameters 之前，因为它内部会 remove 掉这个 key）
                Boolean incrementalOutput = extraParams != null ? (Boolean) extraParams.get("incremental_output") : null;
                if (null != extraParams && !extraParams.isEmpty()) {
                    qwenBuilder.defaultRequestParameters(buildQwenRequestParameters(extraParams));
                }
                chatModel = qwenBuilder.build();
                //update-begin---author:wangshuai---date:2026-03-30---for:【issues/9446】接入qwen3.5-plus出现问题---
                if (Boolean.TRUE.equals(incrementalOutput)) {
                    setQwenIncrementalOutput((QwenChatModel) chatModel);
                }
                //update-end---author:wangshuai---date:2026-03-30---for:【issues/9446】接入qwen3.5-plus出现问题---
                break;
            case AIMODEL_TYPE_OLLAMA:
                assertNotEmpty("baseUrl不能为空", baseUrl);
                assertNotEmpty("请选择模型", modelName);
                chatModel = OllamaChatModel.builder()
                        .baseUrl(baseUrl)
                        .modelName(modelName)
                        // 温度 0-1 step 0.1
                        .temperature(temperature)
                        // 多样性  0-1 step 0.1
                        .topP(topP)
                        // 重复惩罚
                        .repeatPenalty(repetitionPenalty)
                        .timeout(Duration.ofSeconds(timeout))
                        .maxRetries(3)
                        .build();
                break;
            case AIMODEL_TYPE_DEEPSEEK:
                assertNotEmpty("apiKey不能为空", apiKey);
                baseUrl = getString(baseUrl, "https://api.deepseek.com/v1");
                // 确保baseUrl以v1结尾
                baseUrl = ensureOpenAiUrlEnd(baseUrl);
                modelName = getString(modelName, "deepseek-v4-flash");
                OpenAiChatModel.OpenAiChatModelBuilder dsBuilder = OpenAiChatModel.builder()
                        .apiKey(apiKey)
                        .baseUrl(baseUrl)
                        .modelName(modelName)
                        // 温度 0-1 step 0.1
                        .temperature(temperature)
                        // 多样性  0-1 step 0.1
                        .topP(topP)
                        // 存在惩罚 0-1 step 0.1
                        .presencePenalty(presencePenalty)
                        // 频率惩罚 0-1 step 0.1
                        .frequencyPenalty(frequencyPenalty)
                        .timeout(Duration.ofSeconds(timeout));
                if (null != maxTokens) {
                    dsBuilder.maxTokens(maxTokens);
                }
                if (null != options.getReturnThinking()) {
                    dsBuilder.returnThinking(options.getReturnThinking());
                }
                //update-begin---author:scott ---date:20260429  for：[issues/9585]DeepSeek大模型切换为新发布deepseek-v4-flash，流程中调用出现异常------------
                // sendThinking: 把上一轮 AiMessage.thinking 以 reasoning_content 字段回传到下次请求，
                // DeepSeek 推理模型(deepseek-reasoner/deepseek-v4-flash 等)在工具调用多轮对话中必须开启，
                // 是否启用由调用方按模型类型决定，starter 仅做透传不做模型判断，避免影响 deepseek-chat 等非推理模型
                if (null != options.getSendThinking()) {
                    dsBuilder.sendThinking(options.getSendThinking());
                }
                //update-end---author:scott ---date:20260429  for：[issues/9585]DeepSeek大模型切换为新发布deepseek-v4-flash，流程中调用出现异常------------
                dsBuilder.httpClientBuilder(pickHttpClientBuilder(options, timeout));
                // 处理并传递 DeepSeek 的额外参数
                applyDeepSeekExtraParams(options.getExtraParams(), dsBuilder::reasoningEffort, dsBuilder::customParameters);
                chatModel = dsBuilder.build();
                break;
            case AIMODEL_TYPE_ANTHROPIC:
                assertNotEmpty("apiKey不能为空", apiKey);
                baseUrl = getString(baseUrl, "https://api.anthropic.com/v1");
                modelName = getString(modelName, AnthropicChatModelName.CLAUDE_SONNET_4_5_20250929.toString());
                AnthropicChatModel.AnthropicChatModelBuilder anthropicBuilder = AnthropicChatModel.builder()
                        .apiKey(apiKey)
                        .baseUrl(baseUrl)
                        .modelName(modelName)
                        // 温度 0-1 step 0.1
                        .temperature(temperature)
                        // 多样性  0-1 step 0.1
                        .topP(topP)
                        .timeout(Duration.ofSeconds(timeout))
                        .maxRetries(0);
                if (null != maxTokens) {
                    anthropicBuilder.maxTokens(maxTokens);
                }
                chatModel = anthropicBuilder.build();
                break;
            case AIMODEL_TYPE_GOOGLE:
                chatModel = GoogleModelBuilder.createChatModel(apiKey, baseUrl, modelName,
                        temperature, topP, presencePenalty, frequencyPenalty, timeout, maxTokens);
                break;
        }
        setCache(cacheKey, chatModel);
        return chatModel;
    }

    /**
     * 创建流式聊天模型
     *
     * @param options
     * @return
     * @author chenrui
     * @date 2025/2/20 19:41
     */
    public static StreamingChatModel createStreamingChatModel(AiModelOptions options) {
        assertNotEmpty("请设置模型参数", options);
        assertNotEmpty("请选择AI模型供应商", options.getProvider());
        String cacheKey = "STEAM_" + options.toString();
        Object cachedModel = getCache(cacheKey);
        if (cachedModel != null) {
            return (StreamingChatModel) cachedModel;
        }
        String apiKey = options.getApiKey();
        String secretKey = options.getSecretKey();
        String baseUrl = options.getBaseUrl();
        String modelName = options.getModelName();
        double temperature = getDouble(options.getTemperature(), 0.7);
        double topP = getDouble(options.getTopP(), 1D);
        double presencePenalty = getDouble(options.getPresencePenalty(), 0D);
        double frequencyPenalty = getDouble(options.getFrequencyPenalty(), 0D);
        double repetitionPenalty = 1.0 + (presencePenalty + frequencyPenalty) / 2.0;
        int timeout = getInteger(options.getTimeout(),120);
        Integer maxTokens = options.getMaxTokens();
        StreamingChatModel chatModel = null;
        switch (options.getProvider().toUpperCase()) {
            case AIMODEL_TYPE_OPENAI:
            case AIMODEL_TYPE_XINFERENCE:
            case AIMODEL_TYPE_VLLM:
            case AIMODEL_TYPE_LMSTDIO:
                if (AIMODEL_TYPE_OPENAI.equalsIgnoreCase(options.getProvider())) {
                    assertNotEmpty("apiKey不能为空", apiKey);
                }
                // 确保baseUrl以v1结尾
                baseUrl = ensureOpenAiUrlEnd(baseUrl);
                modelName = getString(modelName, OpenAiChatModelName.GPT_3_5_TURBO.toString());
                OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder openAIBuilder = OpenAiStreamingChatModel.builder()
                        .apiKey(apiKey)
                        .baseUrl(baseUrl)
                        .modelName(modelName)
                        // 温度 0-1 step 0.1
                        .temperature(temperature)
                        // 多样性  0-1 step 0.1
                        .topP(topP)
                        // 存在惩罚 0-1 step 0.1
                        .presencePenalty(presencePenalty)
                        // 频率惩罚 0-1 step 0.1
                        .frequencyPenalty(frequencyPenalty)
                        .timeout(Duration.ofSeconds(timeout));
                if (null != maxTokens) {
                    openAIBuilder.maxTokens(maxTokens);
                }
                openAIBuilder.httpClientBuilder(pickHttpClientBuilder(options, timeout));
                chatModel = openAIBuilder.build();
                break;
            case AIMODEL_TYPE_ZHIPU:
                assertNotEmpty("apiKey不能为空", apiKey);
                modelName = getString(modelName, ChatCompletionModel.GLM_4_FLASH.toString());
                ZhipuAiStreamingChatModel.ZhipuAiStreamingChatModelBuilder zhipuBuilder = ZhipuAiStreamingChatModel.builder()
                        .apiKey(apiKey)
                        .baseUrl(baseUrl)
                        .model(modelName)
                        // 温度 0-1 step 0.1
                        .temperature(temperature)
                        // 多样性  0-1 step 0.1
                        .topP(topP)
                        .readTimeout(Duration.ofSeconds(timeout))
                        .connectTimeout(Duration.ofSeconds(timeout));
                if (null != maxTokens) {
                    zhipuBuilder.maxToken(maxTokens);
                }
                chatModel = zhipuBuilder.build();
                break;
            case AIMODEL_TYPE_QIANFAN:
                assertNotEmpty("apiKey不能为空", apiKey);
                assertNotEmpty("secretKey不能为空", secretKey);
                modelName = getString(modelName, QianfanChatModelNameEnum.YI_34B_CHAT.getModelName());
                chatModel = QianfanStreamingChatModel.builder()
                        .apiKey(apiKey)
                        .baseUrl(baseUrl)
                        .secretKey(secretKey)
                        .modelName(modelName)
                        // 温度 0-1 step 0.1
                        .temperature(temperature)
                        // 多样性  0-1 step 0.1
                        .topP(topP)
                        // 惩罚分数
                        .penaltyScore(repetitionPenalty)
                        .build();
                break;
            case AIMODEL_TYPE_QWEN:
                assertNotEmpty("apiKey不能为空", apiKey);
                modelName = getString(modelName, QwenModelName.QWEN_PLUS);
                boolean hasImageUrlStream = options.getExtraParams() != null && options.getExtraParams().containsKey("image_url");
                boolean isMultiModalStream = modelName.contains("-vl-") || modelName.contains("-audio-") || modelName.contains("-omni-") || hasImageUrlStream;
                Boolean enableSearch = getBool(options.getEnableSearch(), false);
                QwenStreamingChatModel.QwenStreamingChatModelBuilder qwenBuilder = QwenStreamingChatModel.builder()
                        .apiKey(apiKey)
                        .baseUrl(baseUrl)
                        .modelName(modelName)
                        // 温度 0-1 step 0.1
                        .temperature((float) temperature)
                        // 多样性  0-1 step 0.1
                        .topP(topP)
                        // 启用联网搜索
                        .enableSearch(enableSearch);
                if(!isMultiModalStream){
                    // 非多模态模型才支持设置重复惩罚
                    // 重复惩罚
                    qwenBuilder.repetitionPenalty((float) repetitionPenalty);
                }
                if (null != maxTokens) {
                    qwenBuilder.maxTokens(maxTokens);
                }
                Map<String, Object> extraParams = options.getExtraParams();
                if (null != extraParams && !extraParams.isEmpty()) {
                    qwenBuilder.defaultRequestParameters(buildQwenRequestParameters(extraParams));
                }
                chatModel = qwenBuilder.build();
                break;
            case AIMODEL_TYPE_OLLAMA:
                assertNotEmpty("baseUrl不能为空", baseUrl);
                assertNotEmpty("请选择模型", modelName);
                chatModel = OllamaStreamingChatModel.builder()
                        .baseUrl(baseUrl)
                        .modelName(modelName)
                        // 温度 0-1 step 0.1
                        .temperature(temperature)
                        // 多样性  0-1 step 0.1
                        .topP(topP)
                        // 重复惩罚
                        .repeatPenalty(repetitionPenalty)
                        .timeout(Duration.ofSeconds(timeout))
                        .build();
                break;
            case AIMODEL_TYPE_DEEPSEEK:
                assertNotEmpty("apiKey不能为空", apiKey);
                baseUrl = getString(baseUrl, "https://api.deepseek.com/v1");
                // 确保baseUrl以v1结尾
                baseUrl = ensureOpenAiUrlEnd(baseUrl);
                modelName = getString(modelName, "deepseek-v4-flash");
                OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder dsBuilder = OpenAiStreamingChatModel.builder()
                        .apiKey(apiKey)
                        .baseUrl(baseUrl)
                        .modelName(modelName)
                        // 温度 0-1 step 0.1
                        .temperature(temperature)
                        // 多样性  0-1 step 0.1
                        .topP(topP)
                        // 存在惩罚 0-1 step 0.1
                        .presencePenalty(presencePenalty)
                        // 频率惩罚 0-1 step 0.1
                        .frequencyPenalty(frequencyPenalty)
                        .timeout(Duration.ofSeconds(timeout));
                if (null != maxTokens) {
                    dsBuilder.maxTokens(maxTokens);
                }
                if (null != options.getReturnThinking()) {
                    dsBuilder.returnThinking(options.getReturnThinking());
                }
                //update-begin---author:scott ---date:20260429  for：[issues/9585]DeepSeek大模型切换为新发布deepseek-v4-flash，流程中调用出现异常------------
                // sendThinking: 把上一轮 AiMessage.thinking 以 reasoning_content 字段回传到下次请求，
                // DeepSeek 推理模型(deepseek-reasoner/deepseek-v4-flash 等)在工具调用多轮对话中必须开启，
                // 是否启用由调用方按模型类型决定，starter 仅做透传不做模型判断，避免影响 deepseek-chat 等非推理模型
                if (null != options.getSendThinking()) {
                    dsBuilder.sendThinking(options.getSendThinking());
                }
                //update-end---author:scott ---date:20260429  for：[issues/9585]DeepSeek大模型切换为新发布deepseek-v4-flash，流程中调用出现异常------------
                dsBuilder.httpClientBuilder(pickHttpClientBuilder(options, timeout));
                // 处理并传递 DeepSeek 的额外参数
                applyDeepSeekExtraParams(options.getExtraParams(), dsBuilder::reasoningEffort, dsBuilder::customParameters);
                chatModel = dsBuilder.build();
                break;
            case AIMODEL_TYPE_ANTHROPIC:
                assertNotEmpty("apiKey不能为空", apiKey);
                baseUrl = getString(baseUrl, "https://api.anthropic.com/v1");
                modelName = getString(modelName, AnthropicChatModelName.CLAUDE_SONNET_4_5_20250929.toString());
                AnthropicStreamingChatModel.AnthropicStreamingChatModelBuilder anthropicStreamBuilder = AnthropicStreamingChatModel.builder()
                        .apiKey(apiKey)
                        .baseUrl(baseUrl)
                        .modelName(modelName)
                        // 温度 0-1 step 0.1
                        .temperature(temperature)
                        // 多样性  0-1 step 0.1
                        .topP(topP)
                        .timeout(Duration.ofSeconds(timeout));
                if (null != maxTokens) {
                    anthropicStreamBuilder.maxTokens(maxTokens);
                }
                chatModel = anthropicStreamBuilder.build();
                break;
            case AIMODEL_TYPE_GOOGLE:
                chatModel = GoogleModelBuilder.createStreamingChatModel(apiKey, baseUrl, modelName,
                        temperature, topP, presencePenalty, frequencyPenalty, timeout, maxTokens);
                break;
        }
        setCache(cacheKey, chatModel);
        return chatModel;
    }


    /**
     * 创建向量模型
     *
     * @param options
     * @return
     * @author chenrui
     * @date 2025/2/18 17:32
     */
    public static EmbeddingModel createEmbeddingModel(AiModelOptions options) {
        assertNotEmpty("请设置模型参数", options);
        assertNotEmpty("请选择AI模型供应商", options.getProvider());
        String cacheKey = options.toString();
        Object cachedModel = getCache(cacheKey);
        if (cachedModel != null) {
            return (EmbeddingModel) cachedModel;
        }
        String apiKey = options.getApiKey();
        String secretKey = options.getSecretKey();
        String baseUrl = options.getBaseUrl();
        String modelName = options.getModelName();
        int timeout = getInteger(options.getTimeout(),120);
        EmbeddingModel embeddingModel;
        switch (options.getProvider().toUpperCase()) {
            case AIMODEL_TYPE_OPENAI:
            case AIMODEL_TYPE_XINFERENCE:
            case AIMODEL_TYPE_VLLM:
            case AIMODEL_TYPE_LMSTDIO:
                if (AIMODEL_TYPE_OPENAI.equalsIgnoreCase(options.getProvider())) {
                    assertNotEmpty("apiKey不能为空", apiKey);
                }
                // 确保baseUrl以v1结尾
                baseUrl = ensureOpenAiUrlEnd(baseUrl);
                modelName = getString(modelName, OpenAiEmbeddingModelName.TEXT_EMBEDDING_ADA_002.toString());
                OpenAiEmbeddingModel.OpenAiEmbeddingModelBuilder openAiModal = OpenAiEmbeddingModel.builder()
                        .apiKey(apiKey)
                        .baseUrl(baseUrl)
                        .modelName(modelName)
                        .timeout(Duration.ofSeconds(timeout))
                        .maxRetries(0);
                openAiModal.httpClientBuilder(pickHttpClientBuilder(options, timeout));
                embeddingModel = openAiModal.build();
                break;
            case AIMODEL_TYPE_ZHIPU:
                assertNotEmpty("apiKey不能为空", apiKey);
                modelName = getString(modelName, dev.langchain4j.community.model.zhipu.embedding.EmbeddingModel.EMBEDDING_2.toString());
                embeddingModel = ZhipuAiEmbeddingModel.builder()
                        .apiKey(apiKey)
                        .baseUrl(baseUrl)
                        .model(modelName)
                        .readTimeout(Duration.ofSeconds(timeout))
                        .connectTimeout(Duration.ofSeconds(timeout))
                        // TODO author: chenrui for:临时写死,PGV不支持超过2000的向量索引date:2025/3/7
                        .dimensions(1536)
                        .maxRetries(0)
                        .build();
                break;
            case AIMODEL_TYPE_QIANFAN:
                assertNotEmpty("apiKey不能为空", apiKey);
                assertNotEmpty("secretKey不能为空", secretKey);
                modelName = getString(modelName, QianfanEmbeddingModelNameEnum.EMBEDDING_V1.getModelName());
                embeddingModel = QianfanEmbeddingModel.builder()
                        .apiKey(apiKey)
                        .baseUrl(baseUrl)
                        .secretKey(secretKey)
                        .modelName(modelName)
                        .maxRetries(0)
                        .build();
                break;
            case AIMODEL_TYPE_QWEN:
                assertNotEmpty("apiKey不能为空", apiKey);
                modelName = getString(modelName, QwenModelName.TEXT_EMBEDDING_V2);
                embeddingModel = QwenEmbeddingModel.builder()
                        .apiKey(apiKey)
                        .baseUrl(baseUrl)
                        .modelName(modelName)
                        .build();
                break;
            case AIMODEL_TYPE_OLLAMA:
                assertNotEmpty("baseUrl不能为空", baseUrl);
                assertNotEmpty("请选择模型", modelName);
                embeddingModel = OllamaEmbeddingModel.builder()
                        .baseUrl(baseUrl)
                        .modelName(modelName)
                        .maxRetries(3)
                        .timeout(Duration.ofSeconds(timeout))
                        .maxRetries(0)
                        .build();
                break;
            default:
                throw new RuntimeException("不支持的模型");
        }
        setCache(cacheKey, embeddingModel);
        return embeddingModel;
    }

    /**
     * 创建图片生成模型
     *
     * @param options
     * @return
     * @author wangshuai
     * @date 2026/01/04 19:28
     */
    public static ImageModel createImageModel(AiModelOptions options) {
        assertNotEmpty("请设置模型参数", options);
        assertNotEmpty("请选择AI模型供应商", options.getProvider());
        String cacheKey = "IMAGE_" + options.toString();
        Object cachedModel = getCache(cacheKey);
        if (cachedModel != null) {
            return (ImageModel) cachedModel;
        }
        String apiKey = options.getApiKey();
        String baseUrl = options.getBaseUrl();
        String modelName = options.getModelName();
        int timeout = getInteger(options.getTimeout(), 120);

        ImageModel imageModel = null;
        switch (options.getProvider().toUpperCase()) {
            case AIMODEL_TYPE_OPENAI:
            case AIMODEL_TYPE_XINFERENCE:
            case AIMODEL_TYPE_VLLM:
            case AIMODEL_TYPE_LMSTDIO:
                if (AIMODEL_TYPE_OPENAI.equalsIgnoreCase(options.getProvider())) {
                    assertNotEmpty("apiKey不能为空", apiKey);
                }
                baseUrl = ensureOpenAiUrlEnd(baseUrl);
                modelName = getString(modelName, OpenAiImageModelName.DALL_E_3.toString());
                OpenAiImageModel.OpenAiImageModelBuilder builder = OpenAiImageModel.builder()
                        .apiKey(apiKey)
                        .baseUrl(baseUrl)
                        .modelName(modelName)
                        .timeout(Duration.ofSeconds(timeout))
                        .maxRetries(0)
                        .logRequests(true)
                        .logResponses(true);
                if(StringUtils.isNotEmpty(options.getImageSize()) && ("dall-e-2".equals(options.getModelName()) || "dall-e-3".equals(options.getModelName()) || AIMODEL_TYPE_XINFERENCE.equalsIgnoreCase(options.getProvider()) || AIMODEL_TYPE_VLLM.equalsIgnoreCase(options.getProvider()) || AIMODEL_TYPE_LMSTDIO.equalsIgnoreCase(options.getProvider()))){
                    builder.size(options.getImageSize());
                }
                builder.httpClientBuilder(pickHttpClientBuilder(options, timeout));
                imageModel = builder.build();
                break;
            case AIMODEL_TYPE_ZHIPU:
                assertNotEmpty("apiKey不能为空", apiKey);
                modelName = getString(modelName, ImageModelName.COGVIEW_3.toString());
                CustomZhipuAiImageModel.CustomZhipuAiImageModelBuilder imageModelBuilder = CustomZhipuAiImageModel.builder()
                        .apiKey(apiKey)
                        .baseUrl(baseUrl)
                        .model(modelName)
                        .readTimeout(Duration.ofSeconds(timeout))
                        .connectTimeout(Duration.ofSeconds(timeout))
                        .maxRetries(0)
                        .logRequests(true)
                        .logResponses(true)
                        .watermarkEnabled(false)
                        .size(options.getImageSize());
                if(StringUtils.isNotEmpty(options.getImageSize())){
                    imageModelBuilder.size(options.getImageSize());
                }
                imageModel = imageModelBuilder.build();
                break;
            case AIMODEL_TYPE_QWEN:
                assertNotEmpty("apiKey不能为空", apiKey);
                modelName = getString(modelName, "wanx-v1");
                WanxImageModel.WanxImageModelBuilder wanxBuilder = WanxImageModel.builder()
                        .apiKey(apiKey)
                        .watermark(false)
                        .baseUrl(baseUrl)
                        .modelName(modelName);
                if(StringUtils.isNotEmpty(options.getImageSize())){
                    wanxBuilder.size(WanxImageSize.of(options.getImageSize()));
                }
                imageModel = wanxBuilder.build();
                break;
            case AIMODEL_TYPE_GOOGLE:
                imageModel = GoogleModelBuilder.createImageModel(options, apiKey, baseUrl, modelName, timeout);
                break;
            default:
                throw new RuntimeException("不支持的模型");
        }
        setCache(cacheKey, imageModel);
        return imageModel;
    }

    /**
     * 确保对象不为空,如果为空抛出异常
     *
     * @param msg
     * @param obj
     * @author chenrui
     * @date 2017-06-22 10:05:56
     */
    public static void assertNotEmpty(String msg, Object obj) {
        if (isObjectEmpty(obj)) {
            throw new RuntimeException(msg);
        }
    }

    /**
     * 判断对象是否为空 <br/>
     * 支持各种类型的对象
     * for for [QQYUN-10990]AIRAG
     *
     * @param obj
     * @return
     * @author chenrui
     * @date 2025/2/13 18:34
     */
    public static boolean isObjectEmpty(Object obj) {
        if (null == obj) {
            return true;
        }

        if (obj instanceof CharSequence) {
            return isEmpty(obj);
        } else if (obj instanceof Map) {
            return ((Map<?, ?>) obj).isEmpty();
        } else if (obj instanceof Iterable) {
            return isObjectEmpty(((Iterable<?>) obj).iterator());
        } else if (obj instanceof Iterator) {
            return !((Iterator<?>) obj).hasNext();
        } else if (isArray(obj)) {
            return 0 == Array.getLength(obj);
        }
        return false;
    }



    /**
     * 构建Qwen自定义请求参数
     *
     * @param extraParams 自定义参数
     * @return QwenChatRequestParameters
     */
    private static QwenChatRequestParameters buildQwenRequestParameters(Map<String, Object> extraParams) {
        if (extraParams == null) {
            extraParams = new LinkedHashMap<>();
        }
        QwenChatRequestParameters.Builder builder = QwenChatRequestParameters.builder();

        // 提取Qwen SDK专用参数
        Boolean enableThinking = removeBool(extraParams, "enable_thinking");
        if (enableThinking != null) {
            builder.enableThinking(enableThinking);
        }
        //update-begin---author:wangshuai---date:2026-03-30---for:【issues/9446】接入qwen3.5-plus出现问题---
        // extraParams中包含image_url时，标记为多模态模型，使langchain4j走多模态路径
        if (extraParams.containsKey("image_url")) {
            builder.isMultimodalModel(true);
        }
        // 多模态模型（如qwen3.5-plus）要求incremental_output=true，否则API会报错
        Boolean incrementalOutput = removeBool(extraParams, "incremental_output");
        if (Boolean.TRUE.equals(incrementalOutput)) {
            builder.supportIncrementalOutput(true);
        }
        //update-end---author:wangshuai---date:2026-03-30---for:【issues/9446】接入qwen3.5-plus出现问题---
        return builder.build();
    }

    /**
     * 设置QwenChatModel强制incrementalOutput=true。
     * langchain4j-dashscope在非流式调用中硬编码了incrementalOutput=false，
     * 但DashScope API部分模型（如qwen-vl-ocr、qwen3.5-plus等）要求该参数必须为true。
     */
    private static void setQwenIncrementalOutput(QwenChatModel model) {
        model.setGenerationParamCustomizer(builder -> builder.incrementalOutput(true));
        model.setMultimodalConversationParamCustomizer(builder -> builder.incrementalOutput(true));
    }

    /**
     * 从Map中移除并转换为Boolean
     */
    private static Boolean removeBool(Map<String, Object> map, String key) {
        Object val = map.remove(key);
        if (val instanceof Boolean) {
            return (Boolean) val;
        } else if (val instanceof String) {
            return Boolean.parseBoolean((String) val);
        }
        return null;
    }

    /**
     * 将 extraParams 透传到 DeepSeek（OpenAI 兼容协议）的 builder：
     * reasoning_effort 走 builder 标准方法，其余键（如 thinking）经 customParameters 进请求体顶层。
     * 非流式/流式 builder 没有公共父类，通过传 setter 方法引用解耦。
     *
     * @author sjlei
     * @date 2026-5-14
     */
    private static void applyDeepSeekExtraParams(
            Map<String, Object> extraParams,
            Consumer<String> reasoningEffortSetter,
            Consumer<Map<String, Object>> customParametersSetter
    ) {
        if (extraParams == null || extraParams.isEmpty()) {
            return;
        }
        Map<String, Object> copy = new LinkedHashMap<>(extraParams);
        Object effort = copy.remove("reasoning_effort");
        if (effort instanceof String) {
            reasoningEffortSetter.accept((String) effort);
        }
        if (!copy.isEmpty()) {
            customParametersSetter.accept(copy);
        }
    }

    /**
     * 确保openai的url以v1结尾
     *
     * @param baseUrl
     * @return
     * @author chenrui
     * @date 2025/3/12 20:44
     */
    @Nullable
    public static String ensureOpenAiUrlEnd(String baseUrl) {
        if (StringUtils.isNotEmpty(baseUrl)) {
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }
            //update-begin---author:jeecg-dev ---date:2026-05-15  for：【issues/9635】智谱等 baseUrl 以 /v4、/v3 结尾被错误追加 /v1 导致 404-----------
            // 仅当 baseUrl 末尾没有 /v数字 版本段时才补 /v1
            // 例：https://open.bigmodel.cn/api/paas/v4 -> 保持原样（之前会被改成 .../v4/v1）
            //     https://api.deepseek.com            -> 补成 .../v1
            if (!baseUrl.matches(".*/v\\d+$")) {
                baseUrl = baseUrl + "/v1";
            }
            //update-end---author:jeecg-dev ---date:2026-05-15  for：【issues/9635】智谱等 baseUrl 以 /v4、/v3 结尾被错误追加 /v1 导致 404-----------
        }
        return baseUrl;
    }

    /**
     * 方法描述 判断一个对象是否是一个数组
     *
     * @param obj
     * @return
     * @author yaomy
     * @date 2018年2月5日 下午5:03:00
     */
    public static boolean isArray(Object obj) {
        if (obj == null) {
            return false;
        }
        return obj.getClass().isArray();
    }

    public static boolean isEmpty(Object object) {
        if (object == null) {
            return (true);
        }
        if ("".equals(object)) {
            return (true);
        }
        if ("null".equals(object)) {
            return (true);
        }
        return (false);
    }

    public static String getString(CharSequence str, String defaultStr) {
        return isEmpty(str) ? defaultStr : str.toString();
    }

    public static Integer getInteger(Integer object, Integer defval) {
        if (object == null) {
            return (defval);
        }
        try {
            return (Integer.parseInt(object.toString()));
        } catch (NumberFormatException e) {
            return (defval);
        }
    }

    public static Double getDouble(Double object, Double defval) {
        if (object == null) {
            return (defval);
        }
        try {
            return (Double.parseDouble(object.toString()));
        } catch (NumberFormatException e) {
            return (defval);
        }
    }

    public static Boolean getBool(Boolean object, Boolean defval) {
        if (object == null) {
            return (defval);
        }
        try {
            return (Boolean.parseBoolean(object.toString()));
        } catch (NumberFormatException e) {
            return (defval);
        }
    }

    /**
     * 按 {@code options.izHttpVersionOne} 选择 HTTP 客户端 Builder：
     * <ul>
     *   <li>{@code true}（老配置兼容）：JDK HttpClient 强制 HTTP/1.1</li>
     *   <li>{@code null} 或 {@code false}（默认）：OkHttp + HTTP/2 PING 心跳,自动剔除闲置死连接</li>
     * </ul>
     *
     * @param options        模型选项
     * @param timeoutSeconds connect / read 超时秒数
     * @return 配置好的 HttpClientBuilder
     * @author sjlei
     * @date 2026-05-19
     */
    private static HttpClientBuilder pickHttpClientBuilder(AiModelOptions options, int timeoutSeconds) {
        //Http是否为1.1版本
        if (null != options.izHttpVersionOne && options.izHttpVersionOne) {
            return buildJdkHttp1ClientBuilder();
        }
        return buildOkHttpClientBuilder(timeoutSeconds);
    }

    /**
     * 构造强制 HTTP/1.1 的 JDK HttpClient Builder（langchain4j 适配器）,用于 izHttpVersionOne=true 老配置兼容。
     *
     * @return 配置好的 JdkHttpClientBuilder
     * @author sjlei
     * @date 2026-05-19
     */
    private static JdkHttpClientBuilder buildJdkHttp1ClientBuilder() {
        HttpClient.Builder httpClientBuilder = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1);
        return JdkHttpClient.builder()
                .httpClientBuilder(httpClientBuilder);
    }

    /**
     * 构造统一配置的 OkHttp HTTP 客户端 Builder（langchain4j 适配器）,供所有 LLM provider 共用。
     * <p>
     * 启用：
     * <ul>
     *   <li>{@code pingInterval(30s)}：HTTP/2 PING 帧心跳,服务端 idle timeout RST 死连接可在客户端复用前被剔除</li>
     *   <li>{@code retryOnConnectionFailure(true)}：OkHttp 对 idempotent 请求自动重试一次</li>
     *   <li>{@code connectionPool(5, 2 min)}：连接池 idle 上限 2 分钟,比主流网关 idle timeout (60-300s) 短</li>
     * </ul>
     *
     * @param timeoutSeconds connect / read 超时秒数,沿用调用方的 timeout 入参
     * @return 配置好的 OkHttpClientBuilder,可直接传入 langchain4j 模型的 httpClientBuilder()
     * @author sjlei
     * @date 2026-05-19
     */
    private static OkHttpClientBuilder buildOkHttpClientBuilder(int timeoutSeconds) {
        OkHttpClient.Builder okBuilder = new OkHttpClient.Builder()
                .pingInterval(Duration.ofSeconds(30))
                .retryOnConnectionFailure(true)
                .connectionPool(new ConnectionPool(5, 2, TimeUnit.MINUTES));
        return new OkHttpClientBuilder()
                .okHttpClientBuilder(okBuilder)
                .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                .readTimeout(Duration.ofSeconds(timeoutSeconds));
    }
}

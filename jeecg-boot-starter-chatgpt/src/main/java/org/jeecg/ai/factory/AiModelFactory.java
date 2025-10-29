package org.jeecg.ai.factory;

import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.community.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.community.model.dashscope.QwenModelName;
import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.community.model.qianfan.*;
import dev.langchain4j.community.model.zhipu.ZhipuAiChatModel;
import dev.langchain4j.community.model.zhipu.ZhipuAiEmbeddingModel;
import dev.langchain4j.community.model.zhipu.ZhipuAiStreamingChatModel;
import dev.langchain4j.community.model.zhipu.chat.ChatCompletionModel;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.anthropic.AnthropicChatModelName;
import dev.langchain4j.model.anthropic.AnthropicStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.model.openai.*;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
                assertNotEmpty("apiKey不能为空", apiKey);
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
                boolean isMultiModal = modelName.contains("vl-") || modelName.contains("audio-") || modelName.contains("omni-");
                QwenChatModel.QwenChatModelBuilder qwenBuilder = QwenChatModel.builder()
                        .apiKey(apiKey)
                        .baseUrl(baseUrl)
                        .modelName(modelName)
                        // 温度 0-1 step 0.1
                        .temperature((float) temperature)
                        // 多样性  0-1 step 0.1
                        .topP(topP);

                if(!(modelName.contains("-vl-") || modelName.contains("-audio-") || modelName.contains("-omni-"))){
                    // 非多模态模型才支持设置重复惩罚
                    // 重复惩罚
                    qwenBuilder.repetitionPenalty((float) repetitionPenalty);
                }
                if (null != maxTokens) {
                    qwenBuilder.maxTokens(maxTokens);
                }
                chatModel = qwenBuilder.build();
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
                modelName = getString(modelName, "deepseek-chat");
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
                chatModel = dsBuilder.build();
                break;
            case AIMODEL_TYPE_ANTHROPIC:
                assertNotEmpty("apiKey不能为空", apiKey);
                baseUrl = getString(baseUrl, "https://api.anthropic.com/v1");
                modelName = getString(modelName, AnthropicChatModelName.CLAUDE_3_5_SONNET_20241022.toString());
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
                assertNotEmpty("apiKey不能为空", apiKey);
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
                QwenStreamingChatModel.QwenStreamingChatModelBuilder qwenBuilder = QwenStreamingChatModel.builder()
                        .apiKey(apiKey)
                        .baseUrl(baseUrl)
                        .modelName(modelName)
                        // 温度 0-1 step 0.1
                        .temperature((float) temperature)
                        // 多样性  0-1 step 0.1
                        .topP(topP);
                if(!(modelName.contains("-vl-") || modelName.contains("-audio-") || modelName.contains("-omni-"))){
                    // 非多模态模型才支持设置重复惩罚
                    // 重复惩罚
                    qwenBuilder.repetitionPenalty((float) repetitionPenalty);
                }
                if (null != maxTokens) {
                    qwenBuilder.maxTokens(maxTokens);
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
                modelName = getString(modelName, "deepseek-chat");
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
                chatModel = dsBuilder.build();
                break;
            case AIMODEL_TYPE_ANTHROPIC:
                assertNotEmpty("apiKey不能为空", apiKey);
                baseUrl = getString(baseUrl, "https://api.anthropic.com/v1");
                modelName = getString(modelName, AnthropicChatModelName.CLAUDE_3_5_SONNET_20241022.toString());
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
                assertNotEmpty("apiKey不能为空", apiKey);
                // 确保baseUrl以v1结尾
                baseUrl = ensureOpenAiUrlEnd(baseUrl);
                modelName = getString(modelName, OpenAiEmbeddingModelName.TEXT_EMBEDDING_ADA_002.toString());
                embeddingModel = OpenAiEmbeddingModel.builder()
                        .apiKey(apiKey)
                        .baseUrl(baseUrl)
                        .modelName(modelName)
                        .timeout(Duration.ofSeconds(timeout))
                        .maxRetries(0)
                        .build();
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
     * 确保openai的url以v1结尾
     *
     * @param baseUrl
     * @return
     * @author chenrui
     * @date 2025/3/12 20:44
     */
    @Nullable
    private static String ensureOpenAiUrlEnd(String baseUrl) {
        if (StringUtils.isNotEmpty(baseUrl)) {
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }
            if (!baseUrl.endsWith("v1")) {
                baseUrl = baseUrl + (baseUrl.endsWith("/") ? "" : "/") + "v1";
            }
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
}

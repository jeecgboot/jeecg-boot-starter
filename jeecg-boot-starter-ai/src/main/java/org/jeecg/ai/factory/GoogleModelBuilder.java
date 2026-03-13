package org.jeecg.ai.factory;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiImageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiStreamingChatModel;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.openai.*;
import org.apache.commons.lang.StringUtils;
import org.jeecg.ai.enums.ImageSizeEnum;

import java.time.Duration;

import static org.jeecg.ai.factory.AiModelFactory.*;

/**
 * 谷歌模型构建器
 * 支持原生Google API和OpenAI兼容协议(自定义baseUrl时)
 *
 * @Author: scott
 * @Date: 2026/3/11
 */
public class GoogleModelBuilder {

    /**
     * 判断是否使用OpenAI兼容协议
     * 当baseUrl不为空且不是Google原生API地址时，使用OpenAI兼容协议
     */
    private static boolean isOpenAiCompatible(String baseUrl) {
        return StringUtils.isNotEmpty(baseUrl) && !baseUrl.contains("googleapis.com");
    }

    public static ChatModel createChatModel(String apiKey, String baseUrl,
                                     String modelName, double temperature, double topP,
                                     double presencePenalty, double frequencyPenalty,
                                     int timeout, Integer maxTokens) {
        assertNotEmpty("apiKey不能为空", apiKey);
        modelName = getString(modelName, "gemini-2.0-flash");
        if (isOpenAiCompatible(baseUrl)) {
            return buildOpenAiChatModel(apiKey, baseUrl, modelName, temperature, topP, presencePenalty, frequencyPenalty, timeout, maxTokens);
        }
        return buildNativeChatModel(apiKey, modelName, temperature, topP, timeout, maxTokens, baseUrl, presencePenalty, frequencyPenalty);
    }

    public static StreamingChatModel createStreamingChatModel(String apiKey, String baseUrl,
                                                       String modelName, double temperature, double topP,
                                                       double presencePenalty, double frequencyPenalty,
                                                       int timeout, Integer maxTokens) {
        assertNotEmpty("apiKey不能为空", apiKey);
        modelName = getString(modelName, "gemini-2.0-flash");
        if (isOpenAiCompatible(baseUrl)) {
            return buildOpenAiStreamingChatModel(apiKey, baseUrl, modelName, temperature, topP, presencePenalty, frequencyPenalty, timeout, maxTokens);
        }
        return buildNativeStreamingChatModel(apiKey, modelName, temperature, topP, timeout, maxTokens, baseUrl, presencePenalty, frequencyPenalty);
    }

    public static ImageModel createImageModel(AiModelOptions options, String apiKey, String baseUrl,
                                       String modelName, int timeout) {
        assertNotEmpty("apiKey不能为空", apiKey);
        modelName = getString(modelName, "gemini-2.5-flash-image");
        if (isOpenAiCompatible(baseUrl)) {
            return buildOpenAiImageModel(apiKey, baseUrl, modelName, timeout, options.getImageSize());
        }
        return buildNativeImageModel(apiKey, modelName, timeout, options.getImageSize(), baseUrl);
    }

    // ==================== OpenAI兼容协议 ====================

    private static ChatModel buildOpenAiChatModel(String apiKey, String baseUrl, String modelName,
                                                   double temperature, double topP,
                                                   double presencePenalty, double frequencyPenalty,
                                                   int timeout, Integer maxTokens) {
        baseUrl = ensureOpenAiUrlEnd(baseUrl);
        OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(temperature)
                .topP(topP)
                .presencePenalty(presencePenalty)
                .frequencyPenalty(frequencyPenalty)
                .timeout(Duration.ofSeconds(timeout))
                .maxRetries(0);
        if (null != maxTokens) {
            builder.maxTokens(maxTokens);
        }
        return builder.build();
    }

    private static StreamingChatModel buildOpenAiStreamingChatModel(String apiKey, String baseUrl, String modelName,
                                                                     double temperature, double topP,
                                                                     double presencePenalty, double frequencyPenalty,
                                                                     int timeout, Integer maxTokens) {
        baseUrl = ensureOpenAiUrlEnd(baseUrl);
        OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder builder = OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(temperature)
                .topP(topP)
                .presencePenalty(presencePenalty)
                .frequencyPenalty(frequencyPenalty)
                .timeout(Duration.ofSeconds(timeout));
        if (null != maxTokens) {
            builder.maxTokens(maxTokens);
        }
        return builder.build();
    }

    private static ImageModel buildOpenAiImageModel(String apiKey, String baseUrl, String modelName,
                                                     int timeout, String imageSize) {
        baseUrl = ensureOpenAiUrlEnd(baseUrl);
        OpenAiImageModel.OpenAiImageModelBuilder builder = OpenAiImageModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .timeout(Duration.ofSeconds(timeout))
                .maxRetries(0);
        if (StringUtils.isNotEmpty(imageSize) && isSupportAspectRatio(modelName)) {
            builder.size(ImageSizeEnum.getRatioBySize(imageSize));
        }
        return builder.build();
    }

    // ==================== Google原生API ====================

    private static ChatModel buildNativeChatModel(String apiKey, String modelName,
                                                  double temperature, double topP,
                                                  int timeout, Integer maxTokens,
                                                  String baseUrl, double presencePenalty, double frequencyPenalty) {
        GoogleAiGeminiChatModel.GoogleAiGeminiChatModelBuilder builder = GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(temperature)
                .frequencyPenalty(frequencyPenalty)
                .presencePenalty(presencePenalty)
                .topP(topP)
                .baseUrl(baseUrl)
                .timeout(Duration.ofSeconds(timeout));
        if (null != maxTokens) {
            builder.maxOutputTokens(maxTokens);
        }
        return builder.build();
    }

    private static StreamingChatModel buildNativeStreamingChatModel(String apiKey, String modelName,
                                                                    double temperature, double topP,
                                                                    int timeout, Integer maxTokens, String baseUrl, double presencePenalty, double frequencyPenalty) {
        GoogleAiGeminiStreamingChatModel.GoogleAiGeminiStreamingChatModelBuilder builder = GoogleAiGeminiStreamingChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(temperature)
                .topP(topP)
                .baseUrl(baseUrl)
                .frequencyPenalty(frequencyPenalty)
                .presencePenalty(presencePenalty)
                .timeout(Duration.ofSeconds(timeout));
        if (null != maxTokens) {
            builder.maxOutputTokens(maxTokens);
        }
        return builder.build();
    }

    private static ImageModel buildNativeImageModel(String apiKey, String modelName,
                                                    int timeout, String imageSize, String baseUrl) {
        GoogleAiGeminiImageModel.GoogleAiGeminiImageModelBuilder builder = GoogleAiGeminiImageModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .timeout(Duration.ofSeconds(timeout));
        // aspectRatio仅gemini-3+系列模型支持，其他模型设置会报错
        if (StringUtils.isNotEmpty(imageSize) && isSupportAspectRatio(modelName)) {
            builder.aspectRatio(ImageSizeEnum.getRatioBySize(imageSize));
        }
        return builder.build();
    }

    /**
     * 判断模型是否支持aspectRatio参数
     * 仅gemini-3+系列模型支持
     */
    private static boolean isSupportAspectRatio(String modelName) {
        if (StringUtils.isEmpty(modelName)) {
            return false;
        }
        // 匹配 gemini-3、gemini-4 等3及以上版本
        return modelName.matches(".*gemini-([3-9]|[1-9]\\d+).*");
    }
}

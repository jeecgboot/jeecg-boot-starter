package org.jeecg.ai.handler;

import dev.langchain4j.rag.query.router.QueryRouter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jeecg.ai.factory.AiModelOptions;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AIParams {

    /**
     * 供应者
     */
    String provider;
    /**
     * 模型名称
     */
    String modelName;
    /**
     * API域名
     */
    String baseUrl;
    /**
     * apiKey
     */
    String apiKey;
    /**
     * secretKey
     */
    String secretKey;
    /**
     * 文本类型的背景知识
     */
    String knowledgeTxt;
    /**
     * 向量路由
     */
    QueryRouter queryRouter;
    /**
     * 知识库匹配的topN
     */
    Integer topNumber = 5;
    /**
     * 知识库匹配的相似度
     */
    Double similarity = 0.75;
    /**
     * 最大记录消息
     */
    Integer maxMsgNumber = 10;

    /**
     * temperature:温度
     */
    Double temperature;

    /**
     * topP:多样性
     */
    Double topP;
    /**
     * frequencyPenalty:存在惩罚
     */
    Double presencePenalty;
    /**
     * frequencyPenalty:频率惩罚
     */
    Double frequencyPenalty;
    /**
     * maxTokens:最大标记
     */
    Integer maxTokens;

    public AiModelOptions toModelOptions() {
        return AiModelOptions.builder()
                .provider(this.getProvider())
                .modelName(this.getModelName())
                .baseUrl(this.getBaseUrl())
                .apiKey(this.getApiKey())
                .secretKey(this.getSecretKey())
                .temperature(this.getTemperature())
                .topP(this.getTopP())
                .presencePenalty(this.getPresencePenalty())
                .frequencyPenalty(this.getFrequencyPenalty())
                .maxTokens(this.getMaxTokens())
                .topNumber(this.getTopNumber())
                .similarity(this.getSimilarity())
                .build();
    }

}
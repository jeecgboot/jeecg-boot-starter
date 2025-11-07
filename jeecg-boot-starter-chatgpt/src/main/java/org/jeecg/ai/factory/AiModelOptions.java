package org.jeecg.ai.factory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * @Description: AI模型创建参数
 * @Author: chenrui
 * @Date: 2025/2/13 19:16
 */
@Getter
@Setter
@AllArgsConstructor
@Builder
public class AiModelOptions {
    /**
     * 供应者
     */
    String provider;
    /**
     * apiKey
     */
    String apiKey;
    /**
     * secretKey
     */
    String secretKey;
    /**
     * baseUrl:基础地址
     */
    String baseUrl;
    /**
     * modelName:模型名称
     */
    String modelName;
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

    /**
     * topK:返回条目数
     */
    Integer topNumber;

    /**
     * similarity:相似分
     */
    Double similarity;

    /**
     * 超时时间
     */
    Integer timeout;

    /**
     * 返回思考过程
     */
    Boolean returnThinking;

    @Override
    public String toString() {
        return "AiModelOptions{" +
                "provider='" + provider + '\'' +
                ", apiKey='" + apiKey + '\'' +
                ", secretKey='" + secretKey + '\'' +
                ", baseUrl='" + baseUrl + '\'' +
                ", modelName='" + modelName + '\'' +
                ", temperature=" + temperature +
                ", topP=" + topP +
                ", presencePenalty=" + presencePenalty +
                ", frequencyPenalty=" + frequencyPenalty +
                ", maxTokens=" + maxTokens +
                ", topNumber=" + topNumber +
                ", similarity=" + similarity +
                ", timeout=" + timeout +
                ", returnThinking=" + returnThinking +
                '}';
    }
}

package org.jeecg.ai.factory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

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

    //update-begin---author:scott ---date:20260429  for：[issues/9585]DeepSeek大模型切换为新发布deepseek-v4-flash，流程中调用出现异常------------
    /**
     * 把上一轮 AI 消息的 thinking 以 reasoning_content 字段回传到下次请求，
     * 用于 DeepSeek 推理模型(deepseek-reasoner/deepseek-v4-flash 等)的多轮工具调用场景。
     * 仅在调用方按模型类型判定为推理模型时启用，对非推理模型(如 deepseek-chat)应保持 null/false。
     */
    Boolean sendThinking;
    //update-end---author:scott ---date:20260429  for：[issues/9585]DeepSeek大模型切换为新发布deepseek-v4-flash，流程中调用出现异常------------

    /**
     * 启用搜索
     * for [issues/8781]千问模型，调用时是否可以增加千问模型自己的调用参数，例如允许联网搜索。 #8781
     */
    Boolean enableSearch;

    /**
     * 图片大小，长*宽(1024*1024)
     */
    String imageSize;

    /**
     * 生成图片的数量
     */
    Integer imageCount;

    /**
     * 是否为http1.1版本
     */
    Boolean izHttpVersionOne;

    /**
     * 自定义参数，透传给大模型厂家
     */
    Map<String, Object> extraParams;
    
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
                ", sendThinking=" + sendThinking +
                ", enableSearch=" + enableSearch +
                ", imageCount=" + imageCount +
                ", imageSize=" + imageSize +
                ", izHttpVersionOne=" + izHttpVersionOne +
                ", extraParams=" + extraParams +
                '}';
    }
}

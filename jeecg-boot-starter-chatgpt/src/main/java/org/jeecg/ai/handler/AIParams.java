package org.jeecg.ai.handler;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.rag.query.router.QueryRouter;
import dev.langchain4j.service.tool.ToolExecutor;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jeecg.ai.factory.AiModelOptions;

import java.util.List;
import java.util.Map;

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
    Integer maxMsgNumber = 4;

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
     * 超时时间
     */
    Integer timeout;

    /**
     * 返回思考过程
     */
    Boolean returnThinking;

    /**
     * FunctionCall工具集合
     */
    Map<ToolSpecification, ToolExecutor> tools;

    /**
     * MCP工具提供者集合
     */
    List<McpToolProvider> mcpToolProviders;

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
                .timeout(this.getTimeout())
                .returnThinking(this.getReturnThinking())
                .build();
    }

}
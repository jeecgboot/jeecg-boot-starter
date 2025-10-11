//package org.jeecg.ai.handler;
//
//import com.alibaba.fastjson.JSONObject;
//import dev.langchain4j.agent.tool.ToolExecutionRequest;
//import dev.langchain4j.data.message.AiMessage;
//import dev.langchain4j.data.message.ChatMessage;
//import dev.langchain4j.data.message.ToolExecutionResultMessage;
//import dev.langchain4j.data.message.UserMessage;
//import dev.langchain4j.mcp.McpToolProvider;
//import dev.langchain4j.mcp.client.DefaultMcpClient;
//import dev.langchain4j.mcp.client.McpClient;
//import dev.langchain4j.mcp.client.transport.McpTransport;
//import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
//import dev.langchain4j.model.chat.ChatModel;
//import dev.langchain4j.model.chat.StreamingChatModel;
//import dev.langchain4j.model.chat.request.ChatRequest;
//import dev.langchain4j.model.chat.response.ChatResponse;
//import dev.langchain4j.service.AiServiceContext;
//import dev.langchain4j.service.AiServiceTokenStream;
//import dev.langchain4j.service.AiServiceTokenStreamParameters;
//import dev.langchain4j.service.AiServices;
//import dev.langchain4j.service.TokenStream;
//import dev.langchain4j.service.output.ServiceOutputParser;
//import dev.langchain4j.service.tool.ToolExecutor;
//import dev.langchain4j.service.tool.ToolService;
//import dev.langchain4j.service.tool.ToolServiceContext;
//import org.jeecg.ai.assistant.AiChatAssistant;
//import org.jeecg.ai.assistant.AiStreamChatAssistant;
//import org.jeecg.ai.factory.AiModelFactory;
//import org.jeecg.ai.factory.AiModelOptions;
//import org.junit.jupiter.api.Assumptions;
//import org.junit.jupiter.api.Test;
//
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.List;
//import java.util.concurrent.CountDownLatch;
//
///**
// * LLM + MCP 集成测试
// *
// * 用例拆分为四种调用方式：
// * 1) AiChatAssistant + MCP（非流式）
// * 2) AiStreamChatAssistant + MCP（流式）
// * 3) 低级 API + MCP（非流式）
// * 4) 低级 API + MCP（流式）
// *
// * @author chenrui
// * @date 2025/8/21 17:08
// */
//public class LLMTest {
//
//
//    private final ServiceOutputParser serviceOutputParser = new ServiceOutputParser();
//
//    private static final String DEFAULT_PROMPT = "明天早上北京市的天气怎么样?";
//    private static final String MEMORY_ID = "default";
//
//    private static final String MCP_SSE_URL_AMAP = "https://mcp.amap.com/sse?key=xxx";
//
//
//    // 根据环境构建模型配置；缺少关键项则返回 null 以便测试跳过
//    private static AiModelOptions buildModelOptionsFromEnv() {
//        String baseUrl = "https://api.gpt.ge";
//        String apiKey = "sk-xxxx";
//        String modelName = "gpt-4.1-mini";
//        return AiModelOptions.builder()
//                .provider(AiModelFactory.AIMODEL_TYPE_OPENAI)
//                .modelName(modelName)
//                .baseUrl(baseUrl)
//                .apiKey(apiKey)
//                .build();
//    }
//
//    // 根据环境构建 MCP 工具提供者；缺少 URL 则返回 null 以便测试跳过
//    private static McpToolProvider buildMcpTool(String sseUrl) {
//        McpTransport transport = new HttpMcpTransport.Builder()
//                .sseUrl(sseUrl)
//                .logRequests(true)
//                .logResponses(true)
//                .build();
//        McpClient mcpClient = new DefaultMcpClient.Builder()
//                .transport(transport)
//                .build();
//        return McpToolProvider.builder()
//                .mcpClients(List.of(mcpClient))
//                .build();
//    }
//
//    // 1) 使用 AiChatAssistant 调用 MCP
//    @Test
//    void testMcpWithAiChatAssistant() {
//        System.out.println("[用例1] AiChatAssistant + MCP（非流式）：验证通过服务接口与工具调用完成一次问答；若缺少 OPENAI_API_* 或 MCP_SSE_URL 将跳过。");
//
//        AiModelOptions modelOp = buildModelOptionsFromEnv();
//        McpToolProvider toolProvider = buildMcpTool(MCP_SSE_URL_AMAP);
//        Assumptions.assumeTrue(modelOp != null, "缺少 OPENAI_API_BASE / OPENAI_API_KEY，跳过测试");
//        Assumptions.assumeTrue(toolProvider != null, "缺少 MCP_SSE_URL，跳过测试");
//
//        // 构建同步聊天模型并通过 AiChatAssistant 发起一次非流式对话
//        ChatModel chatModel = AiModelFactory.createChatModel(modelOp);
//        AiChatAssistant bot = AiServices.builder(AiChatAssistant.class)
//                .chatModel(chatModel)
//                .toolProvider(toolProvider)
//                .build();
//        String chat = bot.chat(DEFAULT_PROMPT);
//        System.out.println("聊天回复: " + chat);
//    }
//
//    // 2) 使用 AiStreamChatAssistant 调用 MCP (流式)
//    @Test
//    void testMcpWithAiStreamChatAssistant() throws InterruptedException {
//        System.out.println("[用例2] AiStreamChatAssistant + MCP（流式）：验证服务接口的流式分片输出与工具调用回调；缺配置将跳过。");
//
//        AiModelOptions modelOp = buildModelOptionsFromEnv();
//        McpToolProvider toolProvider = buildMcpTool(MCP_SSE_URL_AMAP);
//        Assumptions.assumeTrue(modelOp != null, "缺少 OPENAI_API_BASE / OPENAI_API_KEY，跳过测试");
//        Assumptions.assumeTrue(toolProvider != null, "缺少 MCP_SSE_URL，跳过测试");
//
//        // 构建流式聊天模型并通过 AiStreamChatAssistant 获取 TokenStream
//        StreamingChatModel streamingChatModel = AiModelFactory.createStreamingChatModel(modelOp);
//        AiStreamChatAssistant bot = AiServices.builder(AiStreamChatAssistant.class)
//                .streamingChatModel(streamingChatModel)
//                .toolProvider(toolProvider)
//                .build();
//
//        TokenStream tokenStream = bot.chat(DEFAULT_PROMPT);
//        CountDownLatch latch = new CountDownLatch(1);
//        tokenStream
//                // 分片增量回调
//                .onPartialResponse(s -> System.out.println("消息回复:" + s))
//                // 工具执行后回调
//                .onToolExecuted(toolExecution -> System.out.println("工具调用后: " + JSONObject.toJSONString(toolExecution.result())))
//                // 中间态回调（有些模型会产生）
//                .onIntermediateResponse(chatResponse -> System.out.println("中间?: " + JSONObject.toJSONString(chatResponse)))
//                // 完整响应回调
//                .onCompleteResponse(chatResponse -> {
//                    System.out.println("完整回复: " + JSONObject.toJSONString(chatResponse));
//                    latch.countDown();
//                })
//                // 错误回调
//                .onError(e -> {
//                    System.err.println("错误: " + e.getMessage());
//                    latch.countDown();
//                })
//                .start();
//        latch.await();
//    }
//
//    // 3) 使用低级 API 调用 MCP (非流式)
//    @Test
//    void testMcpLowLevel() {
//        System.out.println("[用例3] 低级API + MCP（非流式）：验证手动循环-工具执行-结果回填的完整闭环。");
//
//        AiModelOptions modelOp = buildModelOptionsFromEnv();
//        McpToolProvider toolProvider = buildMcpTool(MCP_SSE_URL_AMAP);
//        Assumptions.assumeTrue(modelOp != null, "缺少 OPENAI_API_BASE / OPENAI_API_KEY，跳过测试");
//        Assumptions.assumeTrue(toolProvider != null, "缺少 MCP_SSE_URL，跳过测试");
//
//        ChatModel chatModel = AiModelFactory.createChatModel(modelOp);
//        UserMessage message = UserMessage.from(DEFAULT_PROMPT);
//
//        // 准备 ToolService，上下文中含工具规范与执行器映射
//        ToolService toolService = new ToolService();
//        toolService.toolProvider(toolProvider);
//        ToolServiceContext toolCtx = toolService.createContext(MEMORY_ID, message);
//
//        // 对话历史：用户消息 -> AI 回复 -> 工具结果 -> ...
//        List<ChatMessage> history = new ArrayList<>();
//        history.add(message);
//
//        while (true) {
//            // 携带工具规范发起请求
//            ChatRequest req = ChatRequest.builder()
//                    .messages(history)
//                    .toolSpecifications(toolCtx.toolSpecifications())
//                    .build();
//
//            ChatResponse resp = chatModel.chat(req);
//            AiMessage ai = resp.aiMessage();
//            history.add(ai);
//
//            // 没有工具调用，则解析文本并结束
//            if (ai.toolExecutionRequests() == null || ai.toolExecutionRequests().isEmpty()) {
//                String text = (String) serviceOutputParser.parse(resp, String.class);
//                System.out.println("聊天回复: " + text);
//                break;
//            }
//
//            // 有工具调用：逐个执行并将结果以 ToolExecutionResultMessage 追加到历史
//            for (ToolExecutionRequest r : ai.toolExecutionRequests()) {
//                ToolExecutor executor = toolCtx.toolExecutors().get(r.name());
//                if (executor == null) {
//                    throw new IllegalStateException("未找到工具执行器: " + r.name());
//                }
//                String result = executor.execute(r, MEMORY_ID);
//                ToolExecutionResultMessage resultMsg = ToolExecutionResultMessage.from(r, result);
//                history.add(resultMsg);
//            }
//        }
//    }
//
//    // 4) 使用低级 API 调用 MCP (流式)
//    @Test
//    void testMcpLowLevelStream() throws InterruptedException {
//        System.out.println("[用例4] 低级API + MCP（流式）：验证直接使用 TokenStream 组合工具上下文的流式交互。");
//
//        AiModelOptions modelOp = buildModelOptionsFromEnv();
//        McpToolProvider toolProvider = buildMcpTool(MCP_SSE_URL_AMAP);
//        Assumptions.assumeTrue(modelOp != null, "缺少 OPENAI_API_BASE / OPENAI_API_KEY，跳过测试");
//        Assumptions.assumeTrue(toolProvider != null, "缺少 MCP_SSE_URL，跳过测试");
//
//        StreamingChatModel streamingChatModel = AiModelFactory.createStreamingChatModel(modelOp);
//        UserMessage message = UserMessage.from(DEFAULT_PROMPT);
//
//        // 工具上下文：提供工具规范与执行器
//        ToolService toolService = new ToolService();
//        toolService.toolProvider(toolProvider);
//        ToolServiceContext toolCtx = toolService.createContext(MEMORY_ID, message);
//
//        // 构造服务上下文并注入流式模型
//        AiServiceContext context = new AiServiceContext(AiStreamChatAssistant.class);
//        context.streamingChatModel = streamingChatModel;
//
//        // 直接使用低级 TokenStream 传入消息、工具规范和执行器
//        TokenStream tokenStream = new AiServiceTokenStream(
//                AiServiceTokenStreamParameters.builder()
//                        .messages(Collections.singletonList(message))
//                        .toolSpecifications(toolCtx.toolSpecifications())
//                        .toolExecutors(toolCtx.toolExecutors())
//                        .context(context)
//                        .memoryId(MEMORY_ID)
//                        .build());
//
//        CountDownLatch latch = new CountDownLatch(1);
//        tokenStream
//                .onPartialResponse(s -> System.out.println("消息回复:" + s))
//                .onToolExecuted(toolExecution -> System.out.println("工具调用后: " + JSONObject.toJSONString(toolExecution.result())))
//                .onIntermediateResponse(chatResponse -> System.out.println("中间?: " + JSONObject.toJSONString(chatResponse)))
//                .onCompleteResponse(chatResponse -> {
//                    System.out.println("完整回复: " + JSONObject.toJSONString(chatResponse));
//                    latch.countDown();
//                })
//                .onError(e -> {
//                    System.err.println("错误: " + e.getMessage());
//                    latch.countDown();
//                })
//                .start();
//        latch.await();
//    }
//
//}

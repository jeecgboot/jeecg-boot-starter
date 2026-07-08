package org.jeecg.ai.handler;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;
import dev.langchain4j.skills.FileSystemSkill;
import dev.langchain4j.skills.FileSystemSkillLoader;
import dev.langchain4j.skills.Skills;
import dev.langchain4j.skills.shell.ShellSkills;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.ai.factory.AiModelFactory;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Skills 功能测试
 *
 * 测试内容：
 * 1. Skill 文件加载（FileSystemSkillLoader）
 * 2. Skills 工具注册（activate_skill、read_skill_resource）
 * 3. 端到端 fillSkillTools 测试
 * 4. 调用聊天接口测试（模型通过 activate_skill 自主激活 skill）
 *
 * @author test
 * @date 2026/3/18
 */
@Slf4j
public class SkillTest {

    /**
     * Skills 文件目录路径
     */
    private static final String SKILLS_DIR = "D:/jeecg-ai-skill";

    private static List<FileSystemSkill> loadedSkills;

    @BeforeAll
    static void setUp() {
        Path skillsPath = Paths.get(SKILLS_DIR);
        Assumptions.assumeTrue(Files.exists(skillsPath),
                "跳过测试：Skills 目录不存在 " + SKILLS_DIR);
        loadedSkills = FileSystemSkillLoader.loadSkills(skillsPath);
        log.info("Loaded {} skills from {}", loadedSkills.size(), SKILLS_DIR);
    }

    // ==================== 1. Skill 文件加载测试 ====================

    @Test
    void testLoadSkillsFromFileSystem() {
        assertNotNull(loadedSkills, "Skills 列表不能为 null");
        assertFalse(loadedSkills.isEmpty(), "Skills 列表不能为空");
        log.info("共加载 {} 个 skill", loadedSkills.size());
        for (FileSystemSkill skill : loadedSkills) {
            log.info("  - name: {}, description: {}", skill.name(), skill.description());
        }
    }

    @Test
    void testSkillHasNameAndDescription() {
        for (FileSystemSkill skill : loadedSkills) {
            assertNotNull(skill.name(), "Skill name 不能为 null");
            assertFalse(skill.name().isEmpty(), "Skill name 不能为空");
            assertNotNull(skill.description(), "Skill description 不能为 null");
            assertFalse(skill.description().isEmpty(), "Skill description 不能为空");
            log.info("Skill [{}] 校验通过, description 长度: {}", skill.name(), skill.description().length());
        }
    }

    @Test
    void testSkillHasContent() {
        for (FileSystemSkill skill : loadedSkills) {
            String content = skill.content();
            assertNotNull(content, "Skill [" + skill.name() + "] content 不能为 null");
            assertFalse(content.isEmpty(), "Skill [" + skill.name() + "] content 不能为空");
            log.info("Skill [{}] content 长度: {}", skill.name(), content.length());
        }
    }

    @Test
    void testChartGeneratorSkillExists() {
        FileSystemSkill chartSkill = findSkillByName("chart-generator");
        assertNotNull(chartSkill, "应包含 chart-generator skill");
        assertTrue(chartSkill.description().contains("图表"), "chart-generator 描述应包含'图表'");
        assertTrue(chartSkill.content().contains("ECharts"), "chart-generator 内容应包含 ECharts");
        log.info("chart-generator skill 校验通过");
    }

    @Test
    void testWeeklyReportSkillExists() {
        FileSystemSkill weeklySkill = findSkillByName("weekly-report");
        assertNotNull(weeklySkill, "应包含 weekly-report skill");
        assertTrue(weeklySkill.description().contains("周报"), "weekly-report 描述应包含'周报'");
        assertTrue(weeklySkill.content().contains("周报"), "weekly-report 内容应包含'周报'");
        log.info("weekly-report skill 校验通过");
    }

    // ==================== 2. Skills 工具注册测试 ====================

    @Test
    void testAllSkillToolsRegistration() {
        Skills skills = Skills.from(loadedSkills);
        UserMessage userMessage = UserMessage.from("测试消息");
        ToolProviderRequest request = new ToolProviderRequest("test-memory-id", userMessage);
        ToolProviderResult result = skills.toolProvider().provideTools(request);

        assertNotNull(result, "ToolProviderResult 不能为 null");
        assertNotNull(result.tools(), "tools 不能为 null");

        // 验证包含 activate_skill 和 read_skill_resource 工具
        Set<String> registeredTools = new HashSet<>();
        for (Map.Entry<ToolSpecification, ToolExecutor> entry : result.tools().entrySet()) {
            String toolName = entry.getKey().name();
            registeredTools.add(toolName);
            log.info("注册的工具: {}", toolName);
        }
        assertTrue(registeredTools.contains("read_skill_resource"), "应注册 read_skill_resource 工具");
        assertTrue(registeredTools.contains("activate_skill"), "应注册 activate_skill 工具");
        log.info("全部 Skill 工具注册测试通过, 工具数: {}", registeredTools.size());
    }


    // ==================== 3. ShellSkills Shell 模式测试 ====================

    @Test
    void testShellSkillsToolRegistration() {
        ShellSkills shellSkills = ShellSkills.from(loadedSkills);
        UserMessage userMessage = UserMessage.from("测试消息");
        ToolProviderRequest request = new ToolProviderRequest("test-memory-id", userMessage);
        ToolProviderResult result = shellSkills.toolProvider().provideTools(request);

        assertNotNull(result, "ShellSkills ToolProviderResult 不能为 null");
        assertNotNull(result.tools(), "ShellSkills tools 不能为 null");

        Set<String> registeredTools = new HashSet<>();
        for (Map.Entry<ToolSpecification, ToolExecutor> entry : result.tools().entrySet()) {
            registeredTools.add(entry.getKey().name());
            log.info("Shell 模式注册的工具: {}", entry.getKey().name());
        }
        // Shell 模式注册 run_shell_command，不注册 activate_skill
        assertTrue(registeredTools.contains("run_shell_command"), "Shell 模式应注册 run_shell_command");
        assertFalse(registeredTools.contains("activate_skill"), "Shell 模式不应注册 activate_skill");
        log.info("ShellSkills 工具注册测试通过, 工具: {}", registeredTools);
    }

    @Test
    void testShellSkillsFormatAvailableSkills() {
        ShellSkills shellSkills = ShellSkills.from(loadedSkills);
        String formatted = shellSkills.formatAvailableSkills();

        assertNotNull(formatted, "formatAvailableSkills 不能为 null");
        assertFalse(formatted.isEmpty(), "formatAvailableSkills 不能为空");
        // 应包含各 skill 的名称和描述
        assertTrue(formatted.contains("chart-generator") || formatted.contains("图表"),
                "应包含 chart-generator skill 信息");
        assertTrue(formatted.contains("weekly-report") || formatted.contains("周报"),
                "应包含 weekly-report skill 信息");
        log.info("ShellSkills formatAvailableSkills 输出:\n{}", formatted);
    }

    @Test
    void testFillSkillToolsShellModeEndToEnd() throws Exception {
        LLMHandler handler = new LLMHandler();

        AIParams params = new AIParams();
        params.setSkillsShellDir(SKILLS_DIR);

        ChatMemory chatMemory = MessageWindowChatMemory.builder().maxMessages(10).build();
        chatMemory.add(UserMessage.from("帮我写周报"));
        UserMessage userMessage = UserMessage.from("帮我写周报");

        Class<?> collateMsgRespClass = Class.forName("org.jeecg.ai.handler.LLMHandler$CollateMsgResp");
        Object chatMessageObj = collateMsgRespClass.getDeclaredConstructors()[0].newInstance(chatMemory, null, userMessage);

        List<ToolSpecification> toolSpecifications = new ArrayList<>();
        Map<String, ToolExecutor> toolExecutors = new HashMap<>();

        // 调用 Shell 模式方法（使用 shellSkillsDir）
        Method method = LLMHandler.class.getDeclaredMethod("fillSkillToolsShellMode",
                AIParams.class, collateMsgRespClass, List.class, Map.class);
        method.setAccessible(true);
        method.invoke(handler, params, chatMessageObj, toolSpecifications, toolExecutors);

        // Shell 模式注册 run_shell_command，不注册 activate_skill
        assertTrue(toolExecutors.containsKey("run_shell_command"), "Shell 模式应注册 run_shell_command");
        assertFalse(toolExecutors.containsKey("activate_skill"), "Shell 模式不应注册 activate_skill");

        // 验证 Skills 列表已注入系统消息
        boolean hasSkillsInfo = false;
        for (ChatMessage msg : chatMemory.messages()) {
            if (msg instanceof SystemMessage sm && sm.text().contains("可用技能")) {
                hasSkillsInfo = true;
                break;
            }
        }
        assertTrue(hasSkillsInfo, "Shell 模式应将可用技能列表注入系统消息");
        log.info("fillSkillToolsShellMode 端到端测试通过, 注册工具数: {}", toolSpecifications.size());
    }

    // ==================== 4. 调用聊天接口测试（需要配置真实 API） ====================

    // ====== 请根据实际情况修改以下配置 ======
    private static final String API_BASE_URL = "https://api.v3.cm/";
    private static final String API_KEY = "sk-??";
    private static final String MODEL_NAME = "gpt-4o";
    private static final String PROVIDER = AiModelFactory.AIMODEL_TYPE_OPENAI;
    // ==========================================

    /**
     * 构建带有 Skills 配置的 AIParams
     */
    private AIParams buildChatParams() {
        AIParams params = new AIParams();
        params.setProvider(PROVIDER);
        params.setBaseUrl(API_BASE_URL);
        params.setApiKey(API_KEY);
        params.setModelName(MODEL_NAME);
        params.setSkillsDir(SKILLS_DIR);
        params.setSkillsShellDir(SKILLS_DIR);
        return params;
    }

    /**
     * 流式调用：模型通过 activate_skill 触发周报 skill
     */
    @Test
    void testStreamChatWithWeeklyReportSkill() throws InterruptedException {
        Assumptions.assumeTrue(!"sk-xxx".equals(API_KEY), "请先配置真实的 API_KEY 再运行此用例");

        LLMHandler handler = new LLMHandler();
        AIParams params = buildChatParams();

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(UserMessage.from("帮我写周报，本周完成了接口联调和性能优化"));

        TokenStream tokenStream = handler.chat(messages, params);

        CountDownLatch latch = new CountDownLatch(1);
        StringBuilder fullResponse = new StringBuilder();
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        tokenStream
                .onPartialResponse(token -> {
                    fullResponse.append(token);
                    System.out.print(token);
                })
                .onCompleteResponse(chatResponse -> {
                    System.out.println();
                    log.info("===== 周报 Skill 流式聊天完成, 总长度: {} =====", fullResponse.length());
                    latch.countDown();
                })
                .onError(e -> {
                    log.error("流式聊天出错: {}", e.getMessage(), e);
                    errorRef.set(e);
                    latch.countDown();
                })
                .start();

        latch.await();

        assertNull(errorRef.get(), "流式聊天不应有错误: " + (errorRef.get() != null ? errorRef.get().getMessage() : ""));
        assertFalse(fullResponse.isEmpty(), "流式聊天返回不能为空");
        log.info("===== 周报 Skill 流式聊天完整返回 =====\n{}", fullResponse);
    }

    /**
     * 流式调用：模型通过 activate_skill 触发图表 skill
     */
    @Test
    void testStreamChatWithChartGeneratorSkill() throws InterruptedException {
        Assumptions.assumeTrue(!"sk-xxx".equals(API_KEY), "请先配置真实的 API_KEY 再运行此用例");

        LLMHandler handler = new LLMHandler();
        AIParams params = buildChatParams();

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(UserMessage.from("做个图表，用柱状图展示：一月销售100万，二月120万，三月95万"));

        TokenStream tokenStream = handler.chat(messages, params);

        CountDownLatch latch = new CountDownLatch(1);
        StringBuilder fullResponse = new StringBuilder();
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        tokenStream
                .onPartialResponse(token -> {
                    fullResponse.append(token);
                    System.out.print(token);
                })
                .onCompleteResponse(chatResponse -> {
                    System.out.println();
                    log.info("===== 图表 Skill 流式聊天完成, 总长度: {} =====", fullResponse.length());
                    latch.countDown();
                })
                .onError(e -> {
                    log.error("流式聊天出错: {}", e.getMessage(), e);
                    errorRef.set(e);
                    latch.countDown();
                })
                .start();

        latch.await();

        assertNull(errorRef.get(), "流式聊天不应有错误: " + (errorRef.get() != null ? errorRef.get().getMessage() : ""));
        assertFalse(fullResponse.isEmpty(), "流式聊天返回不能为空");
        log.info("===== 图表 Skill 流式聊天完整返回 =====\n{}", fullResponse);
    }

    // ==================== 辅助方法 ====================

    /**
     * 根据 name 查找 skill
     */
    private FileSystemSkill findSkillByName(String name) {
        for (FileSystemSkill skill : loadedSkills) {
            if (name.equals(skill.name())) {
                return skill;
            }
        }
        return null;
    }
}

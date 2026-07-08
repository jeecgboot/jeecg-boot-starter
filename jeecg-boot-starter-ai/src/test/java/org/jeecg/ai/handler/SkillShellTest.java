package org.jeecg.ai.handler;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
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

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * db-query Skill 测试（兼容 activate_skill 模式 与 Shell 模式）
 *
 * <p>两种模式使用独立的 Skills 目录：
 * <ul>
 *   <li><b>skillsDir（activate_skill 模式）</b>：注册 activate_skill + read_skill_resource，
 *       skill 内部使用 execute_sql 等 FunctionCall 工具</li>
 *   <li><b>shellSkillsDir（Shell 模式）</b>：注册 run_shell_command + read_skill_resource，
 *       skill 通过命令行工具（mysql/psql）执行操作</li>
 * </ul>
 *
 * <p>两个目录可以同时配置，LLMHandler 会同时加载两种模式的 Skills。
 *
 * @author test
 * @date 2026/3/19
 */
@Slf4j
public class SkillShellTest {

    /**
     * activate_skill 模式 Skills 目录（db-query 使用 execute_sql）
     */
    private static final String SKILLS_DIR = "D:/jeecg-ai-skill";

    /**
     * Shell 模式 Skills 目录（db-shell-query 使用 run_shell_command）
     */
    private static final String SHELL_SKILLS_DIR = "D:/jeecg-ai-skill-shell";

    // ====== API 配置（请根据实际情况修改） ======
    private static final String API_BASE_URL = "https://api.v3.cm/";
    private static final String API_KEY = "sk-??";
    private static final String MODEL_NAME = "gpt-4o";
    private static final String PROVIDER = AiModelFactory.AIMODEL_TYPE_OPENAI;
    // ==========================================

    private static List<FileSystemSkill> loadedSkills;
    private static List<FileSystemSkill> loadedShellSkills;

    @BeforeAll
    static void setUp() {
        Assumptions.assumeTrue(Files.exists(Paths.get(SKILLS_DIR)),
                "跳过测试：Skills 目录不存在 " + SKILLS_DIR);
        Assumptions.assumeTrue(Files.exists(Paths.get(SHELL_SKILLS_DIR)),
                "跳过测试：Shell Skills 目录不存在 " + SHELL_SKILLS_DIR);

        loadedSkills = FileSystemSkillLoader.loadSkills(Paths.get(SKILLS_DIR));
        log.info("Loaded {} skills from {}", loadedSkills.size(), SKILLS_DIR);

        loadedShellSkills = FileSystemSkillLoader.loadSkills(Paths.get(SHELL_SKILLS_DIR));
        log.info("Loaded {} shell skills from {}", loadedShellSkills.size(), SHELL_SKILLS_DIR);
    }

    // ==================== 1. Skill 加载验证 ====================

    @Test
    void testDbQuerySkillExists() {
        FileSystemSkill dbSkill = findSkillByName(loadedSkills, "db-query");
        assertNotNull(dbSkill, "skillsDir 应包含 db-query skill");
        assertTrue(dbSkill.description().contains("数据库"), "db-query 描述应包含'数据库'");
        assertTrue(dbSkill.content().contains("execute_sql"), "db-query 内容应包含 execute_sql");
        log.info("db-query skill (activate_skill 模式) 校验通过, content 长度: {}", dbSkill.content().length());
    }

    @Test
    void testDbShellQuerySkillExists() {
        FileSystemSkill shellSkill = findSkillByName(loadedShellSkills, "db-shell-query");
        assertNotNull(shellSkill, "shellSkillsDir 应包含 db-shell-query skill");
        assertTrue(shellSkill.description().contains("命令行"), "db-shell-query 描述应包含'命令行'");
        assertTrue(shellSkill.content().contains("run_shell_command"), "db-shell-query 内容应包含 run_shell_command");
        log.info("db-shell-query skill (Shell 模式) 校验通过, content 长度: {}", shellSkill.content().length());
    }

    @Test
    void testShellSkillHasResources() {
        FileSystemSkill shellSkill = findSkillByName(loadedShellSkills, "db-shell-query");
        assertNotNull(shellSkill, "shellSkillsDir 应包含 db-shell-query skill");
        assertNotNull(shellSkill.resources(), "db-shell-query 应包含 resources");
        assertFalse(shellSkill.resources().isEmpty(), "db-shell-query resources 不能为空");
        log.info("db-shell-query resources 数量: {}", shellSkill.resources().size());
        for (var resource : shellSkill.resources()) {
            log.info("  resource: {}, 长度: {}", resource.relativePath(), resource.content().length());
        }
    }

    // ==================== 2. 工具注册测试（两种模式独立验证） ====================

    @Test
    void testActivateSkillModeToolRegistration() {
        Skills skills = Skills.from(loadedSkills);
        UserMessage userMessage = UserMessage.from("帮我查一下数据库里的用户表");
        ToolProviderRequest request = new ToolProviderRequest("test-memory-id", userMessage);
        ToolProviderResult result = skills.toolProvider().provideTools(request);

        assertNotNull(result, "ToolProviderResult 不能为 null");
        Set<String> registeredTools = new HashSet<>();
        for (Map.Entry<ToolSpecification, ToolExecutor> entry : result.tools().entrySet()) {
            registeredTools.add(entry.getKey().name());
        }

        assertTrue(registeredTools.contains("activate_skill"), "activate_skill 模式应注册 activate_skill");
        assertTrue(registeredTools.contains("read_skill_resource"), "activate_skill 模式应注册 read_skill_resource");
        assertFalse(registeredTools.contains("run_shell_command"), "activate_skill 模式不应注册 run_shell_command");
        log.info("activate_skill 模式工具注册测试通过, 工具: {}", registeredTools);
    }

    @Test
    void testShellModeToolRegistration() {
        ShellSkills shellSkills = ShellSkills.from(loadedShellSkills);
        UserMessage userMessage = UserMessage.from("用命令行查一下数据库");
        ToolProviderRequest request = new ToolProviderRequest("test-memory-id", userMessage);
        ToolProviderResult result = shellSkills.toolProvider().provideTools(request);

        assertNotNull(result, "ToolProviderResult 不能为 null");
        Set<String> registeredTools = new HashSet<>();
        for (Map.Entry<ToolSpecification, ToolExecutor> entry : result.tools().entrySet()) {
            registeredTools.add(entry.getKey().name());
        }

        assertTrue(registeredTools.contains("run_shell_command"), "Shell 模式应注册 run_shell_command");
        assertFalse(registeredTools.contains("activate_skill"), "Shell 模式不应注册 activate_skill");
        log.info("Shell 模式工具注册测试通过, 工具: {}", registeredTools);
    }

    // ==================== 3. 真实 API 调用测试（需要配置 API） ====================

    private AIParams buildChatParams() {
        AIParams params = new AIParams();
        params.setProvider(PROVIDER);
        params.setBaseUrl(API_BASE_URL);
        params.setApiKey(API_KEY);
        params.setModelName(MODEL_NAME);
        params.setSkillsDir(SKILLS_DIR);
        params.setSkillsShellDir(SHELL_SKILLS_DIR);
        return params;
    }

    /**
     * 两种模式同时配置，流式调用
     */
    @Test
    void testStreamChatWithBothModes() throws InterruptedException {
        Assumptions.assumeTrue(!"sk-xxx".equals(API_KEY), "请先配置真实的 API_KEY 再运行此用例");

        LLMHandler handler = new LLMHandler();
        AIParams params = buildChatParams();

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(UserMessage.from(
                "用命令行查一下MySQL数据库有哪些表，连接信息：host=localhost, port=3306, user=root, password=123456, database=jeecg-boot"));

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
                    log.info("===== 流式聊天完成, 总长度: {} =====", fullResponse.length());
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
        log.info("===== 流式聊天完整返回 =====\n{}", fullResponse);
    }

    // ==================== 辅助方法 ====================

    private FileSystemSkill findSkillByName(List<FileSystemSkill> skills, String name) {
        for (FileSystemSkill skill : skills) {
            if (name.equals(skill.name())) {
                return skill;
            }
        }
        return null;
    }
}

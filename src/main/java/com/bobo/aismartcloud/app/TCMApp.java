package com.bobo.aismartcloud.app;


import com.bobo.aismartcloud.advisor.MyLoggerAdvisor;
import com.bobo.aismartcloud.rag.TCMDocumentLoader;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.minimax.MiniMaxChatModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * AI 中医问诊应用
 */
@Component
@Slf4j
public class TCMApp {

    private final ChatClient chatClient;

    /**
     * 对话记忆每次检索的消息数量
     */
    private static final int DEFAULT_CHAT_MEMORY_RETRIEVE_SIZE = 1;

    /**
     * 对话记忆检索大小的常量key（Spring AI 不同版本常量名可能不同）
     */
    private static final String CHAT_MEMORY_RETRIEVE_SIZE_KEY = "chat_memory_retrieve_size";

    private static final String SYSTEM_PROMPT = "# Role: AI 中医问诊助手\n" +
            "\n" +
            "  ## 身份设定\n" +
            "  你是一位资深的中华老中医，拥有40年以上临床经验，擅长中医体质辨识、亚健康调理、食疗养生指导。你待人和蔼、说话温和、耐心\n" +
            "  细致，像一位关心晚辈的邻家老爷爷。\n" +
            "\n" +
            "  ## 核心原则\n" +
            "  1. **辨证论治**：像真实中医一样，通过\"望、闻、问，切\"了解用户，循序渐进地引导用户描述症状，不得急于给出结论\n" +
            "  2. **追问细节**：主动询问与症状相关的关键信息（舌苔、睡眠、二便、情绪、月经等），引导越详细，判断越准确\n" +
            "  3. **温暖关怀**：关注用户的健康焦虑，用温和语言安慰用户情绪，不要制造恐慌\n" +
            "  4. **养生为主**：以食疗、穴位按摩、作息调整等养生方法为主，涉及处方/严重症状必须引导就医\n" +
            "  5. **文化融入**：适当引用《黄帝内经》《伤寒论》等中医经典理论，增强专业感和信任感\n" +
            "\n" +
            "  ## 问诊流程\n" +
            "  1. **寒暄问候**：先关心用户的状态，营造舒适的问诊氛围\n" +
            "  2. **主诉收集**：让用户描述当前最困扰的身体不适\n" +
            "  3. **追问引导**：围绕主诉，主动询问以下维度（每次问1-3个问题）：\n" +
            "     - 持续时间、频率、诱发因素\n" +
            "     - 睡眠情况（入睡困难/多梦/易醒/嗜睡）\n" +
            "     - 饮食情况（食欲/口味/二便/饮水）\n" +
            "     - 情绪状态（焦虑/抑郁/易怒/低落）\n" +
            "     - 特殊人群（女性加问月经/生育，男性加问体力/夜尿）\n" +
            "  4. **体质辨识**：综合症状给出体质判断（平和/气虚/阳虚/阴虚/痰湿/湿热/血瘀/气郁/特禀）\n" +
            "  5. **调理建议**：根据体质和症状，给出包含以下方面的个性化方案：\n" +
            "     - 饮食调理（宜吃/忌吃食材、食疗方）\n" +
            "     - 作息建议（起床/入睡时间、运动推荐）\n" +
            "     - 穴位按摩（2-3个关键穴位及按揉方法）\n" +
            "     - 情志调节（疏解情绪的方法）\n" +
            "  6. **关怀收尾**：询问用户是否还有其他不适，叮嘱注意事项\n" +
            "\n" +
            "  ## 限制\n" +
            "  - 不提供西医诊断、处方药物建议\n" +
            "  - 涉及胸痛、呼吸困难、出血、昏迷等急症，立刻建议就医\n" +
            "  - 所有建议添加免责声明：\"以上内容仅供参考，具体调理方案请咨询专业中医师\"\n" +
            "\n" +
            "  ## 输出风格\n" +
            "  - 语言风格：温和、专业、像聊天一样自然，不用冰冷的条目式输出\n" +
            "  - 遇到不确定的情况，主动说\"这个情况我建议您线下就医确认\"\n" +
            "  - 多用\"咱们\"、\"您\"、\"我帮您分析分析\"等亲和用语";

    /**
     * 初始化 ChatClient
     *
     * @param chatModel MiniMax 大模型
     */
    public TCMApp(MiniMaxChatModel chatModel) {
        // 初始化基于内存的对话记忆
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20)
                .build();
        chatClient = ChatClient.builder(chatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        // 自定义日志 Advisor
                        new MyLoggerAdvisor()
                        // ,new SimpleLoggerAdvisor() //Spring AI 已经内置了 SimpleLogger Advisor 日志拦截器，但是以 Debug 级别输出日志
                )
                .build();
    }

    /**
     * AI 基础对话（支持多轮对话记忆）
     *
     * @param message 用户输入
     * @param chatId  会话ID
     * @return AI 回复
     */
    public String doChat(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, DEFAULT_CHAT_MEMORY_RETRIEVE_SIZE))
                .call()
                .chatResponse();
        if (chatResponse.getResult() == null || chatResponse.getResult().getOutput() == null) {
            log.error("AI 回复为空，请检查 API 配置和模型名称");
            return "AI 回复为空，请检查 API 配置";
        }
        String content = chatResponse.getResult().getOutput().getText();
        log.info("AI 回复: {}", content);
        return content;
    }

    /**
     * AI 基础对话（支持多轮对话记忆，SSE 流式传输）
     *
     * @param message 用户输入
     * @param chatId  会话ID
     * @return AI 回复（流式）
     */
    public Flux<String> doChatByStream(String message, String chatId) {
        return chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, DEFAULT_CHAT_MEMORY_RETRIEVE_SIZE))
                .stream()
                .content();
    }

    /**
     * 问诊报告结构体
     */
    public record ConsultationReport(String title, List<String> suggestions) {
    }

    /**
     * AI 问诊报告功能（结构化输出）
     *
     * @param message 用户输入
     * @param chatId  会话ID
     * @return 问诊报告
     */
    public ConsultationReport doChatWithReport(String message, String chatId) {
        ConsultationReport consultationReport = chatClient
                .prompt()
                .system(SYSTEM_PROMPT + "每次对话后都要生成问诊结果，标题为{用户名}的问诊报告，内容为调理建议列表")
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, DEFAULT_CHAT_MEMORY_RETRIEVE_SIZE))
                .call()
                .entity(ConsultationReport.class);
        log.info("问诊报告: {}", consultationReport);
        return consultationReport;
    }

    // ==================== RAG 知识库相关 ====================

    @Resource
    private VectorStore tcmVectorStore;

    //    @Resource
    //    private Advisor tcmRagCloudAdvisor;

    //    @Resource
    //    private VectorStore pgVectorStore;

    //    @Resource
    //    private QueryRewriter queryRewriter;

    @Resource
    private TCMDocumentLoader tcmDocumentLoader;

    /**
     * 和 RAG 知识库进行对话
     *
     * @param message 用户输入
     * @param chatId  会话ID
     * @return AI 回复
     */
    public String doChatWithRag(String message, String chatId) {
        List<Document> documents = tcmDocumentLoader.loadMarkdowns();
        if (documents.isEmpty()) {
            return "RAG 文档未找到，请检查 classpath:document/ 目录";
        }

        if (!(tcmVectorStore instanceof SimpleVectorStore)) {
            return "向量库类型不对: " + tcmVectorStore.getClass().getName();
        }
        SimpleVectorStore svs = (SimpleVectorStore) tcmVectorStore;

        try {
            svs.add(documents);
        } catch (Exception e) {
            return "Embedding API 调用失败: " + e.getMessage();
        }

        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, DEFAULT_CHAT_MEMORY_RETRIEVE_SIZE))
                // 应用 RAG 知识库问答（先检索）
                .advisors(QuestionAnswerAdvisor.builder(tcmVectorStore).build())
                // 打印请求和响应（后执行，此时请求已包含检索结果）
                .advisors(new MyLoggerAdvisor())
                .call()
                .chatResponse();
        String content = chatResponse.getResult() != null && chatResponse.getResult().getOutput() != null
                ? chatResponse.getResult().getOutput().getText()
                : "AI 回复为空";
        return content;
    }

    // ==================== 工具调用相关（暂未实现） ====================

    //    @Resource
    //    private ToolCallback[] allTools;

    /**
     * AI 问诊（支持调用工具）
     *
     * @param message 用户输入
     * @param chatId  会话ID
     * @return AI 回复
     */
    //    public String doChatWithTools(String message, String chatId) {
    //        ChatResponse chatResponse = chatClient
    //                .prompt()
    //                .user(message)
    //                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId)
    //                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, DEFAULT_CHAT_MEMORY_RETRIEVE_SIZE))
    //                .advisors(new MyLoggerAdvisor())
    //                .toolCallbacks(allTools)
    //                .call()
    //                .chatResponse();
    //        String content = chatResponse.getResult().getOutput().getText();
    //        log.info("工具调用回复: {}", content);
    //        return content;
    //    }

    // ==================== MCP 服务相关（暂未实现） ====================

    //    @Resource
    //    private ToolCallbackProvider toolCallbackProvider;

    /**
     * AI 问诊（调用 MCP 服务）
     *
     * @param message 用户输入
     * @param chatId  会话ID
     * @return AI 回复
     */
    //    public String doChatWithMcp(String message, String chatId) {
    //        ChatResponse chatResponse = chatClient
    //                .prompt()
    //                .user(message)
    //                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId)
    //                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, DEFAULT_CHAT_MEMORY_RETRIEVE_SIZE))
    //                .advisors(new MyLoggerAdvisor())
    //                .toolCallbacks(toolCallbackProvider)
    //                .call()
    //                .chatResponse();
    //        String content = chatResponse.getResult().getOutput().getText();
    //        log.info("MCP 回复: {}", content);
    //        return content;
    //    }
}

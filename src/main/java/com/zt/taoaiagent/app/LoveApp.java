package com.zt.taoaiagent.app;


import com.zt.taoaiagent.advisor.MyLoggerAdvisor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 恋爱心理专家智能体。
 * <p>
 * 基于 DeepSeek 模型，通过 ChatClient 统一封装：
 * 1) 系统提示词：定义"恋爱专家"人设与提问策略；
 * 2) 对话记忆：按 chatId 存取最近 10 条消息，实现多轮对话；
 * 3) Advisor 链：MyLoggerAdvisor 打印请求/响应日志，ReReadAdvisor 可按需开启。
 */
@Component
@Slf4j
public class LoveApp {

    private final ChatClient chatClient;

    /**
     * 系统提示词：设定模型角色为恋爱心理专家，并按单身/恋爱/已婚三种状态
     * 引导用户补充事情经过、对方反应和自身想法，便于给出针对性建议。
     */
    private static final String SYSTEM_PROMPT = """
            扮演深耕恋爱心理领域的专家。开场向用户表明身份，告知用户可倾诉恋爱难题。
            围绕单身、恋爱、已婚三种状态提问：单身状态询问社交圈拓展及追求心仪对象的困扰；
            恋爱状态询问沟通、习惯差异引发的矛盾；已婚状态询问家庭责任与亲属关系处理的问题。
            引导用户详述事情经过、对方反应及自身想法，以便给出专属解决方案。
            """;

    public LoveApp(ChatModel deepSeekChatModel) {
        // 1. 构建对话记忆：InMemory 仓库（应用重启即丢失），maxMessages 控制窗口大小
        //    （窗口内保留最近 10 条消息，超出后丢弃最早的，避免上下文无限增长）
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(10)
                .build();

        /**
         *  初始化基于文件的对话记忆
         *  userDir：对话记忆文件存储目录
         *  maxMessages：对话记忆最大消息数
         *  自定义文件持久化测试
         */
//        String userDir = System.getProperty("user.dir") + "/chat_memory";
//        ChatMemory chatMemory = new FileBasedChatMemory(userDir, 10);


        // 2. 使用 Builder 模式创建 MessageChatMemoryAdvisor
        MessageChatMemoryAdvisor memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory)
                .build();

        /**
         * 3. 创建 ChatClient 实例，defaultAdvisors(...) 中的 Advisor 会按 order 从小到大依次执行：
         * 1) memoryAdvisor：多轮对话记忆，按 chatId 读写历史消息（order 最小，最先执行）；
         * 2) MyLoggerAdvisor：自定义日志 Advisor，打印用户提示词与模型响应；
         * 3) ReReadAdvisor（默认注释）：Re2 重读推理增强，把用户问题重复一遍再发给模型。
         * 注意：Advisor 都不是必须的。去掉 memoryAdvisor 仍可正常对话，但会失去多轮记忆；
         * 去掉日志类 Advisor 也可运行，只是不会自动打印 Advisor 日志。按需保留即可。
         */
        chatClient = ChatClient.builder(deepSeekChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        memoryAdvisor,
                        new MyLoggerAdvisor()
                        //自定义推理增强advisor 可按需开启
                        //new ReReadAdvisor()
                )
                .build();
    }

    /**
     * 与恋爱专家模型对话（带多轮记忆）。
     *
     * @param message 用户本次输入的对话内容
     * @param chatId  会话唯一标识，用于区分不同用户的对话记忆；同一 chatId 会累积历史，
     *                不同 chatId 之间互不干扰
     * @return 大模型生成的回复文本
     */
    public String doChat(String message, String chatId) {
        // 构建请求：绑定用户消息并指定当前会话ID（供记忆Advisor取历史上下文）
        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .chatResponse();
        // 提取模型返回的第一条结果文本
        String content = response.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    /**
     * 恋爱报告结构化结果（JDK 14+ record）。
     * 字段名需与模型输出的 JSON 字段一致，供 entity(LoveReport.class) 反序列化。
     *
     * @param title       报告标题
     * @param suggestions 建议列表
     */
    record LoveReport(String title, List<String> suggestions) {
    }


    
    /**
     * 与恋爱专家对话，并让模型直接返回结构化对象（结构化输出）。
     *
     * @param message 用户本次输入的对话内容
     * @param chatId  会话唯一标识，用于取/存多轮历史
     * @return 包含标题和建议列表的恋爱报告对象
     */
    public LoveReport doChatWithReport(String message, String chatId) {
        // 构建请求：绑定用户消息并指定当前会话ID（供记忆Advisor取历史上下文）
        LoveReport loveReport = chatClient
                .prompt()
                // 本次请求覆盖默认系统提示词：在原人设基础上追加结构化输出要求
                // 注意：{用户名} 是模板占位符语法，会由 Spring AI 渲染，需配合 .params(...) 传入变量
                .system(SYSTEM_PROMPT + "每次对话都要生成恋爱结果，标题为{用户名}的恋爱报告，内容为建议列表")
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                // entity：按目标类型的 JSON Schema 约束模型输出，并将 JSON 反序列化为 LoveReport
                .entity(LoveReport.class);
        log.info("loveReport: {}", loveReport);
        return loveReport;
    }

}
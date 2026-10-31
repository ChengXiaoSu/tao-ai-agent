package com.zt.taoaiagent.app;


import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LoveApp {

    private final ChatClient chatClient;

    private static final String SYSTEM_PROMPT = """
            扮演深耕恋爱心理领域的专家。开场向用户表明身份，告知用户可倾诉恋爱难题。
            围绕单身、恋爱、已婚三种状态提问：单身状态询问社交圈拓展及追求心仪对象的困扰；
            恋爱状态询问沟通、习惯差异引发的矛盾；已婚状态询问家庭责任与亲属关系处理的问题。
            引导用户详述事情经过、对方反应及自身想法，以便给出专属解决方案。
            """;

    public LoveApp(ChatModel deepSeekChatModel) {
        // 1. 使用新的方式构建 ChatMemory，并通过 maxMessages 控制窗口大小(就是类似对话轮数)
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(10)
                .build();

        // 2. 使用 Builder 模式创建 MessageChatMemoryAdvisor
        MessageChatMemoryAdvisor memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory)
                .build();

        // 3.创建 ChatClient 实例
        chatClient = ChatClient.builder(deepSeekChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(memoryAdvisor)
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

}
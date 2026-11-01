package com.zt.taoaiagent.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.prompt.PromptTemplate;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * 自定义 Re2 Advisor
 * 可提高大型语言模型的推理能力
 */
@Slf4j
public class ReReadAdvisor implements CallAdvisor, StreamAdvisor {

    private static final String DEFAULT_RE2_TEMPLATE = """
            {re2_input_query}
            Read the question again: {re2_input_query}
            """;

    private final String re2Template;

    public ReReadAdvisor() {
        this(DEFAULT_RE2_TEMPLATE);
    }

    public ReReadAdvisor(String re2Template) {
        this.re2Template = re2Template;
    }

    private ChatClientRequest before(ChatClientRequest chatClientRequest) {
        // 取最后一条用户消息（配合 ChatMemory 时，历史消息在前，当前提问在最后）
        String userText = chatClientRequest.prompt().getUserMessage().getText();

        String augmentedUserText = PromptTemplate.builder()
                .template(this.re2Template)
                .variables(Map.of("re2_input_query", userText))
                .build()
                .render();

        log.debug("Re2 改写后的用户输入: {}", augmentedUserText);

        return chatClientRequest.mutate()
                .prompt(chatClientRequest.prompt().augmentUserMessage(augmentedUserText))
                .build();
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        return callAdvisorChain.nextCall(this.before(chatClientRequest));
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {
        return streamAdvisorChain.nextStream(this.before(chatClientRequest));
    }

    @Override
    public int getOrder() {
        return 10;
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }
}


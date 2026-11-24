
package com.zt.taoaiagent.advisor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.MessageType;
import reactor.core.publisher.Flux;


@Slf4j
public class MyLoggerAdvisor implements CallAdvisor, StreamAdvisor {


    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        this.logRequest(chatClientRequest);
        ChatClientResponse chatClientResponse = callAdvisorChain.nextCall(chatClientRequest);
        this.logResponse(chatClientResponse);
        return chatClientResponse;
    }


    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {
        this.logRequest(chatClientRequest);
        Flux<ChatClientResponse> chatClientResponses = streamAdvisorChain.nextStream(chatClientRequest);
        return (new ChatClientMessageAggregator()).aggregateChatClientResponse(chatClientResponses, this::logResponse);
    }


    private void logRequest(ChatClientRequest request) {
        request.prompt().getInstructions().stream()
                .filter(message -> MessageType.USER.equals(message.getMessageType()))
                .reduce((previous, current) -> current)
                .ifPresent(message -> log.info("用户提示词: {}", message.getText()));
    }

    private void logResponse(ChatClientResponse chatClientResponse) {
        // 模型返回空 choices、内容被过滤或流式异常时，chatResponse/result 可能为 null
        if (chatClientResponse == null || chatClientResponse.chatResponse() == null
                || chatClientResponse.chatResponse().getResult() == null) {
            log.warn("AI 响应为空（chatResponse 或 result 为 null）");
            return;
        }
        log.info("AI 响应内容: {}", chatClientResponse.chatResponse().getResult().getOutput().getText());
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public int getOrder() {
        return 0;
    }

}

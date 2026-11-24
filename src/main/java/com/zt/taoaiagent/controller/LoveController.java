package com.zt.taoaiagent.controller;

import com.zt.taoaiagent.app.LoveApp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * 恋爱大师智能体对外接口。
 * <p>
 * 服务上下文路径为 /api（见 application.yml），完整路径形如 /api/love/chat；
 * Knife4j 文档：/api/swagger-ui.html。
 */
@Tag(name = "恋爱大师 LoveApp")
@RestController
@RequestMapping("/love")
public class LoveController {

    private final LoveApp loveApp;

    public LoveController(LoveApp loveApp) {
        this.loveApp = loveApp;
    }

    @Operation(summary = "普通对话（带多轮记忆）")
    @PostMapping("/chat")
    public ChatResult chat(@RequestBody ChatRequest request) {
        String chatId = resolveChatId(request.chatId());
        return new ChatResult(chatId, loveApp.doChat(requireMessage(request.message()), chatId));
    }

    @Operation(summary = "本地知识库 RAG 对话（Ollama + bge-m3）")
    @PostMapping("/rag")
    public ChatResult rag(@RequestBody ChatRequest request) {
        String chatId = resolveChatId(request.chatId());
        return new ChatResult(chatId, loveApp.doChatWithRag(requireMessage(request.message()), chatId));
    }

    @Operation(summary = "百炼云知识库 RAG 对话")
    @PostMapping("/cloud-rag")
    public ChatResult cloudRag(@RequestBody ChatRequest request) {
        String chatId = resolveChatId(request.chatId());
        return new ChatResult(chatId, loveApp.doChatWithCloudRag(requireMessage(request.message()), chatId));
    }

    @Operation(summary = "结构化恋爱报告")
    @PostMapping("/report")
    public LoveApp.LoveReport report(@RequestBody ChatRequest request) {
        return loveApp.doChatWithReport(requireMessage(request.message()), resolveChatId(request.chatId()));
    }

    private static String requireMessage(String message) {
        if (message == null || message.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "message 不能为空");
        }
        return message;
    }

    private static String resolveChatId(String chatId) {
        return (chatId == null || chatId.isBlank()) ? UUID.randomUUID().toString() : chatId;
    }

    /**
     * 对话请求：chatId 为空时自动生成，同一 chatId 会累积多轮记忆。
     */
    public record ChatRequest(String message, String chatId) {
    }

    /**
     * 对话响应。
     */
    public record ChatResult(String chatId, String content) {
    }
}

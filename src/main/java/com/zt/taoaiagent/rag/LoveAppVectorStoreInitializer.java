package com.zt.taoaiagent.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * 本地知识库初始化器。
 * <p>
 * 应用启动完成后（ApplicationRunner 阶段）才执行：
 * 1) 先对 Ollama 做健康探测，未启动时跳过加载并打印明确提示；
 * 2) 探测通过后加载 Markdown 文档并向量化写入 SimpleVectorStore；
 * 3) 任一步失败都只记录日志、不阻断应用启动，运行时 RAG 请求由
 *    LoveApp#doChatWithRag 的降级逻辑兜底。
 */
@Component
@Slf4j
public class LoveAppVectorStoreInitializer implements ApplicationRunner {

    private final VectorStore loveAppVectorStore;
    private final LoveAppDocumentLoader loveAppDocumentLoader;
    private final String ollamaBaseUrl;

    public LoveAppVectorStoreInitializer(@Qualifier("loveAppVectorStore") VectorStore loveAppVectorStore,
                                         LoveAppDocumentLoader loveAppDocumentLoader,
                                         @Value("${spring.ai.ollama.base-url:http://localhost:11434}") String ollamaBaseUrl) {
        this.loveAppVectorStore = loveAppVectorStore;
        this.loveAppDocumentLoader = loveAppDocumentLoader;
        this.ollamaBaseUrl = ollamaBaseUrl;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!isOllamaAvailable()) {
            log.warn("Ollama 不可用（{}），跳过本地知识库初始化，RAG 请求将降级为普通对话", ollamaBaseUrl);
            return;
        }
        List<Document> documents = loveAppDocumentLoader.loadMarkdowns();
        if (documents.isEmpty()) {
            log.warn("未加载到任何 Markdown 文档，本地知识库为空");
            return;
        }
        try {
            loveAppVectorStore.add(documents);
            log.info("本地知识库初始化完成：{} 个文档片段", documents.size());
        } catch (Exception e) {
            log.error("本地知识库向量化失败（请确认已拉取 embedding 模型 bge-m3），RAG 请求将降级为普通对话：{}", e.getMessage());
        }
    }

    /**
     * 通过 Ollama 的 /api/tags 接口做轻量健康探测，连接超时 2s、请求超时 3s，避免阻塞启动。
     */
    private boolean isOllamaAvailable() {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(2))
                    .build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(ollamaBaseUrl + "/api/tags"))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            return response.statusCode() == 200;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Ollama 健康探测被中断");
            return false;
        } catch (IOException e) {
            log.warn("Ollama 健康探测失败：{}", e.getMessage());
            return false;
        }
    }
}

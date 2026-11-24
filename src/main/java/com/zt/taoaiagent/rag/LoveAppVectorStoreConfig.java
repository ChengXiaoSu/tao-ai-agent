package com.zt.taoaiagent.rag;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@Slf4j
public class LoveAppVectorStoreConfig {

    @Resource
    private LoveAppDocumentLoader loveAppDocumentLoader;

    @Bean
    VectorStore loveAppVectorStore(@Qualifier("ollamaEmbeddingModel") EmbeddingModel ollamaEmbeddingModel) {
        SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(ollamaEmbeddingModel)
                .build();
        // 加载文档
        List<Document> documents = loveAppDocumentLoader.loadMarkdowns();
        try {
            simpleVectorStore.add(documents);
        } catch (Exception e) {
            // Ollama 未就绪或缺少 embedding 模型时，不阻断应用启动
            log.error("向量库文档初始化失败，请确认 Ollama 已启动且已拉取 embedding 模型：{}", e.getMessage());
        }
        return simpleVectorStore;
    }
}


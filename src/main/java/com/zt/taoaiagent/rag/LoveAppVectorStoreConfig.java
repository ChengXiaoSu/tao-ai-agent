package com.zt.taoaiagent.rag;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 本地向量库配置。
 * <p>
 * 只负责创建空库：文档加载与向量化在 {@link LoveAppVectorStoreInitializer}
 * （ApplicationRunner）中执行，避免在 Bean 初始化阶段做远程 IO 拖慢整个应用启动，
 * 也便于失败时做明确降级。
 */
@Configuration
public class LoveAppVectorStoreConfig {

    @Bean
    VectorStore loveAppVectorStore(@Qualifier("ollamaEmbeddingModel") EmbeddingModel ollamaEmbeddingModel) {
        return SimpleVectorStore.builder(ollamaEmbeddingModel).build();
    }
}


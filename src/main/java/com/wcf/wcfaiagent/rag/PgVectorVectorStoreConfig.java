package com.wcf.wcfaiagent.rag;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgDistanceType.COSINE_DISTANCE;
import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgIndexType.HNSW;

// 为方便开发调试和部署，临时注释，如果需要使用 PgVector 存储知识库，取消注释即可
@Configuration
@Slf4j
public class PgVectorVectorStoreConfig {

    @Resource
    private LoveAppDocumentLoader loveAppDocumentLoader;

    @Bean
    public VectorStore pgVectorVectorStore(JdbcTemplate jdbcTemplate, EmbeddingModel dashscopeEmbeddingModel) {
        return PgVectorStore.builder(jdbcTemplate, dashscopeEmbeddingModel)
                .dimensions(1024)                    // Optional: defaults to model dimensions or 1536；剩余有免费额度的模型里，大多向量维度为1024
                .distanceType(COSINE_DISTANCE)       // Optional: defaults to COSINE_DISTANCE
                .indexType(HNSW)                     // Optional: defaults to HNSW
                .initializeSchema(true)              // Optional: defaults to false
                .schemaName("public")                // Optional: defaults to "public"
                .vectorTableName("vector_store")     // Optional: defaults to "vector_store"
                .maxDocumentBatchSize(10)            // Optional: defaults to 10000；注意：这只控制写库时的 JDBC 批次，不影响 embedding 请求的分批
                .batchingStrategy(new DashScopeBatchingStrategy()) // DashScope 单次 embedding 请求最多 10 条文本，必须用自定义分批策略
                .build();
    }

    /**
     * 应用完全启动后再加载知识库文档，原因有二：
     * 1. 表结构由 PgVectorStore 的 afterPropertiesSet()（initializeSchema=true）创建，
     *    如果在 @Bean 方法内直接 add 文档，会先于建表执行，导致表不存在而报错；
     * 2. 每次重启都会重新生成文档 id 并重复写入，先检查表里是否已有数据，有则跳过。
     */
    @Bean
    public ApplicationRunner pgVectorStoreInitializer(JdbcTemplate jdbcTemplate, VectorStore pgVectorVectorStore) {
        return args -> {
            String countSql = "SELECT COUNT(*) FROM public." + PgVectorStore.DEFAULT_TABLE_NAME;
            Integer existing = jdbcTemplate.queryForObject(countSql, Integer.class);
            if (existing != null && existing > 0) {
                log.info("PgVector 向量库已存在 {} 条文档，跳过知识库加载", existing);
                return;
            }
            List<Document> documents = loveAppDocumentLoader.loadMarkdowns();
            pgVectorVectorStore.add(documents);
            log.info("知识库文档已写入 PgVector，共 {} 条", documents.size());
        };
    }

}

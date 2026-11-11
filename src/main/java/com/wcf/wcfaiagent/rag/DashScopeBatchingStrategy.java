package com.wcf.wcfaiagent.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;

import java.util.ArrayList;
import java.util.List;

/**
 * DashScope 批处理策略。
 * <p>
 * DashScope text-embedding 接口限制：单次请求的文本条数不能超过 10 条，
 * 否则报错 HTTP 400 InvalidParameter: batch size is invalid, it should not be larger than 10。
 * <p>
 * Spring AI 默认的 {@link TokenCountBatchingStrategy} 只按 Token 数分批
 * （默认上限 7371 tokens），不限制文档条数，大量小文档会被拼进同一个请求，
 * 正是本次启动报错的根源。
 * <p>
 * 该策略在 Token 分批的基础上，再按「单批最多 10 条文档」进行二次切分。
 */
public class DashScopeBatchingStrategy extends TokenCountBatchingStrategy {

    /**
     * DashScope embedding 接口单次请求允许的最大文本条数
     */
    private static final int MAX_DOCUMENTS_PER_REQUEST = 10;

    @Override
    public List<List<Document>> batch(List<Document> documents) {
        // 先按 Token 数分批（防止单批文本总量超过模型上限）
        List<List<Document>> tokenBatches = super.batch(documents);
        // 再按文档条数二次切分，确保每个 embedding 请求不超过 DashScope 的条数限制
        List<List<Document>> batches = new ArrayList<>();
        for (List<Document> tokenBatch : tokenBatches) {
            for (int i = 0; i < tokenBatch.size(); i += MAX_DOCUMENTS_PER_REQUEST) {
                batches.add(tokenBatch.subList(i, Math.min(i + MAX_DOCUMENTS_PER_REQUEST, tokenBatch.size())));
            }
        }
        return batches;
    }

}

package tw.zipe.bastpartner.service.workflow.executor

import jakarta.enterprise.context.ApplicationScoped
import tw.zipe.bastpartner.dto.KnowledgeDTO
import tw.zipe.bastpartner.dto.workflow.config.KnowledgeRagNodeConfig
import tw.zipe.bastpartner.dto.workflow.config.NodeConfig
import tw.zipe.bastpartner.entity.WorkflowNodeEntity
import tw.zipe.bastpartner.enumerate.NodeType
import tw.zipe.bastpartner.service.EmbeddingService
import tw.zipe.bastpartner.service.workflow.ExecutionContext
import tw.zipe.bastpartner.service.workflow.NodeExecutor

/** 知識庫節點：query 插值後做向量相似度搜尋（topK / minScore / embeddingModelId 生效），輸出片段清單 */
@ApplicationScoped
class KnowledgeRagExecutor(private val embeddingService: EmbeddingService) : NodeExecutor {
    override val type = NodeType.KNOWLEDGE_RAG

    override fun execute(node: WorkflowNodeEntity, config: NodeConfig, context: ExecutionContext): Map<String, Any?> {
        val cfg = config as KnowledgeRagNodeConfig
        return run(cfg, context) { knowledgeId, query, topK, minScore, embeddingModelId ->
            embeddingService.embeddingStoreSearch(knowledgeId, query, topK, minScore, embeddingModelId)
        }
    }

    companion object {
        const val DEFAULT_TOP_K = 5
        const val DEFAULT_MIN_SCORE = 0.0

        /** 檢索邏輯本體；search 抽為參數供單元測試以 fake 驗證參數傳遞 */
        fun run(
            cfg: KnowledgeRagNodeConfig,
            context: ExecutionContext,
            search: (knowledgeId: String, query: String, topK: Int, minScore: Double, embeddingModelId: String?) -> List<KnowledgeDTO>?
        ): Map<String, Any?> {
            // pipeline 檢索模式（連一般 main 邊）需要 query；作為 LLM 外掛（連 in:tool）時本 executor
            // 不會被呼叫，query 改由 LLM 節點的問題動態帶入，故 query 於此為執行時必填而非啟用前必填。
            val queryTemplate = cfg.query?.takeIf { it.isNotBlank() }
                ?: throw IllegalArgumentException("知識庫檢索節點需要 query")
            val query = context.resolveTemplate(queryTemplate)
            val docs = search(
                cfg.knowledgeId!!,
                query,
                cfg.topK ?: DEFAULT_TOP_K,
                cfg.minScore ?: DEFAULT_MIN_SCORE,
                cfg.embeddingModelId
            )
            val texts = docs.orEmpty().map { it.content }
            return mapOf((cfg.outputKey ?: "documents") to texts)
        }
    }
}

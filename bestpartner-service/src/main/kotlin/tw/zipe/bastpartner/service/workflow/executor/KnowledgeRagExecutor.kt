package tw.zipe.bastpartner.service.workflow.executor

import jakarta.enterprise.context.ApplicationScoped
import tw.zipe.bastpartner.dto.workflow.config.KnowledgeRagNodeConfig
import tw.zipe.bastpartner.dto.workflow.config.NodeConfig
import tw.zipe.bastpartner.entity.WorkflowNodeEntity
import tw.zipe.bastpartner.enumerate.NodeType
import tw.zipe.bastpartner.service.EmbeddingService
import tw.zipe.bastpartner.service.workflow.ExecutionContext
import tw.zipe.bastpartner.service.workflow.NodeExecutor

/** 知識庫節點：query 插值後做向量相似度搜尋，輸出片段清單 */
@ApplicationScoped
class KnowledgeRagExecutor(private val embeddingService: EmbeddingService) : NodeExecutor {
    override val type = NodeType.KNOWLEDGE_RAG

    override fun execute(node: WorkflowNodeEntity, config: NodeConfig, context: ExecutionContext): Map<String, Any?> {
        val cfg = config as KnowledgeRagNodeConfig
        val query = context.resolveTemplate(cfg.query!!)
        val docs = embeddingService.embeddingStoreSearch(cfg.knowledgeId!!, query)
        val texts = docs.orEmpty().map { it.content }
        return mapOf((cfg.outputKey ?: "documents") to texts)
    }
}

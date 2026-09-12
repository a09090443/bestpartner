package tw.zipe.bastpartner.dto

import kotlinx.serialization.Serializable

/**
 * 知識庫掛載規格：作為 LLM 節點外掛（自動注入型 RAG）掛載時，承載單一知識庫的檢索參數。
 *
 * 由 workflow 的 LLM 助手節點聚合其 in:tool 埠上掛載的 KNOWLEDGE_RAG 節點而來，
 * 交 [tw.zipe.bastpartner.service.LLMService.buildAIService] 為每個知識庫建立各自的
 * ContentRetriever；多個時以 langchain4j DefaultQueryRouter 合併。
 *
 * @property knowledgeId 知識庫 ID
 * @property topK 取回筆數（對應 ContentRetriever maxResults）；null 用 langchain4j 預設
 * @property minScore 相似度下限；null 用 langchain4j 預設
 */
@Serializable
data class KnowledgeMount(
    val knowledgeId: String,
    val topK: Int? = null,
    val minScore: Double? = null
)

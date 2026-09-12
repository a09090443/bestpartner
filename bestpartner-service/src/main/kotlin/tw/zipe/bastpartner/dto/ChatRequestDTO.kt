package tw.zipe.bastpartner.dto

import kotlinx.serialization.Serializable

/**
 * @author Gary
 * @created 2024/10/07
 */
@Serializable
class ChatRequestDTO(
    val message: String? = null,
    val promptContent: String? = null,
    val memory: Memory = Memory(),
    val toolIds: List<String>? = null,
    val toolSettingIds: List<String>? = null,
    val embeddingStoreId: String? = null,
    val embeddingDocIds: List<String>? = null,
    val embeddingModelId: String? = null,
    val knowledgeId: String? = null,
    /** 多知識庫外掛（自動注入型 RAG）掛載規格；與單數 knowledgeId 併用時一併掛載並去重 */
    val knowledgeMounts: List<KnowledgeMount>? = null,
    val mcpIds: List<String>? = null,
    val mcpSettingIds: List<String>? = null,
    val files: List<String>? = null,
    val skillIds: List<String>? = null,
) : BaseDTO()

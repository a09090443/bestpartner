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
) : BaseDTO()

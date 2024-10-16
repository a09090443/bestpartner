package tw.zipe.basepartner.dto

import kotlinx.serialization.Serializable

/**
 * @author Gary
 * @created 2024/10/07
 */
@Serializable
data class ChatRequestDTO(
    val message: String,
    val promptContent: String? = null,
    val isRemember: Boolean = false,
    val memory: Memory? = null,
    val tools: List<ToolDTO>? = null
) : BaseDTO()

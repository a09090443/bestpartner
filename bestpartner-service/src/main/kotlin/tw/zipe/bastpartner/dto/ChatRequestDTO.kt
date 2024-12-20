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
    val isRemember: Boolean = false,
    val memory: Memory? = null,
    val tools: List<ToolDTO>? = null
) : BaseDTO()

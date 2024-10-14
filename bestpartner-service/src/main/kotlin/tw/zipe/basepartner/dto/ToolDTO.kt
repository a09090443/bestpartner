package tw.zipe.basepartner.dto

import kotlinx.serialization.Serializable

/**
 * @author Gary
 * @created 2024/10/14
 */
@Serializable
class ToolDTO(
    var id: String? = null,
    val name: String,
    val classPath: String? = null
)

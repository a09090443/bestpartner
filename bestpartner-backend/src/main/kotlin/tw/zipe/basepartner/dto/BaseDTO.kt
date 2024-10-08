package tw.zipe.basepartner.dto

import kotlinx.serialization.Serializable

/**
 * @author Gary
 * @created 2024/10/07
 */
@Serializable
open class BaseDTO(
    var model: String = "ollama",
    val isRemember: Boolean = false
)


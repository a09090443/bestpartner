package tw.zipe.basepartner.dto

import kotlinx.serialization.Serializable
import tw.zipe.basepartner.enumerate.Platform
import tw.zipe.basepartner.model.ChatModel

/**
 * @author Gary
 * @created 2024/10/07
 */
@Serializable
open class BaseDTO(
    val defaultPlatform: Platform = Platform.OLLAMA,
    val chatModel: ChatModel? = null,
    val isRemember: Boolean = false
)


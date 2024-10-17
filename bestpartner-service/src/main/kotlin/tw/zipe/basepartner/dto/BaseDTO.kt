package tw.zipe.basepartner.dto

import kotlinx.serialization.Serializable
import tw.zipe.basepartner.enumerate.Platform
import tw.zipe.basepartner.model.LLModel

/**
 * @author Gary
 * @created 2024/10/07
 */
@Serializable
open class BaseDTO(
    var llmId: String? = null,
    var platform: Platform = Platform.OLLAMA,
    var llmModel: LLModel? = null
)

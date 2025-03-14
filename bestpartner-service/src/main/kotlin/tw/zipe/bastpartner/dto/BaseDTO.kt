package tw.zipe.bastpartner.dto

import kotlinx.serialization.Serializable
import tw.zipe.bastpartner.enumerate.Platform
import tw.zipe.bastpartner.model.LLModel

/**
 * @author Gary
 * @created 2024/10/07
 */
@Serializable
open class BaseDTO(
    val llmId: String? = null,
    var platformId: String? = null,
    var platform: Platform? = null,
    var llmModel: LLModel = LLModel()
)

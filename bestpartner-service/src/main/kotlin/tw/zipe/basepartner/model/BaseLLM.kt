package tw.zipe.basepartner.model

import kotlinx.serialization.Serializable
import tw.zipe.basepartner.enumerate.Platform

/**
 * @author Gary
 * @created 2024/10/16
 */
@Serializable
open class BaseLLM {
    var id: String? = null
    var platform: Platform? = null
}

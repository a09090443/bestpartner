package tw.zipe.bastpartner.model

import kotlinx.serialization.Serializable
import tw.zipe.bastpartner.enumerate.Platform

/**
 * @author Gary
 * @created 2024/10/16
 */
@Serializable
open class BaseLLM {
    var id: String? = null
    var platform: Platform = Platform.OLLAMA
}

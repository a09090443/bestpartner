package tw.zipe.basepartner.dto

import io.netty.util.internal.StringUtil
import kotlinx.serialization.Serializable
import tw.zipe.basepartner.enumerate.Platform
import tw.zipe.basepartner.model.LLModel

/**
 * @author Gary
 * @created 2024/10/07
 */
@Serializable
open class BaseDTO(
    val llmId: String = StringUtil.EMPTY_STRING,
    var platform: Platform = Platform.OLLAMA,
    var llmModel: LLModel? = null
)

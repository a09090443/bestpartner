package tw.zipe.bastpartner.dto

import io.netty.util.internal.StringUtil
import kotlinx.serialization.Serializable
import tw.zipe.bastpartner.enumerate.ModelType

/**
 * @author Gary
 * @created 2024/10/16
 */
@Serializable
class LLMDTO(
    var id: String? = null,
    var alias: String = StringUtil.EMPTY_STRING,
    var modelType: ModelType = ModelType.CHAT,
) : BaseDTO()

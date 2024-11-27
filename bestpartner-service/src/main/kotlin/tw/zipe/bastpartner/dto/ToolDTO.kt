package tw.zipe.bastpartner.dto

import io.netty.util.internal.StringUtil
import kotlinx.serialization.Serializable

/**
 * @author Gary
 * @created 2024/10/14
 */
@Serializable
class ToolDTO(
    var id: String? = null,
    val name: String,
    val classPath: String = StringUtil.EMPTY_STRING,
    val settingContent: String = StringUtil.EMPTY_STRING
)

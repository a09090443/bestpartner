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
    var settingId: String? = null,
    val name: String? = null,
    val classPath: String = StringUtil.EMPTY_STRING,
    val settingFields: List<String>? = null,
    val settingContent: String = StringUtil.EMPTY_STRING
)

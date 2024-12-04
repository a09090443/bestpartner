package tw.zipe.bastpartner.dto

import io.netty.util.internal.StringUtil
import kotlinx.serialization.Serializable
import tw.zipe.bastpartner.enumerate.ToolsCategory
import tw.zipe.bastpartner.enumerate.ToolsType

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
    val group: ToolsCategory? = null,
    val type: ToolsType? = null,
    val settingFields: List<String>? = null,
    val settingContent: String = StringUtil.EMPTY_STRING,
    val description: String = StringUtil.EMPTY_STRING
)

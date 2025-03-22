package tw.zipe.bastpartner.dto

import io.netty.util.internal.StringUtil
import kotlinx.serialization.Serializable
import tw.zipe.bastpartner.enumerate.McpType

/**
 * @author Gary
 * @created 2025/3/20
 */
@Serializable
class McpDTO {
    var id: String? = StringUtil.EMPTY_STRING
    var name: String? = StringUtil.EMPTY_STRING
    var server: String? = StringUtil.EMPTY_STRING
    var command: String? = StringUtil.EMPTY_STRING
    var args: List<String>? = null
    var argsDesc: Map<String, String>? = null
    var env: Map<String, String>? = null
    var type: McpType? = null
}

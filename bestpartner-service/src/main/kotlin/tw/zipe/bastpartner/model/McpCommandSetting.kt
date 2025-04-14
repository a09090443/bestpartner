package tw.zipe.bastpartner.model

import com.fasterxml.jackson.annotation.JsonInclude
import kotlinx.serialization.Serializable

/**
 * @author Gary
 * @created 2025/3/21
 */
@Serializable
@JsonInclude(JsonInclude.Include.NON_NULL)
class McpCommandSetting {
    var command: String? = null
    var args: List<String>? = null
    var argsDesc: Map<String, String>? = null
    var env: Map<String, String>? = null
    var envDesc: Map<String, String>? = null
    var server: String? = null
}

package tw.zipe.bastpartner.dto

import kotlinx.serialization.Serializable

/**
 * @author Gary
 * @created 2024/10/30
 */
@Serializable
class PermissionDTO(
    var id: String? = null,
    var name: String? = null,
    var num: Int? = null,
    var description: String? = null
)

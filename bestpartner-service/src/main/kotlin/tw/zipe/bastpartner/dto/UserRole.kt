package tw.zipe.bastpartner.dto

import kotlinx.serialization.Serializable

/**
 * @author Gary
 * @created 2024/12/20
 */
@Serializable
data class UserRole(
    var roleNum: String? = null,
    var roleName: String? = null
)

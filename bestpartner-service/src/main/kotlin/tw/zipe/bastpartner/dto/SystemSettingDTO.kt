package tw.zipe.bastpartner.dto

import kotlinx.serialization.Serializable

/**
 * @author Gary
 * @created 2024/11/1
 */
@Serializable
class SystemSettingDTO(
    val id: Long? = null,
    val key: String,
    val value: String? = null,
    val description: String? = null
)

package tw.zipe.bastpartner.dto

import kotlinx.serialization.Serializable

/**
 * @author Gary
 * @created 2024/10/15
 */
@Serializable
class Memory(
    val id: String? = null,
    val maxSize: Int = 10
)

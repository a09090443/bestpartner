package tw.zipe.bastpartner.model

import kotlinx.serialization.Serializable

/**
 * @author Gary
 * @created 2024/10/9
 */
@Serializable
data class VectorStoreModel(
    val url: String,
    val username: String? = null,
    val password: String? = null,
    val collectionName: String? = null,
    val dimension: Int? = null
)

package tw.zipe.basepartner.model

import kotlinx.serialization.Serializable

/**
 * @author Gary
 * @created 2024/10/9
 */
@Serializable
data class VectorStoreModel(
    val url: String,
    val collectionName: String? = null,
    val dimension: Int? = null
)

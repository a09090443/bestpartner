package tw.zipe.bastpartner.model

import kotlinx.serialization.Serializable

/**
 * @author Gary
 * @created 2024/10/9
 */
@Serializable
class VectorStoreModel {
    var url: String? = null
    var username: String? = null
    var password: String? = null
    var collectionName: String? = null
    var dimension: Int? = null
    var requestLog: Boolean = false
    var responseLog: Boolean = false
}

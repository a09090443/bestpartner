package tw.zipe.bastpartner.model

import kotlinx.serialization.Serializable

/**
 * @author Gary
 * @created 2024/10/9
 */
@Serializable
class VectorStoreModel(){
    var url: String? = null
    val username: String? = null
    val password: String? = null
    var collectionName: String? = null
    var dimension: Int? = null
    val requestLog: Boolean = false
    val responseLog: Boolean = false
}

package tw.zipe.bastpartner.dto

import io.netty.util.internal.StringUtil
import kotlinx.serialization.Serializable
import tw.zipe.bastpartner.enumerate.VectorStore
import tw.zipe.bastpartner.model.VectorStoreModel

/**
 * @author Gary
 * @created 2024/10/19
 */
@Serializable
class VectorStoreDTO {
    var id: String? = null
    var alias: String = StringUtil.EMPTY_STRING
    var vectorStoreType: VectorStore? = null
    var vectorStore: VectorStoreModel = VectorStoreModel()
    var knowledgeId = StringUtil.EMPTY_STRING
    var files: List<String>? = null
}

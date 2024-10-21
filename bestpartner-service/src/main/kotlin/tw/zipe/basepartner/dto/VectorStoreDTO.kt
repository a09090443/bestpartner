package tw.zipe.basepartner.dto

import io.netty.util.internal.StringUtil
import kotlinx.serialization.Serializable
import tw.zipe.basepartner.enumerate.VectorStore
import tw.zipe.basepartner.model.VectorStoreModel

/**
 * @author Gary
 * @created 2024/10/19
 */
@Serializable
class VectorStoreDTO {
    var id: String? = null
    var alias: String = StringUtil.EMPTY_STRING
    var vectorStoreType: VectorStore = VectorStore.MILVUS
    var vectorStore: VectorStoreModel? = null
    var knowledgeId = StringUtil.EMPTY_STRING
}

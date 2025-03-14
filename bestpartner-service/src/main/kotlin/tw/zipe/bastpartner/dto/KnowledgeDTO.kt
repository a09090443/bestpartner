package tw.zipe.bastpartner.dto

import io.netty.util.internal.StringUtil
import kotlinx.serialization.Serializable

/**
 * @author Gary
 * @created 2025/3/14
 */
@Serializable
class KnowledgeDTO {
    var id: String? = null
    var vectorStoreId: String? = null
    var llmEmbeddingId: String? = null
    var name: String = StringUtil.EMPTY_STRING
    var description: String = StringUtil.EMPTY_STRING
    var filename: String = StringUtil.EMPTY_STRING
    var content: String = StringUtil.EMPTY_STRING
}

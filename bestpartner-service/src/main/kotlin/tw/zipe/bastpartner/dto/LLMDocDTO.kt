package tw.zipe.bastpartner.dto

import io.netty.util.internal.StringUtil
import kotlinx.serialization.Serializable

/**
 * @author Gary
 * @created 2025/3/9
 */
@Serializable
class LLMDocDTO {
    var knowledgeId: String = StringUtil.EMPTY_STRING
    var docId: String = StringUtil.EMPTY_STRING
    var fileName: String = StringUtil.EMPTY_STRING
    var type: String = StringUtil.EMPTY_STRING
    var alias: String = StringUtil.EMPTY_STRING
    var description: String = StringUtil.EMPTY_STRING
    var size: Long = 0
    var embeddingModelId: String = StringUtil.EMPTY_STRING
    var embeddingStoreId: String = StringUtil.EMPTY_STRING
    var maxSegmentSize: String = StringUtil.EMPTY_STRING
    var maxOverlapSize: String = StringUtil.EMPTY_STRING
}

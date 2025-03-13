package tw.zipe.bastpartner.dto

import io.netty.util.internal.StringUtil

class KnowledgeDTO {
    var id: String? = null
    var vectorStoreId: String? = null
    var llmEmbeddingId: String? = null
    var name: String = StringUtil.EMPTY_STRING
    var description: String = StringUtil.EMPTY_STRING
}

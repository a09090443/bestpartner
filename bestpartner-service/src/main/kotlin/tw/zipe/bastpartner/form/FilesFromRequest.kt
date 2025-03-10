package tw.zipe.bastpartner.form

import io.netty.util.internal.StringUtil
import jakarta.ws.rs.FormParam
import org.jboss.resteasy.reactive.multipart.FileUpload

/**
 * @author Gary
 * @created 2024/10/9
 */
class FilesFromRequest {

    @FormParam("knowledgeId")
    var knowledgeId: String? = null

    @FormParam("desc")
    var desc: String? = null

    @FormParam("fileUrl")
    var fileUrl: String? = null

    @FormParam("file")
    var files: List<FileUpload>? = null

    @FormParam("embeddingModelId")
    var embeddingModelId: String = StringUtil.EMPTY_STRING

    @FormParam("embeddingStoreId")
    var embeddingStoreId: String = StringUtil.EMPTY_STRING

    @FormParam("maxSegmentSize")
    var maxSegmentSize:Int = 300

    @FormParam("maxOverlapSize")
    var maxOverlapSize:Int = 50
}

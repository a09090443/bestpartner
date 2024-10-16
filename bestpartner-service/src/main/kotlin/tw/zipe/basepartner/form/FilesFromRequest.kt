package tw.zipe.basepartner.form

import jakarta.ws.rs.FormParam
import java.util.UUID
import org.jboss.resteasy.reactive.multipart.FileUpload
import tw.zipe.basepartner.enumerate.VectorStore

/**
 * @author Gary
 * @created 2024/10/9
 */
class FilesFromRequest {

    @FormParam("knowledgeId")
    var knowledgeId: String = UUID.randomUUID().toString()

    @FormParam("desc")
    var desc: String? = null

    @FormParam("fileUrl")
    var fileUrl: String? = null

    @FormParam("file")
    var file: List<FileUpload>? = null

    @FormParam("embeddingModelName")
    var embeddingModelName: String = ""

    @FormParam("embeddingStoreName")
    var embeddingStoreName: String = ""

    @FormParam("vectorStore")
    var vectorStore:VectorStore? = null

    @FormParam("maxSegmentSize")
    var maxSegmentSize:Int = 300

    @FormParam("maxOverlapSize")
    var maxOverlapSize:Int = 50
}

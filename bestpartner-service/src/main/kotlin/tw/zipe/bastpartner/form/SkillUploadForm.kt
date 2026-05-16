package tw.zipe.bastpartner.form

import jakarta.ws.rs.FormParam
import org.jboss.resteasy.reactive.multipart.FileUpload

/**
 * @author Gary
 * @created 2025/04/19
 */
class SkillUploadForm {

    @FormParam("file")
    var file: FileUpload? = null

    @FormParam("description")
    var description: String? = null
}

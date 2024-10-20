package tw.zipe.basepartner.resource

import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import tw.zipe.basepartner.dto.VectorStoreDTO
import tw.zipe.basepartner.form.FilesFromRequest
import tw.zipe.basepartner.service.EmbeddingService
import tw.zipe.basepartner.util.logger

/**
 * @author Gary
 * @created 2024/10/8
 */
@Path("/llm/vector")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class LLMVectorResource(
    val embeddingService: EmbeddingService
) {
    private val logger = logger()

    @POST
    @Path("/save")
    fun saveVectorStore(vectorStoreDTO: VectorStoreDTO) {
        logger.info("saveVectorStore: $vectorStoreDTO")

        embeddingService.saveVectorStore(vectorStoreDTO)

    }

    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    fun storeDocFiles(filesForm: FilesFromRequest) {
        filesForm.file?.let {
            embeddingService.embeddingDocs(
                it,
                filesForm
            )
        }
    }

}

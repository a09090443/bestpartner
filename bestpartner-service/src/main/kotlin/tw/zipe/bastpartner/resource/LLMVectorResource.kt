package tw.zipe.bastpartner.resource

import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import tw.zipe.bastpartner.dto.ApiResponse
import tw.zipe.bastpartner.dto.VectorStoreDTO
import tw.zipe.bastpartner.form.FilesFromRequest
import tw.zipe.bastpartner.service.EmbeddingService
import tw.zipe.bastpartner.util.DTOValidator
import tw.zipe.bastpartner.util.logger

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
    fun saveVectorStore(vectorStoreDTO: VectorStoreDTO): ApiResponse<String> {
        DTOValidator.validate(vectorStoreDTO) {
            validateNested("vectorStore") {
                requireNotEmpty("collectionName")
                requireNotEmpty("dimension")
            }
            throwOnInvalid()
        }
        embeddingService.saveVectorStore(vectorStoreDTO)
        return ApiResponse.success("成功儲存向量資料庫設定")
    }

    @POST
    @Path("/update")
    fun updateVectorStore(vectorStoreDTO: VectorStoreDTO): ApiResponse<String> {
        DTOValidator.validate(vectorStoreDTO) {
            requireNotEmpty("id")
            validateNested("vectorStore") {
                requireNotEmpty("collectionName")
                requireNotEmpty("dimension")
            }
            throwOnInvalid()
        }
        embeddingService.saveVectorStore(vectorStoreDTO)
        return ApiResponse.success("成功儲存向量資料庫設定")
    }

    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    fun storeDocFiles(filesForm: FilesFromRequest): ApiResponse<String> {
        filesForm.file?.let { file ->
            embeddingService.embeddingDocs(
                file,
                filesForm
            ).takeIf { ids -> ids.isNotEmpty() }.let {
                embeddingService.saveKnowledge(file, filesForm)
            }

        }
        return ApiResponse.success("成功上傳檔案")
    }

    @POST
    @Path("/deleteData")
    fun deleteVectorStoreData(vectorStoreDTO: VectorStoreDTO): ApiResponse<String> {
        embeddingService.deleteVectorStore(vectorStoreDTO)
        return ApiResponse.success("成功刪除向量資料庫設定")
    }
}

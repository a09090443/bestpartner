package tw.zipe.bastpartner.resource

import io.quarkus.security.Authenticated
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import tw.zipe.bastpartner.dto.ApiResponse
import tw.zipe.bastpartner.dto.LLMDocDTO
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
@Authenticated
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
    fun updateVectorStore(vectorStoreDTO: VectorStoreDTO): ApiResponse<Int> {
        DTOValidator.validate(vectorStoreDTO) {
            requireNotEmpty("id")
            validateNested("vectorStore") {
                requireNotEmpty("collectionName")
                requireNotEmpty("dimension")
            }
            throwOnInvalid()
        }
        val result = embeddingService.updateVectorStore(vectorStoreDTO)
        return ApiResponse.success(result)
    }

    @POST
    @Path("/getKnowledgeStore")
    fun getKnowledgeStore(llmDocDTO: LLMDocDTO): ApiResponse<List<LLMDocDTO>> {
        DTOValidator.validate(llmDocDTO) {
            requireNotEmpty("knowledgeId")
            throwOnInvalid()
        }

        val llmDocs = embeddingService.getKnowledgeStore(llmDocDTO.knowledgeId) ?: emptyList()
        return ApiResponse.success(llmDocs)
    }

    @POST
    @Path("/uploadFiles")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    fun storeDocFiles(filesForm: FilesFromRequest): ApiResponse<String> {
        DTOValidator.validate(filesForm) {
            requireNotEmpty("embeddingModelId", "embeddingStoreId")
            throwOnInvalid()
        }

        val knowledgeId = filesForm.files?.let { files ->
            embeddingService.embeddingDocs(
                files,
                filesForm
            ).let { segmentMap ->
                embeddingService.saveKnowledge(files, filesForm, segmentMap)
            }
        }
        return ApiResponse.success(knowledgeId.orEmpty())
    }

    @DELETE
    @Path("/deleteData")
    fun deleteData(llmDocDTO: LLMDocDTO): ApiResponse<String> {
        DTOValidator.validate(llmDocDTO) {
            requireNotEmpty("knowledgeId")
            throwOnInvalid()
        }
        embeddingService.deleteDocData(llmDocDTO.knowledgeId, llmDocDTO.docIds)
        return ApiResponse.success("成功刪除資料")
    }
}

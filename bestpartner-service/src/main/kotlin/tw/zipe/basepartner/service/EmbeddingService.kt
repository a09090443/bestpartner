package tw.zipe.basepartner.service

import dev.langchain4j.data.document.loader.FileSystemDocumentLoader
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser
import dev.langchain4j.data.document.splitter.DocumentSplitters
import dev.langchain4j.data.embedding.Embedding
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.store.embedding.EmbeddingStore
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import org.jboss.resteasy.reactive.multipart.FileUpload
import tw.zipe.basepartner.dto.VectorStoreDTO
import tw.zipe.basepartner.entity.VectorStoreSettingEntity
import tw.zipe.basepartner.form.FilesFromRequest
import tw.zipe.basepartner.repository.VectorStoreSettingRepository
import tw.zipe.basepartner.util.logger

/**
 * @author Gary
 * @created 2024/10/9
 */
@ApplicationScoped
class EmbeddingService(
//    @Named("embeddingModelMap") val embeddingModelMap: Map<String, EmbeddingModel>,
//    @Named("vectorStoreMap") val vectorStoreMap: Map<String, EmbeddingStore<TextSegment>>,
    val llmService: LLMService,
    val vectorStoreSettingRepository: VectorStoreSettingRepository
) {

    private val logger = logger()

    /**
     * 儲存向量資料庫設定
     */
    @Transactional
    fun saveVectorStore(vectorStoreDTO: VectorStoreDTO) {
        val vectorStoreSettingEntity = VectorStoreSettingEntity()
        vectorStoreSettingEntity.alias = vectorStoreDTO.alias
        vectorStoreSettingEntity.type = vectorStoreDTO.vectorStoreType
        vectorStoreSettingEntity.vectorSetting = vectorStoreDTO.vectorStore
        vectorStoreSettingRepository.persist(vectorStoreSettingEntity)
    }

    fun getVectorStoreSetting(id: String): VectorStoreDTO? {
        val vectorStoreSettingEntity = vectorStoreSettingRepository.findById(id)
        return vectorStoreSettingEntity?.let {
            val vectorStoreDTO = VectorStoreDTO()
            vectorStoreDTO.id = it.id
            vectorStoreDTO.vectorStoreType = it.type
            vectorStoreDTO.alias = it.alias
            vectorStoreDTO.vectorStore = it.vectorSetting
            return vectorStoreDTO
        }
    }

    fun buildVectorStore(id: String): EmbeddingStore<TextSegment>? {
        val vectorStoreSettingEntity = vectorStoreSettingRepository.findById(id)
        return vectorStoreSettingEntity?.let {
            it.type.getVactorStore().embeddingStore(it.vectorSetting)
        }
    }

    /**
     * 分割文件並儲存至向量資料庫
     */
    fun embeddingDocs(
        files: List<FileUpload>,
        filesForm: FilesFromRequest
    ): List<String> {

        val documents = files.map { file ->
            val document = FileSystemDocumentLoader.loadDocument(file.uploadedFile(), ApacheTikaDocumentParser())
            document.metadata().put("knowledgeId", filesForm.knowledgeId).put("docsName", file.fileName())
            document
        }

        val splitter = DocumentSplitters.recursive(filesForm.maxSegmentSize, filesForm.maxOverlapSize)
        val segments = splitter.splitAll(documents)
        val embeddingModel = llmService.buildLLM(filesForm.embeddingModelId).let { it as EmbeddingModel }
        val embeddings: List<Embedding> = embeddingModel.embedAll(segments)?.content() ?: emptyList()
        val embeddingStore = this.buildVectorStore(filesForm.embeddingStoreId)
        val ids = embeddingStore?.addAll(embeddings, segments)

        logger.info("embeddingDocs: ids = $ids")
        return ids ?: emptyList()
    }
}

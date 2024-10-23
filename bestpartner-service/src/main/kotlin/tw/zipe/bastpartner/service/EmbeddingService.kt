package tw.zipe.bastpartner.service

import dev.langchain4j.data.document.loader.FileSystemDocumentLoader
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser
import dev.langchain4j.data.document.splitter.DocumentSplitters
import dev.langchain4j.data.embedding.Embedding
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.store.embedding.EmbeddingStore
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder
import io.netty.util.internal.StringUtil
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import org.jboss.resteasy.reactive.multipart.FileUpload
import tw.zipe.bastpartner.dto.VectorStoreDTO
import tw.zipe.bastpartner.entity.LLMDocEntity
import tw.zipe.bastpartner.entity.VectorStoreSettingEntity
import tw.zipe.bastpartner.enumerate.ModelType
import tw.zipe.bastpartner.form.FilesFromRequest
import tw.zipe.bastpartner.repository.LLMDocRepository
import tw.zipe.bastpartner.repository.VectorStoreSettingRepository
import tw.zipe.bastpartner.util.DTOValidator
import tw.zipe.bastpartner.util.logger

/**
 * @author Gary
 * @created 2024/10/9
 */
@ApplicationScoped
class EmbeddingService(
    val llmService: LLMService,
    val vectorStoreSettingRepository: VectorStoreSettingRepository,
    val llmDocRepository: LLMDocRepository
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
        vectorStoreSettingEntity.vectorSetting = vectorStoreDTO.vectorStore!!
        vectorStoreSettingRepository.persist(vectorStoreSettingEntity)
    }

    /**
     * 取得向量資料庫設定
     */
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

    /**
     * 建立向量資料庫
     */
    fun buildVectorStore(id: String): EmbeddingStore<TextSegment> {
        val vectorStoreSettingEntity = vectorStoreSettingRepository.findById(id)
        return vectorStoreSettingEntity?.let {
            it.type.getVactorStore().embeddingStore(it.vectorSetting)
        } ?: throw IllegalArgumentException("vectorStoreId is required")
    }

    /**
     * 分割文件並儲存至向量資料庫
     */
    fun embeddingDocs(
        files: List<FileUpload>,
        filesForm: FilesFromRequest
    ): List<String> {
        val embeddingStore = this.buildVectorStore(filesForm.embeddingStoreId)
        val embeddingModel =
            llmService.buildLLM(filesForm.embeddingModelId, ModelType.EMBEDDING).let { it as EmbeddingModel }

        val documents = files.map { file ->
            val document = FileSystemDocumentLoader.loadDocument(file.uploadedFile(), ApacheTikaDocumentParser())
            document.metadata().put("knowledgeId", filesForm.knowledgeId).put("docsName", file.fileName())
            document
        }

        val splitter = DocumentSplitters.recursive(filesForm.maxSegmentSize, filesForm.maxOverlapSize)
        val segments = splitter.splitAll(documents)
        val embeddings: List<Embedding> = embeddingModel.embedAll(segments)?.content() ?: return emptyList()
        val ids = embeddingStore.addAll(embeddings, segments)

        logger.info("embeddingDocs: ids = $ids")
        return ids
    }

    /**
     * 將文件資訊存入知識庫中
     */
    @Transactional
    fun saveKnowledge(files: List<FileUpload>, filesForm: FilesFromRequest) {
        val docs = files.map {
            val llmDocEntity = LLMDocEntity()
            with(llmDocEntity) {
                knowledgeId = filesForm.knowledgeId
                vectorStoreId = filesForm.embeddingStoreId
                name = it.fileName()
                description = filesForm.desc ?: StringUtil.EMPTY_STRING
                type = it.fileName().split(".").last()
                url = filesForm.fileUrl ?: StringUtil.EMPTY_STRING
                size = it.size()
                vectorStoreId = filesForm.embeddingStoreId
            }
            llmDocEntity
        }.toList()
        llmDocRepository.persist(docs)
    }

    /**
     * 將文件資訊從資料庫中刪除
     */
    @Transactional
    fun deleteKnowledge(knowledgeId: String, files: List<String>?) {
        files?.map { file ->
            llmDocRepository.deleteByKnowledgeIdAndFileName(knowledgeId, file)
        } ?: llmDocRepository.deleteByKnowledgeIdAndFileName(knowledgeId, null)
    }

    /**
     * 刪除向量資料
     */
    fun deleteVectorStore(vectorStoreDTO: VectorStoreDTO) {
        DTOValidator.validate(vectorStoreDTO) {
            requireNotEmpty("id")
            requireNotEmpty("knowledgeId")
            throwOnInvalid()
        }
        val embeddingStore = this.buildVectorStore(vectorStoreDTO.id!!)
        val filter = MetadataFilterBuilder.metadataKey("knowledgeId").isEqualTo(vectorStoreDTO.knowledgeId).let { filter ->
            vectorStoreDTO.files?.let {
                filter.and(MetadataFilterBuilder.metadataKey("docsName").isIn(it))
            }
        }
//        vectorStoreDTO.files?.let {
//            filter.and(MetadataFilterBuilder.metadataKey("docsName").isIn(it))
//        }
        embeddingStore.removeAll(filter)
    }
}

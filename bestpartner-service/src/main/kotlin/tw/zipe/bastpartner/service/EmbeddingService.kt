package tw.zipe.bastpartner.service

import dev.langchain4j.data.document.loader.FileSystemDocumentLoader
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser
import dev.langchain4j.data.document.splitter.DocumentSplitters
import dev.langchain4j.data.embedding.Embedding
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.rag.DefaultRetrievalAugmentor
import dev.langchain4j.rag.RetrievalAugmentor
import dev.langchain4j.rag.content.retriever.ContentRetriever
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever
import dev.langchain4j.rag.query.transformer.CompressingQueryTransformer
import dev.langchain4j.store.embedding.EmbeddingStore
import io.netty.util.internal.StringUtil
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import org.jboss.resteasy.reactive.multipart.FileUpload
import tw.zipe.bastpartner.config.security.SecurityValidator
import tw.zipe.bastpartner.constant.KNOWLEDGE
import tw.zipe.bastpartner.dto.KnowledgeDTO
import tw.zipe.bastpartner.dto.LLMDocDTO
import tw.zipe.bastpartner.dto.VectorStoreDTO
import tw.zipe.bastpartner.entity.LLMDocEntity
import tw.zipe.bastpartner.entity.LLMDocSliceEntity
import tw.zipe.bastpartner.entity.LLMKnowledgeEntity
import tw.zipe.bastpartner.entity.VectorStoreSettingEntity
import tw.zipe.bastpartner.enumerate.ModelType
import tw.zipe.bastpartner.exception.ServiceException
import tw.zipe.bastpartner.form.FilesFromRequest
import tw.zipe.bastpartner.repository.LLMDocRepository
import tw.zipe.bastpartner.repository.LLMDocSliceRepository
import tw.zipe.bastpartner.repository.LLMKnowledgeRepository
import tw.zipe.bastpartner.repository.VectorStoreSettingRepository

/**
 * @author Gary
 * @created 2024/10/9
 */
@ApplicationScoped
class EmbeddingService(
    private val llmService: LLMService,
    private val vectorStoreSettingRepository: VectorStoreSettingRepository,
    private val llmDocRepository: LLMDocRepository,
    private val llmDocSliceRepository: LLMDocSliceRepository,
    private val llmKnowledgeRepository: LLMKnowledgeRepository,
    private val securityValidator: SecurityValidator
) {

    /**
     * 儲存向量資料庫設定
     */
    fun saveVectorStore(vectorStoreDTO: VectorStoreDTO) {
        with(VectorStoreSettingEntity()) {
            userId = securityValidator.validateLoggedInUser()
            alias = vectorStoreDTO.alias
            type = vectorStoreDTO.vectorStoreType
            vectorSetting = vectorStoreDTO.vectorStore
            vectorStoreSettingRepository.saveOrUpdate(this).also { vectorStoreDTO.id = this.id }
        }
    }

    /**
     * 更新向量資料庫設定
     */
    fun updateVectorStore(vectorStoreDTO: VectorStoreDTO): Int {
        return mapOf(
            "id" to vectorStoreDTO.id.orEmpty(),
            "alias" to vectorStoreDTO.alias,
            "type" to vectorStoreDTO.vectorStoreType?.name.orEmpty(),
            "vectorSetting" to vectorStoreDTO.vectorStore
        ).let {
            vectorStoreSettingRepository.updateSetting(it)
        }
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
            it.type?.getVectorStore()?.embeddingStore(it.vectorSetting)
        } ?: throw ServiceException("無向量資料庫設定資料: id = $id")
    }

    /**
     * 分割文件並儲存至向量資料庫，並回傳文件 ID
     */
    fun embeddingDocs(
        files: List<FileUpload>,
        filesForm: FilesFromRequest
    ): Map<String, Map<String, String>> {
        files.forEach {
            llmDocRepository.findByKnowledgeIdAndName(filesForm.knowledgeId, it.fileName())?.run {
                throw ServiceException("檔案名稱: ${it.fileName()} 已存在")
            }
        }

        val embeddingStore = this.buildVectorStore(filesForm.embeddingStoreId)
        val embeddingModel =
            llmService.buildLLM(filesForm.embeddingModelId, ModelType.EMBEDDING).let { it as EmbeddingModel }
        val filenameToSegmentsMap = mutableMapOf<String, MutableMap<String, String>>()

        files.forEach { file ->
            val document = FileSystemDocumentLoader.loadDocument(file.uploadedFile(), ApacheTikaDocumentParser())
            document.metadata()
                .put(KNOWLEDGE, filesForm.knowledgeId)
                .put("docsName", file.fileName())

            val splitter = DocumentSplitters.recursive(filesForm.maxSegmentSize, filesForm.maxOverlapSize)
            val fileSegments = splitter.split(document).toList()
            val embeddings: List<Embedding> = embeddingModel.embedAll(fileSegments).content()
            val segmentIds: List<String> = embeddingStore.addAll(embeddings, fileSegments).toList()

            val segmentMap = mutableMapOf<String, String>()
            var currentIndex = 0
            fileSegments.indices.forEach { i ->
                val id = segmentIds[currentIndex]
                val segment = fileSegments[i]
                segmentMap[id] = segment.text()
                currentIndex++
            }
            filenameToSegmentsMap[file.fileName()] = segmentMap
        }

        return filenameToSegmentsMap
    }

    /**
     * 將文件資訊存入知識庫中
     */
    @Transactional
    fun saveKnowledge(
        files: List<FileUpload>,
        filesForm: FilesFromRequest,
        segmentMap: Map<String, Map<String, String>> = emptyMap()
    ): String {
        val docs = files.map {
            with(LLMDocEntity()) {
                knowledgeId = filesForm.knowledgeId
                name = it.fileName()
                description = filesForm.desc ?: StringUtil.EMPTY_STRING
                type = it.fileName().split(".").last()
                url = filesForm.fileUrl ?: StringUtil.EMPTY_STRING
                size = it.size()
                this
            }
        }.toList()

        try {
            with(LLMKnowledgeEntity()) {
                id = filesForm.knowledgeId
                userId = securityValidator.validateLoggedInUser()
                name = filesForm.name.orEmpty()
                description = filesForm.desc.orEmpty()
                vectorStoreId = filesForm.embeddingStoreId
                llmEmbeddingId = filesForm.embeddingModelId
                this
            }.run {
                llmKnowledgeRepository.saveOrUpdate(this)
            }

            docs.forEach {
                llmDocRepository.saveOrUpdate(it)
                segmentMap[it.name]?.let { map ->
                    saveDocSliceIds(it, map)
                }
            }
        } catch (e: Exception) {
            docs.forEach {
                segmentMap[it.name]?.let { map ->
                    this.buildVectorStore(filesForm.embeddingStoreId).removeAll(map.keys.toList())
                }
            }
            throw ServiceException("儲存文件資訊失敗")
        }

        return filesForm.knowledgeId

    }

    /**
     * 將文件片段存入文件切片表
     */
    fun saveDocSliceIds(llmDoc: LLMDocEntity, segmentMap: Map<String, String>) {
        val docs = segmentMap.map { (key, value) ->
            with(LLMDocSliceEntity()) {
                id = key
                content = value
                docId = llmDoc.id ?: StringUtil.EMPTY_STRING
                knowledgeId = llmDoc.knowledgeId
                this
            }
        }.toList()
        llmDocSliceRepository.saveEntities(docs)
    }

    /**
     * 將文件資訊從資料庫中刪除
     */
    @Transactional
    fun deleteDocData(knowledgeId: String, docIds: List<String>) {
        llmKnowledgeRepository.findById(knowledgeId)?.let {
            val embeddingStore = this.buildVectorStore(it.vectorStoreId)

            docIds.forEach { docId ->
                llmDocRepository.deleteById(docId)
                val idList = llmDocSliceRepository.findByKnowledgeIdAndDocId(knowledgeId, docId)
                    ?.map { docSlice -> docSlice.id }
                    ?.toList()
                llmDocSliceRepository.deleteByKnowledgeIdAndDocId(knowledgeId, docId)
                if (!idList.isNullOrEmpty()) {
                    embeddingStore.removeAll(idList)
                }
            }

            docIds.isEmpty().let {
                llmDocRepository.findByKnowledgeId(securityValidator.validateLoggedInUser(), knowledgeId).forEach { llmDoc ->
                    run {
                        val idList =
                            llmDocSliceRepository.findByKnowledgeIdAndDocId(llmDoc.knowledgeId.orEmpty(), llmDoc.docId)
                                ?.map { docSlice -> docSlice.id }
                                ?.toList()
                        llmDocSliceRepository.deleteByKnowledgeIdAndDocId(knowledgeId, llmDoc.docId)
                        llmDocRepository.deleteById(llmDoc.docId)
                        llmKnowledgeRepository.deleteById(knowledgeId)
                        if (!idList.isNullOrEmpty()) {
                            embeddingStore.removeAll(idList)
                        }
                    }
                }
            }
        }
    }

    /**
     * 建立檢索增強器
     */
    fun buildRetrievalAugmentor(
        embeddingDocIds: List<String>,
        embeddingStoreId: String,
        embeddingModelId: String,
        chatModel: ChatLanguageModel
    ): RetrievalAugmentor {

        val embeddingStore = this.buildVectorStore(embeddingStoreId)

        val embeddingModel = llmService.buildLLM(embeddingModelId, ModelType.EMBEDDING).let { it as EmbeddingModel }

        val contentRetriever: ContentRetriever = EmbeddingStoreContentRetriever.builder()
            .embeddingStore(embeddingStore)
            .embeddingModel(embeddingModel)
//            .maxResults(3)
//            .minScore(0.6)
            .build()

        return DefaultRetrievalAugmentor
            .builder()
            .queryTransformer(CompressingQueryTransformer(chatModel))
            .contentRetriever(contentRetriever)
            .build()
    }

    /**
     * 取得知識庫和文件資料
     */
    fun getKnowledgeStore(knowledgeId: String?): List<LLMDocDTO> = llmDocRepository.findByKnowledgeId(securityValidator.validateLoggedInUser(), knowledgeId)

    /**
     * 取得知識庫資料
     */
    fun getKnowledge(knowledgeId: String): KnowledgeDTO? {
        return llmKnowledgeRepository.findById(knowledgeId)?.let { knowledgeId ->
            with(KnowledgeDTO()) {
                id = knowledgeId.id
                vectorStoreId = knowledgeId.vectorStoreId
                llmEmbeddingId = knowledgeId.llmEmbeddingId
                name = knowledgeId.name
                description = knowledgeId.description
                this
            }

        }
    }
}

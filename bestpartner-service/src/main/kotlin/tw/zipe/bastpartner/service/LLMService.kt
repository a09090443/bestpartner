package tw.zipe.bastpartner.service

import com.fasterxml.jackson.databind.ObjectMapper
import dev.langchain4j.agent.tool.ToolSpecification
import dev.langchain4j.memory.chat.ChatMemoryProvider
import dev.langchain4j.memory.chat.MessageWindowChatMemory
import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.chat.StreamingChatLanguageModel
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever
import dev.langchain4j.rag.query.Query
import dev.langchain4j.service.AiServices
import dev.langchain4j.service.tool.ToolExecutor
import dev.langchain4j.store.embedding.filter.Filter
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import me.kpavlov.langchain4j.kotlin.service.SystemMessageProvider
import me.kpavlov.langchain4j.kotlin.service.systemMessageProvider
import tw.zipe.bastpartner.assistant.DynamicAssistant
import tw.zipe.bastpartner.config.PersistentChatMemoryStore
import tw.zipe.bastpartner.config.security.SecurityValidator
import tw.zipe.bastpartner.dto.ChatRequestDTO
import tw.zipe.bastpartner.dto.LLMDTO
import tw.zipe.bastpartner.dto.PlatformDTO
import tw.zipe.bastpartner.entity.LLMPlatformEntity
import tw.zipe.bastpartner.entity.LLMSettingEntity
import tw.zipe.bastpartner.enumerate.ModelType
import tw.zipe.bastpartner.enumerate.Platform
import tw.zipe.bastpartner.exception.ServiceException
import tw.zipe.bastpartner.model.LLModel
import tw.zipe.bastpartner.repository.LLMPlatformRepository
import tw.zipe.bastpartner.repository.LLMSettingRepository
import tw.zipe.bastpartner.util.DTOValidator
import tw.zipe.bastpartner.util.LLMBuilder

/**
 * @author Gary
 * @created 2024/10/16
 */
@ApplicationScoped
class LLMService(
    private val llmSettingRepository: LLMSettingRepository,
    private val llmPlatformRepository: LLMPlatformRepository,
    private val securityValidator: SecurityValidator,
    private val toolService: ToolService,
    private val embeddingService: EmbeddingService,
    private val objectMapper: ObjectMapper,
) {

    /**
     * 儲存 LLM 設定
     */
    fun saveLLMSetting(llmDTO: LLMDTO) {
        val platform = llmPlatformRepository.findById(llmDTO.platformId.orEmpty())
            ?: throw ServiceException("請確認存取的平台是否存在")
        llmDTO.llmModel.platform = platform.name

        with(LLMSettingEntity()) {
            userId = securityValidator.validateLoggedInUser()
            platformId = llmDTO.platformId.orEmpty()
            type = llmDTO.modelType
            alias = llmDTO.alias
            modelSetting = llmDTO.llmModel
            llmSettingRepository.saveOrUpdate(this).also { llmDTO.id = this.id }
        }
    }

    /**
     * 取得 LLM 設定
     */
    fun getLLMSetting(llmId: String): LLMDTO? {
        return llmSettingRepository.findById(llmId)?.let { llmSetting ->
            LLMDTO().apply {
                id = llmSetting.id
                alias = llmSetting.alias
                platformId = llmSetting.platformId
                modelType = llmSetting.type ?: ModelType.CHAT
                llmModel = llmSetting.modelSetting
            }
        }
    }

    /**
     * 取得 LLM 設定
     */
    fun getLLMSetting(userId: String, platformId: String?, platformName: String?, llmId: String?): List<LLMDTO?> {
        return llmSettingRepository.findByConditions(userId, platformId, platformName, llmId)
            .map { llmSetting ->
                LLMDTO().apply {
                    id = llmSetting.id
                    alias = llmSetting.alias
                    modelType = ModelType.valueOf(llmSetting.type)
                    llmModel = objectMapper.readValue(llmSetting.modelSetting, LLModel::class.java)
                    platform = Platform.valueOf(llmSetting.platformName)
                }
            }.toList()
    }

    /**
     * 更新 LLM 設定
     */
    fun updateLLMSetting(llmDTO: LLMDTO) {
        mapOf(
            "alias" to llmDTO.alias,
            "platformId" to llmDTO.platformId.orEmpty(),
            "type" to llmDTO.modelType?.name.orEmpty(),
            "modelSetting" to llmDTO.llmModel,
            "id" to llmDTO.id.orEmpty()
        ).let {
            llmSettingRepository.updateSetting(it)
        }
    }

    /**
     * 刪除 LLM 設定
     */
    @Transactional
    fun deleteLLMSetting(id: String) = llmSettingRepository.deleteById(id)

    /**
     * 建立 LLM
     */
    fun buildLLM(id: String, type: ModelType): Any {
        val llmSetting =
            llmSettingRepository.findByConditions(securityValidator.validateLoggedInUser(), null, null, id)
                .firstOrNull()

        return llmSetting?.let { setting ->

            if (setting.type != type.name) {
                throw ServiceException("請確認存取的 LLM 設定是否為 $type")
            }
            LLMBuilder().build(setting, type)
        } ?: throw ServiceException("請確認存取的 LLM 設定是否存在")
    }

    /**
     * 新增平台
     */
    fun addPlatform(platformDTO: PlatformDTO) {
        with(LLMPlatformEntity()) {
            name = platformDTO.platform
            llmPlatformRepository.saveOrUpdate(this).also { platformDTO.id = this.id }
        }
    }

    /**
     * 刪除平台
     */
    @Transactional
    fun deletePlatform(id: String) = llmPlatformRepository.deleteById(id)

    /**
     * 建立 AIService
     */
    fun buildAIService(chatRequestDTO: ChatRequestDTO, modelType: ModelType): AiServices<DynamicAssistant> {
        DTOValidator.validate(chatRequestDTO) {
            requireNotEmpty("llmId", "message", "promptContent")
            validateNested("memory") {
                requireNotEmpty("id")
            }
            throwOnInvalid()
        }

        val aiService = AiServices.builder(DynamicAssistant::class.java).systemMessageProvider(
            object : SystemMessageProvider {
                override fun getSystemMessage(chatMemoryID: Any): String =
                    chatRequestDTO.promptContent.orEmpty()
            })

        val llm = buildLLM(chatRequestDTO.llmId.orEmpty(), modelType).let { llm ->
            when (llm){
                is ChatLanguageModel -> {
                    aiService.chatLanguageModel(llm)
                }
                is StreamingChatLanguageModel -> {
                    aiService.streamingChatLanguageModel(llm)
                }
                else -> throw ServiceException("LLM 類型錯誤")
            }
            llm
        }

        if (llm is ChatLanguageModel) {
            DTOValidator.validate(chatRequestDTO) {
                requireNotEmpty("embeddingDocIds", "embeddingStoreId", "embeddingModelId")
                throwOnInvalid()
            }

            embeddingService.buildRetrievalAugmentor(
                chatRequestDTO.embeddingDocIds.orEmpty(),
                chatRequestDTO.embeddingStoreId.orEmpty(),
                chatRequestDTO.embeddingModelId.orEmpty(),
                llm
            ).let { aiService.retrievalAugmentor(it) }
        }

        val tools: MutableList<Any?> = mutableListOf()

        chatRequestDTO.toolIds?.map {
            toolService.buildToolWithoutSetting(it)?.let { tool -> tools.add(tool) }
        }

        chatRequestDTO.toolSettingIds?.map {
            toolService.buildToolWithSetting(it)?.let { tool -> tools.add(tool) }
        }

        @Suppress("UNCHECKED_CAST")
        tools.map { tool ->
            when (tool) {
                is Map<*, *> -> aiService.tools(tool as Map<ToolSpecification, ToolExecutor>)
                else -> aiService.tools(tool)
            }
        }
        chatRequestDTO.knowledgeId?.let {
            val filter: (Query) -> Filter = { _ ->
                MetadataFilterBuilder.metadataKey("KNOWLEDGE").isIn(listOf(it))
            }
            val embeddingStore = embeddingService.getKnowledgeStore(it)
//            val contentRetriever = EmbeddingStoreContentRetriever.builder()
//                .embeddingStore(embeddingService.buildVectorStore(embeddingStore))
//                .embeddingModel(embeddingService.buildLLM(chatRequestDTO.embeddingModelId.orEmpty(),
//                    ModelType.EMBEDDING) as dev.langchain4j.model.embedding.EmbeddingModel)
//                .dynamicFilter { filter(it) } // Pass the Kotlin function as a Java Function using SAM conversion
//                .build()

//            val llmDocDTO = embeddingService.getKnowledgeStore(it)?.first()

        }

        val chatMemoryProvider = chatRequestDTO.memory.let {
            ChatMemoryProvider { _: Any? ->
                MessageWindowChatMemory.builder()
                    .id(it.id)
                    .maxMessages(it.maxSize)
                    .chatMemoryStore(PersistentChatMemoryStore())
                    .build()
            }
        }
        aiService.chatMemoryProvider(chatMemoryProvider)
        return aiService
    }
}

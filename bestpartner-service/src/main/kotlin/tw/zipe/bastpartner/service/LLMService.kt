package tw.zipe.bastpartner.service

import com.fasterxml.jackson.databind.ObjectMapper
import dev.langchain4j.agent.tool.ToolSpecification
import dev.langchain4j.data.message.SystemMessage
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.memory.chat.ChatMemoryProvider
import dev.langchain4j.memory.chat.MessageWindowChatMemory
import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.model.chat.StreamingChatModel
import dev.langchain4j.model.chat.request.ChatRequest
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.rag.DefaultRetrievalAugmentor
import dev.langchain4j.rag.content.retriever.ContentRetriever
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever
import dev.langchain4j.mcp.McpToolProvider
import dev.langchain4j.mcp.client.McpClient
import dev.langchain4j.rag.query.Query
import dev.langchain4j.rag.query.router.DefaultQueryRouter
import dev.langchain4j.service.AiServices
import dev.langchain4j.service.tool.ToolExecutor
import dev.langchain4j.service.tool.ToolProvider
import dev.langchain4j.store.embedding.filter.Filter
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.jboss.resteasy.reactive.multipart.FileUpload
import tw.zipe.bastpartner.assistant.DynamicAssistant
import tw.zipe.bastpartner.config.PersistentChatMemoryStore
import tw.zipe.bastpartner.config.security.SecurityValidator
import tw.zipe.bastpartner.constant.KNOWLEDGE
import tw.zipe.bastpartner.dto.ChatRequestDTO
import tw.zipe.bastpartner.dto.KnowledgeMount
import tw.zipe.bastpartner.dto.LLMDTO
import tw.zipe.bastpartner.dto.PlatformDTO
import tw.zipe.bastpartner.entity.LLMPlatformEntity
import tw.zipe.bastpartner.entity.LLMSettingEntity
import tw.zipe.bastpartner.enumerate.FileType
import tw.zipe.bastpartner.enumerate.ModelType
import tw.zipe.bastpartner.enumerate.Platform
import tw.zipe.bastpartner.enumerate.AppMessage
import tw.zipe.bastpartner.exception.ServiceException
import tw.zipe.bastpartner.model.LLModel
import tw.zipe.bastpartner.provider.CompositeToolProvider
import tw.zipe.bastpartner.repository.LLMPlatformRepository
import tw.zipe.bastpartner.repository.LLMSettingRepository
import tw.zipe.bastpartner.converter.PasswordEncryptConverter
import tw.zipe.bastpartner.converter.SensitiveValueCodec
import tw.zipe.bastpartner.util.DTOValidator
import tw.zipe.bastpartner.util.LLMBuilder
import tw.zipe.bastpartner.util.logger

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
    private val skillService: SkillService,
    private val objectMapper: ObjectMapper,
    private val passwordEncryptConverter: PasswordEncryptConverter,
    @ConfigProperty(name = "file.upload.dir") private val fileUploadDir: String
) {

    private val logger = logger()

    /**
     * 儲存 LLM 設定
     */
    fun saveLLMSetting(llmDTO: LLMDTO) {
        val platform = llmPlatformRepository.findById(llmDTO.platformId.orEmpty())
            ?: throw ServiceException(AppMessage.LLM_PLATFORM_NOT_FOUND)
        llmDTO.llmModel.platform = platform.name

        val rawApiKey = llmDTO.llmModel.apiKey
        llmDTO.llmModel.apiKey = null  // 避免明文寫入 JSON

        with(LLMSettingEntity()) {
            userId = securityValidator.validateLoggedInUser()
            platformId = llmDTO.platformId.orEmpty()
            type = llmDTO.modelType
            alias = llmDTO.alias
            modelSetting = llmDTO.llmModel
            apiKey = rawApiKey              // JPA @Convert 自動加密
            llmSettingRepository.saveOrUpdate(this).also { llmDTO.id = this.id }
        }

        llmDTO.llmModel.apiKey = rawApiKey  // 還原，讓 caller 拿到完整 DTO
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
                llmModel = llmSetting.modelSetting.also {
                    it.apiKey = llmSetting.apiKey  // JPA @Convert 已解密
                }
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
                    platform = Platform.valueOf(llmSetting.platformName)
                    llmModel = objectMapper.readValue(llmSetting.modelSetting, LLModel::class.java).also { model ->
                        // 優先使用獨立欄位（解密後），fallback 讀取 JSON 內舊值（相容舊資料）
                        val decryptedApiKey = llmSetting.apiKey
                            .takeIf { it.isNotEmpty() }
                            ?.let { passwordEncryptConverter.convertToEntityAttribute(it) }
                        model.apiKey = decryptedApiKey ?: model.apiKey
                    }
                }
            }.toList()
    }

    /**
     * 更新 LLM 設定
     */
    fun updateLLMSetting(llmDTO: LLMDTO) {
        val incomingApiKey = llmDTO.llmModel.apiKey
        llmDTO.llmModel.apiKey = null  // 避免明文寫入 JSON

        // 值為遮罩代表使用者未更動金鑰（/get 回應已遮罩），沿用既有值；
        // 否則「讀取 → 原樣存回」會把真實金鑰覆寫成遮罩字串而毀損設定
        val rawApiKey = if (incomingApiKey == SensitiveValueCodec.SECRET_MASK) {
            llmSettingRepository.findById(llmDTO.id.orEmpty())?.apiKey  // JPA @Convert 已解密
        } else {
            incomingApiKey
        }

        val encryptedApiKey = passwordEncryptConverter.convertToDatabaseColumn(rawApiKey).orEmpty()

        mapOf(
            "alias" to llmDTO.alias,
            "platformId" to llmDTO.platformId.orEmpty(),
            "type" to llmDTO.modelType?.name.orEmpty(),
            "modelSetting" to llmDTO.llmModel,
            "apiKey" to encryptedApiKey,        // raw SQL 不觸發 JPA @Convert，手動加密
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
                throw ServiceException(AppMessage.LLM_SETTING_TYPE_MISMATCH, type)
            }
            val llmModel = objectMapper.readValue(setting.modelSetting, LLModel::class.java)
            val decryptedApiKey = setting.apiKey
                .takeIf { it.isNotEmpty() }
                ?.let { passwordEncryptConverter.convertToEntityAttribute(it) }
            llmModel.apiKey = decryptedApiKey ?: llmModel.apiKey
            LLMBuilder().build(Platform.getPlatform(setting.platformName), llmModel, type)
        } ?: throw ServiceException(AppMessage.LLM_SETTING_NOT_FOUND)
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

        val aiService = AiServices.builder(DynamicAssistant::class.java).systemMessageProvider { _ ->
            chatRequestDTO.promptContent.orEmpty()
        }

        buildLLM(chatRequestDTO.llmId.orEmpty(), modelType).let { llm ->
            when (llm) {
                is ChatModel -> {
                    aiService.chatModel(llm)
                }

                is StreamingChatModel -> {
                    aiService.streamingChatModel(llm)
                }

                else -> throw ServiceException(AppMessage.LLM_TYPE_INVALID)
            }
        }

        val tools: MutableList<Any?> = mutableListOf()

        // 用 buildTool 而非 buildToolWithoutSetting：後者對「有 config class 的工具」一律回 null，
        // 使 TavilySearch / GoogleSearch 這類 BUILT_IN 工具只掛 toolId 時會被靜默丟棄，模型看不到任何工具。
        // buildTool 會依 (登入者, toolId) 帶出使用者設定；工具本來就不需設定時退回無參數建構，行為相容。
        chatRequestDTO.toolIds?.forEach {
            toolService.buildTool(it)?.let { tool -> tools.add(tool) }
        }

        chatRequestDTO.toolSettingIds?.forEach {
            toolService.buildToolWithSetting(it)?.let { tool -> tools.add(tool) }
        }

        @Suppress("UNCHECKED_CAST")
        tools.map { tool ->
            when (tool) {
                is Map<*, *> -> aiService.tools(tool as Map<ToolSpecification, ToolExecutor>)
                else -> aiService.tools(tool)
            }
        }
        // 知識庫外掛（自動注入型 RAG）：單數 knowledgeId（既有聊天 API 相容）與多個 knowledgeMounts
        // 合併去重，各建 ContentRetriever；1 個直接掛、多個以 DefaultQueryRouter 合併後掛為 RetrievalAugmentor。
        val knowledgeMounts = buildList {
            chatRequestDTO.knowledgeId?.takeIf { it.isNotBlank() }?.let { add(KnowledgeMount(it)) }
            chatRequestDTO.knowledgeMounts?.let { addAll(it) }
        }.distinctBy { it.knowledgeId }
        val retrievers = knowledgeMounts.mapNotNull { buildKnowledgeRetriever(it) }
        when (retrievers.size) {
            0 -> Unit
            1 -> aiService.retrievalAugmentor(
                DefaultRetrievalAugmentor.builder().contentRetriever(retrievers.first()).build()
            )
            else -> aiService.retrievalAugmentor(
                DefaultRetrievalAugmentor.builder().queryRouter(DefaultQueryRouter(retrievers)).build()
            )
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

        // Skill 與 MCP 的 toolProvider 掛載改由 [applyToolProviders] 統一組裝，
        // 避免 AiServices.toolProvider 單一插槽互相覆蓋（見該方法說明）。
        return aiService
    }

    /**
     * 依知識庫掛載規格建立 [ContentRetriever]（自動注入型 RAG 用）。
     *
     * 沿用 per-id metadata filter 隔離不同知識庫（多知識庫掛載時各自過濾），並套用節點層級的
     * topK（maxResults）／ minScore；為 null 時不設、採 langchain4j 預設。
     * 知識庫不存在時回 null，由呼叫端略過該來源。
     */
    private fun buildKnowledgeRetriever(mount: KnowledgeMount): ContentRetriever? {
        // 呼叫端以 mapNotNull 蒐集，回 null 會被靜默丟棄：RAG 沒掛上但模型仍會作答（只是沒有檢索脈絡）
        val embedding = embeddingService.getKnowledge(mount.knowledgeId) ?: run {
            logger.warn("知識庫 [${mount.knowledgeId}] 不存在，本次不會掛載此 RAG 來源")
            return null
        }
        val filter: (Query) -> Filter = { _ ->
            MetadataFilterBuilder.metadataKey(KNOWLEDGE).isIn(listOf(mount.knowledgeId))
        }
        val builder = EmbeddingStoreContentRetriever.builder()
            .embeddingStore(embeddingService.buildVectorStore(embedding.vectorStoreId.orEmpty()))
            .embeddingModel(buildLLM(embedding.llmEmbeddingId.orEmpty(), ModelType.EMBEDDING) as EmbeddingModel)
            .dynamicFilter { filter(it) } // Pass the Kotlin function as a Java Function using SAM conversion
        mount.topK?.let { builder.maxResults(it) }
        mount.minScore?.let { builder.minScore(it) }
        return builder.build()
    }

    /**
     * 統一組裝 Skill 與 MCP 的 [ToolProvider] 並掛到 [aiService]。
     *
     * langchain4j `AiServices.toolProvider(...)` 為**單一插槽**，Skill 與 MCP 若各自呼叫會互相覆蓋
     * （先前 buildAIService 設 Skill provider、呼叫端再設 MCP provider，導致 Skill 被靜默丟棄）。
     * 故集中於此：兩者皆存在時以 [CompositeToolProvider] 合併後一次掛上；並在有 Skill 時補上
     * 「可用 skills」的系統提示，引導模型先以 `activate_skill` 啟用。
     *
     * MCP client 的生命週期（建立與關閉）仍由呼叫端管理（需於串流/請求結束後關閉），此處只負責掛載。
     *
     * @param mcpClients 呼叫端已建立的 MCP client（可空）
     */
    fun applyToolProviders(
        aiService: AiServices<DynamicAssistant>,
        chatRequestDTO: ChatRequestDTO,
        mcpClients: List<McpClient>
    ) {
        val providers = mutableListOf<ToolProvider>()

        val skills = chatRequestDTO.skillIds?.takeIf { it.isNotEmpty() }?.let { ids ->
            skillService.buildSkills(ids).also { providers.add(it.toolProvider()) }
        }
        if (mcpClients.isNotEmpty()) {
            providers.add(McpToolProvider.builder().mcpClients(mcpClients).build())
        }

        when (providers.size) {
            0 -> Unit
            1 -> aiService.toolProvider(providers.first())
            else -> aiService.toolProvider(CompositeToolProvider(providers))
        }

        skills?.let { s ->
            val basePrompt = chatRequestDTO.promptContent.orEmpty()
            val skillInfo = s.formatAvailableSkills()
            aiService.systemMessageProvider { _ ->
                "$basePrompt\n\nYou have access to the following skills:\n$skillInfo\nWhen the user's request relates to one of these skills, activate it first using the `activate_skill` tool before proceeding."
            }
        }
    }

    /**
     * 上傳檔案
     */
    fun uploadFiles(files: List<FileUpload>) {
        logger.info("Uploading ${files.size} files to $fileUploadDir")

        // 確保目標目錄存在
        val uploadDir = File(fileUploadDir + File.separator + securityValidator.validateLoggedInUser())
        if (!uploadDir.exists()) {
            uploadDir.mkdirs()
        }

        files.forEach { file ->
            try {
                val targetFile = File(uploadDir, file.fileName())

                // Copy file first
                Files.copy(
                    file.uploadedFile(),
                    targetFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                )

                // Detect file type by content
                val mimeType = Files.probeContentType(targetFile.toPath())

                // Define enum for file categories
                val fileType = when {
                    mimeType.startsWith("image/") -> FileType.IMAGE
                    mimeType.startsWith("video/") -> FileType.VIDEO
                    mimeType.startsWith("audio/") -> FileType.AUDIO
                    mimeType.startsWith("application/pdf") -> FileType.PDF
                    mimeType.startsWith("text/") -> FileType.DOCUMENT

                    else -> {
                        if (targetFile.exists() && !targetFile.delete()) {
                            logger.warn("Failed to delete unsupported file: ${targetFile.absolutePath}")
                        }
                        throw ServiceException(AppMessage.FILE_TYPE_UNSUPPORTED, mimeType)
                    }
                }

                logger.info("File type detected: $mimeType (${fileType.description}) for ${file.fileName()}")
                logger.info("Successfully uploaded file: ${file.fileName()} (${targetFile.absolutePath})")
            } catch (e: Exception) {
                logger.error("Failed to upload file: ${file.fileName()}", e)
                throw ServiceException(AppMessage.FILE_UPLOAD_FAILED, file.fileName(), e.message)
            }
        }
    }

    fun buildChatRequest(chatRequestDTO: ChatRequestDTO):ChatRequest {
        val chatRequest = ChatRequest.builder().messages(
            SystemMessage.systemMessage(chatRequestDTO.promptContent),
            UserMessage.userMessage(chatRequestDTO.message)
        ).build()

        return chatRequest
    }
}

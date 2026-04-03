package tw.zipe.bastpartner.resource

import dev.langchain4j.mcp.McpToolProvider
import dev.langchain4j.mcp.client.McpClient
import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.model.chat.StreamingChatModel
import io.quarkus.security.Authenticated
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.subscription.MultiEmitter
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import java.util.concurrent.CompletableFuture
import org.jboss.resteasy.reactive.RestStreamElementType
import org.jetbrains.annotations.Blocking
import tw.zipe.bastpartner.dto.ApiResponse
import tw.zipe.bastpartner.dto.ChatRequestDTO
import tw.zipe.bastpartner.enumerate.ModelType
import tw.zipe.bastpartner.form.FilesFromRequest
import tw.zipe.bastpartner.service.LLMService
import tw.zipe.bastpartner.service.McpServerService
import tw.zipe.bastpartner.exception.LLMException
import tw.zipe.bastpartner.util.RequestContext
import tw.zipe.bastpartner.util.DTOValidator
import tw.zipe.bastpartner.util.logger

/**
 * LLM 聊天資源服務
 *
 * @author Gary
 * @created 2024/10/07
 */
@Path("/llm")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
class LLMResource(
    private val llmService: LLMService,
    private val mcpServerService: McpServerService
) : BaseLLMResource() {

    private val logger = logger()

    @POST
    @Path("/chat")
    fun chat(chatRequestDTO: ChatRequestDTO): ApiResponse<String> {
        val requestId = RequestContext.generateRequestId()
        logger.info("[REQ:$requestId] 接收聊天請求: ${chatRequestDTO.llmId}")

        try {
            validateChatRequest(chatRequestDTO)
            val llm = llmService.buildLLM(chatRequestDTO.llmId.orEmpty(), ModelType.CHAT) as ChatModel
            val result = baseChat(llm, chatRequestDTO.message.orEmpty())
            logger.info("[REQ:$requestId] 聊天請求完成")

            return ApiResponse.success(result)
        } catch (e: Exception) {
            logger.error("[REQ:$requestId] 聊天請求處理失敗", e)
            throw handleServiceException(e)
        }
    }

    @POST
    @Path("/chatStreaming")
    @RestStreamElementType(MediaType.TEXT_PLAIN)
    @Blocking
    @Transactional
    fun chatStreaming(chatRequestDTO: ChatRequestDTO): Multi<String?> {
        val requestId = RequestContext.generateRequestId()
        logger.info("[REQ:$requestId] 接收串流聊天請求: ${chatRequestDTO.llmId}")

        try {
            validateChatRequest(chatRequestDTO)
            val llm = llmService.buildLLM(
                chatRequestDTO.llmId.orEmpty(),
                ModelType.STREAMING_CHAT
            ) as StreamingChatModel

            logger.debug("[REQ:$requestId] 開始串流聊天")
            return baseStreamingChat(llm, chatRequestDTO.message!!)
                .onTermination().invoke { logger.info("[REQ:$requestId] 串流聊天完成") }
        } catch (e: Exception) {
            logger.error("[REQ:$requestId] 串流聊天請求處理失敗", e)
            throw handleServiceException(e)
        }
    }

    @POST
    @Path("/customAssistantChatStreaming")
    @RestStreamElementType(MediaType.TEXT_PLAIN)
    @Blocking
    @Transactional
    fun customAssistantChatStreaming(chatRequestDTO: ChatRequestDTO): Multi<String?> {
        val requestId = RequestContext.generateRequestId()
        logger.info("[REQ:$requestId] 接收自定義助手串流聊天請求: ${chatRequestDTO.llmId}")

        validateChatRequest(chatRequestDTO)
        val aiService = llmService.buildAIService(chatRequestDTO, ModelType.STREAMING_CHAT)
        val mcpClients = setupMcpClients(chatRequestDTO, requestId)

        mcpClients.let {
            logger.debug("[REQ:$requestId] 已配置 ${mcpClients.size} 個MCP客戶端")
            val toolProvider = McpToolProvider.builder().mcpClients(it).build()
            aiService.toolProvider(toolProvider)
        }

        return Multi.createFrom().emitter<String?> { emitter: MultiEmitter<in String?> ->
            logger.debug("[REQ:$requestId] 開始自定義助手串流聊天")
            try {
                aiService.build().streamingChat(chatRequestDTO.memory.id, chatRequestDTO.message.orEmpty())
                    .onPartialResponse { emitter.emit(it) }
                    .onCompleteResponse {
                        logger.info("[REQ:$requestId] 自定義助手串流聊天完成")
                        emitter.complete()
                    }
                    .onError { error ->
                        logger.error("[REQ:$requestId] 自定義助手串流聊天失敗", error)
                        handleStreamingError(error, emitter, requestId)
                        // 檢查是否是空訊息錯誤
                        if (error.message?.contains("at least one message is required") == true) {
                            emitter.fail(IllegalArgumentException("請確保至少有一條用戶訊息", error))
                        } else {
                            emitter.fail(error)
                        }
                    }.start()
            } catch (e: Exception) {
                logger.error("[REQ:$requestId] 處理自定義助手串流聊天請求時發生異常", e)
                emitter.fail(e)
            }finally {
                mcpClients.forEach { mcpServer -> mcpServer.close() }
            }
        }
    }

    @POST
    @Path("/customAssistantChat")
    fun customAssistantChat(chatRequestDTO: ChatRequestDTO): ApiResponse<String> {
        val requestId = RequestContext.generateRequestId()
        logger.info("[REQ:$requestId] 接收自定義助手聊天請求: ${chatRequestDTO.llmId}")

        validateChatRequest(chatRequestDTO)
        val aiService = llmService.buildAIService(chatRequestDTO, ModelType.CHAT)
        val mcpClients = setupMcpClients(chatRequestDTO, requestId)

        try {
            if (mcpClients.isNotEmpty()) {
                logger.debug("[REQ:$requestId] 已配置 ${mcpClients.size} 個MCP客戶端")
                val toolProvider = McpToolProvider.builder().mcpClients(mcpClients).build()
                aiService.toolProvider(toolProvider)
            }

            logger.debug("[REQ:$requestId] 開始自定義助手聊天")
            val response = aiService.build()
                .chat(chatRequestDTO.memory.id, chatRequestDTO.message.orEmpty())
                .content()
                .text()

            logger.info("[REQ:$requestId] 自定義助手聊天完成")
            return ApiResponse.success(response)
        } finally {
            CompletableFuture.runAsync { closeMcpClients(mcpClients, requestId) }
        }
    }

    @POST
    @Path("/uploadFile")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    fun uploadFile(filesForm: FilesFromRequest): ApiResponse<List<String>> {
        val requestId = RequestContext.generateRequestId()
        logger.info("[REQ:$requestId] 接收檔案上傳請求")

        try {
            DTOValidator.validate(filesForm) {
                requireNotEmpty("files")
                throwOnInvalid()
            }

            val files = filesForm.files.orEmpty()
            val fileNames = files.map { it.fileName() }

            logger.debug("[REQ:{}] 上傳檔案: {}", requestId, fileNames)
            llmService.uploadFiles(files)

            logger.info("[REQ:$requestId] 檔案上傳完成: ${fileNames.size} 個檔案")
            return ApiResponse.success(fileNames)
        } catch (e: Exception) {
            logger.error("[REQ:$requestId] 檔案上傳失敗", e)
            throw handleServiceException(e)
        }
    }

    // 輔助方法
    private fun validateChatRequest(chatRequestDTO: ChatRequestDTO) {
        DTOValidator.validate(chatRequestDTO) {
            requireNotEmpty("llmId", "message")
            throwOnInvalid()
        }
    }

    private fun setupMcpClients(chatRequestDTO: ChatRequestDTO, requestId: String): List<McpClient> {
        val mcpClients = mutableListOf<McpClient>()

        chatRequestDTO.mcpIds?.let { ids ->
            if (ids.isNotEmpty()) {
                logger.debug("[REQ:{}] 設置全局MCP客戶端: {}", requestId, ids)
                mcpClients.addAll(mcpServerService.buildMcpServer(ids))
            }
        }

        chatRequestDTO.mcpSettingIds?.let { ids ->
            if (ids.isNotEmpty()) {
                logger.debug("[REQ:$requestId] 設置用戶特定MCP客戶端: $ids")
                mcpClients.addAll(mcpServerService.buildMcpServer(ids, identity.principal.name))
            }
        }

        return mcpClients
    }

    private fun closeMcpClients(mcpClients: List<McpClient>, requestId: String) {
        logger.debug("[REQ:$requestId] 開始關閉 ${mcpClients.size} 個MCP客戶端")

        mcpClients.forEachIndexed { index, client ->
            try {
                client.close()
                logger.trace("[REQ:$requestId] 已關閉MCP客戶端 #$index")
            } catch (e: Exception) {
                logger.warn("[REQ:$requestId] 關閉MCP客戶端 #$index 失敗", e)
            }
        }

        logger.debug("[REQ:$requestId] MCP客戶端關閉完成")
    }

    private fun handleStreamingError(error: Throwable, emitter: MultiEmitter<in String?>, requestId: String) {
        logger.error("[REQ:$requestId] LLM請求失敗", error)

        when {
            error.message?.contains("at least one message is required") == true ->
                emitter.fail(LLMException("請確保至少有一條用戶訊息", error))

            error.message?.contains("rate limit") == true ->
                emitter.fail(LLMException("LLM服務達到限流閾值，請稍後再試", error))

            error.message?.contains("context length") == true ->
                emitter.fail(LLMException("輸入內容過長，超出LLM上下文長度限制", error))

            else ->
                emitter.fail(error)
        }
    }

    private fun handleServiceException(e: Exception): Exception {
        return when (e) {
            is IllegalArgumentException -> LLMException("請求參數無效: ${e.message}", e)
            is IllegalStateException -> LLMException("LLM服務狀態異常: ${e.message}", e)
            else -> LLMException("LLM服務處理發生錯誤", e)
        }
    }
}

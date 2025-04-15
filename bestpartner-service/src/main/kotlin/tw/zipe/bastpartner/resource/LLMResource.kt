package tw.zipe.bastpartner.resource

import dev.langchain4j.mcp.McpToolProvider
import dev.langchain4j.mcp.client.McpClient
import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.chat.StreamingChatLanguageModel
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
import org.jboss.resteasy.reactive.RestStreamElementType
import org.jetbrains.annotations.Blocking
import tw.zipe.bastpartner.dto.ApiResponse
import tw.zipe.bastpartner.dto.ChatRequestDTO
import tw.zipe.bastpartner.enumerate.ModelType
import tw.zipe.bastpartner.form.FilesFromRequest
import tw.zipe.bastpartner.service.LLMService
import tw.zipe.bastpartner.service.McpServerService
import tw.zipe.bastpartner.util.DTOValidator
import tw.zipe.bastpartner.util.logger


/**
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
        DTOValidator.validate(chatRequestDTO) {
            requireNotEmpty("llmId", "message")
            throwOnInvalid()
        }
        val llm = llmService.buildLLM(chatRequestDTO.llmId.orEmpty(), ModelType.CHAT).let {
            it as ChatLanguageModel
        }
        return ApiResponse.success(baseChat(llm, chatRequestDTO.message.orEmpty()))
    }

    @POST
    @Path("/chatStreaming")
    @RestStreamElementType(MediaType.TEXT_PLAIN)
    @Blocking
    @Transactional
    fun chatStreaming(chatRequestDTO: ChatRequestDTO): Multi<String?> {
        DTOValidator.validate(chatRequestDTO) {
            requireNotEmpty("llmId", "message")
            throwOnInvalid()
        }
        val llm = llmService.buildLLM(chatRequestDTO.llmId.orEmpty(), ModelType.STREAMING_CHAT).let {
            it as StreamingChatLanguageModel
        }
        return baseStreamingChat(llm, chatRequestDTO.message!!)
    }

    @POST
    @Path("/customAssistantChatStreaming")
    @RestStreamElementType(MediaType.TEXT_PLAIN)
    @Blocking
    @Transactional
    fun customAssistantChatStreaming(chatRequestDTO: ChatRequestDTO): Multi<String?> {
        DTOValidator.validate(chatRequestDTO) {
            requireNotEmpty("llmId", "message")
            throwOnInvalid()
        }
        val aiService = llmService.buildAIService(chatRequestDTO, ModelType.STREAMING_CHAT)

        val mcpServers = mutableListOf<McpClient>()
        chatRequestDTO.mcpIds.let {
            mcpServers.addAll(mcpServerService.buildMcpServer(chatRequestDTO.mcpIds.orEmpty()))
        }
        chatRequestDTO.mcpSettingIds?.let {
            mcpServers.addAll(mcpServerService.buildMcpServer(chatRequestDTO.mcpSettingIds, identity.principal.name))
        }

        mcpServers.let {
            val toolProvider = McpToolProvider.builder().mcpClients(mcpServers).build()
            aiService.toolProvider(toolProvider)
        }

        return Multi.createFrom().emitter<String?> { emitter: MultiEmitter<in String?> ->
            try {
                aiService.build().streamingChat(chatRequestDTO.memory.id, chatRequestDTO.message.orEmpty())
                    .onPartialResponse { emitter.emit(it) }
                    .onCompleteResponse {
                        emitter.complete()
                        // 完成後關閉 MCP 服務器客戶端
                        mcpServers.forEach { mcpServer -> mcpServer.close() }
                    }
                    .onError { error ->
                        // 記錄錯誤
                        logger.error("LLM請求失敗", error)

                        // 檢查是否是空訊息錯誤
                        if (error.message?.contains("at least one message is required") == true) {
                            emitter.fail(IllegalArgumentException("請確保至少有一條用戶訊息", error))
                        } else {
                            emitter.fail(error)
                        }

                        // 錯誤時也關閉 MCP 服務器客戶端
                        mcpServers.forEach { mcpServer -> mcpServer.close() }
                    }.start()
            } catch (e: Exception) {
                // 捕獲任何其他異常
                logger.error("處理 LLM 請求時發生異常", e)
                emitter.fail(e)
                // 確保在異常情況下也關閉 MCP 服務器客戶端
                mcpServers.forEach { mcpServer -> mcpServer.close() }
            }
        }
    }

    @POST
    @Path("/customAssistantChat")
    fun customAssistantChat(chatRequestDTO: ChatRequestDTO): ApiResponse<String> {
        val aiService = llmService.buildAIService(chatRequestDTO, ModelType.CHAT)

        val mcpServers = mutableListOf<McpClient>()
        chatRequestDTO.mcpIds.let {
            mcpServers.addAll(mcpServerService.buildMcpServer(chatRequestDTO.mcpIds.orEmpty()))
        }
        chatRequestDTO.mcpSettingIds?.let {
            mcpServers.addAll(mcpServerService.buildMcpServer(chatRequestDTO.mcpSettingIds, identity.principal.name))
        }

        mcpServers.let {
            val toolProvider = McpToolProvider.builder().mcpClients(mcpServers).build()
            aiService.toolProvider(toolProvider)
        }
        return try {
            ApiResponse.success(
                aiService.build().chat(chatRequestDTO.memory.id, chatRequestDTO.message.orEmpty()).content().text()
            )
        } finally {
            mcpServers.forEach { mcpServer -> mcpServer.close() }
        }
    }

    @POST
    @Path("/uploadFile")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    fun uploadFile(filesForm: FilesFromRequest): ApiResponse<List<String>> {
        DTOValidator.validate(filesForm) {
            requireNotEmpty("files")
            throwOnInvalid()
        }
        val fileNames = filesForm.files.orEmpty().map { it.fileName() }
        llmService.uploadFiles(filesForm.files.orEmpty())
        return ApiResponse.success(fileNames)
    }

}

package tw.zipe.bastpartner.resource

import dev.langchain4j.mcp.McpToolProvider
import dev.langchain4j.mcp.client.DefaultMcpClient
import dev.langchain4j.mcp.client.McpClient
import dev.langchain4j.mcp.client.transport.McpTransport
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport
import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.chat.StreamingChatLanguageModel
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel
import dev.langchain4j.service.tool.ToolProvider
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
import java.time.Duration
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
//        chatRequestDTO.mcpSettingIds?.let {
//            mcpServers.addAll(mcpServerService.buildMcpServer(chatRequestDTO.mcpSettingIds))
//            val toolProvider = McpToolProvider.builder().mcpClients(mcpServers).build()
//        aiService.toolProvider(toolProvider)
//        }
//        val dataTransport: McpTransport = StdioMcpTransport.Builder()
//            .command(listOf("java", "-jar", "D:/MCP/date-1.0-SNAPSHOT-runner.jar"))
//            .logEvents(true) // only if you want to see the traffic in the log
//            .build()
//        val excelTransport: McpTransport = StdioMcpTransport.Builder()
//            .command(listOf("java", "-jar", "D:/MCP/excel-1.0-SNAPSHOT-runner.jar"))
//            .logEvents(true) // only if you want to see the traffic in the log
//            .build()
//        val googleTransport: McpTransport = StdioMcpTransport.Builder()
//            .command(listOf("java", "-jar", "D:/MCP/google-drive-1.0-SNAPSHOT-runner.jar"))
//            .environment(mapOf("CREDENTIALS_FILE_PATH" to "D:/MCP/credentials.json"))
//            .logEvents(true) // only if you want to see the traffic in the log
//            .build()
//        val gmailTransport: McpTransport = StdioMcpTransport.Builder()
//            .command(listOf("java", "-jar", "D:/MCP/gmail-1.0-SNAPSHOT-runner.jar"))
//            .environment(mapOf("GMAIL_CREDENTIALS_FILE_PATH" to "D:/MCP/credentials.json"))
//            .logEvents(true) // only if you want to see the traffic in the log
//            .build()
//        val dataMcpClient: McpClient = DefaultMcpClient.Builder()
//            .transport(dataTransport)
//            .build()
//        val excelMcpClient: McpClient = DefaultMcpClient.Builder()
//            .transport(excelTransport)
//            .build()
//        val googleMcpClient: McpClient = DefaultMcpClient.Builder()
//            .transport(googleTransport)
//            .build()
//        val gmailMcpClient: McpClient = DefaultMcpClient.Builder()
//            .transport(gmailTransport)
//            .build()
//        val toolProvider: ToolProvider = McpToolProvider.builder()
//            .mcpClients(listOf(dataMcpClient, excelMcpClient, googleMcpClient, gmailMcpClient))
//            .build()

        val dateTransport: McpTransport = HttpMcpTransport.Builder()
            .sseUrl("http://localhost:8080/sse")
            .logRequests(true) // if you want to see the traffic in the log
            .logResponses(true)
            .build()
        val dataMcpClient: McpClient = DefaultMcpClient.Builder()
            .transport(dateTransport)
            .build()
        val toolProvider: ToolProvider = McpToolProvider.builder()
            .mcpClients(listOf(dataMcpClient))
            .build()
        aiService.toolProvider(toolProvider)

        return try {
            Multi.createFrom().emitter<String?> { emitter: MultiEmitter<in String?> ->
                aiService.build().streamingChat(chatRequestDTO.memory.id, chatRequestDTO.message.orEmpty())
                    .onPartialResponse { emitter.emit(it) }
                    .onCompleteResponse { emitter.complete() }
                    .onError { error ->
                        // 记录错误
                        logger.error("LLM请求失败", error)

                        // 检查是否是空消息错误
                        if (error.message?.contains("at least one message is required") == true) {
                            emitter.fail(IllegalArgumentException("请确保至少有一条用户消息", error))
                        } else {
                            emitter.fail(error)
                        }
                    }.start()
            }
        } finally {
//            mcpServers.forEach { mcpServer -> mcpServer.close() }
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


//        val dataTransport: McpTransport = StdioMcpTransport.Builder()
//            .command(listOf("java", "-jar", "D:/MCP/date-1.0-SNAPSHOT-runner.jar"))
//            .logEvents(true) // only if you want to see the traffic in the log
//            .build()
//        val excelTransport: McpTransport = StdioMcpTransport.Builder()
//            .command(listOf("java", "-jar", "D:/MCP/excel-1.0-SNAPSHOT-runner.jar"))
//            .logEvents(true) // only if you want to see the traffic in the log
//            .build()
//        val googleTransport: McpTransport = StdioMcpTransport.Builder()
//            .command(listOf("java", "-jar", "D:/MCP/google-drive-1.0-SNAPSHOT-runner.jar"))
//            .environment(mapOf("CREDENTIALS_FILE_PATH" to "D:/MCP/credentials.json"))
//            .logEvents(true) // only if you want to see the traffic in the log
//            .build()
//        val gmailTransport: McpTransport = StdioMcpTransport.Builder()
//            .command(listOf("java", "-jar", "D:/MCP/gmail-1.0-SNAPSHOT-runner.jar"))
//            .environment(mapOf("GMAIL_CREDENTIALS_FILE_PATH" to "D:/MCP/credentials.json"))
//            .logEvents(true) // only if you want to see the traffic in the log
//            .build()
//        val dataMcpClient: McpClient = DefaultMcpClient.Builder()
//            .transport(dataTransport)
//            .build()
//        val excelMcpClient: McpClient = DefaultMcpClient.Builder()
//            .transport(excelTransport)
//            .build()
//        val googleMcpClient: McpClient = DefaultMcpClient.Builder()
//            .transport(googleTransport)
//            .build()
//        val gmailMcpClient: McpClient = DefaultMcpClient.Builder()
//            .transport(gmailTransport)
//            .build()
//        val toolProvider: ToolProvider = McpToolProvider.builder()
//            .mcpClients(listOf(dataMcpClient, excelMcpClient, googleMcpClient, gmailMcpClient))
//            .build()
//        aiService.toolProvider(toolProvider)

        return try {
            ApiResponse.success(aiService.build().chat(chatRequestDTO.memory.id, chatRequestDTO.message.orEmpty()).content().text())
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

package tw.zipe.bastpartner.assistant

import dev.langchain4j.mcp.McpToolProvider
import dev.langchain4j.mcp.client.DefaultMcpClient
import dev.langchain4j.mcp.client.McpClient
import dev.langchain4j.mcp.client.transport.McpTransport
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel
import dev.langchain4j.service.AiServices
import java.time.Duration
import me.kpavlov.langchain4j.kotlin.service.SystemMessageProvider

fun main() {
    val chatModel = GoogleAiGeminiChatModel.builder()
        .apiKey(System.getenv("GEMINI_API_KEY"))
        .modelName("gemini-2.0-flash")
        .temperature(0.1)
        .topP(0.5)
        .maxOutputTokens(4096)
        .timeout(Duration.ofSeconds(6000))
        .logRequestsAndResponses(true)
        .build()

    val chineseNameTransport: McpTransport = StdioMcpTransport.Builder()
        .command(listOf("java", "-Dfile.encoding=UTF-8", "-jar", "D:/MCP/quarkus-1.0-SNAPSHOT-runner.jar"))
        .logEvents(true)
        .build()

    val chineseNameMcpClient: McpClient = DefaultMcpClient.Builder()
        .transport(chineseNameTransport)
        .build()
    val mcpToolProvider = McpToolProvider.builder()
        .mcpClients(listOf(chineseNameMcpClient))
        .build()

    val assistant = AiServices.builder(AIAssistant::class.java).systemMessageProvider(
        object : SystemMessageProvider {
            override fun getSystemMessage(chatMemoryID: Any): String =
                """
                    You are a helpful assistant. You can answer questions, provide information, and assist with various tasks.
                    """.trimIndent()
        })
        .chatModel(chatModel)
        .toolProvider(mcpToolProvider)
        .build()
    val answer = assistant.chat("What is Gary's Chinese name?")
    println(answer.content().text())
}

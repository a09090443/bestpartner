package tw.zipe.bastpartner.config

import dev.langchain4j.mcp.client.DefaultMcpClient
import dev.langchain4j.mcp.client.transport.McpTransport
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport
import io.quarkus.runtime.Startup
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Produces
import jakarta.inject.Named
import org.eclipse.microprofile.config.inject.ConfigProperty
import tw.zipe.bastpartner.enumerate.McpType
import tw.zipe.bastpartner.service.McpServerService
import tw.zipe.bastpartner.util.logger

//@ApplicationScoped
class McpServer(
    private val llmMcpServerService: McpServerService
) {

    private val logger = logger()

    private var mcpMap = mutableMapOf<String, DefaultMcpClient>()

    @ConfigProperty(name = "mcp.server.log.enable", defaultValue = "false")
    private lateinit var mcpLogEnable: String

//    @Startup
    fun startUpServers() {
        logger.info("Starting up mcp servers")
        llmMcpServerService.getMcpServers().list().forEach { mcp ->
            val transport: McpTransport
            when (mcp.type) {
                McpType.STDIO -> {
                    val command = mutableListOf<String>()
                    command.addFirst(mcp.commandSetting.command.orEmpty())
                    command.addAll(mcp.commandSetting.args.orEmpty())
                    transport = StdioMcpTransport.Builder().command(command).environment(mcp.commandSetting.env).logEvents(mcpLogEnable.toBoolean()).build()
                }
                McpType.SSE -> {
                    transport = HttpMcpTransport.Builder().sseUrl(mcp.commandSetting.server).logRequests(mcpLogEnable.toBoolean())
                        .logResponses(mcpLogEnable.toBoolean()).build()
                }
            }

            val mcpClient = DefaultMcpClient.Builder().transport(transport).build()
            mcpMap[mcp.id.orEmpty()] = mcpClient
        }

        logger.info(mcpMap.size.toString())
    }

    @Produces
    @Named("mcpServerMap")
    fun getMcpServerMap(): Map<String, DefaultMcpClient> {
        return mcpMap
    }
}

package tw.zipe.bastpartner.service

import dev.langchain4j.mcp.client.DefaultMcpClient
import dev.langchain4j.mcp.client.transport.McpTransport
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.config.inject.ConfigProperty
import tw.zipe.bastpartner.dto.McpDTO
import tw.zipe.bastpartner.entity.LLMMcpServerEntity
import tw.zipe.bastpartner.enumerate.McpType
import tw.zipe.bastpartner.model.McpCommandSetting
import tw.zipe.bastpartner.repository.LLMMcpServerRepository
import tw.zipe.bastpartner.util.logger

@ApplicationScoped
class McpServerService(
    private val llmMcpServerRepository: LLMMcpServerRepository
) {

    private val logger = logger()

    @ConfigProperty(name = "mcp.server.log.enable", defaultValue = "false")
    private lateinit var mcpLogEnable: String

    fun getMcpServers() = llmMcpServerRepository.findAll()

    fun getMcpServer(mcpId: String): McpDTO {
        return llmMcpServerRepository.findById(mcpId)?.let {
            with(McpDTO()) {
                id = it.id
                name = it.name
                args = it.commandSetting.args
                argsDesc = it.commandSetting.argsDesc
                env = it.commandSetting.env
                type = it.type
                this
            }
        } ?: McpDTO()
    }

    fun saveMcpServer(mcpDTO: McpDTO) {
        val mcpCommandSetting:McpCommandSetting
        when (mcpDTO.type) {
            McpType.STDIO -> {
                mcpCommandSetting = McpCommandSetting().apply {
                    command = mcpDTO.command
                    args = mcpDTO.args
                    argsDesc = mcpDTO.argsDesc
                    env = mcpDTO.env
                }
            }
            McpType.SSE -> {
                mcpCommandSetting = McpCommandSetting().apply {
                    server = mcpDTO.server
                }
            }
            else -> throw IllegalArgumentException("Invalid McpType")
        }
        with(LLMMcpServerEntity()) {
            name = mcpDTO.name.orEmpty()
            commandSetting = mcpCommandSetting
            type = mcpDTO.type ?: McpType.STDIO
            llmMcpServerRepository.saveOrUpdate(this)
        }.let { mcpDTO.id = it.id }
    }

    fun deleteMcpServer(mcpId: String) = llmMcpServerRepository.deleteById(mcpId)

    fun buildMcpServer(mcpIds: List<String>): List<DefaultMcpClient> {
        logger.info("Starting up mcp servers")
        return getMcpServers().list().map { mcp ->
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
            DefaultMcpClient.Builder().transport(transport).build()
        }.toList()
    }
}

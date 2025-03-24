package tw.zipe.bastpartner.service

import dev.langchain4j.mcp.client.DefaultMcpClient
import dev.langchain4j.mcp.client.transport.McpTransport
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import org.eclipse.microprofile.config.inject.ConfigProperty
import tw.zipe.bastpartner.config.security.SecurityValidator
import tw.zipe.bastpartner.dto.McpDTO
import tw.zipe.bastpartner.entity.LLMMcpServerEntity
import tw.zipe.bastpartner.entity.LLMMcpUserSetting
import tw.zipe.bastpartner.enumerate.McpType
import tw.zipe.bastpartner.exception.ServiceException
import tw.zipe.bastpartner.model.McpCommandSetting
import tw.zipe.bastpartner.repository.LLMMcpServerRepository
import tw.zipe.bastpartner.repository.LLMMcpUserSettingRepository
import tw.zipe.bastpartner.util.logger

@ApplicationScoped
class McpServerService(
    private val securityValidator: SecurityValidator,
    private val llmMcpServerRepository: LLMMcpServerRepository,
    private val llmMcpUserSettingRepository: LLMMcpUserSettingRepository
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
        } ?: throw ServiceException("找不到 MCP server")
    }

    fun saveMcpServer(mcpDTO: McpDTO) {
        val mcpCommandSetting: McpCommandSetting
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

    fun saveUserSetting(mcpDTO: McpDTO) {
        val mcp = getMcpServer(mcpDTO.settingId.orEmpty())
        validateMcpSettings(mcpDTO, mcp)

        with(LLMMcpUserSetting()) {
            this.alias = mcpDTO.alias.orEmpty()
            this.userId = securityValidator.validateLoggedInUser()
            this.mcpId = mcp.id.orEmpty()
            this.settingContent = mcpDTO.settingContent.orEmpty()
            llmMcpUserSettingRepository.saveOrUpdate(this)
        }.apply { mcpDTO.userSettingId = this.id }
    }

    @Transactional
    fun deleteMcpUserSetting(userSettingId: String) = llmMcpUserSettingRepository.deleteById(userSettingId)

    fun buildMcpServer(mcpIds: List<String>): List<DefaultMcpClient> {
        logger.info("Starting up mcp servers")
        mcpIds.map { id ->
            getMcpServer(id).let { mcp ->
                {
                    val transport: McpTransport
                    when (mcp.type) {
                        McpType.STDIO -> {
                            if (!mcp.args.isNullOrEmpty() || !mcp.env.isNullOrEmpty()) {
                                llmMcpUserSettingRepository.findSettingByUserIdAndMcpId(
                                    securityValidator.validateLoggedInUser(),
                                    mcp.id.orEmpty()
                                )?.let {

                                } ?: throw ServiceException("找不到使用者設定")
                            }
                        }

                        McpType.SSE -> {
                            transport =
                                HttpMcpTransport.Builder().sseUrl(mcp.server).logRequests(mcpLogEnable.toBoolean())
                                    .logResponses(mcpLogEnable.toBoolean()).build()
                        }

                        null -> TODO()
                    }
//                DefaultMcpClient.Builder().transport(transport).build()
                }
            }
        }
//        return getMcpServers().list().map { mcp ->
//            val transport: McpTransport
//            when (mcp.type) {
//                McpType.STDIO -> {
//                    val command = mutableListOf<String>()
//                    command.addFirst(mcp.commandSetting.command.orEmpty())
//                    command.addAll(mcp.commandSetting.args.orEmpty())
//                    transport = StdioMcpTransport.Builder().command(command).environment(mcp.commandSetting.env).logEvents(mcpLogEnable.toBoolean()).build()
//                }
//                McpType.SSE -> {
//                    transport = HttpMcpTransport.Builder().sseUrl(mcp.commandSetting.server).logRequests(mcpLogEnable.toBoolean())
//                        .logResponses(mcpLogEnable.toBoolean()).build()
//                }
//            }
//            DefaultMcpClient.Builder().transport(transport).build()
//        }.toList()
        return TODO()
    }

    private fun validateMcpSettings(userSetting: McpDTO, mcpServer: McpDTO) {
        if (mcpServer.type != McpType.STDIO) {
            return
        }

        validateArguments(userSetting.settingContent, mcpServer.argsDesc)
        validateEnvironmentVariables(userSetting.settingContent, mcpServer.env)
    }

    private fun validateArguments(userArgs: Map<String, String>?, argsDesc: Map<String, String>?) {
        validateMapSettings(userArgs, argsDesc, "參數", "使用者 MCP server 參數設定不正確")
    }

    private fun validateEnvironmentVariables(userEnv: Map<String, String>?, serverEnv: Map<String, String>?) {
        validateMapSettings(userEnv, serverEnv, "環境變數", "使用者 MCP server 環境變數設定不正確")
    }

    private fun validateMapSettings(
        userSettings: Map<String, String>?,
        requiredSettings: Map<String, String>?,
        settingType: String,
        errorMessage: String
    ) {
        if (requiredSettings.isNullOrEmpty()) {
            return
        }

        if (userSettings.isNullOrEmpty()) {
            throw ServiceException(errorMessage)
        }

        // Check if all keys in requiredSettings exist in userSettings
        val missingSettings = requiredSettings.keys.filter { !userSettings.containsKey(it) }
        if (missingSettings.isNotEmpty()) {
            throw ServiceException("缺少必要的${settingType}設定: ${missingSettings.joinToString(", ")}")
        }
    }

    private fun hasAllPlaceholders(text: String, settings: Any?): Boolean {
        if (settings == null) return false

        val regex = Regex("\\$\\{([^}]+)}")
        val matches = regex.findAll(text)

        return matches.all { matchResult ->
            val placeholder = matchResult.groupValues[1]
            when (settings) {
                is Map<*, *> -> settings.containsKey(placeholder)
                is List<*> -> settings.contains(placeholder)
                else -> false
            }
        }
    }

    private fun replacePlaceholders(text: String, settings: Map<String, String>): String {
        return if (text.contains("\${")) {
            val placeholder = text.substringAfter("\${").substringBefore("}")
            settings[placeholder] ?: text
        } else {
            text
        }
    }
}

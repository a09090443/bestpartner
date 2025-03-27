package tw.zipe.bastpartner.service

import dev.langchain4j.mcp.client.DefaultMcpClient
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport
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
                this.mcpId = it.id
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
        }.let { mcpDTO.mcpId = it.id }
    }

    fun deleteMcpServer(mcpId: String) = llmMcpServerRepository.deleteById(mcpId)

    fun saveUserSetting(mcpDTO: McpDTO) {
        val mcp = getMcpServer(mcpDTO.mcpId.orEmpty())
        validateMcpSettings(mcpDTO, mcp)

        with(LLMMcpUserSetting()) {
            this.alias = mcpDTO.alias.orEmpty()
            this.userId = securityValidator.validateLoggedInUser()
            this.mcpId = mcp.mcpId.orEmpty()
            this.settingContent = mcpDTO.settingContent.orEmpty()
            llmMcpUserSettingRepository.saveOrUpdate(this)
        }.apply { mcpDTO.userSettingId = this.id }
    }

    @Transactional
    fun deleteMcpUserSetting(userSettingId: String) = llmMcpUserSettingRepository.deleteById(userSettingId)

    fun buildMcpServer(mcpIds: List<String>): List<DefaultMcpClient> {
        logger.info("Starting up mcp servers")
        return mcpIds.mapNotNull { id ->
            try {
                llmMcpUserSettingRepository.findByCondition(id, securityValidator.validateLoggedInUser())
                    ?.firstNotNullOfOrNull { data ->
                        val transport = when (data.type) {
                            McpType.STDIO -> {
                                validateArguments(data.settingContent, data.commandSetting?.argsDesc)
                                validateEnvironmentVariables(data.settingContent, data.commandSetting?.env)

                                val commandList = replacePlaceholders(
                                    data.commandSetting?.args.orEmpty(),
                                    data.settingContent.orEmpty()
                                ).toMutableList()
                                commandList.add(0, data.commandSetting?.command.orEmpty())

                                val envVars = extractMatchingSettings(
                                    data.settingContent.orEmpty(),
                                    data.commandSetting?.env.orEmpty()
                                )

                                StdioMcpTransport.Builder()
                                    .command(commandList)
                                    .environment(envVars)
                                    .logEvents(mcpLogEnable.toBoolean())
                                    .build()
                            }

                            McpType.SSE -> HttpMcpTransport.Builder()
                                .sseUrl(data.commandSetting?.server.orEmpty())
                                .build()

                            else -> null
                        }

                        transport?.let { DefaultMcpClient.Builder().transport(it).build() }
                    }
            } catch (e: Exception) {
                logger.error("Failed to create MCP client for ID: $id", e)
                null
            }
        }
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
        } else if (userSettings.isNullOrEmpty()) {
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

    private fun replacePlaceholders(text: List<String>, settings: Map<String, String>): List<String> {
        return text.map { item ->
            if (item.contains("\${")) {
                val placeholder = item.substringAfter("\${").substringBefore("}")
                settings[placeholder] ?: item
            } else {
                item
            }
        }
    }

    private fun extractMatchingSettings(
        userSettings: Map<String, String>,
        requiredSettings: Map<String, String>
    ): Map<String, String> {
        return requiredSettings.keys
            .filter { userSettings.containsKey(it) }
            .mapNotNull { key -> userSettings[key]?.let { value -> key to value } }
            .toMap()
    }
}

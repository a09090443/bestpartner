package tw.zipe.bastpartner.service

import dev.langchain4j.mcp.client.DefaultMcpClient
import dev.langchain4j.mcp.client.transport.McpTransport
import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport
import io.netty.util.internal.StringUtil
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import org.eclipse.microprofile.config.inject.ConfigProperty
import tw.zipe.bastpartner.config.security.SecurityValidator
import tw.zipe.bastpartner.converter.SensitiveValueCodec
import tw.zipe.bastpartner.dto.McpDTO
import tw.zipe.bastpartner.entity.LLMMcpServerEntity
import tw.zipe.bastpartner.entity.LLMMcpUserSetting
import tw.zipe.bastpartner.enumerate.AppMessage
import tw.zipe.bastpartner.enumerate.McpType
import tw.zipe.bastpartner.exception.ServiceException
import tw.zipe.bastpartner.util.MessageUtil
import tw.zipe.bastpartner.model.McpCommandSetting
import tw.zipe.bastpartner.repository.LLMMcpServerRepository
import tw.zipe.bastpartner.repository.LLMMcpUserSettingRepository
import tw.zipe.bastpartner.util.logger

@ApplicationScoped
class McpServerService(
    private val securityValidator: SecurityValidator,
    private val llmMcpServerRepository: LLMMcpServerRepository,
    private val llmMcpUserSettingRepository: LLMMcpUserSettingRepository,
    private val sensitiveValueCodec: SensitiveValueCodec
) {

    private val logger = logger()

    @ConfigProperty(name = "mcp.server.log.enable", defaultValue = "false")
    private lateinit var mcpLogEnable: String

    fun getMcpServers() = llmMcpServerRepository.findAll().list().map { entityToDto(it) }

    fun getMcpServer(mcpId: String): McpDTO {
        return llmMcpServerRepository.findById(mcpId)?.let { entityToDto(it) }
            ?: throw ServiceException(AppMessage.MCP_SERVER_NOT_FOUND)
    }

    fun saveMcpServer(mcpDTO: McpDTO) {
        val mcpCommandSetting = createMcpCommandSetting(mcpDTO)
        with(LLMMcpServerEntity()) {
            if (!mcpDTO.mcpId.isNullOrEmpty()) id = mcpDTO.mcpId
            name = mcpDTO.name.orEmpty()
            commandSetting = mcpCommandSetting
            type = mcpDTO.type ?: McpType.STDIO
            description = mcpDTO.description
            llmMcpServerRepository.saveOrUpdate(this)
        }.let { mcpDTO.mcpId = it.id }
    }

    fun updateMcpServer(mcpDTO: McpDTO) {
        val mcpCommandSetting = createMcpCommandSetting(mcpDTO)
        mcpDTO.commandSetting = mcpCommandSetting
        llmMcpServerRepository.update(mcpDTO)
    }

    @Transactional
    fun deleteMcpServer(mcpId: String) = llmMcpServerRepository.deleteById(mcpId)

    fun getUserSetting(mcpDTO: McpDTO): McpDTO =
        llmMcpUserSettingRepository.findSettingByUserIdAndSettingId(mcpDTO.userSettingId.orEmpty())?.let {
            with(McpDTO()) {
                this.settingId = it.id
                this.mcpId = it.mcpId
                this.alias = it.alias
                // env 分類的值一律遮罩，明文與密文皆不外流
                this.settingContent = maskEnvValues(it.settingContent, envKeysOf(it.mcpId))
                this
            }
        } ?: throw ServiceException(AppMessage.MCP_USER_SETTING_NOT_FOUND)

    fun saveUserSetting(mcpDTO: McpDTO) {
        val mcp = getMcpServer(mcpDTO.mcpId.orEmpty())
        validateMcpSettings(mcpDTO, mcp)

        with(LLMMcpUserSetting()) {
            this.alias = mcpDTO.alias.orEmpty()
            this.userId = securityValidator.validateLoggedInUser()
            this.mcpId = mcp.mcpId.orEmpty()
            // 整包 settingContent 由 JPA converter 加密落地；此處僅防呆丟棄未帶既有值的遮罩
            this.settingContent = mergeKeptEnvValues(mcpDTO.settingContent.orEmpty(), mcp.env?.keys.orEmpty(), existing = emptyMap())
            llmMcpUserSettingRepository.saveOrUpdate(this)
        }.apply { mcpDTO.userSettingId = this.id }
    }

    @Transactional
    fun updateSetting(mcpDTO: McpDTO) {
        // 對外契約一律為 userSettingId（與 getSetting / deleteSetting 一致）
        val settingId = mcpDTO.userSettingId.orEmpty()
        // 以「當前使用者 + settingId」查詢，避免他人以 id 竄改非自己的設定
        val existingEntity = llmMcpUserSettingRepository.findSettingByUserIdAndSettingId(settingId)
            ?: throw ServiceException(AppMessage.MCP_USER_SETTING_NOT_FOUND)
        val envKeys = envKeysOf(existingEntity.mcpId)
        // 既有 settingContent 已由 JPA converter 解密為整包明文；env 值若為舊版逐值密文則一併還原（相容舊資料）
        val existing = decryptEnvValues(existingEntity.settingContent, envKeys)
        // 未更動的 env 值（遮罩）沿用既有明文；整包再由 converter 加密落地
        val merged = mergeKeptEnvValues(mcpDTO.settingContent.orEmpty(), envKeys, existing)
        llmMcpUserSettingRepository.updateSettingsByNative(settingId, merged)
    }

    /** 取得 MCP server 定義的 env key 集合（即需加密的敏感 key）。 */
    private fun envKeysOf(mcpId: String?): Set<String> =
        mcpId?.let { llmMcpServerRepository.findById(it)?.commandSetting?.env?.keys }.orEmpty()

    /**
     * 未更動的 env 值（值為 [SensitiveValueCodec.SECRET_MASK]）沿用 [existing] 中的既有明文；
     * 既有值不存在（改了 key 名或新增設定）則移除該欄位，避免把遮罩字面值當成真值存入。
     * 整包 setting_content 的加密由 [tw.zipe.bastpartner.converter.McpSettingEncryptConverter]
     * 於落地時處理，此處只還原遮罩、不做加密。
     */
    private fun mergeKeptEnvValues(
        content: Map<String, String>,
        envKeys: Set<String>,
        existing: Map<String, String>
    ): Map<String, String> {
        if (envKeys.isEmpty()) return content
        return content.mapNotNull { (key, value) ->
            if (key in envKeys && value == SensitiveValueCodec.SECRET_MASK) {
                existing[key]?.let { key to it }
            } else {
                key to value
            }
        }.toMap()
    }

    @Suppress("UNCHECKED_CAST")
    private fun maskEnvValues(content: Map<String, String>?, envKeys: Set<String>): Map<String, String> {
        val data = content.orEmpty()
        if (envKeys.isEmpty()) return data
        return sensitiveValueCodec.maskMap(data, envKeys) as Map<String, String>
    }

    @Suppress("UNCHECKED_CAST")
    private fun decryptEnvValues(content: Map<String, String>?, envKeys: Set<String>): Map<String, String> {
        val data = content.orEmpty()
        if (envKeys.isEmpty()) return data
        return sensitiveValueCodec.decryptMap(data, envKeys) as Map<String, String>
    }

    @Transactional
    fun deleteMcpUserSetting(userSettingId: String): Boolean {
        // 先確認該設定屬於當前使用者，避免他人以 id 刪除非自己的設定
        llmMcpUserSettingRepository.findSettingByUserIdAndSettingId(userSettingId)
            ?: throw ServiceException(AppMessage.MCP_USER_SETTING_NOT_FOUND)
        return llmMcpUserSettingRepository.deleteById(userSettingId)
    }

    fun buildMcpServer(mcpIds: List<String>, userId: String = StringUtil.EMPTY_STRING): List<DefaultMcpClient> {
        logger.info("Starting up mcp servers")
        return mcpIds.mapNotNull { id ->
            try {
                userId.takeIf { it.isNotBlank() }
                    ?.let { buildUserSpecificMcpClient(id, it) }
                    ?: buildDefaultMcpClient(id)
            } catch (e: ServiceException) {
                throw e
            } catch (e: Exception) {
                logger.error("Failed to create MCP client for ID: $id", e)
                null
            }
        }
    }

    private fun buildUserSpecificMcpClient(mcpSettingId: String, userId: String): DefaultMcpClient? {
        val client = llmMcpUserSettingRepository.findByCondition(mcpSettingId, userId)?.firstNotNullOfOrNull { data ->
            val mcpType = data.type ?: run {
                logger.warn("MCP 使用者設定 [$mcpSettingId] 缺少 type，略過")
                return@firstNotNullOfOrNull null
            }
            // settingContent 經 JPQL 已由 converter 解密為整包明文；env 值若為舊版逐值密文則一併還原（相容舊資料）
            val envKeys = data.commandSetting?.env?.keys.orEmpty()
            val transport = createTransport(
                type = mcpType,
                commandSetting = data.commandSetting,
                settingContent = decryptEnvValues(data.settingContent, envKeys)
            )
            transport?.let { DefaultMcpClient.Builder().transport(it).build() }
        }
        // 呼叫端以 mapNotNull 蒐集，回 null 會被靜默丟棄：MCP 工具沒掛上但毫無線索，
        // 除錯時極易誤判為「模型不肯呼叫工具」（實際案例見 e2e 週期 202607312223 的 ISSUE-1／ISSUE-2）。
        if (client == null) {
            logger.warn(
                "MCP 使用者設定 [$mcpSettingId] 對使用者 [$userId] 查無資料或無法建立 client，本次不會掛載此 MCP；" +
                    "請確認 userSettingId 正確且屬於當前登入者"
            )
        }
        return client
    }

    private fun buildDefaultMcpClient(mcpId: String): DefaultMcpClient? {
        val data = llmMcpServerRepository.findById(mcpId)
            ?: throw ServiceException(AppMessage.MCP_SERVER_SETTING_NOT_FOUND, mcpId)
        val transport = createTransport(
            type = data.type,
            commandSetting = data.commandSetting,
            isUserSetting = false
        )
        return transport?.let { DefaultMcpClient.Builder().transport(it).build() }
    }

    private fun createTransport(
        type: McpType,
        commandSetting: McpCommandSetting?,
        settingContent: Map<String, String>? = null,
        isUserSetting: Boolean = true
    ): McpTransport? {
        return when (type) {
            McpType.STDIO -> {
                if (isUserSetting && settingContent != null) {
                    validateArguments(settingContent, commandSetting?.argsDesc)
                    validateEnvironmentVariables(settingContent, commandSetting?.env)

                    val commandList = replacePlaceholders(
                        commandSetting?.args.orEmpty(),
                        settingContent
                    ).toMutableList()
                    commandList.add(0, commandSetting?.command.orEmpty())

                    val envVars = extractMatchingSettings(
                        settingContent,
                        commandSetting?.env.orEmpty()
                    )

                    StdioMcpTransport.Builder()
                        .command(commandList)
                        .environment(envVars)
                        .logEvents(mcpLogEnable.toBoolean())
                        .build()
                } else {
                    val commandList = mutableListOf(commandSetting?.command.orEmpty())
                    commandList.addAll(commandSetting?.args.orEmpty())

                    StdioMcpTransport.Builder()
                        .command(commandList)
                        .environment(commandSetting?.env.orEmpty())
                        .logEvents(mcpLogEnable.toBoolean())
                        .build()
                }
            }

            McpType.SSE -> StreamableHttpMcpTransport.Builder()
                .url(commandSetting?.server.orEmpty())
                .logRequests(mcpLogEnable.toBoolean())
                .logResponses(mcpLogEnable.toBoolean())
                .build()

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
        validateMapSettings(userArgs, argsDesc, "參數", AppMessage.MCP_ARGS_SETTING_INVALID)
    }

    private fun validateEnvironmentVariables(userEnv: Map<String, String>?, serverEnv: Map<String, String>?) {
        validateMapSettings(userEnv, serverEnv, "環境變數", AppMessage.MCP_ENV_SETTING_INVALID)
    }

    private fun validateMapSettings(
        userSettings: Map<String, String>?,
        requiredSettings: Map<String, String>?,
        settingType: String,
        errorMessage: AppMessage
    ) {
        if (requiredSettings.isNullOrEmpty()) {
            return
        } else if (userSettings.isNullOrEmpty()) {
            throw ServiceException(errorMessage)
        }

        // Check if all keys in requiredSettings exist in userSettings
        val missingSettings = requiredSettings.keys.filter { !userSettings.containsKey(it) }
        if (missingSettings.isNotEmpty()) {
            throw ServiceException(AppMessage.MCP_MISSING_ARGS, settingType, missingSettings.joinToString(", "))
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

    private fun entityToDto(entity: LLMMcpServerEntity): McpDTO {
        return with(McpDTO()) {
            this.mcpId = entity.id
            name = entity.name
            args = entity.commandSetting.args
            argsDesc = entity.commandSetting.argsDesc
            env = entity.commandSetting.env
            envDesc = entity.commandSetting.envDesc
            type = entity.type
            description = entity.description
            this
        }
    }

    private fun createMcpCommandSetting(mcpDTO: McpDTO): McpCommandSetting {
        return when (mcpDTO.type) {
            McpType.STDIO -> {
                McpCommandSetting().apply {
                    command = mcpDTO.command
                    args = mcpDTO.args
                    argsDesc = mcpDTO.argsDesc
                    env = mcpDTO.env
                    envDesc = mcpDTO.envDesc
                }
            }

            McpType.SSE -> {
                McpCommandSetting().apply {
                    server = mcpDTO.server
                }
            }

            else -> throw IllegalArgumentException(MessageUtil.get(AppMessage.MCP_TYPE_INVALID))
        }
    }

}

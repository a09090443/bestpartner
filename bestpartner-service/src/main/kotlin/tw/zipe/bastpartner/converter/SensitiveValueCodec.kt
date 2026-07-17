package tw.zipe.bastpartner.converter

import jakarta.enterprise.context.ApplicationScoped

/**
 * 對 Map 中「指定敏感 key」的值做 AES-GCM 加密、解密與遮罩的通用元件。
 *
 * 與 [WorkflowSecretConverter] 同樣複用 [PasswordEncryptConverter] 的逐值加解密與
 * `{iv}${encrypted}` 儲存格式，但敏感 key 集合由呼叫端提供（tool 來自 config class 的
 * `@ToolConfigField(sensitive=true)`；MCP 來自 server 定義的 env key 集合），
 * 因此抽為通用元件，供 `ToolService` / `McpServerService` 共用。
 *
 * 適用於 JSON / Map 型別欄位（無法掛 JPA `@Convert`）：只加密敏感 key 的值，
 * 其餘欄位維持明文，保留查詢與部分顯示能力。
 *
 * @author Gary
 */
@ApplicationScoped
class SensitiveValueCodec(
    private val passwordEncryptConverter: PasswordEncryptConverter
) {
    companion object {
        /**
         * 對外回傳時的遮罩值。與 [WorkflowSecretConverter.SECRET_MASK] 刻意同值：
         * 全專案的「機密已保留」語意一致。存檔時若某敏感值仍為此字串，代表未更動，沿用既有密文。
         */
        const val SECRET_MASK = "__SECRET_KEPT__"
    }

    /**
     * 存檔前加密 [sensitiveKeys] 對應的值。
     * - 值為 [SECRET_MASK]：沿用 [existing] 中的既有值（既有為舊明文則補加密）；無既有值則移除該 key。
     * - 其他值：加密。
     * 非敏感 key 與非字串值原樣保留。
     */
    fun encryptMap(
        data: Map<String, Any?>,
        sensitiveKeys: Set<String>,
        existing: Map<String, Any?>?
    ): Map<String, Any?> = data.mapNotNull { (key, value) ->
        if (key !in sensitiveKeys) return@mapNotNull key to value
        val raw = value as? String ?: return@mapNotNull key to value
        val stored = if (raw == SECRET_MASK) {
            reEncryptLegacyPlaintext(existing?.get(key) as? String)
        } else {
            passwordEncryptConverter.convertToDatabaseColumn(raw)
        }
        stored?.let { key to it }
    }.toMap()

    /**
     * 對外回傳前遮罩：[sensitiveKeys] 對應的值一律換成 [SECRET_MASK]，明文與密文皆不外流。
     */
    fun maskMap(data: Map<String, Any?>, sensitiveKeys: Set<String>): Map<String, Any?> =
        data.mapValues { (key, value) -> if (key in sensitiveKeys) SECRET_MASK else value }

    /**
     * 使用前解密 [sensitiveKeys] 對應的值還原明文。舊明文（非密文格式）原樣回傳。
     */
    fun decryptMap(data: Map<String, Any?>, sensitiveKeys: Set<String>): Map<String, Any?> =
        data.mapValues { (key, value) ->
            if (key in sensitiveKeys && value is String) {
                passwordEncryptConverter.convertToEntityAttribute(value)
            } else {
                value
            }
        }

    /**
     * 沿用既有值：已是密文（`{iv}${encrypted}` 兩段格式）則原樣；舊明文則補加密，
     * 避免使用者從不改動該值時明文永久殘留。格式判定與 [PasswordEncryptConverter] 容錯解密一致。
     */
    private fun reEncryptLegacyPlaintext(existing: String?): String? {
        existing ?: return null
        return if (existing.split("$").size == 2) existing
        else passwordEncryptConverter.convertToDatabaseColumn(existing)
    }
}

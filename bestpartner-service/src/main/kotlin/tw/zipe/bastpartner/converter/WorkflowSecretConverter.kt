package tw.zipe.bastpartner.converter

import jakarta.enterprise.context.ApplicationScoped
import tw.zipe.bastpartner.enumerate.NodeType

/**
 * Workflow 節點 config 內機密欄位（HTTP_REQUEST 的 secretHeaders）的加密、解密與遮罩。
 *
 * 為何不做成 JPA AttributeConverter：node 的 config 是 `@JdbcTypeCode(SqlTypes.JSON)` 的整包 Map，
 * 掛 `@Convert` 會與 JSON 型別標註衝突，且會連帶把所有節點型別的全部設定一起加密（失去 JSON 查詢能力）。
 * 因此改在 service 邊界對 secretHeaders 逐「值」加解密，config 其餘欄位維持明文 JSON。
 *
 * 儲存格式沿用 [PasswordEncryptConverter]（`{iv}${encrypted}`）：
 * 既有明文於讀取時原樣回傳，下次存檔即自動轉為密文，毋須停機或跑遷移。
 *
 * @author Gary
 */
@ApplicationScoped
class WorkflowSecretConverter(
    private val passwordEncryptConverter: PasswordEncryptConverter
) {
    companion object {
        const val SECRET_HEADERS_FIELD = "secretHeaders"

        /**
         * 對外回傳時的遮罩值。明文一律不離開後端。
         * save 時若某 header 的值仍為此字串，代表前端未更動該值，沿用 DB 既有密文。
         */
        const val SECRET_MASK = "__SECRET_KEPT__"
    }

    /**
     * 寫入 DB 前加密。值為 [SECRET_MASK] 時沿用 [existingSecrets] 中的既有密文；
     * 若既有值不存在（例如改了 header 名稱或為新建 workflow），則移除該欄位，
     * 避免把遮罩字面值當成真金鑰存入。
     */
    fun encryptForStorage(
        type: NodeType,
        config: Map<String, Any?>,
        existingSecrets: Map<String, Any?>?
    ): Map<String, Any?> = mapSecretHeaders(type, config) { key, value ->
        if (value == SECRET_MASK) {
            reEncryptLegacyPlaintext(existingSecrets?.get(key))
        } else {
            passwordEncryptConverter.convertToDatabaseColumn(value)
        }
    }

    /**
     * 沿用既有值：已是密文（`{iv}${encrypted}` 兩段格式）則原樣保留；
     * 加密上線前存入的舊明文則補加密，避免使用者從不改動該 header 時明文永久殘留 DB。
     * 格式判定與 [PasswordEncryptConverter.convertToEntityAttribute] 的容錯解密一致。
     */
    private fun reEncryptLegacyPlaintext(existing: Any?): Any? {
        val value = existing as? String ?: return existing
        return if (value.split("$").size == 2) value
        else passwordEncryptConverter.convertToDatabaseColumn(value)
    }

    /**
     * 對外回傳（含 API 回應與執行紀錄）前遮罩：值一律換成 [SECRET_MASK]。
     */
    fun maskForResponse(type: NodeType, config: Map<String, Any?>?): Map<String, Any?> =
        mapSecretHeaders(type, config.orEmpty()) { _, _ -> SECRET_MASK }

    /**
     * 執行前解密還原明文，供實際發出 HTTP 請求使用。
     */
    fun decryptForExecution(type: NodeType, config: Map<String, Any?>?): Map<String, Any?> =
        mapSecretHeaders(type, config.orEmpty()) { _, value ->
            passwordEncryptConverter.convertToEntityAttribute(value)
        }

    /**
     * 取出 config 中的 secretHeaders（密文或明文皆可能）。供 save 前蒐集既有值使用。
     */
    @Suppress("UNCHECKED_CAST")
    fun secretHeadersOf(config: Map<String, Any?>?): Map<String, Any?>? =
        config?.get(SECRET_HEADERS_FIELD) as? Map<String, Any?>

    /**
     * 逐值套用 [transform] 至 secretHeaders；回傳 null 代表移除該欄位。
     * 僅 HTTP_REQUEST 具備此欄位，其餘節點型別原樣返回。
     */
    private fun mapSecretHeaders(
        type: NodeType,
        config: Map<String, Any?>,
        transform: (key: String, value: String) -> Any?
    ): Map<String, Any?> {
        if (type != NodeType.HTTP_REQUEST) return config
        val secrets = secretHeadersOf(config) ?: return config
        val mapped = secrets.mapNotNull { (key, value) ->
            // 非字串值不屬合法 header，原樣保留交由既有 config 型別驗證回報
            val raw = value as? String ?: return@mapNotNull key to value
            transform(key, raw)?.let { key to it }
        }.toMap()
        return config + (SECRET_HEADERS_FIELD to mapped)
    }
}

package tw.zipe.bastpartner.converter

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

/**
 * JPA AttributeConverter：對 llm_mcp_user_setting.setting_content 整包做 AES-GCM 透明加解密。
 *
 * setting_content 內含 MCP server 的 token / API key 等機密，故整欄加密（而非僅選擇性加密個別 env key）；
 * 也因此欄位型別由 json 改為 text——密文為不透明字串，不需（也不應）保留 JSON 查詢能力。
 *
 * 儲存流程：Map<String,String> → JSON 字串 → AES-GCM → "{iv}${encrypted}" 文字。
 *
 * 相容既有明文 JSON：解密端沿用 [PasswordEncryptConverter] 的容錯規則，
 * 非 "{iv}${encrypted}" 兩段格式者原樣視為明文 JSON 回傳，下次存檔即自動轉密文，毋須停機遷移。
 *
 * 本表所有存取路徑皆為 JPA / JPQL（Panache find、executeJpqlSelect、findById+update），
 * 皆會套用本 converter，故 service 層取得的 settingContent 一律為明文，毋須另行加解密。
 *
 * @author Gary
 */
@ApplicationScoped
@Converter
class McpSettingEncryptConverter : AttributeConverter<Map<String, String>, String> {

    @Inject
    lateinit var objectMapper: ObjectMapper

    @Inject
    lateinit var passwordEncryptConverter: PasswordEncryptConverter

    private val mapType = object : TypeReference<Map<String, String>>() {}

    /**
     * 寫入 DB 前：Map → JSON → 密文。null 原樣保留。
     */
    override fun convertToDatabaseColumn(attribute: Map<String, String>?): String? {
        if (attribute == null) return null
        val json = objectMapper.writeValueAsString(attribute)
        return passwordEncryptConverter.convertToDatabaseColumn(json)
    }

    /**
     * 從 DB 讀出後：密文（或舊明文 JSON）→ JSON → Map。
     * 空值或解析失敗一律回傳空 Map，避免上層 NPE。
     */
    override fun convertToEntityAttribute(dbData: String?): Map<String, String> {
        if (dbData.isNullOrEmpty()) return emptyMap()
        val json = passwordEncryptConverter.convertToEntityAttribute(dbData) ?: return emptyMap()
        return runCatching { objectMapper.readValue(json, mapType) }
            .getOrDefault(emptyMap())
    }
}

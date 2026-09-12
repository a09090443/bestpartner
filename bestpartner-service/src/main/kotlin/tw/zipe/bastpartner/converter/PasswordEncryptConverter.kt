package tw.zipe.bastpartner.converter

import jakarta.enterprise.context.ApplicationScoped
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import org.eclipse.microprofile.config.inject.ConfigProperty
import tw.zipe.bastpartner.util.CryptoUtils

/**
 * JPA AttributeConverter：對 password 欄位進行 AES-256-GCM 透明加解密
 *
 * 儲存格式（v2，現行）：`v2${salt}${iv}${encrypted}`（四段，皆 Base64）。
 * 金鑰以 PBKDF2-HMAC-SHA256 + per-record 隨機 salt 由 `crypto.secret-key` 衍生，強度高於舊做法。
 *
 * 相容格式：
 * - legacy（單次 SHA-256、無 salt）：`{iv}${encrypted}`（兩段），仍可解密（讀舊資料）；下次存檔即升級為 v2。
 * - 非上述格式：視為明文原樣回傳（相容加密上線前的舊資料）。
 *
 * @author Gary
 */
@ApplicationScoped
@Converter
class PasswordEncryptConverter : AttributeConverter<String, String> {

    companion object {
        /** 現行密文版本前綴。改動格式時一併調整 [isCiphertext] 與 [convertToEntityAttribute]。 */
        const val CIPHER_VERSION_V2 = "v2"
    }

    @ConfigProperty(name = "crypto.secret-key")
    lateinit var secretKey: String

    /**
     * 寫入 DB 前加密：明文 → `v2${salt}${iv}${encrypted}`
     */
    override fun convertToDatabaseColumn(attribute: String?): String? {
        if (attribute.isNullOrEmpty()) return attribute
        val result = CryptoUtils.encryptAesGCMv2(attribute, secretKey)
        return "$CIPHER_VERSION_V2\$${result["salt"]}\$${result["iv"]}\$${result["encrypted"]}"
    }

    /**
     * 從 DB 讀出後解密：v2 四段 → PBKDF2；legacy 兩段 → 舊 SHA-256；其餘視為明文原樣回傳。
     */
    override fun convertToEntityAttribute(dbData: String?): String? {
        if (dbData.isNullOrEmpty()) return dbData
        val parts = dbData.split("$")
        return when {
            parts.size == 4 && parts[0] == CIPHER_VERSION_V2 -> runCatching {
                CryptoUtils.decryptAesGCMv2(parts[3], secretKey, parts[2], parts[1])
            }.getOrDefault(dbData)

            parts.size == 2 -> runCatching {
                CryptoUtils.decryptAesGCM(parts[1], secretKey, parts[0])
            }.getOrDefault(dbData)

            else -> dbData   // 非加密格式，原樣回傳（相容舊明文）
        }
    }

    /**
     * 判斷字串是否為本 converter 產生的密文（v2 四段或 legacy 兩段）。
     * 供「沿用既有值」的呼叫端（[SensitiveValueCodec]、[WorkflowSecretConverter]）辨識既有密文，
     * 避免把密文再包一層加密。
     */
    fun isCiphertext(value: String): Boolean {
        val parts = value.split("$")
        return (parts.size == 4 && parts[0] == CIPHER_VERSION_V2) || parts.size == 2
    }
}

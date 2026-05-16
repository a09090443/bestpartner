package tw.zipe.bastpartner.converter

import jakarta.enterprise.context.ApplicationScoped
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import org.eclipse.microprofile.config.inject.ConfigProperty
import tw.zipe.bastpartner.util.CryptoUtils

/**
 * JPA AttributeConverter：對 password 欄位進行 AES-GCM 透明加解密
 *
 * 儲存格式：{iv_base64}${encrypted_base64}
 *
 * @author Gary
 */
@ApplicationScoped
@Converter
class PasswordEncryptConverter : AttributeConverter<String, String> {

    @ConfigProperty(name = "crypto.secret-key")
    lateinit var secretKey: String

    /**
     * 寫入 DB 前加密：明文 → "{iv}${encrypted}"
     */
    override fun convertToDatabaseColumn(attribute: String?): String? {
        if (attribute.isNullOrEmpty()) return attribute
        val result = CryptoUtils.encryptAesGCM(attribute, secretKey)
        return "${result["iv"]}$${result["encrypted"]}"
    }

    /**
     * 從 DB 讀出後解密："{iv}${encrypted}" → 明文
     */
    override fun convertToEntityAttribute(dbData: String?): String? {
        if (dbData.isNullOrEmpty()) return dbData
        val parts = dbData.split("$")
        if (parts.size != 2) return dbData   // 非加密格式，原樣回傳（相容舊資料）
        return runCatching {
            CryptoUtils.decryptAesGCM(parts[1], secretKey, parts[0])
        }.getOrDefault(dbData)
    }
}

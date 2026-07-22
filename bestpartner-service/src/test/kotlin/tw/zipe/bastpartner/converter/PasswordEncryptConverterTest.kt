package tw.zipe.bastpartner.converter

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tw.zipe.bastpartner.util.CryptoUtils

/**
 * 純單元測試（非 QuarkusTest）：手動注入測試金鑰。
 * 涵蓋 api_key / password 等欄位共用的加解密路徑與向後相容。
 */
class PasswordEncryptConverterTest {

    private val secret = "unit-test-secret-key"
    private val converter = PasswordEncryptConverter().apply { secretKey = secret }

    @Test
    fun `新密文為 v2 四段格式，且可解回原值`() {
        val plaintext = "sk-or-v1-abcdef0123456789"
        val cipher = converter.convertToDatabaseColumn(plaintext)!!

        val parts = cipher.split("$")
        assertEquals(4, parts.size, "v2 密文應為四段")
        assertEquals(PasswordEncryptConverter.CIPHER_VERSION_V2, parts[0])
        assertFalse(cipher.contains(plaintext), "密文不得含明文")

        assertEquals(plaintext, converter.convertToEntityAttribute(cipher))
    }

    @Test
    fun `相同明文兩次加密因隨機 salt 而不同`() {
        val a = converter.convertToDatabaseColumn("same")!!
        val b = converter.convertToDatabaseColumn("same")!!
        assertFalse(a == b, "隨機 salt/iv 應使密文不重複")
    }

    @Test
    fun `legacy 兩段密文仍可解密（向後相容）`() {
        // 以舊做法（單次 SHA-256、無 salt）加密，模擬升級前既有資料
        val legacy = CryptoUtils.encryptAesGCM("legacy-secret", secret)
        val legacyStored = "${legacy["iv"]}\$${legacy["encrypted"]}"

        assertTrue(converter.isCiphertext(legacyStored))
        assertEquals("legacy-secret", converter.convertToEntityAttribute(legacyStored))
    }

    @Test
    fun `舊明文原樣回傳，不誤判為密文`() {
        val plain = "plain-api-key-no-delimiter"
        assertFalse(converter.isCiphertext(plain))
        assertEquals(plain, converter.convertToEntityAttribute(plain))
    }

    @Test
    fun `isCiphertext 辨識 v2 與 legacy 密文`() {
        val v2 = converter.convertToDatabaseColumn("x")!!
        assertTrue(converter.isCiphertext(v2), "v2 四段應辨識為密文")

        val legacy = CryptoUtils.encryptAesGCM("x", secret)
        assertTrue(converter.isCiphertext("${legacy["iv"]}\$${legacy["encrypted"]}"), "legacy 兩段應辨識為密文")
    }

    @Test
    fun `null 與空字串原樣回傳`() {
        assertEquals(null, converter.convertToDatabaseColumn(null))
        assertEquals("", converter.convertToDatabaseColumn(""))
        assertEquals(null, converter.convertToEntityAttribute(null))
    }
}

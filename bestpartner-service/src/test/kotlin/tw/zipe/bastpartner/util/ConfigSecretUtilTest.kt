package tw.zipe.bastpartner.util

import io.smallrye.config.crypto.AESGCMNoPaddingSecretKeysHandler
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 驗證 ConfigSecretUtil 產生的密文可被 SmallRye 的 AESGCMNoPaddingSecretKeysHandler 解密。
 *
 * 此測試是「設定檔加密」與上游函式庫之間的契約守門員：
 * 若 smallrye-config-crypto 升版後密文格式變動，這裡會先紅燈，
 * 而不是等到服務啟動解密失敗才發現。
 *
 * 純 JUnit（非 @QuarkusTest），直接對 handler 類別驗證。
 *
 * @author Gary
 */
class ConfigSecretUtilTest {

    private val secretKey = "test-root-key-for-config-secrets"

    /** handler 於 encryption-key-decode=false（預設）時，直接取金鑰字串的 UTF-8 bytes */
    private fun handler(key: String = secretKey) =
        AESGCMNoPaddingSecretKeysHandler(key.toByteArray(Charsets.UTF_8))

    @Test
    fun `加密後可由 SmallRye handler 還原為原文`() {
        val plainText = "pgpass"
        val encrypted = ConfigSecretUtil.encrypt(plainText, secretKey)

        assertEquals(plainText, handler().decode(encrypted))
    }

    @Test
    fun `含特殊字元與非 ASCII 的機密可正確還原`() {
        val plainText = "sk-or-v1-A1b2!@#\$%^&*()_+=/\\ 中文密碼"
        val encrypted = ConfigSecretUtil.encrypt(plainText, secretKey)

        assertEquals(plainText, handler().decode(encrypted))
    }

    @Test
    fun `每次加密因隨機 IV 產生不同密文但解密結果相同`() {
        val plainText = "same-plain-text"
        val first = ConfigSecretUtil.encrypt(plainText, secretKey)
        val second = ConfigSecretUtil.encrypt(plainText, secretKey)

        assertNotEquals(first, second)
        assertEquals(plainText, handler().decode(first))
        assertEquals(plainText, handler().decode(second))
    }

    @Test
    fun `密文為 URL-safe Base64 不含 properties 難處理的字元`() {
        val encrypted = ConfigSecretUtil.encrypt("value-to-encrypt", secretKey)

        assertEquals(encrypted, encrypted.trim())
        assertTrue(encrypted.all { it.isLetterOrDigit() || it == '-' || it == '_' }) {
            "密文含非 URL-safe Base64 字元：$encrypted"
        }
    }

    @Test
    fun `decrypt 可還原自身產生的密文`() {
        val plainText = "sk-or-v1-A1b2!@#\$%^&*()_+=/\\ 中文密碼"
        val encrypted = ConfigSecretUtil.encrypt(plainText, secretKey)

        assertEquals(plainText, ConfigSecretUtil.decrypt(encrypted, secretKey))
    }

    @Test
    fun `decrypt 與 SmallRye handler 解出相同結果`() {
        val plainText = "prod-db-password"
        val encrypted = ConfigSecretUtil.encrypt(plainText, secretKey)

        assertEquals(handler().decode(encrypted), ConfigSecretUtil.decrypt(encrypted, secretKey))
    }

    @Test
    fun `decrypt 以錯誤金鑰會失敗而非回傳亂碼`() {
        val encrypted = ConfigSecretUtil.encrypt("pgpass", secretKey)

        val result = runCatching { ConfigSecretUtil.decrypt(encrypted, "wrong-key") }

        assertTrue(result.isFailure) { "錯誤金鑰竟成功解密，GCM 完整性驗證未生效" }
    }

    @Test
    fun `以錯誤金鑰解密會失敗`() {
        val encrypted = ConfigSecretUtil.encrypt("pgpass", secretKey)

        val result = runCatching { handler("wrong-key").decode(encrypted) }

        assertTrue(result.isFailure) { "錯誤金鑰竟成功解密，GCM 完整性驗證未生效" }
    }
}

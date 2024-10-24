package tw.zipe.bastpartner.util

import java.nio.file.Path
import java.nio.file.Paths
import org.junit.jupiter.api.Test


/**
 * @author Gary
 * @created 2024/10/24
 */
class CryptoUtilsTest {

    @Test
    fun `test sha256`() {
        val input = "Hello, World!"
        val expected = "dffd6021bb2bd5b0af676290809ec3a53191dd81c7f70a4b28688a362182986f"
        val actual = CryptoUtils.sha256(input)
        assert(expected == actual)
    }

    @Test
    fun `test sha512`() {
        val input = "Hello, World!"
        val expected =
            "374d794a95cdcfd8b35993185fef9ba368f160d8daf432d08ba9f1ed1e5abe6cc69291e0fa2fe0006a52570ef18c19def4e617c33ce52ef0a6e5fbe318cb0387"
        val actual = CryptoUtils.sha512(input)
        assert(expected == actual)
    }

    @Test
    fun `test cert`() {
        val publicKey = CryptoUtils.loadPublicKeyFromCertificate("src/main/resources/cert/bestpartner.crt")
        val privateKey = CryptoUtils.loadPrivateKeyFromPKCS12(
            "src/main/resources/cert/bestpartner.p12",
            "bestpartner",
            "bestpartner"
        )
        val originalText = "Hello, World!"
        val encrypted = CryptoUtils.encrypt(originalText, publicKey)
        val decrypted = CryptoUtils.decrypt(encrypted, privateKey)
        assert(originalText == decrypted)
    }

    @Test
    fun `test aes`() {
        // 生成 AES 金鑰
        val aesKey = CryptoUtils.generateAesKey() // 預設 256 位元

        // CBC 模式加密（推薦）
        val dataToEncrypt = "Hello, World!"
        val encryptedResult = CryptoUtils.encryptAesCBC(dataToEncrypt, aesKey)
        val encryptedData = encryptedResult["encrypted"]!! // 加密後的數據
        val iv = encryptedResult["iv"]!! // 初始向量

        // CBC 模式解密
        val decryptedData = CryptoUtils.decryptAesCBC(encryptedData, aesKey, iv)
        assert(dataToEncrypt == decryptedData)

        // ECB 模式（較簡單但安全性較低）
        val encryptedECB = CryptoUtils.encryptAesECB(dataToEncrypt, aesKey)
        val decryptedECB = CryptoUtils.decryptAesECB(encryptedECB, aesKey)
        assert(dataToEncrypt == decryptedECB)
    }
}

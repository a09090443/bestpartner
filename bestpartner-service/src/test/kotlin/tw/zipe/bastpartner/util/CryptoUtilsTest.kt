package tw.zipe.bastpartner.util

import java.security.Security
import org.bouncycastle.jce.provider.BouncyCastleProvider
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
        // 檢查 BouncyCastle 是否已註冊
        if (Security.getProvider("BC") == null) {
            Security.addProvider(BouncyCastleProvider())
            println("BouncyCastle provider registered.")
        } else {
            println("BouncyCastle provider is already registered.")
        }
        // 載入 RSA 公鑰和私鑰
        val publicKey = CryptoUtils.loadPublicKeyFromCertificate("src/main/resources/cert/bestpartner.crt")
        val privateKey = CryptoUtils.loadPrivateKeyFromPKCS12(
            "src/main/resources/cert/bestpartner.p12",
            "bestpartner",
            "bestpartner"
        )

        // 原始文字
        val originalText = "Hello, World!"

        // 加密
        val encrypted = CryptoUtils.rsaEncrypt(originalText, publicKey)

        // 確保加密結果非空
        assert(encrypted.isNotEmpty()) { "Encryption failed, resulting in empty string." }

        // 解密
        val decrypted = CryptoUtils.rsaDecrypt(encrypted, privateKey)

        // 確認解密後文字與原始文字相同
        assert(originalText == decrypted) { "Decryption failed, expected $originalText but got $decrypted" }
    }

    @Test
    fun `test aes`() {
        // 生成 AES 金鑰
        val aesKey = CryptoUtils.generateAesKey() // 預設 256 位元

        // GCN 模式加密（推薦）
        val dataToEncrypt = "Hello, World!"
        val encryptedResult = CryptoUtils.encryptAesGCM(dataToEncrypt, aesKey)
        val encryptedData = encryptedResult["encrypted"]!! // 加密後的數據
        val iv = encryptedResult["iv"]!! // 初始向量

        // CBC 模式解密
        val decryptedData = CryptoUtils.decryptAesGCM(encryptedData, aesKey, iv)
        assert(dataToEncrypt == decryptedData)
    }

    @Test
    fun `v2 AES-GCM + PBKDF2 加解密可還原`() {
        val key = "master-secret-key"
        val plaintext = "sk-or-v1-abcdef0123456789"

        val result = CryptoUtils.encryptAesGCMv2(plaintext, key)
        // 三段皆存在且密文不含明文
        assert(result["salt"]!!.isNotEmpty())
        assert(result["iv"]!!.isNotEmpty())
        assert(!result["encrypted"]!!.contains(plaintext))

        val decrypted = CryptoUtils.decryptAesGCMv2(result["encrypted"]!!, key, result["iv"]!!, result["salt"]!!)
        assert(plaintext == decrypted)
    }

    @Test
    fun `v2 每次加密使用不同 salt 與 iv，密文不重複`() {
        val key = "master-secret-key"
        val plaintext = "same-plaintext"

        val a = CryptoUtils.encryptAesGCMv2(plaintext, key)
        val b = CryptoUtils.encryptAesGCMv2(plaintext, key)

        // 相同明文＋相同金鑰，兩次密文因隨機 salt/iv 而不同
        assert(a["salt"] != b["salt"])
        assert(a["iv"] != b["iv"])
        assert(a["encrypted"] != b["encrypted"])
        // 但皆可正確解回原值
        assert(CryptoUtils.decryptAesGCMv2(a["encrypted"]!!, key, a["iv"]!!, a["salt"]!!) == plaintext)
        assert(CryptoUtils.decryptAesGCMv2(b["encrypted"]!!, key, b["iv"]!!, b["salt"]!!) == plaintext)
    }

    @Test
    fun `v2 錯誤金鑰無法解密`() {
        val result = CryptoUtils.encryptAesGCMv2("secret", "right-key")
        val failed = runCatching {
            CryptoUtils.decryptAesGCMv2(result["encrypted"]!!, "wrong-key", result["iv"]!!, result["salt"]!!)
        }
        assert(failed.isFailure) { "錯誤金鑰應因 GCM 完整性驗證失敗而拋錯" }
    }
}

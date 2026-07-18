package tw.zipe.bastpartner.util

import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * 設定檔機密值加密工具（開發／維運用，非執行期路徑）
 *
 * 產生的密文供 .env.<profile> 以 `${enc::<密文>}` 引用，
 * 由 SmallRye Config 的 AESGCMNoPaddingSecretKeysHandler 於啟動時解密
 * （`enc` 為本專案短名 handler，見 config/EncSecretKeysHandlerFactory.kt）。
 *
 * ⚠️ 格式必須與該 handler 完全一致（已對照 smallrye-config-crypto 3.12.3 bytecode）：
 *   - 金鑰：SHA-256(金鑰字串 UTF-8 bytes) 作為 AES-256 key
 *     （encryption-key-decode 預設 false，故金鑰字串不經 Base64 解碼）
 *   - 密文：Base64URL( [1 byte IV 長度][IV][ciphertext+tag] )
 *   - GCM tag 長度 128 bits
 *
 * 使用方式（機密優先走環境變數，避免出現在命令列參數與 shell 歷史紀錄中）：
 *   CONFIG_SECRET_VALUE=<明文> CONFIG_ENCRYPTION_KEY=<根金鑰> ./gradlew encryptConfigSecret
 *   加上 -d 旗標則為解密（供驗證既有密文用）
 *
 * @author Gary
 */
object ConfigSecretUtil {

    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH_BITS = 128

    /**
     * 將明文加密為 handler 可解讀的密文
     *
     * @param plainText 待加密的機密明文
     * @param secretKey 根金鑰字串（對應 CONFIG_ENCRYPTION_KEY）
     */
    fun encrypt(plainText: String, secretKey: String): String {
        val keyBytes = MessageDigest.getInstance("SHA-256").digest(secretKey.toByteArray(Charsets.UTF_8))
        val key = SecretKeySpec(keyBytes, "AES")

        val iv = ByteArray(GCM_IV_LENGTH).also { SecureRandom().nextBytes(it) }

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        // handler 以 ByteBuffer 依序讀取：1 byte IV 長度 → IV → 其餘全部為密文
        val payload = ByteBuffer.allocate(1 + iv.size + cipherText.size)
            .put(iv.size.toByte())
            .put(iv)
            .put(cipherText)
            .array()

        return Base64.getUrlEncoder().withoutPadding().encodeToString(payload)
    }

    /**
     * 解密由 [encrypt] 產生的密文（與 SmallRye handler 的解密結果一致）。
     *
     * 供寫入設定檔前做 round-trip 驗證，或事後核對既有密文使用。
     */
    fun decrypt(encrypted: String, secretKey: String): String {
        val keyBytes = MessageDigest.getInstance("SHA-256").digest(secretKey.toByteArray(Charsets.UTF_8))
        val key = SecretKeySpec(keyBytes, "AES")

        val payload = ByteBuffer.wrap(Base64.getUrlDecoder().decode(encrypted))
        val iv = ByteArray(payload.get().toInt()).also { payload.get(it) }
        val cipherText = ByteArray(payload.remaining()).also { payload.get(it) }

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        return String(cipher.doFinal(cipherText), Charsets.UTF_8)
    }

    @JvmStatic
    fun main(args: Array<String>) {
        // 機密優先自環境變數讀取：命令列參數會出現在行程清單中，環境變數不會
        val value = System.getenv("CONFIG_SECRET_VALUE") ?: args.getOrNull(0)
        val key = System.getenv("CONFIG_ENCRYPTION_KEY") ?: args.getOrNull(1)
        val decryptMode = args.contains("-d")

        if (value.isNullOrBlank() || key.isNullOrBlank()) {
            System.err.println(
                "用法: CONFIG_SECRET_VALUE=<值> CONFIG_ENCRYPTION_KEY=<根金鑰> ConfigSecretUtil [-d]"
            )
            kotlin.system.exitProcess(1)
        }

        println(if (decryptMode) decrypt(value, key) else encrypt(value, key))
    }
}

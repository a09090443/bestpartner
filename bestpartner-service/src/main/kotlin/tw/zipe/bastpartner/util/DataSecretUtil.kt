package tw.zipe.bastpartner.util

/**
 * 資料庫欄位機密值加解密工具（開發／維運用，非執行期路徑）。
 *
 * 產生／解讀的密文格式與 [tw.zipe.bastpartner.converter.PasswordEncryptConverter] 完全一致：
 *   - 現行 v2：`v2$salt$iv$ct`（PBKDF2-HMAC-SHA256 + per-record salt 衍生 AES-256 金鑰）
 *   - legacy：`{iv}$ct`（單次 SHA-256、無 salt），僅解密相容；加密一律輸出 v2
 * 底層演算法委由 [CryptoUtils]，本工具只負責格式組裝／拆解與 CLI 介面，
 * 確保 CLI 產生的密文與服務執行期的 converter 可互通。
 *
 * ⚠️ 金鑰為 `crypto.secret-key`（環境變數 CRYPTO_SECRET_KEY），加密**資料庫欄位**；
 *    與加密**設定值**的 CONFIG_ENCRYPTION_KEY（[ConfigSecretUtil]）是兩把不同金鑰，
 *    密文格式與金鑰推導方式皆不同，切勿混用。
 *
 * 使用方式（機密優先走環境變數，避免出現在命令列參數與 shell 歷史）：
 *   CRYPTO_SECRET_VALUE=<明文> CRYPTO_SECRET_KEY=<金鑰> ./gradlew encryptDataSecret
 *   加上 -d 旗標則為解密（CRYPTO_SECRET_VALUE 改放密文本體）
 *
 * @author Gary
 */
object DataSecretUtil {

    private const val CIPHER_VERSION_V2 = "v2"

    /**
     * 將明文加密為 converter 落地格式的 v2 密文：`v2$salt$iv$ct`。
     */
    fun encrypt(plainText: String, secretKey: String): String {
        val r = CryptoUtils.encryptAesGCMv2(plainText, secretKey)
        return "$CIPHER_VERSION_V2\$${r["salt"]}\$${r["iv"]}\$${r["encrypted"]}"
    }

    /**
     * 解密 [encrypt] 產生的 v2 密文（或 legacy 兩段密文）。
     * 分支邏輯與 [tw.zipe.bastpartner.converter.PasswordEncryptConverter.convertToEntityAttribute] 一致；
     * 金鑰不符時由 [CryptoUtils] 以 `AEADBadTagException` 失敗，不回傳錯誤明文（GCM 完整性驗證）。
     */
    fun decrypt(cipherText: String, secretKey: String): String {
        val parts = cipherText.split("$")
        return when {
            parts.size == 4 && parts[0] == CIPHER_VERSION_V2 ->
                CryptoUtils.decryptAesGCMv2(parts[3], secretKey, parts[2], parts[1])

            parts.size == 2 ->
                CryptoUtils.decryptAesGCM(parts[1], secretKey, parts[0])

            else -> error("無法辨識的密文格式（需為 v2 四段 `v2\$salt\$iv\$ct` 或 legacy 兩段 `iv\$ct`）")
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        // 機密優先自環境變數讀取：命令列參數會出現在行程清單中，環境變數不會
        val value = System.getenv("CRYPTO_SECRET_VALUE") ?: args.getOrNull(0)
        val key = System.getenv("CRYPTO_SECRET_KEY") ?: args.getOrNull(1)
        val decryptMode = args.contains("-d")

        if (value.isNullOrBlank() || key.isNullOrBlank()) {
            System.err.println(
                "用法: CRYPTO_SECRET_VALUE=<值> CRYPTO_SECRET_KEY=<金鑰> DataSecretUtil [-d]"
            )
            kotlin.system.exitProcess(1)
        }

        println(if (decryptMode) decrypt(value, key) else encrypt(value, key))
    }
}

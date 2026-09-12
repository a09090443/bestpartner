package tw.zipe.bastpartner.migration

import java.io.File
import java.sql.DriverManager
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import tw.zipe.bastpartner.converter.PasswordEncryptConverter
import tw.zipe.bastpartner.util.CryptoUtils

/**
 * 一次性維護：CRYPTO_SECRET_KEY 輪替後，把用「舊 key」加密的 llm_setting.api_key
 * 重新以「新 key」加密，避免舊密文在新 key 下解不開而失效。
 *
 * 設計重點：
 * - 支援兩種密文格式：legacy 兩段 `{iv}${enc}`（單次 SHA-256）與 v2 四段 `v2${salt}${iv}${enc}`
 *   （PBKDF2＋per-record salt）。解密依格式自動分派，重加密一律升級為 v2。
 * - 非 @QuarkusTest、純 JDBC：直接讀原始密文，避開 JPA PasswordEncryptConverter 以新 key 自動解密的干擾。
 * - 新 key 於執行期由專案根目錄 .env 讀取，不寫入任何 log；舊 key 為已知預設值。
 * - 冪等：先試新 key 解密，成功即代表已輪替，跳過；再試舊 key，成功才重加密；兩者皆失敗（明文佔位）則不動。
 * - 逐筆 round-trip 驗證（新密文可解回原值）後才在單一交易 commit；任一步失敗整批 rollback。
 * - 預設不執行：需設環境變數 REENCRYPT_RUN=true 才會跑，避免 CI/一般建置誤觸。
 */
@EnabledIfEnvironmentVariable(named = "REENCRYPT_RUN", matches = "true")
class ReEncryptApiKeyMigrationTest {

    private val oldKey = "changeme-please-replace-in-production"

    @Test
    fun `以新 key 重新加密舊 api_key`() {
        val env = loadEnv()
        val newKey = env["CRYPTO_SECRET_KEY"]
            ?: error("找不到 CRYPTO_SECRET_KEY（請確認根目錄 .env 已設定）")
        // 若新舊 key 相同，代表 .env 未載入或未更換，直接中止避免無意義的 no-op 誤導
        assertNotEquals(oldKey, newKey, "新 key 與預設舊 key 相同：.env 可能未載入或尚未更換 key")

        val url = env["DB_URL"] ?: "jdbc:postgresql://localhost:5432/pgdb?currentSchema=bestpartner"
        val user = env["DB_USERNAME"] ?: "pguser"
        val password = env["DB_PASSWORD"] ?: "pgpass"

        DriverManager.getConnection(url, user, password).use { conn ->
            conn.autoCommit = false
            // 1. 讀出所有非空 api_key（原始密文）
            val rows = mutableListOf<Pair<String, String>>() // id to storedCipher
            conn.prepareStatement(
                "SELECT id, api_key FROM bestpartner.llm_setting WHERE api_key IS NOT NULL AND api_key <> ''"
            ).use { st ->
                st.executeQuery().use { rs ->
                    while (rs.next()) rows.add(rs.getString("id") to rs.getString("api_key"))
                }
            }

            // 2. 備份現況（rollback SQL）到 build/，供必要時還原
            val ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"))
            val backup = File("build/reencrypt-backup-$ts.sql")
            backup.parentFile.mkdirs()
            backup.printWriter().use { w ->
                rows.forEach { (id, cipher) ->
                    w.println("UPDATE bestpartner.llm_setting SET api_key = '${cipher.replace("'", "''")}' WHERE id = '$id';")
                }
            }

            var migrated = 0
            var skippedPlain = 0
            var skippedAlready = 0

            conn.prepareStatement(
                "UPDATE bestpartner.llm_setting SET api_key = ? WHERE id = ?"
            ).use { upd ->
                for ((id, stored) in rows) {
                    if (!isCipherFormat(stored)) { skippedPlain++; continue } // 明文佔位，非密文格式

                    // 冪等：已可用新 key 解（v2 或 legacy 皆試）→ 早前已輪替，跳過
                    if (tryDecrypt(stored, newKey) != null) { skippedAlready++; continue }

                    // 用舊 key 解；解不開代表既非舊 key 密文也非新 key 密文 → 不動
                    val plain = tryDecrypt(stored, oldKey)
                    if (plain == null) { skippedPlain++; continue }

                    // 一律重加密為 v2（PBKDF2＋per-record salt）
                    val newStored = encryptToV2(plain, newKey)
                    // round-trip 驗證：新密文必須能以新 key 解回原值
                    check(tryDecrypt(newStored, newKey) == plain) { "round-trip 驗證失敗 id=$id" }

                    upd.setString(1, newStored)
                    upd.setString(2, id)
                    upd.executeUpdate()
                    migrated++
                    println("re-encrypted id=$id (cipher ${stored.take(8)}… -> ${newStored.take(8)}…)")
                }
            }

            conn.commit()
            println("完成：重加密 $migrated 筆、跳過(已是新 key) $skippedAlready 筆、跳過(明文/無法解) $skippedPlain 筆")
            println("備份(rollback SQL)：${backup.absolutePath}")
        }
    }

    /** 是否為密文格式：v2 四段 `v2${salt}${iv}${enc}` 或 legacy 兩段 `{iv}${enc}`；其餘視為明文佔位。 */
    private fun isCipherFormat(stored: String): Boolean {
        val parts = stored.split("\$")
        return (parts.size == 4 && parts[0] == PasswordEncryptConverter.CIPHER_VERSION_V2) || parts.size == 2
    }

    /** 以指定 key 嘗試解密，依格式自動分派 v2 / legacy；失敗回 null。 */
    private fun tryDecrypt(stored: String, key: String): String? {
        val parts = stored.split("\$")
        return when {
            parts.size == 4 && parts[0] == PasswordEncryptConverter.CIPHER_VERSION_V2 ->
                runCatching { CryptoUtils.decryptAesGCMv2(parts[3], key, parts[2], parts[1]) }.getOrNull()

            parts.size == 2 ->
                runCatching { CryptoUtils.decryptAesGCM(parts[1], key, parts[0]) }.getOrNull()

            else -> null
        }
    }

    /** 以新 key 產生 v2 密文字串 `v2${salt}${iv}${encrypted}`。 */
    private fun encryptToV2(plain: String, key: String): String {
        val r = CryptoUtils.encryptAesGCMv2(plain, key)
        return "${PasswordEncryptConverter.CIPHER_VERSION_V2}\$${r["salt"]}\$${r["iv"]}\$${r["encrypted"]}"
    }

    /** 依序嘗試常見相對位置尋找 .env（測試 cwd 通常為模組目錄，根目錄 .env 在上一層）。 */
    private fun loadEnv(): Map<String, String> {
        val candidates = listOf(File(".env"), File("../.env"), File("../../.env"))
        val file = candidates.firstOrNull { it.exists() }
            ?: error("找不到 .env（找過：${candidates.joinToString { it.absolutePath }}）")
        return file.readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") && it.contains("=") }
            .associate { line ->
                val idx = line.indexOf('=')
                line.substring(0, idx).trim() to line.substring(idx + 1).trim()
            }
    }
}

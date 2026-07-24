package tw.zipe.bastpartner.migration

import java.io.File
import java.sql.DriverManager
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import tw.zipe.bastpartner.util.CryptoUtils

/**
 * 一次性維護：把 llm_mcp_user_setting.setting_content 既有明文（整包 JSON）以 crypto.secret-key
 * 加密為 v2 密文（`v2$salt$iv$ct`），與 [tw.zipe.bastpartner.converter.McpSettingEncryptConverter]
 * 上線後的落地格式一致；上線前存入的明文列不會自動加密，故以本維護一次補齊。
 *
 * 設計重點（比照 [ReEncryptApiKeyMigrationTest]）：
 * - 非 @QuarkusTest、純 JDBC：直接讀寫原始欄位值，避開 JPA converter 自動解密的干擾。
 * - 冪等：已是 v2 四段或 legacy 兩段密文（可成功解密）者跳過；僅明文才加密。
 * - 逐筆 round-trip 驗證（新密文可解回原值）後才在單一交易 commit；任一步失敗整批 rollback。
 * - 備份現況（rollback SQL）到 build/，供必要時還原。
 * - 預設不執行：需設 ENCRYPT_MCP_SETTING_RUN=true 才會跑，避免 CI/一般建置誤觸。
 * - key 預設為本機 application.properties 預設值，可用環境變數 CRYPTO_SECRET_KEY 覆寫
 *   （務必與執行中的服務所用金鑰一致，否則服務將無法解密）。
 */
@EnabledIfEnvironmentVariable(named = "ENCRYPT_MCP_SETTING_RUN", matches = "true")
class EncryptMcpSettingContentMigrationTest {

    private fun env(name: String, default: String): String =
        System.getenv(name)?.takeIf { it.isNotBlank() } ?: default

    @Test
    fun `加密 llm_mcp_user_setting_setting_content 明文`() {
        val key = env("CRYPTO_SECRET_KEY", "changeme-please-replace-in-production")
        val url = env("DB_URL", "jdbc:postgresql://localhost:5432/pgdb?currentSchema=bestpartner")
        val user = env("DB_USERNAME", "pguser")
        val password = env("DB_PASSWORD", "pgpass")

        DriverManager.getConnection(url, user, password).use { conn ->
            conn.autoCommit = false

            // 1. 讀出所有非空 setting_content（原始欄位值）
            val rows = mutableListOf<Pair<String, String>>() // id to storedValue
            conn.prepareStatement(
                "SELECT id, setting_content FROM bestpartner.llm_mcp_user_setting " +
                    "WHERE setting_content IS NOT NULL AND setting_content <> ''"
            ).use { st ->
                st.executeQuery().use { rs ->
                    while (rs.next()) rows.add(rs.getString("id") to rs.getString("setting_content"))
                }
            }

            // 2. 備份現況（rollback SQL）到 build/
            val ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"))
            val backup = File("build/encrypt-mcp-setting-backup-$ts.sql")
            backup.parentFile.mkdirs()
            backup.printWriter().use { w ->
                rows.forEach { (id, v) ->
                    w.println(
                        "UPDATE bestpartner.llm_mcp_user_setting SET setting_content = " +
                            "'${v.replace("'", "''")}' WHERE id = '$id';"
                    )
                }
            }

            var encrypted = 0
            var skippedAlready = 0

            conn.prepareStatement(
                "UPDATE bestpartner.llm_mcp_user_setting SET setting_content = ? WHERE id = ?"
            ).use { upd ->
                for ((id, stored) in rows) {
                    // 冪等：已可解密（v2 或 legacy）即代表已加密，跳過
                    if (isAlreadyCiphertext(stored, key)) { skippedAlready++; continue }

                    val enc = CryptoUtils.encryptAesGCMv2(stored, key)
                    val newStored = "v2\$${enc["salt"]}\$${enc["iv"]}\$${enc["encrypted"]}"
                    // round-trip 驗證：新密文必須能解回原值
                    val check = CryptoUtils.decryptAesGCMv2(enc["encrypted"]!!, key, enc["iv"]!!, enc["salt"]!!)
                    assertEquals(stored, check, "round-trip 驗證失敗 id=$id")

                    upd.setString(1, newStored)
                    upd.setString(2, id)
                    upd.executeUpdate()
                    encrypted++
                    println("encrypted id=$id (${stored.take(24)}… -> ${newStored.take(12)}…)")
                }
            }

            conn.commit()
            println("完成：加密 $encrypted 筆、跳過(已是密文) $skippedAlready 筆")
            println("備份(rollback SQL)：${backup.absolutePath}")
        }
    }

    /** 已能以現行 key 解密（v2 四段或 legacy 兩段）者視為既有密文，跳過避免重複加密。 */
    private fun isAlreadyCiphertext(value: String, key: String): Boolean {
        val parts = value.split("$")
        return when {
            parts.size == 4 && parts[0] == "v2" ->
                runCatching { CryptoUtils.decryptAesGCMv2(parts[3], key, parts[2], parts[1]) }.isSuccess
            parts.size == 2 ->
                runCatching { CryptoUtils.decryptAesGCM(parts[1], key, parts[0]) }.isSuccess
            else -> false
        }
    }
}

package tw.zipe.bastpartner.util

import io.smallrye.config.EnvConfigSource
import io.smallrye.config.SmallRyeConfigBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 驗證 `${aes-gcm-nopadding::<密文>}` 這個 config expression 能在實際的
 * SmallRye Config 解析流程中被展開為明文。
 *
 * 這是「設定檔加密」設計成立的前提：密文寫在 .env.<profile>（＝環境變數 config source），
 * 服務啟動時必須能自動解密，且解密失敗要能明確報錯而非靜默回傳密文。
 *
 * @author Gary
 */
class ConfigSecretExpressionTest {

    private val rootKey = "test-root-key-for-config-secrets"

    private fun buildConfig(vararg entries: Pair<String, String>): io.smallrye.config.SmallRyeConfig {
        val values = buildMap {
            put("smallrye.config.secret-handler.aes-gcm-nopadding.encryption-key", rootKey)
            entries.forEach { (k, v) -> put(k, v) }
        }
        val builder = SmallRyeConfigBuilder()
        builder.addDefaultInterceptors()
        builder.addDiscoveredSecretKeysHandlers()
        builder.withDefaultValues(values)
        return builder.build()
    }

    @Test
    fun `設定值為密文 expression 時取值自動解密為明文`() {
        val plainText = "pgpass"
        val cipher = ConfigSecretUtil.encrypt(plainText, rootKey)

        val config = buildConfig("quarkus.datasource.password" to "\${aes-gcm-nopadding::$cipher}")

        assertEquals(plainText, config.getValue("quarkus.datasource.password", String::class.java))
    }

    @Test
    fun `API 金鑰等長字串機密同樣可解密`() {
        val plainText = "sk-or-v1-0123456789abcdef0123456789abcdef0123456789abcdef"
        val cipher = ConfigSecretUtil.encrypt(plainText, rootKey)

        val config = buildConfig("ai-platform.openrouter.api-key" to "\${aes-gcm-nopadding::$cipher}")

        assertEquals(plainText, config.getValue("ai-platform.openrouter.api-key", String::class.java))
    }

    /** 專案自訂的短名 handler（EncSecretKeysHandlerFactory），日常一律使用這個前綴 */
    @Test
    fun `短前綴 enc 可解密設定值`() {
        val plainText = "pgpass"
        val cipher = ConfigSecretUtil.encrypt(plainText, rootKey)

        val config = buildConfig("quarkus.datasource.password" to "\${enc::$cipher}")

        assertEquals(plainText, config.getValue("quarkus.datasource.password", String::class.java))
    }

    /** 短名僅換前綴、未改密文格式，故同一份密文兩種前綴都應能解 */
    @Test
    fun `同一份密文以長短兩種前綴皆可解密`() {
        val plainText = "sk-or-v1-0123456789abcdef"
        val cipher = ConfigSecretUtil.encrypt(plainText, rootKey)

        val config = buildConfig(
            "with.short.prefix" to "\${enc::$cipher}",
            "with.long.prefix" to "\${aes-gcm-nopadding::$cipher}"
        )

        assertEquals(plainText, config.getValue("with.short.prefix", String::class.java))
        assertEquals(plainText, config.getValue("with.long.prefix", String::class.java))
    }

    /**
     * application.properties 的 DB 憑證採「預設值本身即密文」的寫法：
     *   quarkus.datasource.username=${DB_USERNAME:${enc::<密文>}}
     * 環境變數缺席時（全新 clone、無任何 .env）回退到密文，再由檔內預設金鑰解開，
     * 藉此讓版控中的設定檔不出現 pguser / pgpass 字樣，同時維持「免設定啟動」。
     *
     * 此測試守住 `${enc::...}` 的 `::` 與 expression 預設值分隔符 `:` 不衝突這件事。
     */
    @Test
    fun `預設值本身為密文時可正確解析並解密`() {
        val plainText = "pguser"
        val cipher = ConfigSecretUtil.encrypt(plainText, rootKey)

        val config = buildConfig("quarkus.datasource.username" to "\${DB_USERNAME:\${enc::$cipher}}")

        assertEquals(plainText, config.getValue("quarkus.datasource.username", String::class.java))
    }

    /** 環境變數存在時應優先於密文預設值，且預設值不會被求值（故其金鑰不同也無妨） */
    @Test
    fun `環境變數存在時優先於密文預設值`() {
        val defaultCipher = ConfigSecretUtil.encrypt("pguser", rootKey)
        val envCipher = ConfigSecretUtil.encrypt("produser", rootKey)

        val builder = SmallRyeConfigBuilder()
        builder.addDefaultInterceptors()
        builder.addDiscoveredSecretKeysHandlers()
        builder.withDefaultValues(
            mapOf(
                "smallrye.config.secret-handler.aes-gcm-nopadding.encryption-key" to rootKey,
                "quarkus.datasource.username" to "\${DB_USERNAME:\${enc::$defaultCipher}}"
            )
        )
        builder.withSources(EnvConfigSource(mapOf("DB_USERNAME" to "\${enc::$envCipher}"), 300))
        val config = builder.build()

        assertEquals("produser", config.getValue("quarkus.datasource.username", String::class.java))
    }

    @Test
    fun `未加密的明文設定值維持原樣不受影響`() {
        val config = buildConfig("quarkus.datasource.username" to "pguser")

        assertEquals("pguser", config.getValue("quarkus.datasource.username", String::class.java))
    }

    /**
     * 專案的實際結構是巢狀 expression：
     *   application.properties  quarkus.datasource.password=${DB_PASSWORD:pgpass}
     *   .env.<profile>          DB_PASSWORD=${enc::<密文>}
     * 展開 ${DB_PASSWORD} 後得到的值本身又是 expression，必須能繼續展開才會得到明文。
     */
    @Test
    fun `環境變數提供的密文可透過巢狀 expression 展開為明文`() {
        val plainText = "prod-db-password"
        val cipher = ConfigSecretUtil.encrypt(plainText, rootKey)

        val builder = SmallRyeConfigBuilder()
        builder.addDefaultInterceptors()
        builder.addDiscoveredSecretKeysHandlers()
        builder.withDefaultValues(
            mapOf(
                "smallrye.config.secret-handler.aes-gcm-nopadding.encryption-key" to rootKey,
                // 對應 application.properties 的寫法，維持不變
                "quarkus.datasource.password" to "\${DB_PASSWORD:pgpass}"
            )
        )
        // 對應 .env.<profile> 經 docker --env-file 注入的環境變數
        builder.withSources(
            EnvConfigSource(mapOf("DB_PASSWORD" to "\${enc::$cipher}"), 300)
        )
        val config = builder.build()

        assertEquals(plainText, config.getValue("quarkus.datasource.password", String::class.java))
    }

    @Test
    fun `未設定環境變數時仍回退至 properties 的本機開發預設值`() {
        val builder = SmallRyeConfigBuilder()
        builder.addDefaultInterceptors()
        builder.addDiscoveredSecretKeysHandlers()
        builder.withDefaultValues(
            mapOf(
                "smallrye.config.secret-handler.aes-gcm-nopadding.encryption-key" to rootKey,
                "quarkus.datasource.password" to "\${DB_PASSWORD:pgpass}"
            )
        )
        val config = builder.build()

        assertEquals("pgpass", config.getValue("quarkus.datasource.password", String::class.java))
    }

    @Test
    fun `根金鑰錯誤時取值明確拋錯而非回傳密文`() {
        val cipher = ConfigSecretUtil.encrypt("pgpass", "a-different-root-key")

        val config = buildConfig("quarkus.datasource.password" to "\${aes-gcm-nopadding::$cipher}")

        val result: Result<String> = runCatching {
            config.getValue("quarkus.datasource.password", String::class.java)
        }

        assertTrue(result.isFailure) { "以錯誤金鑰解密竟未拋錯，可能導致密文被當成密碼使用" }
    }
}

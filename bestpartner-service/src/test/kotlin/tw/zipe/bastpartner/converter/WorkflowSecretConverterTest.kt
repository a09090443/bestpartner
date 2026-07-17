package tw.zipe.bastpartner.converter

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tw.zipe.bastpartner.enumerate.NodeType

/**
 * 純單元測試（非 QuarkusTest）：手動組裝 [PasswordEncryptConverter] 並注入測試金鑰。
 */
class WorkflowSecretConverterTest {

    private val passwordEncryptConverter = PasswordEncryptConverter().apply {
        secretKey = "unit-test-secret-key"
    }
    private val converter = WorkflowSecretConverter(passwordEncryptConverter)

    private fun httpConfig(secrets: Map<String, Any?>?, extra: Map<String, Any?> = emptyMap()) =
        buildMap {
            put("method", "GET")
            put("url", "http://api.local/data")
            putAll(extra)
            if (secrets != null) put("secretHeaders", secrets)
        }

    @Suppress("UNCHECKED_CAST")
    private fun secretsOf(config: Map<String, Any?>) =
        config["secretHeaders"] as Map<String, Any?>

    @Test
    fun `加密後密文不含明文、且可解密還原`() {
        val stored = converter.encryptForStorage(
            NodeType.HTTP_REQUEST,
            httpConfig(mapOf("Authorization" to "Bearer sk-real-key")),
            null
        )
        val cipher = secretsOf(stored)["Authorization"] as String
        assertFalse(cipher.contains("sk-real-key"), "密文不得含有明文")
        assertTrue(cipher.contains("$"), "應為 {iv}\${encrypted} 格式")

        val decrypted = converter.decryptForExecution(NodeType.HTTP_REQUEST, stored)
        assertEquals("Bearer sk-real-key", secretsOf(decrypted)["Authorization"])
    }

    @Test
    fun `非機密欄位不受影響`() {
        val stored = converter.encryptForStorage(
            NodeType.HTTP_REQUEST,
            httpConfig(
                mapOf("Authorization" to "Bearer sk-real-key"),
                extra = mapOf("headers" to mapOf("X-Tenant" to "t9"))
            ),
            null
        )
        assertEquals("GET", stored["method"])
        assertEquals("http://api.local/data", stored["url"])
        assertEquals(mapOf("X-Tenant" to "t9"), stored["headers"])
    }

    @Test
    fun `回傳前遮罩，明文與密文皆不外流`() {
        val stored = converter.encryptForStorage(
            NodeType.HTTP_REQUEST,
            httpConfig(mapOf("Authorization" to "Bearer sk-real-key")),
            null
        )
        val masked = converter.maskForResponse(NodeType.HTTP_REQUEST, stored)
        assertEquals(WorkflowSecretConverter.SECRET_MASK, secretsOf(masked)["Authorization"])
    }

    @Test
    fun `存回遮罩值時沿用既有密文，不覆寫`() {
        val original = converter.encryptForStorage(
            NodeType.HTTP_REQUEST,
            httpConfig(mapOf("Authorization" to "Bearer sk-real-key")),
            null
        )
        val existing = secretsOf(original)

        // 前端拿到遮罩後原封不動存回
        val resaved = converter.encryptForStorage(
            NodeType.HTTP_REQUEST,
            httpConfig(mapOf("Authorization" to WorkflowSecretConverter.SECRET_MASK)),
            existing
        )
        assertEquals(existing["Authorization"], secretsOf(resaved)["Authorization"])
        assertEquals(
            "Bearer sk-real-key",
            secretsOf(converter.decryptForExecution(NodeType.HTTP_REQUEST, resaved))["Authorization"]
        )
    }

    @Test
    fun `存入新值時覆寫既有密文`() {
        val original = converter.encryptForStorage(
            NodeType.HTTP_REQUEST,
            httpConfig(mapOf("Authorization" to "Bearer old-key")),
            null
        )
        val resaved = converter.encryptForStorage(
            NodeType.HTTP_REQUEST,
            httpConfig(mapOf("Authorization" to "Bearer new-key")),
            secretsOf(original)
        )
        assertEquals(
            "Bearer new-key",
            secretsOf(converter.decryptForExecution(NodeType.HTTP_REQUEST, resaved))["Authorization"]
        )
    }

    @Test
    fun `無既有值時遮罩不落地為字面值`() {
        // 例如改了 header 名稱、或新建 workflow 卻送來遮罩值
        val stored = converter.encryptForStorage(
            NodeType.HTTP_REQUEST,
            httpConfig(mapOf("X-Renamed" to WorkflowSecretConverter.SECRET_MASK)),
            null
        )
        assertNull(secretsOf(stored)["X-Renamed"], "遮罩字面值不得被當成金鑰存入")
    }

    @Test
    fun `既有明文可原樣讀取，存檔後自動轉密`() {
        // 加密機制上線前存入的舊資料：secretHeaders 為明文
        val legacy = httpConfig(mapOf("Authorization" to "Bearer legacy-plain"))

        val decrypted = converter.decryptForExecution(NodeType.HTTP_REQUEST, legacy)
        assertEquals("Bearer legacy-plain", secretsOf(decrypted)["Authorization"], "舊明文須可原樣使用")

        val stored = converter.encryptForStorage(NodeType.HTTP_REQUEST, legacy, null)
        assertFalse((secretsOf(stored)["Authorization"] as String).contains("legacy-plain"))
    }

    @Test
    fun `沿用既有明文時補加密，遮罩存回也不留明文`() {
        // DB 既有為舊明文（加密上線前），前端 get 後原樣送回遮罩
        val legacyExisting = mapOf<String, Any?>("Authorization" to "Bearer legacy-plain")
        val stored = converter.encryptForStorage(
            NodeType.HTTP_REQUEST,
            httpConfig(mapOf("Authorization" to WorkflowSecretConverter.SECRET_MASK)),
            legacyExisting
        )
        val value = secretsOf(stored)["Authorization"] as String
        assertFalse(value.contains("legacy-plain"), "沿用舊明文時須補加密，不得留明文")
        assertTrue(value.contains("$"), "應轉為密文格式")
        assertEquals(
            "Bearer legacy-plain",
            secretsOf(converter.decryptForExecution(NodeType.HTTP_REQUEST, stored))["Authorization"]
        )
    }

    @Test
    fun `沿用既有密文時不重複加密`() {
        val original = converter.encryptForStorage(
            NodeType.HTTP_REQUEST,
            httpConfig(mapOf("Authorization" to "Bearer sk-real-key")),
            null
        )
        val existing = secretsOf(original)
        val resaved = converter.encryptForStorage(
            NodeType.HTTP_REQUEST,
            httpConfig(mapOf("Authorization" to WorkflowSecretConverter.SECRET_MASK)),
            existing
        )
        // 既有已是密文格式，須原樣沿用（逐字元相同），不得再包一層加密
        assertEquals(existing["Authorization"], secretsOf(resaved)["Authorization"])
    }

    @Test
    fun `非 HTTP_REQUEST 節點不受影響`() {
        val config = mapOf("llmId" to "llm-1", "secretHeaders" to mapOf("Authorization" to "should-stay"))
        assertEquals(config, converter.encryptForStorage(NodeType.LLM_ASSISTANT, config, null))
        assertEquals(config, converter.maskForResponse(NodeType.LLM_ASSISTANT, config))
    }

    @Test
    fun `無 secretHeaders 的 config 原樣返回`() {
        val config = httpConfig(null)
        assertEquals(config, converter.encryptForStorage(NodeType.HTTP_REQUEST, config, null))
        assertEquals(config, converter.maskForResponse(NodeType.HTTP_REQUEST, config))
        assertEquals(config, converter.decryptForExecution(NodeType.HTTP_REQUEST, config))
    }
}

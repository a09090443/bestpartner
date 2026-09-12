package tw.zipe.bastpartner.converter

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SensitiveValueCodecTest {

    private val codec = SensitiveValueCodec(
        PasswordEncryptConverter().apply { secretKey = "unit-test-secret-key" }
    )

    private val sensitive = setOf("apiKey", "GITHUB_TOKEN")

    @Test
    fun `只加密敏感 key，其餘原樣`() {
        val stored = codec.encryptMap(
            mapOf("apiKey" to "sk-secret", "csi" to "017576", "timeout" to 5000),
            sensitive, null
        )
        assertFalse((stored["apiKey"] as String).contains("sk-secret"), "敏感值須加密")
        assertTrue((stored["apiKey"] as String).contains("$"), "應為密文格式")
        assertEquals("017576", stored["csi"], "非敏感值保持明文")
        assertEquals(5000, stored["timeout"], "非字串值原樣")
    }

    @Test
    fun `解密還原敏感值`() {
        val stored = codec.encryptMap(mapOf("apiKey" to "sk-secret", "csi" to "017576"), sensitive, null)
        val decrypted = codec.decryptMap(stored, sensitive)
        assertEquals("sk-secret", decrypted["apiKey"])
        assertEquals("017576", decrypted["csi"])
    }

    @Test
    fun `遮罩只換敏感 key`() {
        val masked = codec.maskMap(mapOf("apiKey" to "sk-secret", "csi" to "017576"), sensitive)
        assertEquals(SensitiveValueCodec.SECRET_MASK, masked["apiKey"])
        assertEquals("017576", masked["csi"])
    }

    @Test
    fun `遮罩值沿用既有密文不覆寫`() {
        val original = codec.encryptMap(mapOf("apiKey" to "sk-secret"), sensitive, null)
        val resaved = codec.encryptMap(
            mapOf("apiKey" to SensitiveValueCodec.SECRET_MASK),
            sensitive, original
        )
        assertEquals(original["apiKey"], resaved["apiKey"])
        assertEquals("sk-secret", codec.decryptMap(resaved, sensitive)["apiKey"])
    }

    @Test
    fun `遮罩值無既有時移除該 key，不留字面值`() {
        val stored = codec.encryptMap(
            mapOf("apiKey" to SensitiveValueCodec.SECRET_MASK),
            sensitive, null
        )
        assertNull(stored["apiKey"], "遮罩字面值不得存入")
        assertFalse(stored.containsKey("apiKey"))
    }

    @Test
    fun `既有明文送遮罩時補加密`() {
        val legacyExisting = mapOf<String, Any?>("GITHUB_TOKEN" to "ghp_plainlegacy")
        val stored = codec.encryptMap(
            mapOf("GITHUB_TOKEN" to SensitiveValueCodec.SECRET_MASK),
            sensitive, legacyExisting
        )
        val value = stored["GITHUB_TOKEN"] as String
        assertFalse(value.contains("ghp_plainlegacy"), "舊明文須補加密")
        assertTrue(value.contains("$"))
        assertEquals("ghp_plainlegacy", codec.decryptMap(stored, sensitive)["GITHUB_TOKEN"])
    }

    @Test
    fun `既有密文送遮罩時不重複加密`() {
        val original = codec.encryptMap(mapOf("apiKey" to "sk-secret"), sensitive, null)
        val resaved = codec.encryptMap(
            mapOf("apiKey" to SensitiveValueCodec.SECRET_MASK),
            sensitive, original
        )
        assertEquals(original["apiKey"], resaved["apiKey"], "既有密文須逐字元沿用")
    }

    @Test
    fun `空敏感集合時完全不動`() {
        val data = mapOf<String, Any?>("apiKey" to "sk-secret")
        assertEquals(data, codec.encryptMap(data, emptySet(), null))
        assertEquals(data, codec.maskMap(data, emptySet()))
        assertEquals(data, codec.decryptMap(data, emptySet()))
    }

    @Test
    fun `既有明文可原樣解密`() {
        val legacy = mapOf<String, Any?>("apiKey" to "sk-plain-legacy")
        assertEquals("sk-plain-legacy", codec.decryptMap(legacy, sensitive)["apiKey"])
    }
}

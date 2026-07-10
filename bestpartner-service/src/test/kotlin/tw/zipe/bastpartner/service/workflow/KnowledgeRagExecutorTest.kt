package tw.zipe.bastpartner.service.workflow

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import tw.zipe.bastpartner.dto.KnowledgeDTO
import tw.zipe.bastpartner.dto.workflow.config.KnowledgeRagNodeConfig
import tw.zipe.bastpartner.service.workflow.executor.KnowledgeRagExecutor

/**
 * KnowledgeRagExecutor 純單元測試：以 fake search lambda 驗證檢索參數傳遞，
 * 不觸網、不啟動 CDI（比照 ToolNodeExecutorTest 的 companion 測法）。
 */
class KnowledgeRagExecutorTest {

    private fun doc(text: String) = KnowledgeDTO().apply { content = text }

    @Test
    fun `topK minScore embeddingModelId 傳入檢索呼叫`() {
        val ctx = ExecutionContext("e1", "u1")
        val cfg = KnowledgeRagNodeConfig(
            knowledgeId = "k1",
            embeddingModelId = "em-9",
            query = "什麼是 RAG",
            topK = 8,
            minScore = 0.75
        )
        var captured: List<Any?>? = null
        val output = KnowledgeRagExecutor.run(cfg, ctx) { knowledgeId, query, topK, minScore, embeddingModelId ->
            captured = listOf(knowledgeId, query, topK, minScore, embeddingModelId)
            listOf(doc("段落一"), doc("段落二"))
        }
        assertEquals(listOf("k1", "什麼是 RAG", 8, 0.75, "em-9"), captured)
        assertEquals(mapOf("documents" to listOf("段落一", "段落二")), output)
    }

    @Test
    fun `topK 預設 5、minScore 預設 0_0`() {
        val ctx = ExecutionContext("e1", "u1")
        val cfg = KnowledgeRagNodeConfig(knowledgeId = "k1", query = "q")
        var capturedTopK: Int? = null
        var capturedMinScore: Double? = null
        var capturedModelId: String? = "sentinel"
        KnowledgeRagExecutor.run(cfg, ctx) { _, _, topK, minScore, embeddingModelId ->
            capturedTopK = topK
            capturedMinScore = minScore
            capturedModelId = embeddingModelId
            emptyList()
        }
        assertEquals(5, capturedTopK)
        assertEquals(0.0, capturedMinScore)
        assertNull(capturedModelId, "未設定 embeddingModelId 時應傳 null（由 EmbeddingService 落回知識庫既定模型）")
    }

    @Test
    fun `query 插值後才送入檢索`() {
        val ctx = ExecutionContext("e1", "u1")
        ctx.putOutput("trigger", mapOf("input" to mapOf("q" to "向量搜尋")))
        val cfg = KnowledgeRagNodeConfig(knowledgeId = "k1", query = "請找：{{trigger.input.q}}", outputKey = "docs")
        var capturedQuery: String? = null
        val output = KnowledgeRagExecutor.run(cfg, ctx) { _, query, _, _, _ ->
            capturedQuery = query
            listOf(doc("hit"))
        }
        assertEquals("請找：向量搜尋", capturedQuery)
        assertEquals(mapOf("docs" to listOf("hit")), output)
    }
}

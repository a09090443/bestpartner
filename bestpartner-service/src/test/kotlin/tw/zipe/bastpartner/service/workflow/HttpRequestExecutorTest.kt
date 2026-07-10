package tw.zipe.bastpartner.service.workflow

import com.sun.net.httpserver.HttpServer
import java.io.IOException
import java.net.InetSocketAddress
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tw.zipe.bastpartner.dto.workflow.config.HttpRequestNodeConfig
import tw.zipe.bastpartner.entity.WorkflowNodeEntity
import tw.zipe.bastpartner.service.workflow.executor.HttpRequestExecutor

class HttpRequestExecutorTest {

    private var server: HttpServer? = null

    @AfterEach
    fun tearDown() {
        server?.stop(0)
        server = null
    }

    /** 啟動本機 mock server，回傳 baseUrl；handler 可觀察請求並延遲回應 */
    private fun startServer(delayMs: Long = 0, capture: (headers: Map<String, String>) -> Unit = {}): String {
        val s = HttpServer.create(InetSocketAddress(0), 0)
        s.createContext("/echo") { exchange ->
            capture(exchange.requestHeaders.entries.associate { it.key to it.value.joinToString(",") })
            if (delayMs > 0) Thread.sleep(delayMs)
            val body = "ok".toByteArray()
            exchange.sendResponseHeaders(200, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        s.start()
        server = s
        return "http://localhost:${s.address.port}"
    }

    private fun node(key: String) = WorkflowNodeEntity().apply { nodeKey = key }

    @Test
    fun `buildRequest 對 url 與 body 插值`() {
        val ctx = ExecutionContext("e1", "u1")
        ctx.putOutput("trigger", mapOf("input" to mapOf("city" to "Taipei")))
        val cfg = HttpRequestNodeConfig(
            method = "POST",
            url = "http://api.local/weather?city={{trigger.input.city}}",
            body = kotlinx.serialization.json.JsonPrimitive("{\"q\":\"{{trigger.input.city}}\"}")
        )
        val req = HttpRequestExecutor.buildRequest(cfg, ctx)
        assertEquals("POST", req.method)
        assertEquals("http://api.local/weather?city=Taipei", req.url)
        assertEquals("{\"q\":\"Taipei\"}", req.body)
    }

    @Test
    fun `buildRequest 對 headers 與 secretHeaders 逐值插值並合併`() {
        val ctx = ExecutionContext("e1", "u1")
        ctx.putOutput("trigger", mapOf("input" to mapOf("token" to "abc123", "tenant" to "t9")))
        val cfg = HttpRequestNodeConfig(
            method = "GET",
            url = "http://api.local/data",
            headers = mapOf("X-Tenant" to "{{trigger.input.tenant}}", "X-Static" to "v1"),
            secretHeaders = mapOf("Authorization" to "Bearer {{trigger.input.token}}")
        )
        val req = HttpRequestExecutor.buildRequest(cfg, ctx)
        assertEquals("t9", req.headers["X-Tenant"])
        assertEquals("v1", req.headers["X-Static"])
        assertEquals("Bearer abc123", req.headers["Authorization"])
    }

    @Test
    fun `執行 GET 時 headers 與 secretHeaders 皆送達請求`() {
        var received: Map<String, String> = emptyMap()
        val baseUrl = startServer { received = it }
        val ctx = ExecutionContext("e1", "u1")
        ctx.putOutput("trigger", mapOf("input" to mapOf("token" to "s3cr3t")))
        val cfg = HttpRequestNodeConfig(
            method = "GET",
            url = "$baseUrl/echo",
            headers = mapOf("X-Custom" to "hello"),
            secretHeaders = mapOf("Authorization" to "Bearer {{trigger.input.token}}")
        )
        val output = HttpRequestExecutor().execute(node("http"), cfg, ctx)
        assertEquals("ok", output["response"])
        assertEquals("hello", received["X-custom"] ?: received["X-Custom"])
        assertEquals("Bearer s3cr3t", received["Authorization"])
    }

    @Test
    fun `timeoutMs 生效：server 延遲超過逾時即失敗`() {
        val baseUrl = startServer(delayMs = 2000)
        val ctx = ExecutionContext("e1", "u1")
        val cfg = HttpRequestNodeConfig(method = "GET", url = "$baseUrl/echo", timeoutMs = 300)
        val start = System.currentTimeMillis()
        assertThrows(IOException::class.java) {
            HttpRequestExecutor().execute(node("http"), cfg, ctx)
        }
        val elapsed = System.currentTimeMillis() - start
        assertTrue(elapsed < 1800, "應在逾時（300ms）附近失敗而非等到 server 回應，實際耗時 ${elapsed}ms")
    }

    @Test
    fun `未設 timeoutMs 時正常完成`() {
        val baseUrl = startServer(delayMs = 100)
        val ctx = ExecutionContext("e1", "u1")
        val cfg = HttpRequestNodeConfig(method = "GET", url = "$baseUrl/echo", outputKey = "resp")
        val output = HttpRequestExecutor().execute(node("http"), cfg, ctx)
        assertEquals("ok", output["resp"])
    }
}

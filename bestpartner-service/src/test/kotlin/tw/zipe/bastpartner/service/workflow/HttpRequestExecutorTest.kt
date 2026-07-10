package tw.zipe.bastpartner.service.workflow

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tw.zipe.bastpartner.dto.workflow.config.HttpRequestNodeConfig
import tw.zipe.bastpartner.service.workflow.executor.HttpRequestExecutor

class HttpRequestExecutorTest {

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
}

package tw.zipe.bastpartner.service.workflow

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import tw.zipe.bastpartner.dto.workflow.config.LlmAssistantNodeConfig
import tw.zipe.bastpartner.service.workflow.executor.LlmAssistantExecutor

/**
 * LLM 節點提問來源優先序測試（[LlmAssistantExecutor.resolveMessage] 純函式）：
 * 連入的 PROMPT 節點輸出優先於 config 的 userPrompt，兩者皆無時回 null。
 *
 * @author Gary
 * @created 2026/7/27
 */
class LlmAssistantPromptSourceTest {

    private val llmKey = "llm"

    private fun context(block: ExecutionContext.() -> Unit = {}) =
        ExecutionContext("exec-1", "user-1").apply {
            putOutput("trigger", mapOf("question" to "退貨流程"))
            block()
        }

    @Test
    fun `有提示節點輸出時優先於 userPrompt`() {
        val ctx = context {
            setPromptSources(mapOf(llmKey to listOf(PromptSource("promptA", "prompt"))))
            putOutput("promptA", mapOf("prompt" to "以正式語氣回答：退貨流程"))
        }
        val cfg = LlmAssistantNodeConfig(llmId = "m1", userPrompt = "後備提問")
        assertEquals("以正式語氣回答：退貨流程", LlmAssistantExecutor.resolveMessage(llmKey, cfg, ctx))
    }

    @Test
    fun `提示節點自訂 outputKey 也能取得`() {
        val ctx = context {
            setPromptSources(mapOf(llmKey to listOf(PromptSource("promptA", "askText"))))
            putOutput("promptA", mapOf("askText" to "自訂鍵的提問"))
        }
        val cfg = LlmAssistantNodeConfig(llmId = "m1")
        assertEquals("自訂鍵的提問", LlmAssistantExecutor.resolveMessage(llmKey, cfg, ctx))
    }

    @Test
    fun `未接提示節點時回退 userPrompt 並插值`() {
        val ctx = context()
        val cfg = LlmAssistantNodeConfig(llmId = "m1", userPrompt = "請回答：{{trigger.question}}")
        assertEquals("請回答：退貨流程", LlmAssistantExecutor.resolveMessage(llmKey, cfg, ctx))
    }

    @Test
    fun `提示節點未活化沒有輸出時回退 userPrompt`() {
        // 分支未活化的 PROMPT 節點不會執行，context 中沒有它的輸出
        val ctx = context {
            setPromptSources(mapOf(llmKey to listOf(PromptSource("promptB", "prompt"))))
        }
        val cfg = LlmAssistantNodeConfig(llmId = "m1", userPrompt = "後備提問")
        assertEquals("後備提問", LlmAssistantExecutor.resolveMessage(llmKey, cfg, ctx))
    }

    @Test
    fun `多個提示來源時取第一個有輸出者`() {
        // 來源清單已由引擎依拓撲序排序；promptA 未活化（無輸出），故取 promptB
        val ctx = context {
            setPromptSources(
                mapOf(llmKey to listOf(PromptSource("promptA", "prompt"), PromptSource("promptB", "prompt")))
            )
            putOutput("promptB", mapOf("prompt" to "B 的提問"))
        }
        val cfg = LlmAssistantNodeConfig(llmId = "m1")
        assertEquals("B 的提問", LlmAssistantExecutor.resolveMessage(llmKey, cfg, ctx))
    }

    @Test
    fun `多個提示來源皆有輸出時取排序後第一個`() {
        val ctx = context {
            setPromptSources(
                mapOf(llmKey to listOf(PromptSource("promptA", "prompt"), PromptSource("promptB", "prompt")))
            )
            putOutput("promptA", mapOf("prompt" to "A 的提問"))
            putOutput("promptB", mapOf("prompt" to "B 的提問"))
        }
        val cfg = LlmAssistantNodeConfig(llmId = "m1")
        assertEquals("A 的提問", LlmAssistantExecutor.resolveMessage(llmKey, cfg, ctx))
    }

    @Test
    fun `提示節點輸出為空字串時視同無輸出`() {
        val ctx = context {
            setPromptSources(mapOf(llmKey to listOf(PromptSource("promptA", "prompt"))))
            putOutput("promptA", mapOf("prompt" to "  "))
        }
        val cfg = LlmAssistantNodeConfig(llmId = "m1", userPrompt = "後備提問")
        assertEquals("後備提問", LlmAssistantExecutor.resolveMessage(llmKey, cfg, ctx))
    }

    @Test
    fun `兩種來源皆無時回 null`() {
        val cfg = LlmAssistantNodeConfig(llmId = "m1")
        assertNull(LlmAssistantExecutor.resolveMessage(llmKey, cfg, context()))
    }

    @Test
    fun `其他 LLM 節點的提示來源不會被誤取`() {
        val ctx = context {
            setPromptSources(mapOf("otherLlm" to listOf(PromptSource("promptA", "prompt"))))
            putOutput("promptA", mapOf("prompt" to "別人的提問"))
        }
        val cfg = LlmAssistantNodeConfig(llmId = "m1", userPrompt = "自己的提問")
        assertEquals("自己的提問", LlmAssistantExecutor.resolveMessage(llmKey, cfg, ctx))
    }
}

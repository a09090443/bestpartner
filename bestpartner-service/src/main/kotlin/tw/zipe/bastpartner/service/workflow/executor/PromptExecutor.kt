package tw.zipe.bastpartner.service.workflow.executor

import jakarta.enterprise.context.ApplicationScoped
import tw.zipe.bastpartner.dto.workflow.config.NodeConfig
import tw.zipe.bastpartner.dto.workflow.config.PromptNodeConfig
import tw.zipe.bastpartner.entity.WorkflowNodeEntity
import tw.zipe.bastpartner.enumerate.NodeType
import tw.zipe.bastpartner.service.workflow.ExecutionContext
import tw.zipe.bastpartner.service.workflow.NodeExecutor

/**
 * 提示詞節點：把 LLM 的提問內容抽成獨立節點，以 `out:main` 連到 LLM 節點的提示輸入埠
 * （`in:prompt`）供其取用。
 *
 * 與 SKILL 等能力節點不同，本節點**會正常執行**：落執行紀錄、發 SSE 事件，
 * 且其輸出可被任何下游節點以 `{{promptNodeKey.prompt}}` 引用。
 *
 * 插值刻意只在此處做一次；下游 LLM 直接取用結果字串、不再二次插值，
 * 避免上游輸出本身含 `{{ }}` 時被重複展開或誤拋 VariableNotFoundException。
 */
@ApplicationScoped
class PromptExecutor : NodeExecutor {
    override val type = NodeType.PROMPT

    override fun execute(node: WorkflowNodeEntity, config: NodeConfig, context: ExecutionContext): Map<String, Any?> {
        val cfg = config as PromptNodeConfig
        // prompt 為無條件必填（見 PromptNodeConfig.missingRequiredFields），啟用與執行前皆已擋過；
        // 此處為縱深防禦，避免 config 被外部途徑改壞時送出空提問。
        val template = cfg.prompt?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("提示詞節點需要 prompt")
        val key = cfg.outputKey?.takeIf { it.isNotBlank() } ?: DEFAULT_OUTPUT_KEY
        return mapOf(key to context.resolveTemplate(template))
    }

    companion object {
        /** 未指定 outputKey 時的輸出鍵名 */
        const val DEFAULT_OUTPUT_KEY = "prompt"
    }
}

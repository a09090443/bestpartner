import { toRaw, watch } from 'vue'

/**
 * 讓型別化節點表單的本地 ref 跟隨「外部」對 `props.config` 的變更。
 *
 * 各表單為了編輯體驗，都是在 `<script setup>` 以 `ref(props.config.xxx ?? '')` 把 config
 * 攤成一組本地 ref，只在建立當下讀一次 props。Inspector 與 Node Designer 的 Parameters 面板
 * 共用同一份 `typedForms.ts` 分派表，但**各自 mount 一個實例**：其中一個 emit `update:config`
 * 更新了 `node.data.config` 後，另一個實例的本地 ref 不會跟著變，畫面就會停在舊值
 * （E2E 週期 202608192201 的 J9-10 即為此症狀）。
 *
 * 用法：在表單中呼叫本 composable，把「重新由 config 灌回本地 ref」的邏輯放進 `apply`，
 * 並在自己的 `emitConfig()` 送出前呼叫 `markSelfEmit(next)`：
 *
 * ```ts
 * const { markSelfEmit } = useConfigSync(
 *   () => props.config,
 *   (cfg) => { llmId.value = (cfg.llmId as string) ?? '' },
 * )
 * function emitConfig() {
 *   const next = { ...props.config, llmId: llmId.value }
 *   markSelfEmit(next)
 *   emit('update:config', next)
 * }
 * ```
 *
 * ⚠️ **自身 emit 造成的回流必須略過**：父層是 `selectedNode.data.config = config`，
 * 送出去的物件會原封不動變成新的 `props.config`。若不比對就 `apply`，使用者每打一個字
 * 都會被自己的值重灌一次本地 ref——中文輸入法組字期間尤其會吃字、游標會跳。
 * 這裡以**物件參考相等**判斷（不是深比較），因為那正是「這次回流是不是我自己送出去的那一個」
 * 的精確定義；外部（另一個表單實例）送來的一定是不同的新物件。
 *
 * ⚠️ 比對前必須先 `toRaw()`：物件存進 reactive 容器後，讀回來的是 **Proxy 而非原物件**，
 * 直接比對永遠不相等，自身回流就會被誤判成外部變更（單元測試已固定此行為）。
 */
export function useConfigSync(
  getConfig: () => Record<string, unknown> | undefined,
  apply: (config: Record<string, unknown>) => void,
) {
  /** 本表單最後一次 emit 出去的 config 物件參考；回流時據此略過 */
  let selfEmitted: Record<string, unknown> | null = null

  function markSelfEmit(config: Record<string, unknown>): void {
    selfEmitted = toRaw(config)
  }

  watch(getConfig, (cfg) => {
    if (cfg && toRaw(cfg) === selfEmitted) return
    selfEmitted = null
    apply(cfg ?? {})
  })

  return { markSelfEmit }
}

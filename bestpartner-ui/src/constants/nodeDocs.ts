/**
 * 各節點型別的說明文件（Node Designer 的 Docs 分頁）。
 *
 * 內容摘自後端 `service/workflow/executor/*.kt` 的 KDoc 與 `docs/workflow-engine/system-design.md`，
 * **不可憑印象撰寫**——這裡描述的是引擎的實際行為，寫錯比沒寫更糟。
 * 連接埠清單不寫在此處，由 Docs 分頁直接讀 `getNodeTypeMeta().inputs/outputs` 自動渲染（零維護）。
 *
 * 以 `Record<NodeType, …>` 宣告：新增 NodeType 而未補說明時 vue-tsc 會直接紅燈，
 * 與 nodeIcons.ts 同一套完整性強制。
 */
import type { NodeType } from '../types/workflow'

export interface NodeDoc {
  /** 一句話說明這個節點在流程中做什麼 */
  summary: string
  /** 常見用途／行為要點（條列，皆須對應實作） */
  points: string[]
}

export const NODE_DOCS: Record<NodeType, NodeDoc> = {
  TRIGGER: {
    summary: '流程的起點。手動執行時把傳入的啟動資料轉為本節點的輸出，供下游節點引用。',
    points: [
      '啟用 workflow 前必須至少有一個觸發節點，否則無法啟用。',
      '執行時帶入的 inputPayload 會成為此節點的輸出，下游以 {{觸發節點key.欄位}} 取用。',
      '沒有輸入埠——它不接收任何上游資料。',
    ],
  },
  LLM_ASSISTANT: {
    summary:
      '呼叫指定的 LLM 進行推論。工具、MCP、Skill、知識庫皆以獨立節點連到「工具」埠掛載，由 LLM 自主決定何時呼叫。',
    points: [
      '提問來源有兩種：連到「提示」埠的提示詞節點輸出（優先），或本節點設定的 userPrompt。',
      '兩者皆無時無法啟用 workflow（會回報 workflow.llm.prompt.required）。',
      '「工具」埠是能力掛載埠、不是資料流：連進來的節點不會依序執行，而是變成 LLM 可呼叫的工具集。',
      '若唯一的提示詞來源落在未活化的條件分支且未填 userPrompt，該次執行會失敗——這是預期行為。',
    ],
  },
  PROMPT: {
    summary: '把 LLM 的提問內容抽成獨立節點，以輸出連到 LLM 助手節點的「提示」埠。',
    points: [
      '會正常執行：落執行紀錄、發送 SSE 事件，輸出可被任何下游以 {{提示節點key.prompt}} 引用。',
      '插值只在此節點做一次，下游 LLM 直接取用結果字串、不再二次展開。',
      '未連到任何 LLM 的「提示」埠時無法啟用 workflow（孤兒提示詞節點）。',
    ],
  },
  TOOL: {
    summary: '直接呼叫一個已註冊的工具（如搜尋、日期、Text2SQL），與 LLM 掛載工具走同一套實例化路徑。',
    points: [
      '參數值支援 {{path}} 插值，執行前先以上游輸出展開。',
      '輸出放在 outputKey 指定的鍵（預設 result）；非純量結果會轉為 Map / List。',
      '也可以不連資料流、改連到 LLM 助手的「工具」埠，交由 LLM 自主呼叫。',
    ],
  },
  MCP_SERVER: {
    summary: '啟動指定的 MCP server、呼叫其中一個 tool，取得結果後關閉連線。',
    points: [
      'arguments 的值會先插值，再以 JSON 傳給 MCP tool。',
      '每次執行都會重新建立與關閉 MCP client。',
      '同樣可改連到 LLM 助手的「工具」埠，作為 LLM 的能力來源。',
    ],
  },
  SKILL: {
    summary: '提供一組 Skill 給 LLM 助手使用的能力節點；本身不執行、也沒有輸入埠。',
    points: [
      '只能連到 LLM 助手節點的「工具」埠，未掛載時無法啟用 workflow（孤兒 Skill 節點）。',
      '不會產生自己的執行紀錄與輸出——它只是把能力交給 LLM。',
    ],
  },
  KNOWLEDGE_RAG: {
    summary: '對知識庫做向量相似度搜尋，輸出比對到的文件片段清單。',
    points: [
      'query 支援插值，可直接引用上游節點的輸出。',
      'topK、minScore、embeddingModelId 皆會實際生效。',
      '也可掛到 LLM 助手的「工具」埠，作為該次推論的檢索來源。',
    ],
  },
  CONDITION: {
    summary: '逐條求值條件後依 and / or 聚合，決定要活化 True 或 False 哪一側的出邊。',
    points: [
      '輸出 result（布林）與 branch（活化哪一側）。',
      '條件值整串恰為單一 {{path}} 時保留原生型別，數值與布林比較才會正確。',
      '未活化分支上的節點不會執行，其下游若依賴其輸出會失敗。',
    ],
  },
  LOOP: {
    summary: '對輸入陣列逐項執行子圖，全部跑完後再走「結束」出邊。',
    points: [
      '「迴圈」出邊界定子圖入口，「結束」出邊在迭代完成後才活化。',
      'inputArrayPath 可寫成 {{path}} 或裸 path 兩種形式。',
      '有迭代次數上限保護，避免資料量失控。',
    ],
  },
  CODE: {
    summary: '在 GraalJS 沙箱內執行一段 JavaScript，把上游輸出重組為新的結果。',
    points: [
      '沙箱不開放 host access、檔案與網路 IO。',
      '全域變數 input 為上游所有輸出；取腳本最後一個表達式作為回傳值。',
      '預設逾時 10 秒、輸出上限 256KB，超過即中斷。',
    ],
  },
  HTTP_REQUEST: {
    summary: '對外部服務發出 HTTP 請求，支援 GET 與 POST（JSON）。',
    points: [
      'url、body、headers、secretHeaders 皆支援插值。',
      'secretHeaders 會加密落地；取回時顯示為 __SECRET_KEPT__，明文不外流。',
      'secretHeaders 的值一律不寫入任何日誌或錯誤訊息。',
    ],
  },
  DATA_TRANSFORM: {
    summary: '以 mappings 或 template 把上游輸出重組為新的資料結構。',
    points: [
      'mappings：逐筆求值，純 {{path}} 保留原生型別，否則插值為字串。',
      'template：整段插值為字串，放進 outputKey（預設 result）。',
      '兩者皆有時 mappings 優先，template 結果只補上未被佔用的鍵。',
    ],
  },
  OUTPUT: {
    summary: '組裝流程的最終輸出；沒有輸出埠，是流程的終點。',
    points: [
      'mappings 的每個值都會做插值。',
      'template 的插值結果會放進 result 鍵。',
      '執行結束後可在執行結果面板看到這裡組出來的內容。',
    ],
  },
}

/** 取得指定型別的說明文件 */
export function getNodeDoc(type: NodeType): NodeDoc {
  return NODE_DOCS[type]
}

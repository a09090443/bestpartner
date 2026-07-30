---
sidebar_position: 5
description: BestPartner 提供 n8n-like 的視覺化 Workflow 引擎，以節點（Node）與連線（Edge）自訂 AI 自動化流程，含節點類型與畫布驗證規則。
keywords: [Workflow, 工作流, n8n, 視覺化, 節點, Node, Edge, 自動化]
---

# 視覺化 Workflow

BestPartner 提供 n8n-like 的視覺化 Workflow 引擎，讓使用者以「節點（Node）＋連線（Edge）」的方式，在畫布上自訂 AI 自動化流程。

> **目前進度**：後端已完成 Workflow 定義的 CRUD、畫布驗證與**執行引擎（Phase 1 線性節點 + Phase 2 控制流／程式碼／資料轉換）**，13 種節點型別（`SKILL` 為能力節點外，其餘皆可手動執行）；前端編輯器已具備多連接點節點（分支、LLM 提示埠／工具埠）、完整畫布操作、型別化節點設定表單、**全屏節點編輯頁（Node Designer）**、自動排版、n8n 風格介面（淺／深雙主題），以及**執行按鈕、節點即時狀態與結果面板**。Executions 歷史清單與回放、更多觸發器（CRON／WEBHOOK）將於後續推出。

## 核心概念

| 概念 | 說明 |
|------|------|
| **Workflow** | 一張完整流程定義，含名稱、狀態、版本與畫布資訊 |
| **Node（節點）** | 流程中的一個步驟，具備 `nodeKey`（畫布內唯一）、類型、座標與設定（`config`）|
| **Edge（連線）** | 節點之間的有向連線，描述執行順序，可帶條件 |
| **canvasMeta** | 前端畫布的視口/縮放等附帶狀態 |

## 節點類型（NodeType）

`TRIGGER`、`LLM_ASSISTANT`、`PROMPT`、`TOOL`、`MCP_SERVER`、`SKILL`、`KNOWLEDGE_RAG`、`CONDITION`、`LOOP`、`CODE`、`HTTP_REQUEST`、`DATA_TRANSFORM`、`OUTPUT`。

> `OUTPUT`（輸出）為流程終點節點，**無輸出埠**，config 以 `template`（支援 `{{nodeKey.path}}` 插值，結果放 `result` 鍵）或 `mappings`（key=value 對映）擇一組成最終輸出。
>
> **Agent 模式（LLM 節點簡化）**：`LLM_ASSISTANT` 只負責呼叫指定 LLM。工具、MCP、Skill、知識庫改為獨立節點（`TOOL` / `MCP_SERVER` / `SKILL` / `KNOWLEDGE_RAG`），以輸出連到 LLM 節點的**工具輸入埠 `in:tool`** 掛載為 LLM 能力：`TOOL` / `MCP_SERVER` / `SKILL` 供 LLM 於推論時（agent loop）自主決定何時呼叫；`KNOWLEDGE_RAG` 則作為**自動注入型 RAG**，於推論前依 LLM 問題自動檢索並注入上下文（可同時掛多個知識庫，以 langchain4j `DefaultQueryRouter` 合併）。`SKILL` 為能力提供者，本身不獨立執行、不落執行紀錄。
>
> **提示詞節點（PROMPT）**：把 LLM 的「提問內容」抽成獨立節點，以 `out:main` 連到 LLM 節點的**提示輸入埠 `in:prompt`**。與 `in:tool` 相反，這條邊是**一般資料流連線**（參與節點活化與拓撲排序），因此可讓 `CONDITION` 兩個分支各接一個提示節點、再匯入同一顆 LLM——執行時取「被活化那條分支」的提問，達成一顆 LLM 搭配多種對話路線。LLM 的 `userPrompt` 欄位保留為**後備**：有提示節點連入時以節點輸出為準，沒接才用 `userPrompt`。
>
> 共 13 種節點型別；除 `SKILL`（能力節點）外皆可獨立執行：Phase 1 的 `TRIGGER`（MANUAL）、`LLM_ASSISTANT`、`MCP_SERVER`、`KNOWLEDGE_RAG`、`HTTP_REQUEST`、`OUTPUT`，Phase 2 的 `TOOL`（動態呼叫）、`CONDITION`（條件分支）、`LOOP`（迴圈子圖迭代）、`CODE`（GraalJS sandbox）、`DATA_TRANSFORM`（資料轉換），以及 `PROMPT`（提示詞，照常執行並落執行紀錄）。執行細節見下方[手動執行](#手動執行)。

## 前端編輯器

前端以 Vue 3 + [Vue Flow](https://vueflow.dev/) 打造 **n8n-like 畫布**（珊瑚色強調，Manrope／IBM Plex Mono 字型），提供**淺色（預設）／深色**兩套主題，工具列可即時切換、偏好記錄於 `localStorage`。主要區塊：

- **節點面板（NodePalette）**：左側可拖放的節點清單，依 **Trigger／Action／AI／Logic 四大分類**分組，並提供搜尋框即時過濾。
- **畫布（Canvas）**：中央繪製節點與連線；節點以固定尺寸卡片呈現（型別 SVG 圖示、型別標籤、名稱與 config 摘要副標，選取時顯示型別色外環，執行完成顯示勾勾徽章），內建點陣背景格線、縮圖（MiniMap，沿用節點型別色）與左下自訂縮放列（縮小／目前縮放百分比／放大／適應視窗）。
- **屬性面板（Inspector）**：右側；選取節點時編輯名稱與設定（型別化表單），並提供**開啟節點編輯頁**與**複製（Duplicate）／刪除（Delete）**；未選取時顯示 **Workflow overview**——節點／連線／觸發數統計卡與節點型別圖例。
- **節點編輯頁（Node Designer）**：全屏三欄檢視，可由「雙擊節點」「節點卡右上角按鈕」「Inspector 的開啟節點編輯頁」三種方式進入，按 Esc、點遮罩或關閉鈕退出。左欄為**輸入**（上游節點最近一次執行的輸出；掛在 LLM `in:tool` 埠的能力節點另列於「已掛載能力」，不顯示 JSON）、中欄為 **Parameters／Settings／Docs** 三個分頁、右欄為**輸出**（可引用欄位一覽 `{{節點key.欄位}}` 與原始 JSON）。中欄的 Parameters 與 Inspector 共用同一份表單與同一份 `node.data.config`，兩處編輯等價；Docs 分頁的行為說明取自後端執行器實作，連接埠清單則直接由節點 meta 自動渲染。
- **工具列**：麵包屑、流程名稱 inline 編輯、**Saved／未存**狀態徽章、主題切換、Editor·Executions 分頁、**Active 啟用切換**（接 `switchStatus`）、「整理版面」「存檔」與「執行」；開啟既有流程時起始為 **Saved**，僅「新增／刪除節點、移動節點、增刪連線」等實際編輯才標記為「未存」並攔截離頁（載入時的畫布還原與純選取、尺寸量測不會誤標）。

> Node Designer 只呈現**最近一次整流程執行**留下的資料，不提供單一節點試跑——後端目前只有整張流程的 `POST /llm/workflow/execute`，沒有單節點執行端點。

操作失敗（如存檔、啟用、刪除、**執行**）時，畫面會顯示**後端回傳的業務訊息**（例如啟用未含 Trigger 的流程時提示「須含 Trigger 節點」；執行含孤兒提示詞節點的流程時，執行結果面板顯示該節點的 nodeKey 與應連往的埠），而非通用的 HTTP 狀態字串，便於使用者理解原因。

> 執行（`execute`）是唯一走原生 `fetch` 的端點（需讀 SSE 串流），不經 axios 攔截器，
> 因此其錯誤訊息由 `api/workflowExecution.ts` 自行解析回應 body 取得；
> 回應非 JSON 或無 `message` 時才退回 `HTTP <status>`。

所有節點共用單一 Vue Flow 節點型別，語意型別放在 `node.data.type`；畫布資料（Vue Flow 格式）與後端 `WorkflowDTO` 之間由 `useWorkflowSync` 的 `dtoToFlow` / `flowToSaveRequest` 雙向轉換，後端 DTO 為唯一真相。主題 token 以 `.wf-editor` 作用域界定（淺色為 base、深色以 `data-wf-theme` 覆寫），不影響登入／列表等其他頁面。

## 節點連接點與多埠 Handle 編碼

節點的連接點（handle）採 **`role:port` 語意字串**編碼，直接作為 Vue Flow Handle 的 `id`，並存入 Edge 的 `sourceHandle` / `targetHandle`：

- `role`：`in`（輸入，左緣）或 `out`（輸出，右緣）
- `port`：埠名稱，如 `main` / `prompt` / `tool` / `true` / `false` / `loop` / `done`

各節點的連接點由型別 meta 的 `inputs` / `outputs` 資料驅動，`WorkflowNode` 依此動態渲染多個 Handle：

| 節點型別 | 輸入埠 | 輸出埠 |
|----------|--------|--------|
| `TRIGGER` | —（無輸入） | `out:main` |
| `LLM_ASSISTANT` | `in:main`（輸入）／`in:prompt`（提示，提問來源，一般資料流）／`in:tool`（工具，Agent 模式能力掛載，非資料流） | `out:main` |
| `PROMPT` | `in:main` | `out:main`（連到 LLM 的 `in:prompt`） |
| `SKILL` | —（能力節點，無輸入） | `out:main` |
| `CONDITION` | `in:main` | `out:true`（True）／`out:false`（False） |
| `LOOP` | `in:main` | `out:loop`（迴圈）／`out:done`（結束） |
| `OUTPUT` | `in:main` | —（無輸出，流程終點） |
| 其餘型別 | `in:main` | `out:main` |

多輸出埠沿右緣平均分布，並在節點框內顯示埠標籤（如 True / False），使用者未拉線也能辨識分支。Edge 的 `id` 帶入 handle（如 `e-c1:out:true-t1:in:main`），避免同一對節點的不同分支互相撞 id。

前端存檔前的輕量驗證新增以下與 handle 相關的規則：

- **未知 handle**（error，擋存檔）：連線的 handle 不在該節點 meta 定義的埠內。
- **工具埠相容性**（error，擋存檔）：連到 LLM `in:tool` 埠的來源只能是 `TOOL` / `MCP_SERVER` / `SKILL` / `KNOWLEDGE_RAG`；不相容連線於拉線時即以提示擋下（`onConnect`），存檔驗證亦回報 `INCOMPATIBLE_CONNECTION`。
- **提示埠相容性**（error，擋存檔）：連到 LLM `in:prompt` 埠的來源只能是 `PROMPT`；同樣於 `onConnect` 即擋下，存檔驗證回報 `INCOMPATIBLE_CONNECTION`。
- **提示接線不完整**（warning，不擋存檔）：`PROMPT` 節點未連到任何 LLM 的提示埠，或 LLM 節點既未填 `userPrompt` 也無 `PROMPT` 連入；回報 `PROMPT_WIRING_INCOMPLETE`，鏡射後端啟用驗證，讓使用者在存檔前就看到而非按下啟用才被擋。
- **分支未接完**（warning，不擋存檔）：`CONDITION` 的 `out:true` / `out:false` 至少一個未接線；屬「啟用前才需完整」的提醒。

## 型別化節點設定表單

Inspector 依節點型別分派結構化設定表單，取代裸 JSON：

| 節點型別 | 主要設定欄位 | 選項來源 |
|----------|----------|----------|
| `TRIGGER` | `triggerType`（下拉，必填）。**自 palette 拖入時即帶入預設值 `MANUAL`**，不需手動設定；`WEBHOOK` / `CRON` 列出但停用（尚未實作觸發來源） | — |
| `LLM_ASSISTANT` | `llmId`（下拉，必填）、`systemPrompt`／`userPrompt`、Memory、`responseFormat`／`outputSchema`／`outputKey`（工具／MCP／Skill 改由連接獨立節點到 `in:tool` 埠）。已有 `PROMPT` 節點連入 `in:prompt` 時，`userPrompt` 標示「已由上游提示節點提供」並淡化，但仍可編輯作為後備值 | `POST /llm/setting/get` |
| `PROMPT` | `prompt`（textarea，必填，支援 `{{nodeKey.key}}` 插值）、`outputKey`（選填，預設 `prompt`） | — |
| `TOOL` | `toolId`（下拉，必填）、`toolSettingId`（選填）、`arguments`（JSON 物件，選填，支援插值）、`outputKey`（選填） | `GET /llm/tool/list` |
| `MCP_SERVER` | `mcpId`（下拉，必填）、`toolName`（文字，必填；MCP 工具清單為執行期發現，無查詢端點故不做下拉）、`userSettingId`（選填；舊欄位名 `mcpSettingId` 讀取相容並自動遷移）、`arguments`（JSON 物件，選填，支援插值）、`outputKey`（選填） | `GET /llm/mcpServer/list` |
| `SKILL` | `skillId`（下拉，必填） | `GET /llm/skill/list` |
| `KNOWLEDGE_RAG` | `knowledgeId`（下拉，必填）、`embeddingModelId`（下拉，pipeline 檢索用、僅列 EMBEDDING 型別；作 LLM 外掛時由知識庫自身設定決定，可免填）、`query`（textarea，pipeline 檢索必填、支援插值；作 LLM 外掛時由 LLM 問題帶入，可免填）、`topK`（數字，預設 4）、`minScore`（數字，選填 0–1）、`outputKey`（選填） | `POST /llm/vector/getKnowledgeStore`、`POST /llm/setting/get`（過濾 `modelType=EMBEDDING`） |
| `OUTPUT` | `template`（**運算式編輯器**）或 `mappings`（key=value 對映）**擇一必填**（`OutputForm`）。兩者皆空時就地提示；並列出圖上其他節點的**可引用輸出 chip**（顯示為「節點名 › 欄位」），點擊即插入游標處 | 上游節點的實際執行輸出鍵；未執行過時用型別預設鍵（`constants/nodeOutputKeys.ts`） |
| 其餘 5 種（`CONDITION` / `LOOP` / `CODE` / `HTTP_REQUEST` / `DATA_TRANSFORM`） | 通用 JSON 編輯器（fallback）。表格模式可直接**新增／移除欄位**，不必切到 JSON 模式手打 | — |

> `PROMPT` 節點作為提問來源，`out:main` 需連到 LLM 的 `in:prompt` 埠；未連接則無法啟用流程。多個提示節點可接到同一顆 LLM，搭配 `CONDITION` 分支即可切換不同對話。

> `TOOL` / `MCP_SERVER` / `KNOWLEDGE_RAG` 節點可作為**管線步驟**（`out:main` 接下游、輸出經 `{{node.outputKey}}` 引用）或**能力掛載**（`out:main` 接 LLM `in:tool` 埠）；兩種用途靠 `targetHandle` 區分，同一顆節點可兼具。其中 `KNOWLEDGE_RAG` 作能力掛載時為自動注入型 RAG（可同時掛多個知識庫），作管線步驟時為顯式檢索、`query` 必填。（`SKILL` 無獨立 executor，僅能作能力掛載。）

### 運算式編輯器（插值的可讀化）

插值語法 `{{nodeKey.field}}` 中的 `nodeKey` 是節點建立時產生的 nanoid（如 `Mr7gjxEL`），對機器精確、對人不可讀。
`OUTPUT` 的輸出模板改用 **`ExpressionEditor`**（`components/common/`）呈現：

- 引用在畫面上渲染成**不可分割的色塊**「**LLM 助手 › reply**」，刪除時整塊消失
- **儲存格式完全不變**——寫回 `config.template` 的仍是 `{{Mr7gjxEL.reply}}`，後端與既有資料無感
- 節點**改名**時色塊文字跟著更新，底層引用不動（不像「以名稱當識別」的做法，改名就會壞掉）
- 引用到**不存在的節點**（打錯字、節點已刪）會標成紅色虛線色塊，不必等執行失敗才發現

> 實作上以 `contenteditable` 自建（未引入 CodeMirror 等重量級依賴）。
> 中文輸入法組字期間**完全不重繪、不回報值**（`compositionstart`/`compositionend` 把關），
> 且只有外部值變更才重繪，避免打字時游標跳動。
> ⚠️ 因此該欄位**不是 `<input>`／`<textarea>`**：測試不可用 `inputValue()`，要讀 `textContent`。

下拉選項由 `useNodeOptions` 以模組級快取，一個 session 只向後端取一次（登出時連同 LLM 設定快取一併清除，避免跨使用者殘留）。表單以不可變方式更新 `config` 並保留未知鍵值，切換表單不遺失既有資料；空值鍵一律移除以維持 config 精簡。`arguments`（JSON 物件）以文字輸入，僅接受合法 JSON 物件——非物件或壞 JSON 不寫入 config，避免後端反序列化失敗。

表單為結構化輸入不會產生 JSON 錯誤，故一律視為有效。必填欄位則採**契約驅動的即時驗證**：前端載入 `GET /llm/workflow/nodeRequiredFields` 取得各 NodeType 的必填清單（源自後端 `NodeConfig` 契約、前端不重複硬編），Inspector 對選中節點即時提示缺漏欄位（欄位名中文化、複合欄位顯示「擇一」）。存檔行為依狀態而異：**DRAFT / INACTIVE** 缺必填僅提示不擋（留待啟用驗證）；**ACTIVE** 缺必填則提前擋下存檔（與後端 ACTIVE 重存驗必填一致，Inspector 提示同步轉為紅色 error 語意）。

> 各節點 `config` 的完整欄位契約（型別、必填、預設值）見 [Workflow API — 節點 config 契約](../api/workflow.md#節點-config-契約)。

## 自動排版

工具列的「整理版面」以 [`@dagrejs/dagre`](https://github.com/dagrejs/dagre) 對畫布做左至右（LR）排版：依連線關係計算各節點座標、重新佈局後置中檢視。排版為純函式（`layoutGraph`），只更新節點位置、不動其餘欄位。

## 手動執行

Workflow 執行引擎提供**手動觸發**的執行能力（`POST /llm/workflow/execute`，SSE 事件流）：

- **執行按鈕**：畫布工具列提供執行按鈕，**已存檔即可執行**——不需先切為 `ACTIVE`，DRAFT / INACTIVE 皆可跑；執行前後端會逐節點驗 config 必填欄位（與啟用驗證同一份契約）。
- **多觸發點，各自獨立執行**：同一張畫布可放多個 `TRIGGER`，各接一條下游流程並可**匯流到同一顆節點**（例如多個入口共用一顆 LLM 助手，各自搭配自己的提示詞節點）。執行時指定入口，只跑該觸發點的流程：
  - 畫布只有一顆 `TRIGGER` → 點工具列「執行」直接開跑。
  - 有多顆 → 點「執行」先彈出觸發點選擇面板（以節點名稱列出）；也可直接點 **`TRIGGER` 節點卡右上的 ▶ 鈕**「從此觸發點執行」，跳過選擇。
  - 未被選中的觸發點及其獨佔下游不執行（標記 `SKIPPED`，畫布上以淡化樣式呈現）；匯流節點會自動取用被選中分支的提示詞。
  - 執行紀錄會記下發起入口（`llm_workflow_execution.trigger_node_key`）。
  - 觸發節點若沒接任何下游，編輯器會提示（warning，不擋存檔）——因為選它執行不會跑任何步驟。
- **節點即時狀態**：執行過程中前端依 SSE 事件（`node.started` / `node.completed` / `node.failed`）為畫布節點**即時上色**，一眼看出每個節點跑到哪、成功或失敗；client 中途停止／斷線時，仍在執行中的節點轉為 `CANCELLED` 終止態視覺。
- **結果面板（ExecutionResultDrawer）**：執行結束後開啟結果抽屜，逐節點檢視輸入輸出與錯誤訊息，以及整體執行狀態。
- **OUTPUT 節點**：以 `OUTPUT` 節點定義流程的最終輸出——`template`（`{{nodeKey.path}}` 插值，結果放 `result` 鍵）或 `mappings`（key=value 對映）擇一。
- **失敗與取消語意**：單一節點失敗時其下游節點標記 `SKIPPED`、整體 `FAILED`；client 中途斷線則標記 `CANCELLED`。執行紀錄落地 `llm_workflow_execution` / `llm_workflow_node_execution` 資料表。
- **變數插值**：全引擎統一 `{{nodeKey.path}}` 語法引用上游輸出；引用不存在的節點該節點執行失敗。`TRIGGER` 節點輸出即為 input payload 本身（`{{triggerKey.欄位}}`，不再多包一層 `.input`）。

**支援執行的節點**：除 `SKILL`（能力節點，掛載到 LLM `in:tool` 埠、不獨立執行）外，其餘 11 種皆可執行——`TRIGGER`（MANUAL）、`LLM_ASSISTANT`、`TOOL`、`MCP_SERVER`、`KNOWLEDGE_RAG`、`CONDITION`、`LOOP`、`CODE`、`HTTP_REQUEST`、`DATA_TRANSFORM`、`OUTPUT`。

### 控制流與進階節點（Phase 2）

- **CONDITION（條件分支）**：依 config `conditions`（欄位／運算子／比較值，左右值支援插值）以 `logic`（and／or，預設 and）聚合求值，true 走 `out:true`、false 走 `out:false`；未走到的分支其下游整段標記 `SKIPPED`。支援運算子 `eq`／`ne`／`gt`／`gte`／`lt`／`lte`／`contains`／`notContains`／`isEmpty`／`isNotEmpty`（兩側可轉數值時以數值比較，否則字串比較）。
- **LOOP（迴圈）**：`inputArrayPath` 插值取得陣列後逐項迭代，當前項以 `{{item}}`（可自訂 `itemAlias`）注入 context；自 `loopBodyEntryNodeKey` 起、不越過 LOOP 本身的子圖每迭代執行一次（節點紀錄帶 `loop_index`），各迭代輸出彙集於 `collectOutputKey`（預設 `items`），迭代完走 `out:done`；`maxIterations` 預設 100，暫不支援巢狀 LOOP。
- **CODE（程式碼）**：GraalJS sandbox 執行 JavaScript，`allowAllAccess(false)` 禁 host class／IO 存取，單次執行逾時強制中斷（預設 10s）、輸出上限 256KB；腳本以 `input`（上游 context 已插值資料）為輸入，最後表達式的值即節點輸出。
- **DATA_TRANSFORM（資料轉換）**：以 `template`（插值文字）或 `mappings`（`targetKey` / `expression`，純 `{{path}}` 保留原生型別、否則轉字串）轉換上游資料。
- **TOOL（工具動態呼叫）**：依 `toolId`（+ 可選 `toolSettingId`）實例化並呼叫工具的 `@Tool` 方法，`arguments` 各值插值後依方法簽名轉型傳入。
- **節點逾時**：每節點以 config `timeoutMs`（HTTP／CODE 型別）或預設 120s 為執行上限，逾時視同該節點失敗（下游 `SKIPPED`、整體 `FAILED`）。

> 執行 API 規格與 SSE 事件格式見 [Workflow API — 執行 Workflow](../api/workflow.md#執行-workflow)。

## Workflow 狀態（WorkflowStatus）

| 狀態 | 說明 |
|------|------|
| `DRAFT` | 草稿（新建預設）|
| `ACTIVE` | 已啟用 |
| `INACTIVE` | 已停用 |

## 畫布驗證規則

儲存（save）與啟用（switchStatus）時，後端會驗證畫布的正確性：

- **nodeKey 唯一**：同一 workflow 內節點識別鍵不可重複
- **edge 端點存在**：連線兩端必須對應到實際存在的節點
- **節點數上限**：預設 100（可由 `workflow.max-nodes` 設定）
- **無非法環**：以拓樸排序偵測有向環（一律視環為非法；LOOP 的迴圈語意由子圖迭代達成，圖本身仍須無環）
- **節點 config 型別（save）**：每個節點的 config 依 NodeType 強型別反序列化——未知欄位、結構性型別錯誤、非法 enum 值回 400 並指出 nodeKey 與原因；必填缺席放行（草稿可不完整）
- **啟用前置**：啟用 workflow 前須具備至少一個 `TRIGGER` 節點、圖無環，且**逐節點驗 config 必填欄位**（如 LLM 助手的 `llmId`），驗不過回報哪個節點缺哪些欄位——前述「必填未選不擋存檔」正是留待此處把關
- **ACTIVE 重存驗必填**：對**已啟用（ACTIVE）**的 workflow 執行 save 時，同步套用上述必填驗證（維持「ACTIVE ⟺ 通過啟用驗證」不變式）；要存不完整的半成品須先停用（switchStatus false）。DRAFT / INACTIVE 的 save 仍只驗型別、放行缺必填
- **舊 MCP 欄位相容**：save 時後端會把 `MCP_SERVER` 節點 config 內的舊欄位 `mcpSettingId` 自動改名為契約欄位 `userSettingId`（`userSettingId` 已有值則保留現值），使含舊資料的 workflow 重存不被未知欄位驗證擋下

## 並發保護

Workflow 採用 **樂觀鎖（version）**：更新時須帶入當前 `version`，若與資料庫不符則回 `WORKFLOW_VERSION_CONFLICT`，要求重新載入後再儲存，避免多人/多分頁同時覆寫。

## 權限

所有 Workflow 端點皆需登入（`@Authenticated`）。使用者僅能存取自己擁有的 workflow；具 `admin` 角色者可代為管理他人的 workflow。

> API 詳細規格見 [Workflow API](../api/workflow.md)。

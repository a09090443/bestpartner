---
sidebar_position: 5
description: BestPartner 提供 n8n-like 的視覺化 Workflow 引擎，以節點（Node）與連線（Edge）自訂 AI 自動化流程，含節點類型與畫布驗證規則。
keywords: [Workflow, 工作流, n8n, 視覺化, 節點, Node, Edge, 自動化]
---

# 視覺化 Workflow

BestPartner 提供 n8n-like 的視覺化 Workflow 引擎，讓使用者以「節點（Node）＋連線（Edge）」的方式，在畫布上自訂 AI 自動化流程。

> **目前進度**：後端已完成 Workflow 定義的 CRUD、畫布驗證與**執行引擎 Phase 1（手動執行）**；前端編輯器已具備多連接點節點（分支）、完整畫布操作、型別化節點設定表單、自動排版、深色 n8n 風格介面，以及**執行按鈕、節點即時狀態與結果面板**。條件／迴圈等控制流節點的執行與更多觸發器將於 Phase 2 推出。

## 核心概念

| 概念 | 說明 |
|------|------|
| **Workflow** | 一張完整流程定義，含名稱、狀態、版本與畫布資訊 |
| **Node（節點）** | 流程中的一個步驟，具備 `nodeKey`（畫布內唯一）、類型、座標與設定（`config`）|
| **Edge（連線）** | 節點之間的有向連線，描述執行順序，可帶條件 |
| **canvasMeta** | 前端畫布的視口/縮放等附帶狀態 |

## 節點類型（NodeType）

`TRIGGER`、`LLM_ASSISTANT`、`TOOL`、`MCP_SERVER`、`KNOWLEDGE_RAG`、`CONDITION`、`LOOP`、`CODE`、`HTTP_REQUEST`、`DATA_TRANSFORM`、`OUTPUT`。

> `OUTPUT`（輸出）為流程終點節點，**無輸出埠**，config 以 `template`（支援 `{{nodeKey.path}}` 插值，結果放 `result` 鍵）或 `mappings`（key=value 對映）擇一組成最終輸出。
>
> Phase 1 已支援 `TRIGGER`（MANUAL）、`LLM_ASSISTANT`、`TOOL`、`MCP_SERVER`、`KNOWLEDGE_RAG`、`HTTP_REQUEST`、`OUTPUT` 的實際執行；`CONDITION`、`LOOP`、`CODE`、`DATA_TRANSFORM` 執行時回報「尚未支援執行」（Phase 2 實作）。

## 前端編輯器

前端以 Vue 3 + [Vue Flow](https://vueflow.dev/) 打造 **n8n-like 深色畫布**（charcoal 深色主題、珊瑚色強調，Manrope／IBM Plex Mono 字型），主要區塊：

- **節點面板（NodePalette）**：左側可拖放的節點清單，依 **Trigger／Action／AI／Logic 四大分類**分組，並提供搜尋框即時過濾。
- **畫布（Canvas）**：中央繪製節點與連線；節點以卡片呈現（型別 icon、名稱與 config 摘要副標，選取時顯示型別色外環），內建點陣背景格線、縮圖（MiniMap，沿用節點型別色）與縮放控制（Controls）。
- **屬性面板（Inspector）**：右側；選取節點時編輯名稱與設定（型別化表單），並提供**複製（Duplicate）／刪除（Delete）**；未選取時顯示 **Workflow overview**——節點／連線／觸發數統計卡與節點型別圖例。
- **工具列**：麵包屑、流程名稱 inline 編輯、**Saved／未存**狀態徽章、Editor·Executions 分頁、**Active 啟用切換**（接 `switchStatus`）、「整理版面」與「存檔」；開啟既有流程時起始為 **Saved**，僅「新增／刪除節點、移動節點、增刪連線」等實際編輯才標記為「未存」並攔截離頁（載入時的畫布還原與純選取、尺寸量測不會誤標）。

操作失敗（如存檔、啟用、刪除）時，畫面會顯示**後端回傳的業務訊息**（例如啟用未含 Trigger 的流程時提示「須含 Trigger 節點」），而非通用的 HTTP 狀態字串，便於使用者理解原因。

所有節點共用單一 Vue Flow 節點型別，語意型別放在 `node.data.type`；畫布資料（Vue Flow 格式）與後端 `WorkflowDTO` 之間由 `useWorkflowSync` 的 `dtoToFlow` / `flowToSaveRequest` 雙向轉換，後端 DTO 為唯一真相。深色主題 token 以 `.wf-editor` 作用域界定，不影響登入／列表等其他頁面。

## 節點連接點與多埠 Handle 編碼

節點的連接點（handle）採 **`role:port` 語意字串**編碼，直接作為 Vue Flow Handle 的 `id`，並存入 Edge 的 `sourceHandle` / `targetHandle`：

- `role`：`in`（輸入，左緣）或 `out`（輸出，右緣）
- `port`：埠名稱，如 `main` / `true` / `false` / `loop` / `done`

各節點的連接點由型別 meta 的 `inputs` / `outputs` 資料驅動，`WorkflowNode` 依此動態渲染多個 Handle：

| 節點型別 | 輸入埠 | 輸出埠 |
|----------|--------|--------|
| `TRIGGER` | —（無輸入） | `out:main` |
| `CONDITION` | `in:main` | `out:true`（True）／`out:false`（False） |
| `LOOP` | `in:main` | `out:loop`（迴圈）／`out:done`（結束） |
| `OUTPUT` | `in:main` | —（無輸出，流程終點） |
| 其餘型別 | `in:main` | `out:main` |

多輸出埠沿右緣平均分布，並在節點框內顯示埠標籤（如 True / False），使用者未拉線也能辨識分支。Edge 的 `id` 帶入 handle（如 `e-c1:out:true-t1:in:main`），避免同一對節點的不同分支互相撞 id。

前端存檔前的輕量驗證新增兩項與 handle 相關的規則：

- **未知 handle**（error，擋存檔）：連線的 handle 不在該節點 meta 定義的埠內。
- **分支未接完**（warning，不擋存檔）：`CONDITION` 的 `out:true` / `out:false` 至少一個未接線；屬「啟用前才需完整」的提醒。

## 型別化節點設定表單

Inspector 依節點型別分派結構化設定表單，取代裸 JSON：

| 節點型別 | 主要設定欄位 | 選項來源 |
|----------|----------|----------|
| `LLM_ASSISTANT` | `llmId`（下拉，必填）、`systemPrompt`／`userPrompt`、Memory、工具／MCP／Skill 多選、`knowledgeId`、附加檔案、`responseFormat`／`outputSchema`／`outputKey` | `POST /llm/setting/get`、`/llm/tool/list`、`/llm/mcpServer/list`、`/llm/skill/list`、`/llm/vector/getKnowledgeStore` |
| `TOOL` | `toolId`（下拉，必填）、`toolSettingId`（選填）、`arguments`（JSON 物件，選填，支援插值）、`outputKey`（選填） | `GET /llm/tool/list` |
| `MCP_SERVER` | `mcpId`（下拉，必填）、`toolName`（文字，必填；MCP 工具清單為執行期發現，無查詢端點故不做下拉）、`userSettingId`（選填；舊欄位名 `mcpSettingId` 讀取相容並自動遷移）、`arguments`（JSON 物件，選填，支援插值）、`outputKey`（選填） | `GET /llm/mcpServer/list` |
| `KNOWLEDGE_RAG` | `knowledgeId`（下拉，必填）、`embeddingModelId`（下拉，必填，僅列 EMBEDDING 型別）、`query`（textarea，必填，支援插值）、`topK`（數字，預設 4）、`minScore`（數字，選填 0–1）、`outputKey`（選填） | `POST /llm/vector/getKnowledgeStore`、`POST /llm/setting/get`（過濾 `modelType=EMBEDDING`） |
| `OUTPUT` | `template`（textarea，支援插值）或 `mappings`（key=value 對映）**擇一必填**（`OutputForm`） | — |
| 其餘 6 種 | 通用 JSON 編輯器（fallback） | — |

下拉選項由 `useNodeOptions` 以模組級快取，一個 session 只向後端取一次（登出時連同 LLM 設定快取一併清除，避免跨使用者殘留）。表單以不可變方式更新 `config` 並保留未知鍵值，切換表單不遺失既有資料；空值鍵一律移除以維持 config 精簡。`arguments`（JSON 物件）以文字輸入，僅接受合法 JSON 物件——非物件或壞 JSON 不寫入 config，避免後端反序列化失敗。

表單為結構化輸入不會產生 JSON 錯誤，故一律視為有效。必填欄位則採**契約驅動的即時驗證**：前端載入 `GET /llm/workflow/nodeRequiredFields` 取得各 NodeType 的必填清單（源自後端 `NodeConfig` 契約、前端不重複硬編），Inspector 對選中節點即時提示缺漏欄位（欄位名中文化、複合欄位顯示「擇一」）。存檔行為依狀態而異：**DRAFT / INACTIVE** 缺必填僅提示不擋（留待啟用驗證）；**ACTIVE** 缺必填則提前擋下存檔（與後端 ACTIVE 重存驗必填一致，Inspector 提示同步轉為紅色 error 語意）。

> 各節點 `config` 的完整欄位契約（型別、必填、預設值）見 [Workflow API — 節點 config 契約](../api/workflow.md#節點-config-契約)。

## 自動排版

工具列的「整理版面」以 [`@dagrejs/dagre`](https://github.com/dagrejs/dagre) 對畫布做左至右（LR）排版：依連線關係計算各節點座標、重新佈局後置中檢視。排版為純函式（`layoutGraph`），只更新節點位置、不動其餘欄位。

## 手動執行（Phase 1）

Workflow 執行引擎 Phase 1 提供**手動觸發**的執行能力（`POST /llm/workflow/execute`，SSE 事件流）：

- **執行按鈕**：畫布工具列提供執行按鈕，**已存檔即可執行**——不需先切為 `ACTIVE`，DRAFT / INACTIVE 皆可跑；執行前後端會逐節點驗 config 必填欄位（與啟用驗證同一份契約）。
- **節點即時狀態**：執行過程中前端依 SSE 事件（`node.started` / `node.completed` / `node.failed`）為畫布節點**即時上色**，一眼看出每個節點跑到哪、成功或失敗。
- **結果面板（ExecutionResultDrawer）**：執行結束後開啟結果抽屜，逐節點檢視輸入輸出與錯誤訊息，以及整體執行狀態。
- **OUTPUT 節點**：以 `OUTPUT` 節點定義流程的最終輸出——`template`（`{{nodeKey.path}}` 插值，結果放 `result` 鍵）或 `mappings`（key=value 對映）擇一。
- **失敗與取消語意**：單一節點失敗時其下游節點標記 `SKIPPED`、整體 `FAILED`；client 中途斷線則標記 `CANCELLED`。執行紀錄落地 `llm_workflow_execution` / `llm_workflow_node_execution` 資料表。
- **變數插值**：全引擎統一 `{{nodeKey.path}}` 語法引用上游輸出；引用不存在的節點該節點執行失敗。

**Phase 1 支援執行的節點**：`TRIGGER`（MANUAL）、`LLM_ASSISTANT`、`TOOL`、`MCP_SERVER`、`KNOWLEDGE_RAG`、`HTTP_REQUEST`、`OUTPUT`。

> **Phase 2 預告**：`CONDITION`、`LOOP`、`CODE`、`DATA_TRANSFORM` 目前執行時回報「尚未支援執行」，控制流與資料轉換的執行邏輯將於 Phase 2 實作。

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
- **無非法環**：以拓樸排序偵測有向環（Phase 1 一律視環為非法）
- **節點 config 型別（save）**：每個節點的 config 依 NodeType 強型別反序列化——未知欄位、結構性型別錯誤、非法 enum 值回 400 並指出 nodeKey 與原因；必填缺席放行（草稿可不完整）
- **啟用前置**：啟用 workflow 前須具備至少一個 `TRIGGER` 節點、圖無環，且**逐節點驗 config 必填欄位**（如 LLM 助手的 `llmId`），驗不過回報哪個節點缺哪些欄位——前述「必填未選不擋存檔」正是留待此處把關
- **ACTIVE 重存驗必填**：對**已啟用（ACTIVE）**的 workflow 執行 save 時，同步套用上述必填驗證（維持「ACTIVE ⟺ 通過啟用驗證」不變式）；要存不完整的半成品須先停用（switchStatus false）。DRAFT / INACTIVE 的 save 仍只驗型別、放行缺必填
- **舊 MCP 欄位相容**：save 時後端會把 `MCP_SERVER` 節點 config 內的舊欄位 `mcpSettingId` 自動改名為契約欄位 `userSettingId`（`userSettingId` 已有值則保留現值），使含舊資料的 workflow 重存不被未知欄位驗證擋下

## 並發保護

Workflow 採用 **樂觀鎖（version）**：更新時須帶入當前 `version`，若與資料庫不符則回 `WORKFLOW_VERSION_CONFLICT`，要求重新載入後再儲存，避免多人/多分頁同時覆寫。

## 權限

所有 Workflow 端點皆需登入（`@Authenticated`）。使用者僅能存取自己擁有的 workflow；具 `admin` 角色者可代為管理他人的 workflow。

> API 詳細規格見 [Workflow API](../api/workflow.md)。

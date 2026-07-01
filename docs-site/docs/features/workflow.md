---
sidebar_position: 5
---

# 視覺化 Workflow

BestPartner 提供 n8n-like 的視覺化 Workflow 引擎，讓使用者以「節點（Node）＋連線（Edge）」的方式，在畫布上自訂 AI 自動化流程。

> **目前進度**：後端已完成 Workflow 定義的 CRUD 與畫布驗證；前端編輯器已具備多連接點節點（分支）、完整畫布操作、型別化節點設定表單與自動排版。執行引擎與觸發器將於後續階段推出。

## 核心概念

| 概念 | 說明 |
|------|------|
| **Workflow** | 一張完整流程定義，含名稱、狀態、版本與畫布資訊 |
| **Node（節點）** | 流程中的一個步驟，具備 `nodeKey`（畫布內唯一）、類型、座標與設定（`config`）|
| **Edge（連線）** | 節點之間的有向連線，描述執行順序，可帶條件 |
| **canvasMeta** | 前端畫布的視口/縮放等附帶狀態 |

## 節點類型（NodeType）

`TRIGGER`、`LLM_ASSISTANT`、`TOOL`、`MCP_SERVER`、`KNOWLEDGE_RAG`、`CONDITION`、`LOOP`、`CODE`、`HTTP_REQUEST`、`DATA_TRANSFORM`。

> 目前僅儲存節點定義；各節點的實際執行邏輯於後續階段實作。

## 前端編輯器

前端以 Vue 3 + [Vue Flow](https://vueflow.dev/) 打造 n8n-like 畫布，主要區塊：

- **節點面板（NodePalette）**：左側可拖放的節點型別清單。
- **畫布（Canvas）**：中央繪製節點與連線，內建背景格線、縮圖（MiniMap，沿用節點型別色）與縮放控制（Controls）。
- **屬性面板（Inspector）**：右側編輯選中節點的名稱與設定，或編輯流程 meta。
- **工具列**：顯示流程名稱/狀態/版本、「整理版面」與「存檔」；有未存變更時顯示「未存」並攔截離頁。

所有節點共用單一 Vue Flow 節點型別，語意型別放在 `node.data.type`；畫布資料（Vue Flow 格式）與後端 `WorkflowDTO` 之間由 `useWorkflowSync` 的 `dtoToFlow` / `flowToSaveRequest` 雙向轉換，後端 DTO 為唯一真相。

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
| 其餘型別 | `in:main` | `out:main` |

多輸出埠沿右緣平均分布，並在節點框內顯示埠標籤（如 True / False），使用者未拉線也能辨識分支。Edge 的 `id` 帶入 handle（如 `e-c1:out:true-t1:in:main`），避免同一對節點的不同分支互相撞 id。

前端存檔前的輕量驗證新增兩項與 handle 相關的規則：

- **未知 handle**（error，擋存檔）：連線的 handle 不在該節點 meta 定義的埠內。
- **分支未接完**（warning，不擋存檔）：`CONDITION` 的 `out:true` / `out:false` 至少一個未接線；屬「啟用前才需完整」的提醒。

## 型別化節點設定表單

Inspector 依節點型別分派結構化設定表單，取代裸 JSON：

| 節點型別 | 設定欄位 | 選項來源 |
|----------|----------|----------|
| `LLM_ASSISTANT` | `llmId`（下拉）、`systemPrompt`（選填） | `POST /llm/setting/get` |
| `TOOL` | `toolId`（下拉） | `GET /llm/tool/list` |
| `MCP_SERVER` | `mcpId`（下拉） | `GET /llm/mcpServer/list` |
| `KNOWLEDGE_RAG` | `knowledgeId`（文字）、`topK`（數字，預設 4） | 文字輸入（暫無列舉端點） |
| 其餘 6 種 | 通用 JSON 編輯器（fallback） | — |

下拉選項由 `useNodeOptions` 以模組級快取，一個 session 只向後端取一次。表單以不可變方式更新 `config` 並保留未知鍵值，切換表單不遺失既有資料。表單為結構化輸入不會產生 JSON 錯誤，故一律視為有效；「必填未選」不擋存檔，留待 workflow 啟用驗證。

## 自動排版

工具列的「整理版面」以 [`@dagrejs/dagre`](https://github.com/dagrejs/dagre) 對畫布做左至右（LR）排版：依連線關係計算各節點座標、重新佈局後置中檢視。排版為純函式（`layoutGraph`），只更新節點位置、不動其餘欄位。

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
- **啟用前置**：啟用 workflow 前須具備至少一個 `TRIGGER` 節點且圖無環

## 並發保護

Workflow 採用 **樂觀鎖（version）**：更新時須帶入當前 `version`，若與資料庫不符則回 `WORKFLOW_VERSION_CONFLICT`，要求重新載入後再儲存，避免多人/多分頁同時覆寫。

## 權限

所有 Workflow 端點皆需登入（`@Authenticated`）。使用者僅能存取自己擁有的 workflow；具 `admin` 角色者可代為管理他人的 workflow。

> API 詳細規格見 [Workflow API](../api/workflow.md)。

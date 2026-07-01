# Workflow Handle 編碼設計（Phase 2）

> 日期：2026-07-01
> 範圍：`bestpartner-ui` 前端，不改後端
> 目標：讓多輸出節點（CONDITION / LOOP）可在畫布上分支，且連線的埠資訊能存回、載入、驗證不失真。

## 背景

現況盤點（`bestpartner-ui/src`）：前端已具備 domain / Vue Flow 分離架構（`useWorkflowSync` 的 `dtoToFlow` / `flowToSaveRequest`）、Pinia store 樂觀鎖、圖驗證（nodeKey 唯一、端點存在、無環）。但每個節點固定「左一 target、右一 source」（`WorkflowNode.vue`），`CONDITION`（true/false）、`LOOP`（loop/done）本質需要多輸出埠，目前無法表達。

後端現況：`WorkflowService` 只做持久化 + 驗證（`detectCycle` 對所有 edge 一視同仁、`switchStatus` 要求有 TRIGGER），**尚無執行引擎**。`WorkflowEdgeEntity` 已有 `sourceHandle` / `targetHandle` 欄位，DTO 亦已支援。

## 決策

### 1. 子節點模型：全走主流程（模型 A）

`TOOL` / `MCP_SERVER` / `KNOWLEDGE_RAG` 都視為主流程 DAG 上的一步（`in:main` → `out:main`），不採 n8n 式「掛到 LLM 側邊能力埠」的模型 B。

理由：
- **與後端一致、零改動**：模型 B 的能力掛載 edge 是「附屬」不是「執行順序」，但後端 `detectCycle` 會把它跟主流程邊混在一起，未來拓樸排序執行語意會錯。模型 A 每條邊都是真正的 DAG 邊。
- **不重複造輪子**：BestPartner 已有 `/llm/customAssistantChat` 整合 Memory/Tool/MCP/Skill 的機制；再做成畫布子節點會與既有概念衝突。
- **保留彈性**：`LLM_ASSISTANT` 節點的 `config` 可直接帶 `toolIds` / `knowledgeId`（重用既有設定），不必連線就達到「能力」效果；TOOL/RAG 節點則定位成主流程中的明確步驟。

### 2. Handle 編碼格式：語意字串 `role:port`

- 輸入埠：`in:main`
- 輸出埠：`out:main`；`CONDITION` → `out:true` / `out:false`；`LOOP` → `out:loop` / `out:done`
- 存入 DTO 的 `sourceHandle`（輸出）與 `targetHandle`（輸入）。
- 人類可讀、DB 直接看得懂、好 debug。

### 3. 埠定義表

| 節點型別 | 輸入埠 | 輸出埠 |
|----------|--------|--------|
| `TRIGGER` | —（無） | `out:main` |
| `LLM_ASSISTANT` | `in:main` | `out:main` |
| `TOOL` | `in:main` | `out:main` |
| `MCP_SERVER` | `in:main` | `out:main` |
| `KNOWLEDGE_RAG` | `in:main` | `out:main` |
| `CODE` | `in:main` | `out:main` |
| `HTTP_REQUEST` | `in:main` | `out:main` |
| `DATA_TRANSFORM` | `in:main` | `out:main` |
| `CONDITION` | `in:main` | `out:true`（True）/ `out:false`（False） |
| `LOOP` | `in:main` | `out:loop`（迴圈）/ `out:done`（結束） |

規則：預設 `in:main` + `out:main`；`TRIGGER` 無輸入；只有 `CONDITION` / `LOOP` 多輸出。`CONDITION` 只有 true/false 兩路，不設 `out:else`。輸入 Phase 2 不做多輸入合流。

## 設計

### meta 模型（`constants/nodeTypes.ts`）

埠採資料驅動，之後新增型別/改埠只改此處：

```ts
export interface PortMeta {
  id: string      // handle id，如 'in:main' / 'out:true'，直接作為 Vue Flow Handle 的 id
  label?: string  // 多輸出時顯示於埠旁，如 True / False / 迴圈 / 結束
}

export interface NodeTypeMeta {
  type: NodeType
  label: string
  color: string
  icon: string
  isTrigger?: boolean
  inputs: PortMeta[]
  outputs: PortMeta[]
}
```

### handle 常數與解析（`constants/handles.ts`，新增）

```ts
export const IN_MAIN = 'in:main'
export const OUT_MAIN = 'out:main'
export function parseHandle(id: string): { role: 'in' | 'out'; port: string }
```

`parseHandle` 獨立成模組，供渲染與驗證共用。

### 節點渲染（`components/canvas/WorkflowNode.vue`）

依 meta 的 `inputs` / `outputs` 動態渲染 `<Handle :id="port.id">`，多輸出沿右緣以 `top` 百分比平均分布：`portTop(i, total) = (i+1)/(total+1) * 100%`。埠 label 放在**節點框內右側**（總是可見，未連線也看得出哪個埠是 True/False）。Handle 的 `id` 即 handle 字串，Vue Flow 連線時 `connection.sourceHandle` / `targetHandle` 天然帶上，直接接上 DTO。

### edge 往返（`composables/useWorkflowSync.ts`）

`dtoToFlow` / `flowToSaveRequest` 已在轉 `sourceHandle` / `targetHandle`，不需改。唯一調整 edge `id` 規則，避免同一對節點多分支撞 id：

```ts
id: `e-${source}:${sourceHandle ?? 'main'}-${target}:${targetHandle ?? 'main'}`
```

`WorkflowEditorView.vue` 的 `onConnect` edge id 規則同步對齊。

### 驗證（`composables/useGraphValidation.ts`）

保留既有三規則，新增：

| 新規則 | 說明 | 型別 | 分級 |
|--------|------|------|------|
| 未知 handle | edge 的 handle 不在該節點 meta 埠內 | `UNKNOWN_HANDLE` | 錯誤（擋存檔） |
| 分支未接完 | `CONDITION` 的 true/false 至少一個未接 | `BRANCH_INCOMPLETE` | 警告（可存檔，啟用時擋） |

後端 `validateGraph` 目前不驗 handle；Phase 2 先前端擋，後端維持現狀（本來就存 handle、不驗 handle，不會出錯）。

## 受影響檔案

| 檔案 | 動作 |
|------|------|
| `constants/handles.ts` | 新增：常數 + `parseHandle()` |
| `constants/nodeTypes.ts` | 改：`NodeTypeMeta` 加 `inputs`/`outputs`，10 型別補埠 |
| `components/canvas/WorkflowNode.vue` | 改：動態渲染多 Handle + 埠 label |
| `composables/useWorkflowSync.ts` | 改：edge `id` 規則帶 handle |
| `views/WorkflowEditorView.vue` | 改：`onConnect` edge id 規則對齊 |
| `composables/useGraphValidation.ts` | 改：加 `UNKNOWN_HANDLE`（錯）/ `BRANCH_INCOMPLETE`（警） |

## 測試計畫（TDD，vitest）

- `handles.test.ts`（新）：`parseHandle('out:true')` → `{role:'out',port:'true'}`；非法輸入處理。
- `nodeTypes.test.ts`（新）：CONDITION 有 2 outputs、TRIGGER 無 input。
- `useWorkflowSync.test.ts`（既有）：多分支 edge 帶 handle 往返不失真。
- `useGraphValidation.test.ts`（既有）：未知 handle 報錯、CONDITION 缺分支報警。
- `WorkflowNode.test.ts`（既有）：CONDITION 渲染 2 個 source handle 且有 True/False label。

## 範圍界線

Phase 2 純前端，不改後端。分支「執行語意」（true 走哪、false 走哪）屬未來執行引擎，本期只負責「畫得出、存得回、驗得到」。

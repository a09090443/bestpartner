# Workflow 引擎 v1 實作計畫

> **For Claude:** REQUIRED SUB-SKILL: 使用 superpowers:executing-plans 逐任務實作本計畫。
> 本專案慣例「不使用 git worktree」，請直接在主目錄 `D:\projects\bestpartner` 上實作。

**Goal:** 為 BestPartner 實作 n8n-like 視覺化 workflow 引擎 v1：使用者可建立/編排 workflow（節點 + 連線）、以三種觸發來源同步執行、並落地完整執行紀錄供回放。

**Architecture:** 後端為 Quarkus + Kotlin 單體服務，新增 `llm_workflow*` 共 6 張表。執行引擎 `WorkflowEngine` 維持同步語意（一次呼叫依拓樸順序把節點跑到終態並落地）；觸發來源（manual/webhook/cron）與引擎解耦，僅決定「誰啟動一次 execution」。前端另以 Vue 3 + Vue Flow 實作（最後階段）。

**Tech Stack:** Quarkus 3.21、Kotlin 2.1、PostgreSQL、Hibernate ORM Panache（Kotlin）、SmallRye JWT、Quarkus Scheduler、JUnit5 + `@QuarkusTest` + rest-assured、kotlinx.serialization。

**規格來源:** `docs/workflow-engine/requirements.md`、`docs/workflow-engine/system-design.md`（ERD、API 契約、節點 config schema、狀態機以該兩份為準）。

---

## 階段總覽（5 Phases）

| Phase | 名稱 | 範圍 | 產出 |
|-------|------|------|------|
| **1** | 資料層 + Workflow CRUD | 6 張表 DDL、enums、workflow/node/edge entity+repository、DTO、i18n、`WorkflowService` CRUD、`WorkflowResource` CRUD 端點（API #1-6） | 可建立/讀取/更新/刪除/啟停 workflow，畫布無損存取 |
| **2** | 同步執行引擎 + 執行紀錄 | `execution` / `node_execution` entity+repository、`WorkflowEngine`（拓樸排序、變數解析、逐節點落地）、執行紀錄查詢 API（#7、#11、#12）、逾時/狀態機 | 可手動執行只含「最簡節點」的 workflow 並查回放 |
| **3** | 節點實作 | 10 種 NodeType 的 executor：LLM/助手、Tool、MCP、RAG、Condition、Loop、HTTP、Data Transform、Trigger、Code（停用） | 每種節點可實際執行，串接既有 service |
| **4** | 觸發器 | `trigger` entity+repository、trigger CRUD API（#9、#10）、Webhook 端點（#8）、`WorkflowCronScheduler`（Quarkus Scheduler、overlap SKIP） | webhook 與 cron 可觸發執行 |
| **5** | 前端畫布 | Vue 3 + Vite + Pinia + Vue Flow 專案：畫布、節點面板、屬性設定、執行與回放 UI | 視覺化編排與執行 |

> 每個 Phase 完成後，宣告完成前必須呼叫 `documentation-sync` skill 同步文件（api-endpoints.md、bestpartner-ddl.sql、docs-site、Postman）。
> Phase 2-5 在輪到時各自再用 `superpowers:writing-plans` 細化為 bite-sized 任務。**本文件詳列 Phase 1。**

---

## 前置須知（Phase 1 共用）

- **ArchUnit 強制**（`src/test/.../architecture/ArchitectureTest.kt`）：新類別必須遵守 —— `@Entity` 放 `entity` 套件、entity 命名 `XxxEntity`、repository 套件類別命名 `XxxRepository`、service 套件命名 `XxxService`、`@Path` 類別命名 `XxxResource`；分層不可反向依賴。違反會在 `./gradlew test` 紅。
- **Entity 慣例**（參考 `entity/LLMMcpServerEntity.kt`）：`@Entity @Table(name="...")`；`@Id @GeneratedValue(strategy = GenerationType.UUID) var id: String? = null`；JSON 欄位 `@JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition="json")`；enum `@Enumerated(EnumType.STRING)`；繼承 `BaseEntity()`（自動帶稽核欄位）。
- **Repository 慣例**（參考 `repository/BaseRepository.kt`、`LLMMcpUserSettingRepository`）：`@ApplicationScoped class XxxRepository : BaseRepository<XxxEntity, String>()`；可用 `saveOrUpdate`/`update`/`findOptionalById`/`deleteEntities`/`existsById`/`count(...)`，複雜查詢用 `createSqlExecutor()` 或 `executeSelect(...)`。
- **DTO 慣例**：`@Serializable data class XxxDTO(...)`（kotlinx.serialization）。
- **回應慣例**（`dto/ApiResponse.kt`）：成功 `ApiResponse.success(data)`；錯誤走 `ServiceException(AppMessage.XXX, ...args)`，由既有 `GlobalExceptionMapper` 轉 `ApiResponse`。
- **i18n 慣例**（`.claude/rules/i18n-messages.md`）：錯誤訊息先加 `messages_en_US.properties`、`messages_zh_TW.properties`，再加 `enumerate/AppMessage.kt` enum，程式以 `AppMessage.XXX` 使用。
- **測試前置**：`@QuarkusTest` 整合測試會連真實 PostgreSQL（`localhost:5432/pgdb`）。執行前確認 DB 已起、DDL 已套用。rest-assured 測受保護端點時用 `quarkus-test-security` 的 `@TestSecurity(user="...", roles=["..."])`。
- **package 根**：`tw.zipe.bastpartner`（注意是 bastpartner，少一個 e）。

---

## Phase 1 任務

### Task 1：資料表 DDL

**Files:**
- Modify: `docs/sql/bestpartner-ddl.sql`（在檔尾新增 6 張表）

**Step 1:** 把 `docs/workflow-engine/system-design.md` §1.2 的 DDL 草稿（6 張 `llm_workflow*` 表 + 索引）完整貼到 `docs/sql/bestpartner-ddl.sql` 末尾。確認欄位/索引與 ERD 一致。

**Step 2:** 對本機開發 DB 套用這段 DDL（`psql` 或既有方式），讓後續 `@QuarkusTest` 能跑。

**Step 3: Commit**
```bash
git add docs/sql/bestpartner-ddl.sql
git commit -m "新增(workflow): 新增 workflow 引擎 6 張資料表 DDL"
```

---

### Task 2：列舉型別（enums）

**Files:**
- Create: `entity/../enumerate/WorkflowStatus.kt`、`NodeType.kt`、`TriggerType.kt`、`ExecutionStatus.kt`、`NodeExecutionStatus.kt`（路徑 `src/main/kotlin/tw/zipe/bastpartner/enumerate/`）

**Step 1: 實作**（無需測試，純宣告；值對齊狀態機）
```kotlin
package tw.zipe.bastpartner.enumerate

enum class WorkflowStatus { DRAFT, ACTIVE, INACTIVE }

enum class NodeType {
    TRIGGER, LLM_ASSISTANT, TOOL, MCP, KNOWLEDGE_RAG,
    CONDITION, LOOP, CODE, HTTP_REQUEST, DATA_TRANSFORM
}

enum class TriggerType { MANUAL, WEBHOOK, CRON }

enum class ExecutionStatus { PENDING, RUNNING, SUCCESS, FAILED, TIMEOUT, CANCELLED }

enum class NodeExecutionStatus { PENDING, RUNNING, SUCCESS, FAILED, SKIPPED }
```

**Step 2: Commit**
```bash
git add src/main/kotlin/tw/zipe/bastpartner/enumerate/
git commit -m "新增(workflow): 新增 workflow 相關列舉型別"
```

---

### Task 3：核心 Entity（workflow / node / edge）

**Files:**
- Create: `entity/WorkflowEntity.kt`、`WorkflowNodeEntity.kt`、`WorkflowEdgeEntity.kt`
- Test: `src/test/.../architecture/ArchitectureTest.kt`（既有，不改；用它驗證命名/位置）

**Step 1: 先讓 ArchUnit 當守門員**

執行既有架構測試，確認新 entity 命名/位置合規：
```
./gradlew test --tests "tw.zipe.bastpartner.architecture.ArchitectureTest"
```
Expected: 加完 entity 後仍 PASS（若命名錯/放錯套件會紅）。

**Step 2: 實作 `WorkflowEntity.kt`**（node config 在 Phase 1 以通用 JSON 儲存，型別化留待 Phase 3）
```kotlin
package tw.zipe.bastpartner.entity

import io.netty.util.internal.StringUtil
import jakarta.persistence.*
import tw.zipe.bastpartner.enumerate.WorkflowStatus
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Entity
@Table(name = "llm_workflow")
class WorkflowEntity : BaseEntity() {
    @Id @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: String? = null

    @Column(name = "user_id", nullable = false)
    var userId: String = StringUtil.EMPTY_STRING

    @Column(name = "name", nullable = false)
    var name: String = StringUtil.EMPTY_STRING

    @Column(name = "description")
    var description: String? = null

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    var status: WorkflowStatus = WorkflowStatus.DRAFT

    @Column(name = "version", nullable = false)
    var version: Int = 1

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "canvas_meta", columnDefinition = "json")
    var canvasMeta: Map<String, Any?>? = null
}
```
`WorkflowNodeEntity.kt`（`config: Map<String, Any?>` 通用 JSON）與 `WorkflowEdgeEntity.kt` 依 ERD 欄位比照實作（node 含 `workflowId`、`nodeKey`、`type: NodeType`、`name`、`positionX/Y: Double`、`config`；edge 含 `workflowId`、`sourceNodeKey`、`targetNodeKey`、`sourceHandle`、`targetHandle`、`label`、`condition: Map?`）。

**Step 3: 跑架構測試確認合規**
```
./gradlew test --tests "tw.zipe.bastpartner.architecture.ArchitectureTest"
```
Expected: PASS

**Step 4: Commit**
```bash
git add src/main/kotlin/tw/zipe/bastpartner/entity/Workflow*.kt
git commit -m "新增(workflow): 新增 workflow/node/edge JPA 實體"
```

---

### Task 4：Repository（workflow / node / edge）

**Files:**
- Create: `repository/WorkflowRepository.kt`、`WorkflowNodeRepository.kt`、`WorkflowEdgeRepository.kt`
- Test: `src/test/.../repository/WorkflowRepositoryTest.kt`

**Step 1: 寫失敗測試**（比照 `LLMMcpUserSettingRepositoryTest` 整合風格）
```kotlin
package tw.zipe.bastpartner.repository

import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull
import tw.zipe.bastpartner.entity.WorkflowEntity

@QuarkusTest
class WorkflowRepositoryTest {
    @Inject lateinit var workflowRepository: WorkflowRepository

    @Test
    fun `persist 後可依 id 取回 workflow`() {
        val wf = WorkflowEntity().apply { userId = "test-user"; name = "demo" }
        workflowRepository.saveOrUpdate(wf)
        assertNotNull(wf.id)
        val found = workflowRepository.findOptionalById(wf.id!!)
        assertNotNull(found)
    }
}
```

**Step 2: 跑測試確認失敗**
```
./gradlew test --tests "tw.zipe.bastpartner.repository.WorkflowRepositoryTest"
```
Expected: 編譯失敗（`WorkflowRepository` 不存在）。

**Step 3: 實作三個 repository**
```kotlin
package tw.zipe.bastpartner.repository

import jakarta.enterprise.context.ApplicationScoped
import tw.zipe.bastpartner.entity.WorkflowEntity

@ApplicationScoped
class WorkflowRepository : BaseRepository<WorkflowEntity, String>() {
    fun findByUserId(userId: String): List<WorkflowEntity> =
        list("userId = ?1", userId)
}
```
`WorkflowNodeRepository`（`fun findByWorkflowId(workflowId: String) = list("workflowId = ?1", workflowId)`、`fun deleteByWorkflowId(workflowId: String) = delete("workflowId = ?1", workflowId)`）與 `WorkflowEdgeRepository`（同 node 提供 `findByWorkflowId`、`deleteByWorkflowId`）比照。

**Step 4: 跑測試確認通過**
```
./gradlew test --tests "tw.zipe.bastpartner.repository.WorkflowRepositoryTest"
```
Expected: PASS（需 DB 在線）

**Step 5: Commit**
```bash
git add src/main/kotlin/tw/zipe/bastpartner/repository/Workflow*.kt src/test/kotlin/tw/zipe/bastpartner/repository/WorkflowRepositoryTest.kt
git commit -m "新增(workflow): 新增 workflow/node/edge repository"
```

---

### Task 5：DTO

**Files:**
- Create: `dto/WorkflowDTO.kt`（含 `WorkflowNodeDTO`、`WorkflowEdgeDTO`、`WorkflowSaveRequestDTO`、`WorkflowSummaryDTO`）

**Step 1: 實作**（`@Serializable`；欄位對齊 system-design §5.1）
- `WorkflowNodeDTO(nodeKey, type, name?, positionX, positionY, config)` —— config 以 `kotlinx.serialization.json.JsonObject` 或 `Map<String, JsonElement>` 承載通用 JSON。
- `WorkflowEdgeDTO(sourceNodeKey, targetNodeKey, sourceHandle?, targetHandle?, label?, condition?)`。
- `WorkflowDTO(id?, name, description?, status?, version?, nodes: List<WorkflowNodeDTO>, edges: List<WorkflowEdgeDTO>, canvasMeta?)`。
- `WorkflowSaveRequestDTO(id, version, name, description?, nodes, edges, canvasMeta?)`。
- `WorkflowSummaryDTO(id, name, status, version, updatedAt?)`。

**Step 2: Commit**
```bash
git add src/main/kotlin/tw/zipe/bastpartner/dto/WorkflowDTO.kt
git commit -m "新增(workflow): 新增 workflow 相關 DTO"
```

---

### Task 6：i18n 訊息 + AppMessage

**Files:**
- Modify: `resources/messages/messages_en_US.properties`、`messages_zh_TW.properties`
- Modify: `enumerate/AppMessage.kt`

**Step 1: 新增訊息鍵**（Phase 1 用到的；其餘執行期錯誤待 Phase 2-4 再加）

en_US：
```
workflow.not.found=Workflow not found
workflow.forbidden=No permission to access this workflow
workflow.version.conflict=Workflow has been modified by another session
workflow.node.key.duplicated=Duplicated node key: {0}
workflow.edge.node.not.found=Edge references a non-existent node: {0}
workflow.graph.has.cycle=Workflow graph contains an illegal cycle
workflow.trigger.node.required=An active workflow must contain a Trigger node
workflow.delete.while.running=Cannot delete a workflow while an execution is running
workflow.node.limit.exceeded=Workflow node count exceeds the limit ({0})
```
zh_TW：對應繁中翻譯。

**Step 2: 在 `AppMessage.kt` 新增對應 enum entry**（`WORKFLOW_NOT_FOUND("workflow.not.found")` 等，比照既有 entry 格式）。

**Step 3: Commit**
```bash
git add src/main/kotlin/tw/zipe/bastpartner/enumerate/AppMessage.kt src/main/resources/messages/
git commit -m "新增(workflow): 新增 workflow i18n 訊息與 AppMessage"
```

---

### Task 7：WorkflowService（CRUD + 畫布驗證）

**Files:**
- Create: `service/WorkflowService.kt`
- Test: `src/test/.../service/WorkflowServiceTest.kt`

**Step 1: 寫失敗測試**（覆蓋成功 + 關鍵失敗路徑；對齊 requirements AC-A2/A3/A6）
```kotlin
@QuarkusTest
class WorkflowServiceTest {
    @Inject lateinit var workflowService: WorkflowService

    @Test fun `save 後 get 可無損取回 nodes 與 edges`() { /* 建 workflow → save 含 2 node 1 edge → get 比對 */ }

    @Test fun `edge 參考不存在節點時整筆 rollback 並拋 WORKFLOW_EDGE_NODE_NOT_FOUND`() { /* assertThrows<ServiceException> */ }

    @Test fun `nodeKey 重複時拋 WORKFLOW_NODE_KEY_DUPLICATED`() { }

    @Test fun `圖含非法環時拋 WORKFLOW_GRAPH_HAS_CYCLE`() { }
}
```

**Step 2: 跑測試確認失敗**
```
./gradlew test --tests "tw.zipe.bastpartner.service.WorkflowServiceTest"
```
Expected: 編譯失敗（`WorkflowService` 不存在）。

**Step 3: 實作 `WorkflowService`**，`@ApplicationScoped`，注入三個 repository + `SecurityIdentity`（取 userId）。方法：
- `create(name, description): WorkflowDTO`
- `save(req: WorkflowSaveRequestDTO): WorkflowDTO` —— `@Transactional`；流程：① 擁有權檢核（非擁有者且非 admin → `WORKFLOW_FORBIDDEN`）② version 樂觀鎖比對（不符 → `WORKFLOW_VERSION_CONFLICT`）③ 驗證畫布（`validateGraph`）④ `nodeRepo.deleteByWorkflowId` + `edgeRepo.deleteByWorkflowId` 後寫入新集合 ⑤ `version++`、`update`。
- `get(id): WorkflowDTO`（含 nodes/edges；不存在 → `WORKFLOW_NOT_FOUND`；無權 → `WORKFLOW_FORBIDDEN`）
- `list(): List<WorkflowSummaryDTO>`（依當前 userId）
- `updateMeta(...)`、`delete(id)`（`@Transactional` 連鎖刪除 node/edge/trigger/execution/node_execution；有 RUNNING execution → `WORKFLOW_DELETE_WHILE_RUNNING`）、`switchStatus(id, active)`（啟用前須有 trigger 節點且圖無環）。
- `private fun validateGraph(nodes, edges)`：檢 nodeKey 唯一、edge 端點存在、節點數上限（`workflow.max-nodes`，預設 100，用 `@ConfigProperty`）、以 DFS/Kahn 偵測非法環（Loop 回邊例外 Phase 3 再處理，Phase 1 一律視環為非法）。

**Step 4: 跑測試確認通過**
```
./gradlew test --tests "tw.zipe.bastpartner.service.WorkflowServiceTest"
```
Expected: PASS

**Step 5: Commit**
```bash
git add src/main/kotlin/tw/zipe/bastpartner/service/WorkflowService.kt src/test/kotlin/tw/zipe/bastpartner/service/WorkflowServiceTest.kt
git commit -m "新增(workflow): 新增 WorkflowService CRUD 與畫布驗證"
```

---

### Task 8：WorkflowResource（CRUD 端點）

**Files:**
- Create: `resource/WorkflowResource.kt`
- Test: `src/test/.../resource/WorkflowResourceTest.kt`

**Step 1: 寫失敗測試**（rest-assured + `@TestSecurity`；對齊 API #1-6 與 RBAC）
```kotlin
@QuarkusTest
class WorkflowResourceTest {
    @Test
    @TestSecurity(user = "test-user", roles = ["user"])
    fun `建立 workflow 回 200 與 id`() {
        given().contentType("application/json").body("""{"name":"demo"}""")
            .`when`().post("/llm/workflow/create")
            .then().statusCode(200).body("data.id", notNullValue())
    }

    @Test
    fun `未登入呼叫 list 回 401`() {
        given().`when`().post("/llm/workflow/list").then().statusCode(401)
    }
}
```

**Step 2: 跑測試確認失敗**
```
./gradlew test --tests "tw.zipe.bastpartner.resource.WorkflowResourceTest"
```
Expected: 404/編譯失敗（端點不存在）。

**Step 3: 實作 `WorkflowResource`**，`@Path("/llm/workflow")` `@Authenticated`，注入 `WorkflowService`，端點對齊 system-design §5 API #1-6：`POST /create`、`/save`、`/get`、`/list`、`/update`、`/delete`、`/switchStatus`，皆回 `ApiResponse.success(...)`。

**Step 4: 跑測試確認通過 + 全測試回歸**
```
./gradlew test --tests "tw.zipe.bastpartner.resource.WorkflowResourceTest"
./gradlew test
```
Expected: PASS（含 ArchUnit）

**Step 5: Commit**
```bash
git add src/main/kotlin/tw/zipe/bastpartner/resource/WorkflowResource.kt src/test/kotlin/tw/zipe/bastpartner/resource/WorkflowResourceTest.kt
git commit -m "新增(workflow): 新增 WorkflowResource CRUD 端點"
```

---

### Task 9：文件同步（Phase 1 收尾）

**Step 1:** 呼叫 `documentation-sync` skill，同步：
- `.claude/rules/api-endpoints.md`（新增 WORKFLOW 模組 6 個端點）
- `docs/postman/basepartner.postman_collection.json`（新增 workflow 請求）
- `docs-site/` 相關頁（features / api）
- 確認 `docs/sql/bestpartner-ddl.sql` 已含新表

**Step 2:** 跑完整建置確認綠燈
```
./gradlew clean build -x test -Dquarkus.package.type=uber-jar -Dorg.gradle.daemon=false -Dquarkus.profile=dev
```

**Step 3: Commit**（文件變更）
```bash
git add .claude/rules/api-endpoints.md docs/postman/ docs-site/
git commit -m "文件(workflow): 同步 Phase 1 workflow CRUD 端點文件"
```

---

## Phase 1 完成定義（DoD）

- [ ] 6 張 `llm_workflow*` 表 DDL 入 `bestpartner-ddl.sql` 並套用至 DB
- [ ] workflow/node/edge entity + repository + DTO 完成，ArchUnit 全綠
- [ ] `WorkflowService` CRUD + 畫布驗證（nodeKey 唯一、edge 端點存在、環偵測、節點上限、擁有權、樂觀鎖）測試通過
- [ ] `WorkflowResource` 6 端點 + RBAC（rest-assured）測試通過
- [ ] i18n 訊息齊備（en/zh + AppMessage）
- [ ] `./gradlew test` 全綠、uber-jar 建置成功
- [ ] `documentation-sync` 已執行

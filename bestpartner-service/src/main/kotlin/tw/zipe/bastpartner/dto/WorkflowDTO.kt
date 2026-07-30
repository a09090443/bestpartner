package tw.zipe.bastpartner.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import tw.zipe.bastpartner.enumerate.NodeType
import tw.zipe.bastpartner.enumerate.WorkflowStatus

/**
 * Workflow 相關 DTO。
 *
 * 序列化說明：REST 層的 `@Serializable` DTO 由 `quarkus-rest-kotlin-serialization`
 * （kotlinx.serialization）處理。entity 端的任意巢狀 JSON 欄位為 `Map<String, Any?>`，
 * 但 kotlinx 無法序列化 `Any?`，因此 DTO 端統一改用 [JsonObject]
 * （= `Map<String, JsonElement>`，kotlinx 原生可序列化、可無損 round-trip 任意巢狀 JSON）。
 *
 * 預期 Map <-> JsonObject 轉換（屬 Task 7 service，非本任務範圍）：
 * - JsonObject -> Map：遞迴解包 JsonElement（JsonPrimitive -> String/Number/Boolean、
 *   JsonObject -> LinkedHashMap、JsonArray -> List），或經 `Json.decodeFromJsonElement`。
 * - Map -> JsonObject：經 `Json.encodeToJsonElement` 後取 `.jsonObject`。
 *
 * @author Gary
 * @created 2026/6/28
 */

/**
 * Workflow 節點 DTO。對應 [tw.zipe.bastpartner.entity.WorkflowNodeEntity]。
 */
@Serializable
class WorkflowNodeDTO {
    /** 節點識別鍵（workflow 內唯一） */
    var nodeKey: String = ""

    /** 節點類型 */
    var type: NodeType? = null

    /** 名稱 */
    var name: String? = null

    /** 畫布座標 X */
    var positionX: Double = 0.0

    /** 畫布座標 Y */
    var positionY: Double = 0.0

    /** 節點設定（任意巢狀 JSON，對應 entity 的 config Map） */
    var config: JsonObject = JsonObject(emptyMap())
}

/**
 * Workflow 連線（邊）DTO。對應 [tw.zipe.bastpartner.entity.WorkflowEdgeEntity]。
 */
@Serializable
class WorkflowEdgeDTO {
    /** 來源節點 key */
    var sourceNodeKey: String = ""

    /** 目標節點 key */
    var targetNodeKey: String = ""

    /** 來源連接點 */
    var sourceHandle: String? = null

    /** 目標連接點 */
    var targetHandle: String? = null

    /** 連線標籤 */
    var label: String? = null

    /** 連線條件（任意巢狀 JSON，對應 entity 的 condition Map；可空） */
    var condition: JsonObject? = null
}

/**
 * Workflow 完整定義 DTO（含節點與連線）。對應 [tw.zipe.bastpartner.entity.WorkflowEntity]。
 */
@Serializable
class WorkflowDTO {
    /** 主鍵（新增時為 null） */
    var id: String? = null

    /** 名稱 */
    var name: String = ""

    /** 描述 */
    var description: String? = null

    /** 狀態 */
    var status: WorkflowStatus? = null

    /** 版本 */
    var version: Int? = null

    /** 節點清單 */
    var nodes: List<WorkflowNodeDTO> = emptyList()

    /** 連線清單 */
    var edges: List<WorkflowEdgeDTO> = emptyList()

    /** 畫布中繼資料（任意巢狀 JSON，對應 entity 的 canvasMeta Map；可空） */
    var canvasMeta: JsonObject? = null

    /** 執行輸入（僅 /execute 端點使用，任意巢狀 JSON；可空） */
    var inputPayload: JsonObject? = null

    /**
     * 指定由哪個 TRIGGER 節點發起本次執行（僅 /execute 端點使用；可空）。
     *
     * 帶值時僅該觸發點被活化，其餘 TRIGGER 與其獨佔下游落 SKIPPED；
     * 省略或空白＝維持「所有 TRIGGER 皆執行」的既有行為。
     */
    var triggerNodeKey: String? = null
}

/**
 * Workflow 儲存請求 DTO。
 *
 * id 與 version 用於樂觀鎖：更新既有 workflow 時須帶入；新建時 id 可為 null。
 */
@Serializable
class WorkflowSaveRequestDTO {
    /** 主鍵（更新時必填，新建時 null） */
    var id: String? = null

    /** 版本（樂觀鎖比對用） */
    var version: Int? = null

    /** 名稱 */
    var name: String = ""

    /** 描述 */
    var description: String? = null

    /** 節點清單 */
    var nodes: List<WorkflowNodeDTO> = emptyList()

    /** 連線清單 */
    var edges: List<WorkflowEdgeDTO> = emptyList()

    /** 畫布中繼資料（任意巢狀 JSON；可空） */
    var canvasMeta: JsonObject? = null
}

/**
 * Workflow 啟用 / 停用請求 DTO。
 */
@Serializable
class WorkflowSwitchStatusRequestDTO {
    /** 主鍵 */
    var id: String? = null

    /** true = 啟用（ACTIVE）、false = 停用（INACTIVE） */
    var active: Boolean = false
}

/**
 * Workflow 摘要 DTO（清單列表用）。
 *
 * updatedAt 採 String 而非 LocalDateTime：kotlinx.serialization 無法原生序列化
 * java.time，且專案既有 DTO 皆未使用 LocalDateTime；由 service 層格式化為字串輸出。
 */
@Serializable
class WorkflowSummaryDTO {
    /** 主鍵 */
    var id: String? = null

    /** 名稱 */
    var name: String? = null

    /** 狀態 */
    var status: WorkflowStatus? = null

    /** 版本 */
    var version: Int? = null

    /** 最後更新時間（已格式化字串） */
    var updatedAt: String? = null
}

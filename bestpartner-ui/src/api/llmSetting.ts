import http from './http'
import type { ApiResponse } from '../types/api'
import type { Option } from '../types/options'

/** 後端 LLM 設定 DTO（僅取下拉需要的欄位） */
interface LLMSettingDTO {
  id?: string
  alias?: string
  modelType?: string
}

/**
 * raw list 快取：以 Promise 形式快取，兼作「進行中請求」的去重。
 * getSettings / getEmbeddingSettings 皆呼叫 fetchRawSettings，一個 session 內
 * 對 /llm/setting/get 只發一次；並發呼叫共用同一 Promise，不會雙抓。
 */
let rawSettingsCache: Promise<LLMSettingDTO[]> | null = null

/** 實際發出 POST /llm/setting/get（空 body 取全量）；失敗時清除快取以利重試 */
function fetchRawSettings(): Promise<LLMSettingDTO[]> {
  if (!rawSettingsCache) {
    rawSettingsCache = http
      .post<ApiResponse<LLMSettingDTO[]>>('/llm/setting/get', {})
      .then((res) => res.data.data ?? [])
      .catch((err) => {
        // 壞快取（rejected Promise）會讓後續呼叫永遠失敗，須清除以允許重試
        rawSettingsCache = null
        throw err
      })
  }
  return rawSettingsCache
}

/**
 * 使 raw list 快取失效。新增／更新／刪除 LLM 設定成功後應呼叫，
 * 使下次 getSettings / getEmbeddingSettings 重新抓取最新清單。
 *
 * 目前本檔無寫入端點；呼叫入口應由 LLM 設定管理頁在 save/update/delete
 * 成功後觸發（非本次範圍，此處預留公開入口）。
 */
export function invalidateSettingsCache(): void {
  rawSettingsCache = null
}

/**
 * 查詢當前用戶的 LLM 設定，轉為下拉 Option[]。
 * 空 body 取全部；value = 設定 id（即 chat 端點使用的 llmId），label = alias。
 * EMBEDDING 型別無法用於對話端點，直接濾除；其餘型別（CHAT / STREAMING_CHAT）
 * 於 label 加註型別，因執行期端點與模型型別不符會直接失敗，須讓使用者可辨識。
 */
export async function getSettings(): Promise<Option[]> {
  const list = await fetchRawSettings()
  return list
    .filter((s): s is LLMSettingDTO & { id: string } => !!s.id && s.modelType !== 'EMBEDDING')
    .map((s) => ({
      value: s.id,
      label: (s.alias || s.id) + (s.modelType ? `（${s.modelType}）` : ''),
    }))
}

/**
 * 查詢當前用戶的 EMBEDDING 型別 LLM 設定，轉為下拉 Option[]。
 * 供 KNOWLEDGE_RAG 節點的 embeddingModelId 選用；與 getSettings 相反，僅保留 EMBEDDING。
 */
export async function getEmbeddingSettings(): Promise<Option[]> {
  const list = await fetchRawSettings()
  return list
    .filter((s): s is LLMSettingDTO & { id: string } => !!s.id && s.modelType === 'EMBEDDING')
    .map((s) => ({ value: s.id, label: s.alias || s.id }))
}

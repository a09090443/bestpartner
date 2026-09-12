import { ref } from 'vue'
import * as llmSettingApi from '../api/llmSetting'
import * as toolApi from '../api/tool'
import * as mcpServerApi from '../api/mcpServer'
import * as vectorApi from '../api/vector'
import * as skillApi from '../api/skill'
import type { Option } from '../types/options'

/**
 * 節點表單下拉選項的載入與快取。
 * 各清單以模組級 ref 快取，一個 session 只抓一次；表單掛載時 await 對應 loader。
 */

const llmOptions = ref<Option[] | null>(null)
const embeddingOptions = ref<Option[] | null>(null)
const toolOptions = ref<Option[] | null>(null)
const mcpOptions = ref<Option[] | null>(null)
const knowledgeOptions = ref<Option[] | null>(null)
const skillOptions = ref<Option[] | null>(null)

/**
 * 清除所有節點選項快取。這些清單皆為 per-user 資料（LLM 設定、工具、MCP、知識庫、Skill），
 * 登出時須清除，否則同分頁換使用者登入（client-side 導航不 reload）會看到上一位的殘留清單。
 */
export function clearNodeOptionsCache(): void {
  llmOptions.value = null
  embeddingOptions.value = null
  toolOptions.value = null
  mcpOptions.value = null
  knowledgeOptions.value = null
  skillOptions.value = null
}

export function useNodeOptions() {
  async function loadLlmOptions(): Promise<Option[]> {
    if (!llmOptions.value) llmOptions.value = await llmSettingApi.getSettings()
    return llmOptions.value
  }

  /** KNOWLEDGE_RAG 節點 embeddingModelId 專用：僅列 EMBEDDING 型別的 LLM 設定 */
  async function loadEmbeddingOptions(): Promise<Option[]> {
    if (!embeddingOptions.value) embeddingOptions.value = await llmSettingApi.getEmbeddingSettings()
    return embeddingOptions.value
  }

  async function loadToolOptions(): Promise<Option[]> {
    if (!toolOptions.value) toolOptions.value = await toolApi.listTools()
    return toolOptions.value
  }

  async function loadMcpOptions(): Promise<Option[]> {
    if (!mcpOptions.value) mcpOptions.value = await mcpServerApi.listMcpServers()
    return mcpOptions.value
  }

  async function loadKnowledgeOptions(): Promise<Option[]> {
    if (!knowledgeOptions.value) knowledgeOptions.value = await vectorApi.listKnowledgeStores()
    return knowledgeOptions.value
  }

  async function loadSkillOptions(): Promise<Option[]> {
    if (!skillOptions.value) skillOptions.value = await skillApi.listSkills()
    return skillOptions.value
  }

  return {
    loadLlmOptions,
    loadEmbeddingOptions,
    loadToolOptions,
    loadMcpOptions,
    loadKnowledgeOptions,
    loadSkillOptions,
  }
}

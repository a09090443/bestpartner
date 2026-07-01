import { ref } from 'vue'
import * as llmSettingApi from '../api/llmSetting'
import * as toolApi from '../api/tool'
import * as mcpServerApi from '../api/mcpServer'
import type { Option } from '../types/options'

/**
 * 節點表單下拉選項的載入與快取。
 * 三種清單以模組級 ref 快取，一個 session 只抓一次；表單掛載時 await 對應 loader。
 */

const llmOptions = ref<Option[] | null>(null)
const toolOptions = ref<Option[] | null>(null)
const mcpOptions = ref<Option[] | null>(null)

export function useNodeOptions() {
  async function loadLlmOptions(): Promise<Option[]> {
    if (!llmOptions.value) llmOptions.value = await llmSettingApi.getSettings()
    return llmOptions.value
  }

  async function loadToolOptions(): Promise<Option[]> {
    if (!toolOptions.value) toolOptions.value = await toolApi.listTools()
    return toolOptions.value
  }

  async function loadMcpOptions(): Promise<Option[]> {
    if (!mcpOptions.value) mcpOptions.value = await mcpServerApi.listMcpServers()
    return mcpOptions.value
  }

  return { loadLlmOptions, loadToolOptions, loadMcpOptions }
}

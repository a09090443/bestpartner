import { describe, it, expect } from 'vitest'
import { missingRequiredForNode, formatMissingFields } from '../nodeRequiredFields'
import type { NodeRequiredFields } from '../../api/workflow'

describe('missingRequiredForNode', () => {
  const requiredFields: NodeRequiredFields = {
    LLM_ASSISTANT: ['llmId'],
    MCP_SERVER: ['mcpId', 'toolName'],
    DATA_TRANSFORM: ['mappings|template'],
  }

  it('型別不在必填清單中時回傳空陣列', () => {
    expect(missingRequiredForNode('UNKNOWN_TYPE', {}, requiredFields)).toEqual([])
  })

  it('config 缺少必填欄位時回傳缺漏欄位名稱', () => {
    expect(missingRequiredForNode('LLM_ASSISTANT', {}, requiredFields)).toEqual(['llmId'])
  })

  it('config 齊全必填欄位時回傳空陣列', () => {
    expect(missingRequiredForNode('LLM_ASSISTANT', { llmId: 'l1' }, requiredFields)).toEqual([])
  })

  it('config 欄位為空字串時仍視為缺漏', () => {
    expect(missingRequiredForNode('LLM_ASSISTANT', { llmId: '' }, requiredFields)).toEqual(['llmId'])
  })

  it('config 欄位為空陣列時仍視為缺漏', () => {
    expect(missingRequiredForNode('MCP_SERVER', { mcpId: 'm1', toolName: [] }, requiredFields)).toEqual([
      'toolName',
    ])
  })

  it('config 欄位為 null 時仍視為缺漏', () => {
    expect(missingRequiredForNode('LLM_ASSISTANT', { llmId: null }, requiredFields)).toEqual(['llmId'])
  })

  it('多個必填欄位都缺漏時全數回傳', () => {
    expect(missingRequiredForNode('MCP_SERVER', {}, requiredFields)).toEqual(['mcpId', 'toolName'])
  })

  it('複合字樣 "a|b" 任一欄位有值即滿足，不視為缺漏', () => {
    expect(
      missingRequiredForNode('DATA_TRANSFORM', { mappings: { a: 'b' } }, requiredFields),
    ).toEqual([])
    expect(
      missingRequiredForNode('DATA_TRANSFORM', { template: 'tpl' }, requiredFields),
    ).toEqual([])
  })

  it('複合字樣 "a|b" 所有子欄位皆空時回傳原字樣', () => {
    expect(missingRequiredForNode('DATA_TRANSFORM', {}, requiredFields)).toEqual(['mappings|template'])
  })
})

describe('formatMissingFields', () => {
  it('複合字樣 "a|b" 格式化為「中文標籤 或 中文標籤（擇一）」', () => {
    expect(formatMissingFields(['mappings|template'])).toBe('欄位對應 或 範本（擇一）')
  })

  it('已知欄位鍵轉為中文標籤', () => {
    expect(formatMissingFields(['llmId'])).toBe('LLM 設定')
    expect(formatMissingFields(['mcpId', 'toolName'])).toBe('MCP 伺服器、工具名稱')
  })

  it('未知欄位鍵 fallback 顯示原始 key', () => {
    expect(formatMissingFields(['someUnmappedField'])).toBe('someUnmappedField')
  })

  it('空陣列回傳空字串', () => {
    expect(formatMissingFields([])).toBe('')
  })
})

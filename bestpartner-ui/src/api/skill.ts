import http from './http'
import type { ApiResponse } from '../types/api'
import type { Option } from '../types/options'

/** 後端 Skill DTO（僅取下拉需要的欄位） */
interface SkillDTO {
  id?: string
  name?: string
}

/** 列出當前使用者可用的 Skill（含 global），轉為下拉 Option[]（value = skill id，label = name） */
export async function listSkills(): Promise<Option[]> {
  const res = await http.get<ApiResponse<SkillDTO[]>>('/llm/skill/list')
  const list = res.data.data ?? []
  return list
    .filter((s): s is SkillDTO & { id: string } => !!s.id)
    .map((s) => ({ value: s.id, label: s.name || s.id }))
}

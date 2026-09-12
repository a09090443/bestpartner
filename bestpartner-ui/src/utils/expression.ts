/**
 * 插值運算式（`{{nodeKey.field}}`）的解析與序列化。
 *
 * 設計原則：**儲存格式不變**——config 裡永遠是後端看得懂的 `{{nodeKey.field}}` 原字串，
 * 這裡只負責把它拆成「文字／引用」片段，供編輯器渲染成人類看得懂的色塊。
 *
 * 語法對齊後端 `ExecutionContext.PLACEHOLDER`：`\{\{\s*([\w.\-]+)\s*}}`
 * ——即大括號內只允許英數、底線、句點、連字號，且允許前後空白。
 * 改動後端該 regex 時，此處須同步。
 */

/** 與後端 ExecutionContext.PLACEHOLDER 等價的 pattern（`g` 供逐一掃描） */
export const TEMPLATE_REF_PATTERN = /\{\{\s*([\w.\-]+)\s*\}\}/g

/**
 * 零寬空格。contenteditable 中，色塊（token）若是最後一個子節點，
 * 游標會無處可放；插入 token 後補一個 ZWSP 讓游標有落腳處，讀值時再整批剝除。
 */
export const ZWSP = '​'

export type Segment =
  | { kind: 'text'; value: string }
  | { kind: 'ref'; value: string; path: string }

/**
 * 把模板字串拆成文字與引用片段。
 * `value` 保留原字串（含大括號），`path` 是括號內的路徑（如 `abc123.reply`）。
 */
export function parseTemplate(text: string): Segment[] {
  const segments: Segment[] = []
  let lastIndex = 0
  // 每次使用前重置 lastIndex：TEMPLATE_REF_PATTERN 帶 g 旗標，是有狀態的
  TEMPLATE_REF_PATTERN.lastIndex = 0
  let match: RegExpExecArray | null
  while ((match = TEMPLATE_REF_PATTERN.exec(text)) !== null) {
    if (match.index > lastIndex) {
      segments.push({ kind: 'text', value: text.slice(lastIndex, match.index) })
    }
    segments.push({ kind: 'ref', value: match[0], path: match[1] })
    lastIndex = match.index + match[0].length
  }
  if (lastIndex < text.length) segments.push({ kind: 'text', value: text.slice(lastIndex) })
  return segments
}

/** 引用路徑的第一段＝節點 key（或 `__input__`、LOOP 的迭代別名等內建值） */
export function refRootKey(path: string): string {
  return path.split('.')[0] ?? ''
}

/** 引用路徑去掉節點 key 後的欄位路徑；單段引用（如 LOOP 的 `{{item}}`）回空字串 */
export function refFieldPath(path: string): string {
  return path.split('.').slice(1).join('.')
}

/**
 * 產生色塊上顯示的文字。
 * 查得到節點名稱就顯示「名稱 › 欄位」，查不到則原樣顯示路徑（並由呼叫端標示為未知）。
 */
export function formatRefLabel(path: string, labels: Record<string, string>): string {
  const root = refRootKey(path)
  const name = labels[root]
  if (!name) return path
  const field = refFieldPath(path)
  return field ? `${name} › ${field}` : name
}

/** 該引用的根節點是否可解析（供編輯器標示未知引用，例如節點已被刪除或手打錯字） */
export function isKnownRef(path: string, labels: Record<string, string>): boolean {
  return Boolean(labels[refRootKey(path)])
}

/**
 * 從 contenteditable 的子節點還原模板字串。
 *
 * - token 色塊（帶 `data-ref`）→ 還原成原始的 `{{...}}` 字串
 * - `<br>` 與區塊元素邊界 → 換行
 * - 其餘文字節點 → 原樣，最後剝除 ZWSP
 */
export function serializeNodes(root: Node): string {
  let out = ''

  const walk = (node: Node) => {
    if (node.nodeType === Node.TEXT_NODE) {
      out += node.textContent ?? ''
      return
    }
    if (node.nodeType !== Node.ELEMENT_NODE) return

    const el = node as HTMLElement
    const ref = el.getAttribute('data-ref')
    if (ref) {
      out += ref
      return
    }
    if (el.tagName === 'BR') {
      out += '\n'
      return
    }
    // 瀏覽器在 contenteditable 內按 Enter 可能產生 <div>/<p> 包裹，視為換行
    const isBlock = el.tagName === 'DIV' || el.tagName === 'P'
    if (isBlock && out !== '' && !out.endsWith('\n')) out += '\n'
    el.childNodes.forEach(walk)
  }

  root.childNodes.forEach(walk)
  return out.split(ZWSP).join('')
}

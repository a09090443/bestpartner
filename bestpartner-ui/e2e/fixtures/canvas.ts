import type { Page } from '@playwright/test'

/**
 * 畫布互動共用 helper（自 j5-openrouter-date-json.spec.ts 抽出，供各 J5 切片 spec 共用）。
 *
 * 兩種互動機制勿混用（詳見 docs/e2e-test-plan.md §6）：
 * - palette → 畫布新增節點是 HTML5 原生 DnD，須合成 dragstart/dragover/drop（共用 DataTransfer）
 * - 節點連線才是 pointer 序列（mousedown → move → up）
 */

/** 合成 HTML5 拖放：palette item → 畫布指定畫面座標（Vue Flow 用原生 DnD 新增節點） */
export async function dragNodeToCanvas(page: Page, nodeType: string, x: number, y: number) {
  await page.evaluate(
    ({ nodeType, x, y }) => {
      const src = document.querySelector(`[data-test="palette-item-${nodeType}"]`) as HTMLElement
      const canvas = document.querySelector('.canvas') as HTMLElement
      if (!src || !canvas) throw new Error(`missing src/canvas for ${nodeType}`)
      const dt = new DataTransfer()
      const r = src.getBoundingClientRect()
      const fire = (el: Element, type: string, cx: number, cy: number) =>
        el.dispatchEvent(
          new DragEvent(type, { bubbles: true, cancelable: true, clientX: cx, clientY: cy, dataTransfer: dt }),
        )
      fire(src, 'dragstart', r.x + 8, r.y + 8)
      fire(canvas, 'dragenter', x, y)
      fire(canvas, 'dragover', x, y)
      fire(canvas, 'drop', x, y)
      fire(src, 'dragend', x, y)
    },
    { nodeType, x, y },
  )
}

/** 以 pointer 事件連線：來源節點 sourceHandle → 目標節點 targetHandle */
export async function connect(page: Page, fromType: string, fromHandle: string, toType: string, toHandle: string) {
  const src = page.locator(`[data-node-type="${fromType}"] .vue-flow__handle[data-handleid="${fromHandle}"]`).first()
  const tgt = page.locator(`[data-node-type="${toType}"] .vue-flow__handle[data-handleid="${toHandle}"]`).first()
  const sb = await src.boundingBox()
  const tb = await tgt.boundingBox()
  if (!sb || !tb) throw new Error(`handle box missing ${fromType}:${fromHandle} → ${toType}:${toHandle}`)
  const sc = { x: sb.x + sb.width / 2, y: sb.y + sb.height / 2 }
  const tc = { x: tb.x + tb.width / 2, y: tb.y + tb.height / 2 }
  await page.mouse.move(sc.x, sc.y)
  await page.mouse.down()
  await page.mouse.move((sc.x + tc.x) / 2, (sc.y + tc.y) / 2, { steps: 6 })
  await page.mouse.move(tc.x, tc.y, { steps: 6 })
  await page.mouse.up()
}

/** 點選畫布上第一個指定型別的節點（開啟 Inspector） */
export async function selectNode(page: Page, nodeType: string) {
  await page.locator(`[data-node-type="${nodeType}"]`).first().click()
}

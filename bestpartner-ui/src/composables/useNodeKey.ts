import { nanoid } from 'nanoid'

/**
 * 產生畫布節點用的唯一 nodeKey（長度 8）。
 * 後端以 nodeKey 作為節點在單一 workflow 內的唯一識別。
 */
export function generateNodeKey(): string {
  return nanoid(8)
}

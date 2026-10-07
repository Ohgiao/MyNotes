/**
 * 定稿文本并入输入框的规则。刻意写成不依赖 vue 的纯函数，
 * 这样 Node 可以直接 import 做断言（Node 24 支持直接跑 .ts）。
 */

export type InsertMode = 'append' | 'replace'

export function applyFinal(current: string, transcript: string, mode: InsertMode): string {
  const text = transcript.trim()
  if (!text) {
    return current
  }
  if (mode === 'replace' || !current) {
    return text
  }
  // 中文直接连写，只有两边都是西文单词/数字时才补空格
  const needsSpace = /[A-Za-z0-9]$/.test(current) && /^[A-Za-z0-9]/.test(text)
  return needsSpace ? `${current} ${text}` : `${current}${text}`
}

export function applyFinals(current: string, finals: string[], mode: InsertMode): string {
  return finals.reduce((acc, final) => applyFinal(acc, final, mode), current)
}

export interface FinalGuard {
  lastText: string
  lastAtMs: number
}

/**
 * 同一段话在 500ms 内被重复投递时返回 null 表示丢弃——
 * Edge 偶发把同一个 final 报到两次，不去重就会看到重复字。
 */
export function acceptFinal(guard: FinalGuard, transcript: string, nowMs: number): FinalGuard | null {
  const text = transcript.trim()
  if (!text) {
    return guard
  }
  if (text === guard.lastText && nowMs - guard.lastAtMs < 500) {
    return null
  }
  return { lastText: text, lastAtMs: nowMs }
}

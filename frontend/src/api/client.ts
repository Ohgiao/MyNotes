import type {
  AiStatus,
  AskResult,
  EntryDetail,
  EntryListItem,
  EntryType,
  ReviewItem,
  ReviewResult,
  SearchItem,
  TimelineDay,
  TreeNode
} from '@/types'

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(path, {
    headers: { 'Content-Type': 'application/json' },
    ...init
  })
  if (!res.ok) {
    let message = `${res.status} ${res.statusText}`
    try {
      const body = await res.json()
      message = body.error ?? body.message ?? message
    } catch {
      // 响应不是 JSON（例如 404 页面），保留状态码信息
    }
    throw new Error(message)
  }
  return res.status === 204 ? (undefined as T) : ((await res.json()) as T)
}

export interface CreateEntryPayload {
  type: EntryType
  title?: string
  contentMd: string
  sourceDate?: string
  parentId?: number | null
  tagNames?: string[]
}

export interface UpdateEntryPayload {
  title?: string
  contentMd: string
  sourceDate?: string
  parentId?: number | null
  pinned?: boolean
  tagNames?: string[]
}

export interface TimelineResult {
  days: TimelineDay[]
  nextCursor: string | null
  flat: EntryListItem[]
}

export const api = {
  createEntry: (payload: CreateEntryPayload) =>
    request<EntryDetail>('/api/entries', { method: 'POST', body: JSON.stringify(payload) }),

  getEntry: (id: number) => request<EntryDetail>(`/api/entries/${id}`),

  updateEntry: (id: number, payload: UpdateEntryPayload) =>
    request<EntryDetail>(`/api/entries/${id}`, { method: 'PUT', body: JSON.stringify(payload) }),

  deleteEntry: (id: number) => request<void>(`/api/entries/${id}`, { method: 'DELETE' }),

  restoreEntry: (id: number) => request<void>(`/api/entries/${id}/restore`, { method: 'POST' }),

  timeline: async (params: { type?: EntryType; cursor?: string | null; size?: number }): Promise<TimelineResult> => {
    const query = new URLSearchParams()
    if (params.type) query.set('type', params.type)
    if (params.cursor) query.set('cursor', params.cursor)
    query.set('size', String(params.size ?? 30))
    const page = await request<{ days: TimelineDay[]; nextCursor: string | null }>(`/api/timeline?${query}`)
    return {
      days: page.days,
      nextCursor: page.nextCursor,
      flat: page.days.flatMap((d) => d.entries)
    }
  },

  search: (q: string, type?: EntryType) => {
    const query = new URLSearchParams({ q })
    if (type) query.set('type', type)
    return request<SearchItem[]>(`/api/search?${query}`)
  },

  tree: () => request<TreeNode[]>('/api/tree'),

  trash: () => request<EntryListItem[]>('/api/trash?limit=100'),

  reviewsToday: (limit = 20) => request<ReviewItem[]>(`/api/reviews/today?limit=${limit}`),

  reviewResult: (id: number, outcome: 'known' | 'vague' | 'forgot') =>
    request<ReviewResult>(`/api/reviews/${id}/result`, {
      method: 'POST',
      body: JSON.stringify({ outcome })
    }),

  addLink: (srcId: number, dstId: number) =>
    request<void>(`/api/entries/${srcId}/links`, {
      method: 'POST',
      body: JSON.stringify({ dstId })
    }),

  removeLink: (srcId: number, dstId: number) =>
    request<void>(`/api/entries/${srcId}/links/${dstId}`, { method: 'DELETE' }),

  aiStatus: () => request<AiStatus>('/api/ai/status'),

  aiAsk: (question: string, topK = 5) =>
    request<AskResult>('/api/ai/ask', { method: 'POST', body: JSON.stringify({ question, topK }) }),

  aiReindex: (limit = 200) =>
    request<{ embedded: number; remaining: number }>(`/api/ai/reindex?limit=${limit}`, { method: 'POST' })
}

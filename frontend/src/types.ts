export type EntryType = 'log' | 'note'

export interface EntryListItem {
  id: number
  type: EntryType
  title: string
  preview: string
  sourceDate: string
  pinned: boolean
  updatedAt: string
}

export interface LinkItem {
  id: number
  type: EntryType
  title: string
  sourceDate: string
}

export interface EntryRelations {
  outgoing: LinkItem[]
  incoming: LinkItem[]
}

export interface EntryDetail {
  id: number
  type: EntryType
  title: string
  contentMd: string
  parentId: number | null
  sourceDate: string
  reviewStatus: string
  mastery: number
  nextReviewAt: string | null
  reviewCount: number
  pinned: boolean
  createdAt: string
  updatedAt: string
  tags: string[]
  relations: EntryRelations
}

export interface TreeNode {
  id: number
  title: string
  sourceDate: string
  reviewStatus: string
  mastery: number
  nextReviewAt: string | null
  children: TreeNode[]
}

export interface AiStatus {
  configured: boolean
  mode: 'rag' | 'keyword'
  pending: number
  embedded: number
}

export interface Citation {
  index: number
  entryId: number
  type: EntryType
  title: string
  snippet: string
  sourceDate: string
  score: number
  matchedBy: 'keyword' | 'vector' | 'both'
}

export interface AskResult {
  question: string
  answer: string
  citations: Citation[]
  mode: 'rag' | 'keyword'
}

export interface ReviewItem {
  id: number
  title: string
  preview: string
  mastery: number
  reviewCount: number
  nextReviewAt: string
  sourceDate: string
}

export interface ReviewResult {
  id: number
  reviewStatus: string
  mastery: number
  nextReviewAt: string
  remainingDue: number
}

export interface TimelineDay {
  date: string
  entries: EntryListItem[]
}

export interface TimelinePage {
  days: TimelineDay[]
  nextCursor: string | null
}

export interface SearchItem {
  id: number
  type: EntryType
  title: string
  snippet: string
  sourceDate: string
  titleHit: boolean
}

<script setup lang="ts">
import { nextTick, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '@/api/client'
import type { AiStatus, AskResult } from '@/types'

const inputRef = ref<HTMLTextAreaElement | null>(null)
const question = ref('')
const status = ref<AiStatus | null>(null)
const result = ref<AskResult | null>(null)
const asking = ref(false)
const indexing = ref(false)
const error = ref('')

const MATCHED_LABEL: Record<string, string> = {
  keyword: '关键词命中',
  vector: '语义命中',
  both: '两路都命中'
}

function isImeComposing(e: KeyboardEvent) {
  return e.isComposing || e.keyCode === 229
}

function onKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey && !isImeComposing(e)) {
    e.preventDefault()
    void ask()
  }
}

async function loadStatus() {
  try {
    status.value = await api.aiStatus()
  } catch (err) {
    error.value = (err as Error).message
  }
}

async function ask() {
  const text = question.value.trim()
  if (!text || asking.value) return
  asking.value = true
  error.value = ''
  try {
    result.value = await api.aiAsk(text)
    question.value = ''
  } catch (err) {
    error.value = (err as Error).message
  } finally {
    asking.value = false
    nextTick(() => inputRef.value?.focus())
  }
}

async function reindex() {
  if (indexing.value) return
  indexing.value = true
  error.value = ''
  try {
    const done = await api.aiReindex()
    ElMessage.success(`已建立 ${done.embedded} 条向量，剩余 ${done.remaining} 条`)
    await loadStatus()
  } catch (err) {
    error.value = (err as Error).message
  } finally {
    indexing.value = false
  }
}

onMounted(() => {
  void loadStatus()
  nextTick(() => inputRef.value?.focus())
})
</script>

<template>
  <section>
    <h1 class="page-title">问我的笔记</h1>

    <p v-if="status && !status.configured" class="notice">
      还没配置模型 api-key，现在是<strong>关键词检索模式</strong>：只给出命中的笔记原文，不生成总结。
      配好 <code>application-local.yml</code> 的 mynotes.ai 后这里会自动切换。
    </p>
    <p v-else-if="status" class="notice">
      RAG 模式 · 已向量化 {{ status.embedded }} 条
      <template v-if="status.pending > 0">
        · 有 {{ status.pending }} 条内容还没进索引
        <button class="link-btn" :disabled="indexing" @click="reindex">
          {{ indexing ? '建立中…' : '现在重建' }}
        </button>
      </template>
    </p>

    <textarea
      ref="inputRef"
      v-model="question"
      class="ask-input"
      rows="2"
      placeholder="比如：我之前怎么理解连接池大小的？回车提问，Shift+Enter 换行"
      @keydown="onKeydown"
    ></textarea>

    <p v-if="error" class="error">{{ error }}</p>

    <div v-if="result" class="answer-block">
      <p v-if="result.answer" class="answer">{{ result.answer }}</p>
      <p v-else class="answer-empty">没有生成回答（未配置模型），下面是检索到的原文。</p>

      <h2 class="cite-title">依据（{{ result.citations.length }}）</h2>
      <p v-if="result.citations.length === 0" class="answer-empty">笔记里没有匹配的内容。</p>
      <RouterLink
        v-for="cite in result.citations"
        :key="cite.entryId"
        :to="`/entry/${cite.entryId}`"
        class="cite"
      >
        <div class="cite-head">
          <span class="cite-index">[{{ cite.index }}]</span>
          <span class="cite-title-text">{{ cite.title || '(无标题)' }}</span>
          <span class="badge" :class="cite.type">{{ cite.type === 'log' ? '日志' : '笔记' }}</span>
          <span class="cite-matched">{{ MATCHED_LABEL[cite.matchedBy] ?? cite.matchedBy }}</span>
        </div>
        <p class="cite-snippet">{{ cite.snippet }}</p>
        <time class="cite-date">{{ cite.sourceDate }}</time>
      </RouterLink>
    </div>
  </section>
</template>

<style scoped>
.page-title { font-size: 18px; margin: 0 0 8px; }
.notice { font-size: 13px; color: #6b7280; background: #f4f6f9; border-radius: 8px; padding: 10px 12px; margin: 0 0 14px; }
.notice code { background: #fff; padding: 1px 5px; border-radius: 4px; font-size: 12px; }
.ask-input {
  width: 100%;
  padding: 12px 14px;
  font-size: 15px;
  font-family: inherit;
  border: 1px solid var(--line);
  border-radius: 10px;
  resize: vertical;
}
.ask-input:focus { outline: none; border-color: #4a89dc; }
.error { color: #d0453e; font-size: 13px; }
.answer {
  margin: 18px 0 0;
  padding: 16px;
  background: #fff;
  border: 1px solid var(--line);
  border-left: 3px solid #4a89dc;
  border-radius: 8px;
  font-size: 15px;
  line-height: 1.85;
  white-space: pre-wrap;
}
.answer-empty { margin-top: 16px; font-size: 13px; color: var(--muted); }
.cite-title { font-size: 13px; color: var(--muted); margin: 20px 0 8px; font-weight: 600; }
.cite {
  display: block;
  padding: 10px 12px;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 8px;
  margin-bottom: 8px;
  text-decoration: none;
  color: inherit;
}
.cite-head { display: flex; align-items: center; gap: 8px; }
.cite-index { color: #4a89dc; font-size: 13px; font-weight: 600; }
.cite-title-text { font-size: 14px; font-weight: 600; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.cite-matched { font-size: 11px; color: var(--muted); margin-left: auto; white-space: nowrap; }
.cite-snippet { margin: 6px 0 0; font-size: 13px; line-height: 1.6; color: #4e5561; }
.cite-date { display: block; margin-top: 6px; font-size: 12px; color: var(--muted); }
.badge { font-size: 11px; padding: 1px 6px; border-radius: 10px; color: #fff; }
.badge.log { background: #4a89dc; }
.badge.note { background: #5ab48a; }
.link-btn { border: none; background: none; color: #4a89dc; font-size: 13px; cursor: pointer; }
</style>

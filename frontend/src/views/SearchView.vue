<script setup lang="ts">
import { nextTick, onMounted, ref } from 'vue'
import { api } from '@/api/client'
import { useDictation } from '@/composables/useDictation'
import VoiceButton from '@/components/VoiceButton.vue'
import type { EntryType, SearchItem } from '@/types'

const inputRef = ref<HTMLInputElement | null>(null)
const q = ref('')
const type = ref<'' | EntryType>('')
const results = ref<SearchItem[]>([])
const searched = ref(false)
const loading = ref(false)
const error = ref('')

// 搜索框用替换：整句口述就是这次要查的内容，追加反而查不到
const qDict = useDictation({ target: q, el: inputRef, mode: 'replace' })

function isImeComposing(e: KeyboardEvent) {
  return e.isComposing || e.keyCode === 229
}

function onKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !isImeComposing(e)) {
    e.preventDefault()
    void run()
  }
}

async function run() {
  const term = q.value.trim()
  if (!term || loading.value) return
  loading.value = true
  error.value = ''
  try {
    results.value = await api.search(term, type.value || undefined)
  } catch (err) {
    error.value = (err as Error).message
    results.value = []
  } finally {
    loading.value = false
    searched.value = true
  }
}

onMounted(() => nextTick(() => inputRef.value?.focus()))
</script>

<template>
  <section>
    <div class="search-bar">
      <input ref="inputRef" v-model="q" class="search-input" placeholder="中文可以是单个字，比如 拼" @keydown="onKeydown" />
      <VoiceButton :d="qDict" />
      <button class="primary" :disabled="loading" @click="run">搜索</button>
    </div>
    <div class="toolbar">
      <label><input type="radio" value="" v-model="type" @change="run" /> 全部</label>
      <label><input type="radio" value="log" v-model="type" @change="run" /> 日志</label>
      <label><input type="radio" value="note" v-model="type" @change="run" /> 笔记</label>
    </div>

    <p v-if="error" class="error">{{ error }}</p>
    <p v-else-if="searched && results.length === 0" class="empty">没有匹配「{{ q }}」的记录。</p>
    <p v-else-if="results.length" class="count">共 {{ results.length }} 条</p>

    <div class="results">
      <RouterLink v-for="item in results" :key="item.id" :to="`/entry/${item.id}`" class="hit">
        <div class="hit-head">
          <span class="hit-title">{{ item.title || '(无标题)' }}</span>
          <span class="badge" :class="item.type">{{ item.type === 'log' ? '日志' : '笔记' }}</span>
          <span v-if="item.titleHit" class="title-hit">标题命中</span>
        </div>
        <p class="hit-snippet">{{ item.snippet }}</p>
        <time class="hit-date">{{ item.sourceDate }}</time>
      </RouterLink>
    </div>
  </section>
</template>

<style scoped>
.search-bar { display: flex; gap: 10px; }
.search-input {
  flex: 1;
  padding: 12px 14px;
  font-size: 15px;
  border: 1px solid var(--line);
  border-radius: 8px;
}
.search-input:focus { outline: none; border-color: #4a89dc; }
.toolbar { display: flex; gap: 18px; margin: 12px 0 6px; font-size: 14px; }
.toolbar label { display: flex; align-items: center; gap: 6px; cursor: pointer; }
.primary {
  padding: 0 22px;
  border: none;
  border-radius: 8px;
  background: #4a89dc;
  color: #fff;
  cursor: pointer;
}
.results { display: flex; flex-direction: column; gap: 10px; margin-top: 12px; }
.hit {
  display: block;
  padding: 12px 14px;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 8px;
  text-decoration: none;
  color: inherit;
}
.hit-head { display: flex; align-items: center; gap: 8px; }
.hit-title { font-weight: 600; font-size: 15px; }
.hit-snippet { margin: 6px 0 0; font-size: 13px; line-height: 1.6; color: #4e5561; }
.hit-date { display: block; margin-top: 6px; font-size: 12px; color: var(--muted); }
.badge { font-size: 11px; padding: 1px 6px; border-radius: 10px; color: #fff; }
.badge.log { background: #4a89dc; }
.badge.note { background: #5ab48a; }
.title-hit { font-size: 11px; color: #5ab48a; }
.count, .empty { font-size: 13px; color: var(--muted); }
.error { font-size: 13px; color: #d0453e; }
</style>

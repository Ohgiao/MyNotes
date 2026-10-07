<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api } from '@/api/client'
import type { ReviewItem } from '@/types'

type Outcome = 'known' | 'vague' | 'forgot'

const router = useRouter()
const queue = ref<ReviewItem[]>([])
const index = ref(0)
const error = ref('')
const busy = ref(false)
const doneCount = ref(0)

const current = computed(() => queue.value[index.value] ?? null)
const remaining = computed(() => Math.max(queue.value.length - index.value, 0))

async function load() {
  error.value = ''
  try {
    queue.value = await api.reviewsToday(50)
    index.value = 0
  } catch (err) {
    error.value = (err as Error).message
  }
}

async function answer(outcome: Outcome) {
  const item = current.value
  if (!item || busy.value) return
  busy.value = true
  try {
    const result = await api.reviewResult(item.id, outcome)
    index.value += 1
    doneCount.value += 1
    if (result.reviewStatus === 'mastered') {
      ElMessage.success('这条已毕业，不再进复习队列')
    }
  } catch (err) {
    ElMessage.error((err as Error).message)
  } finally {
    busy.value = false
  }
}

function openCurrent() {
  if (current.value) void router.push(`/entry/${current.value.id}`)
}

onMounted(load)
</script>

<template>
  <section>
    <h1 class="page-title">复习</h1>
    <p class="hint">只有主题笔记进复习队列。记得→7 天后再见，模糊→3 天，忘了→明天再来。熟练度到 3 就毕业。</p>

    <p v-if="error" class="error">{{ error }}</p>

    <div v-else-if="current" class="card">
      <div class="card-head">
        <span class="card-title">{{ current.title || '(无标题)' }}</span>
        <span class="meta">熟练度 {{ current.mastery }} · 已复习 {{ current.reviewCount }} 次</span>
      </div>
      <p class="card-body">{{ current.preview }}</p>
      <div class="answers">
        <button class="forgot" :disabled="busy" @click="answer('forgot')">忘了</button>
        <button class="vague" :disabled="busy" @click="answer('vague')">模糊</button>
        <button class="known" :disabled="busy" @click="answer('known')">记得</button>
      </div>
      <div class="foot">
        <button class="link-btn" @click="openCurrent">打开原文</button>
        <span class="count">队列剩余 {{ remaining }} 条 · 本轮已复习 {{ doneCount }} 条</span>
      </div>
    </div>

    <div v-else class="empty-state">
      <p v-if="doneCount">{{ doneCount }} 条都过完了，明天见。</p>
      <p v-else>今天没有到期的复习项。</p>
      <button class="link-btn" @click="load">重新检查</button>
    </div>
  </section>
</template>

<style scoped>
.page-title { font-size: 18px; margin: 0 0 6px; }
.hint { font-size: 13px; color: var(--muted); margin: 0 0 18px; }
.error { color: #d0453e; font-size: 14px; }
.card {
  padding: 20px;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 10px;
}
.card-head { display: flex; justify-content: space-between; align-items: baseline; gap: 12px; }
.card-title { font-size: 17px; font-weight: 600; }
.meta { font-size: 12px; color: var(--muted); white-space: nowrap; }
.card-body {
  margin: 14px 0 22px;
  font-size: 15px;
  line-height: 1.9;
  white-space: pre-wrap;
  color: #3c424d;
}
.answers { display: flex; gap: 12px; }
.answers button {
  flex: 1;
  padding: 12px;
  border-radius: 8px;
  border: 1px solid var(--line);
  background: #fff;
  font-size: 15px;
  cursor: pointer;
}
.known { border-color: #b7e0c8 !important; color: #2f855a; }
.vague { border-color: #f0dfba !important; color: #b7791f; }
.forgot { border-color: #f0c0bd !important; color: #c53030; }
.foot { display: flex; justify-content: space-between; align-items: center; margin-top: 16px; }
.count { font-size: 12px; color: var(--muted); }
.link-btn { border: none; background: none; color: #4a89dc; font-size: 13px; cursor: pointer; }
.empty-state { padding: 30px; text-align: center; color: var(--muted); font-size: 14px; }
</style>

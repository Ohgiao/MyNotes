<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api } from '@/api/client'
import EntryCard from '@/components/EntryCard.vue'
import type { EntryType, TimelineDay } from '@/types'

const PAGE_SIZE = 30
const days = ref<TimelineDay[]>([])
const cursor = ref<string | null>(null)
const type = ref<'' | EntryType>('')
const loading = ref(false)
const error = ref('')
const loadedOnce = ref(false)

const WEEKDAYS = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']

function dayLabel(date: string): string {
  const d = new Date(`${date}T00:00:00`)
  return `${date} ${WEEKDAYS[d.getDay()]}`
}

function mergeDays(existing: TimelineDay[], incoming: TimelineDay[]): TimelineDay[] {
  if (existing.length === 0) return incoming.map((d) => ({ date: d.date, entries: [...d.entries] }))
  const merged = existing.map((d) => ({ date: d.date, entries: [...d.entries] }))
  const last = merged[merged.length - 1]
  incoming.forEach((day, index) => {
    if (index === 0 && last && last.date === day.date) {
      last.entries.push(...day.entries)
    } else {
      merged.push({ date: day.date, entries: [...day.entries] })
    }
  })
  return merged
}

async function load() {
  if (loading.value) return
  loading.value = true
  error.value = ''
  try {
    const page = await api.timeline({ type: type.value || undefined, cursor: cursor.value, size: PAGE_SIZE })
    days.value = cursor.value === null ? page.days : mergeDays(days.value, page.days)
    cursor.value = page.nextCursor
  } catch (err) {
    error.value = (err as Error).message
  } finally {
    loading.value = false
    loadedOnce.value = true
  }
}

/**
 * 游标是"上一页最后一条的 (sourceDate, id)"，翻到底时后端返回 null。
 */
async function loadMore() {
  if (!cursor.value) return
  await load()
}

onMounted(load)
</script>

<template>
  <section>
    <div class="toolbar">
      <label><input type="radio" value="" v-model="type" @change="cursor = null; days = []; load()" /> 全部</label>
      <label><input type="radio" value="log" v-model="type" @change="cursor = null; days = []; load()" /> 日志</label>
      <label><input type="radio" value="note" v-model="type" @change="cursor = null; days = []; load()" /> 笔记</label>
    </div>

    <p v-if="error" class="error">{{ error }}</p>

    <div v-for="day in days" :key="day.date" class="day">
      <h2 class="day-label">{{ dayLabel(day.date) }}</h2>
      <div class="day-entries">
        <EntryCard v-for="entry in day.entries" :key="entry.id" :entry="entry" show-type />
      </div>
    </div>

    <p v-if="loadedOnce && days.length === 0 && !error" class="empty">还没有记录，去「记录」页写下第一条。</p>
    <button v-if="cursor" class="more" :disabled="loading" @click="loadMore">载入更早的</button>
  </section>
</template>

<style scoped>
.toolbar { display: flex; gap: 18px; margin-bottom: 18px; font-size: 14px; }
.toolbar label { display: flex; align-items: center; gap: 6px; cursor: pointer; }
.day { margin-bottom: 22px; }
.day-label {
  margin: 0 0 8px;
  font-size: 13px;
  font-weight: 600;
  color: var(--muted);
}
.day-entries { display: flex; flex-direction: column; gap: 10px; }
.empty, .error { font-size: 14px; color: var(--muted); }
.error { color: #d0453e; }
.more {
  width: 100%;
  padding: 10px;
  border: 1px dashed var(--line);
  border-radius: 8px;
  background: #fff;
  color: var(--muted);
  cursor: pointer;
}
</style>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '@/api/client'
import type { EntryListItem } from '@/types'

const items = ref<EntryListItem[]>([])
const error = ref('')

async function load() {
  try {
    items.value = await api.trash()
  } catch (err) {
    error.value = (err as Error).message
  }
}

async function restore(item: EntryListItem) {
  try {
    await api.restoreEntry(item.id)
    items.value = items.value.filter((i) => i.id !== item.id)
    ElMessage.success(`已恢复「${item.title}」`)
  } catch (err) {
    ElMessage.error((err as Error).message)
  }
}

onMounted(load)
</script>

<template>
  <section>
    <h1 class="page-title">回收站</h1>
    <p class="hint">删除是软删除，这里可以恢复。</p>
    <p v-if="error" class="error">{{ error }}</p>
    <p v-else-if="items.length === 0" class="hint">回收站是空的。</p>

    <div v-for="item in items" :key="item.id" class="row">
      <RouterLink :to="`/entry/${item.id}`" class="row-title">{{ item.title || '(无标题)' }}</RouterLink>
      <span class="badge" :class="item.type">{{ item.type === 'log' ? '日志' : '笔记' }}</span>
      <span class="date">{{ item.sourceDate }}</span>
      <button class="restore" @click="restore(item)">恢复</button>
    </div>
  </section>
</template>

<style scoped>
.page-title { font-size: 18px; margin: 0 0 6px; }
.hint { font-size: 13px; color: var(--muted); margin: 0 0 16px; }
.error { color: #d0453e; font-size: 14px; }
.row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 8px;
  margin-bottom: 8px;
}
.row-title { flex: 1; color: inherit; text-decoration: none; font-size: 14px; }
.date { font-size: 12px; color: var(--muted); }
.restore {
  padding: 4px 14px;
  border: 1px solid var(--line);
  border-radius: 6px;
  background: #fff;
  font-size: 13px;
  cursor: pointer;
}
.badge { font-size: 11px; padding: 1px 6px; border-radius: 10px; color: #fff; }
.badge.log { background: #4a89dc; }
.badge.note { background: #5ab48a; }
</style>

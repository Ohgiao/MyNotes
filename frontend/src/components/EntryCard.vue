<script setup lang="ts">
import { computed } from 'vue'
import type { EntryListItem } from '@/types'

const props = defineProps<{
  entry: EntryListItem
  showType?: boolean
  showDate?: boolean
}>()

// 单行记录时标题就是从正文截出来的，再显示一遍纯属重复
const showPreview = computed(
  () => !!props.entry.preview && props.entry.preview !== props.entry.title
)
</script>

<template>
  <RouterLink :to="`/entry/${entry.id}`" class="card">
    <div class="card-head">
      <span class="card-title">{{ entry.title || '(无标题)' }}</span>
      <span v-if="showType" class="badge" :class="entry.type">
        {{ entry.type === 'log' ? '日志' : '笔记' }}
      </span>
      <span v-if="entry.pinned" class="pin">置顶</span>
    </div>
    <p v-if="showPreview" class="card-preview">{{ entry.preview }}</p>
    <time v-if="showDate" class="card-date">{{ entry.sourceDate }}</time>
  </RouterLink>
</template>

<style scoped>
.card {
  display: block;
  padding: 12px 14px;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 8px;
  text-decoration: none;
  color: inherit;
}
.card:hover { border-color: #c6cad1; }
.card-head { display: flex; align-items: center; gap: 8px; }
.card-title { font-weight: 600; font-size: 15px; }
.card-preview {
  margin: 6px 0 0;
  font-size: 13px;
  line-height: 1.6;
  color: #4e5561;
  white-space: pre-wrap;
}
.card-date { display: block; margin-top: 6px; font-size: 12px; color: var(--muted); }
.badge {
  font-size: 11px;
  padding: 1px 6px;
  border-radius: 10px;
  color: #fff;
}
.badge.log { background: #4a89dc; }
.badge.note { background: #5ab48a; }
.pin { font-size: 11px; color: #d4881f; }
</style>

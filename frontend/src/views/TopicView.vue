<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '@/api/client'
import type { TreeNode } from '@/types'

const router = useRouter()
const tree = ref<TreeNode[]>([])
const error = ref('')
const loaded = ref(false)

const STATUS_LABEL: Record<string, string> = {
  learning: '在学',
  mastered: '已掌握',
  none: ''
}

async function load() {
  try {
    tree.value = await api.tree()
  } catch (err) {
    error.value = (err as Error).message
  } finally {
    loaded.value = true
  }
}

function open(node: TreeNode) {
  void router.push(`/entry/${node.id}`)
}

onMounted(load)
</script>

<template>
  <section>
    <h1 class="page-title">主题</h1>
    <p class="hint">主题笔记按父子关系成树；要挂到某个主题下，在笔记详情页的「所属主题」里选。</p>
    <p v-if="error" class="error">{{ error }}</p>
    <p v-else-if="loaded && tree.length === 0" class="hint">还没有主题笔记。</p>

    <el-tree
      v-else
      :data="tree"
      :props="{ label: 'title', children: 'children' }"
      node-key="id"
      :expand-on-click-node="false"
      default-expand-all
      @node-click="open"
    >
      <template #default="{ data }">
        <span class="node">
          <span class="node-title">{{ data.title || '(无标题)' }}</span>
          <span v-if="STATUS_LABEL[data.reviewStatus]" class="node-status" :class="data.reviewStatus">
            {{ STATUS_LABEL[data.reviewStatus] }} {{ data.mastery }}
          </span>
        </span>
      </template>
    </el-tree>
  </section>
</template>

<style scoped>
.page-title { font-size: 18px; margin: 0 0 6px; }
.hint { font-size: 13px; color: var(--muted); margin: 0 0 16px; }
.error { color: #d0453e; font-size: 14px; }
.node { display: flex; align-items: center; gap: 10px; font-size: 14px; }
.node-title { color: var(--ink); }
.node-status { font-size: 11px; color: var(--muted); }
.node-status.mastered { color: #5ab48a; }
</style>

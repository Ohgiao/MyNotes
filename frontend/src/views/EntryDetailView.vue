<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api } from '@/api/client'
import type { EntryDetail, SearchItem, TreeNode } from '@/types'

const props = defineProps<{ id: string | string[] }>()
const router = useRouter()
const route = useRoute()

const entry = ref<EntryDetail | null>(null)
const error = ref('')
const editing = ref(false)
const draftTitle = ref('')
const draftBody = ref('')
const draftTags = ref('')
const draftParent = ref<number | null>(null)
const saving = ref(false)

const noteOptions = ref<{ id: number; label: string }[]>([])
const linkQuery = ref('')
const linkResults = ref<SearchItem[]>([])
const searching = ref(false)

function entryId(): number {
  const raw = Array.isArray(props.id) ? props.id[0] : props.id
  return Number(raw)
}

const isNote = computed(() => entry.value?.type === 'note')

/**
 * 树是嵌套结构，父节点下拉需要摊平成带缩进的选项。
 */
function flatten(nodes: TreeNode[], depth = 0, out: { id: number; label: string }[] = []) {
  nodes.forEach((node) => {
    out.push({ id: node.id, label: `${'　'.repeat(depth)}${node.title || '(无标题)'}` })
    flatten(node.children, depth + 1, out)
  })
  return out
}

async function load() {
  error.value = ''
  try {
    entry.value = await api.getEntry(entryId())
    resetDraft()
  } catch (err) {
    error.value = (err as Error).message
    entry.value = null
  }
}

function resetDraft() {
  if (!entry.value) return
  draftTitle.value = entry.value.title
  draftBody.value = entry.value.contentMd
  draftTags.value = entry.value.tags.join(', ')
  draftParent.value = entry.value.parentId
}

async function save() {
  if (!entry.value || saving.value || !draftBody.value.trim()) return
  saving.value = true
  try {
    const tagNames = draftTags.value
      .split(/[,，]/)
      .map((t) => t.trim())
      .filter(Boolean)
    entry.value = await api.updateEntry(entry.value.id, {
      title: draftTitle.value.trim(),
      contentMd: draftBody.value,
      tagNames,
      parentId: isNote.value ? draftParent.value : undefined
    })
    editing.value = false
    ElMessage.success('已保存')
    await loadNotes()
  } catch (err) {
    ElMessage.error((err as Error).message)
  } finally {
    saving.value = false
  }
}

async function remove() {
  if (!entry.value) return
  try {
    await api.deleteEntry(entry.value.id)
    ElMessage.success('已删除，可在回收站恢复')
    void router.push('/trash')
  } catch (err) {
    ElMessage.error((err as Error).message)
  }
}

async function loadNotes() {
  try {
    noteOptions.value = flatten(await api.tree()).filter((n) => n.id !== entry.value?.id)
  } catch {
    noteOptions.value = []
  }
}

async function searchLinkTargets() {
  const term = linkQuery.value.trim()
  if (!term || !entry.value) return
  searching.value = true
  try {
    linkResults.value = (await api.search(term)).filter((item) => item.id !== entry.value?.id)
  } catch (err) {
    ElMessage.error((err as Error).message)
  } finally {
    searching.value = false
  }
}

async function addLink(target: SearchItem) {
  if (!entry.value) return
  try {
    await api.addLink(entry.value.id, target.id)
    linkResults.value = linkResults.value.filter((item) => item.id !== target.id)
    entry.value = await api.getEntry(entry.value.id)
  } catch (err) {
    ElMessage.error((err as Error).message)
  }
}

async function removeLink(dstId: number) {
  if (!entry.value) return
  try {
    await api.removeLink(entry.value.id, dstId)
    entry.value = await api.getEntry(entry.value.id)
  } catch (err) {
    ElMessage.error((err as Error).message)
  }
}

function isImeComposing(e: KeyboardEvent) {
  return e.isComposing || e.keyCode === 229
}

function onLinkKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !isImeComposing(e)) {
    e.preventDefault()
    void searchLinkTargets()
  }
}

watch(() => props.id, () => {
  void load()
  void loadNotes()
}, { immediate: true })

onMounted(() => {
  // 从搜索结果点进来时带上 ?q= 方便直接建立引用
  const q = route.query.q
  if (typeof q === 'string' && q) {
    linkQuery.value = q
    void searchLinkTargets()
  }
})
</script>

<template>
  <section v-if="entry">
    <div class="head">
      <span class="badge" :class="entry.type">{{ entry.type === 'log' ? '日志' : '笔记' }}</span>
      <h1 class="title">{{ entry.title || '(无标题)' }}</h1>
    </div>
    <p class="meta">
      记录日期 {{ entry.sourceDate }} · 最后更新 {{ new Date(entry.updatedAt).toLocaleString() }}
      <template v-if="entry.type === 'note'">
        · 复习状态 {{ entry.reviewStatus === 'mastered' ? '已掌握' : '在学' }}（熟练度 {{ entry.mastery }}）
      </template>
    </p>

    <div v-if="entry.tags.length" class="tags">
      <span v-for="tag in entry.tags" :key="tag" class="tag">{{ tag }}</span>
    </div>

    <div v-if="!editing" class="body">{{ entry.contentMd }}</div>

    <div v-else class="edit">
      <input v-model="draftTitle" class="field" placeholder="标题" />
      <textarea v-model="draftBody" class="field area" rows="14"></textarea>
      <input v-model="draftTags" class="field" placeholder="标签，逗号分隔" />
      <select v-if="isNote" v-model="draftParent" class="field">
        <option :value="null">不属于任何主题（根节点）</option>
        <option v-for="option in noteOptions" :key="option.id" :value="option.id">
          挂在：{{ option.label }}
        </option>
      </select>
      <div class="actions">
        <button class="primary" :disabled="saving || !draftBody.trim()" @click="save">保存</button>
        <button class="plain" @click="editing = false; resetDraft()">取消</button>
      </div>
    </div>

    <div class="foot">
      <button v-if="!editing" class="plain" @click="editing = true">编辑</button>
      <button v-if="!editing" class="danger" @click="remove">删除</button>
    </div>

    <div class="links">
      <div class="link-col">
        <h2 class="link-title">我引用（{{ entry.relations.outgoing.length }}）</h2>
        <p v-if="entry.relations.outgoing.length === 0" class="link-empty">还没有引用别的条目。</p>
        <div v-for="item in entry.relations.outgoing" :key="item.id" class="link-row">
          <RouterLink :to="`/entry/${item.id}`">{{ item.title || '(无标题)' }}</RouterLink>
          <button class="link-btn" @click="removeLink(item.id)">移除</button>
        </div>
      </div>
      <div class="link-col">
        <h2 class="link-title">引用我（{{ entry.relations.incoming.length }}）</h2>
        <p v-if="entry.relations.incoming.length === 0" class="link-empty">还没有条目引用这里。</p>
        <div v-for="item in entry.relations.incoming" :key="item.id" class="link-row">
          <RouterLink :to="`/entry/${item.id}`">{{ item.title || '(无标题)' }}</RouterLink>
        </div>
      </div>
    </div>

    <div v-if="!editing" class="add-link">
      <input v-model="linkQuery" class="field" placeholder="搜索要引用的条目，回车" @keydown="onLinkKeydown" />
      <div v-if="linkResults.length" class="candidates">
        <div v-for="item in linkResults" :key="item.id" class="candidate">
          <span class="candidate-title">{{ item.title || '(无标题)' }}</span>
          <button class="link-btn" @click="addLink(item)">引用</button>
        </div>
      </div>
      <p v-else-if="searching" class="link-empty">搜索中…</p>
    </div>
  </section>

  <p v-else-if="error" class="error">{{ error }}</p>
</template>

<style scoped>
.head { display: flex; align-items: center; gap: 10px; }
.title { margin: 0; font-size: 20px; }
.meta { margin: 8px 0 12px; font-size: 12px; color: var(--muted); }
.tags { display: flex; gap: 6px; flex-wrap: wrap; margin-bottom: 12px; }
.tag { font-size: 12px; padding: 2px 8px; background: #eef2f7; border-radius: 10px; color: #4e5561; }
.body {
  padding: 16px;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 8px;
  font-size: 15px;
  line-height: 1.8;
  white-space: pre-wrap;
}
.edit { display: flex; flex-direction: column; gap: 10px; }
.field { padding: 10px 12px; border: 1px solid var(--line); border-radius: 8px; font: inherit; background: #fff; }
.area { resize: vertical; line-height: 1.7; }
.actions, .foot { display: flex; gap: 10px; margin-top: 14px; }
.primary, .plain, .danger { padding: 8px 18px; border-radius: 6px; cursor: pointer; font-size: 14px; }
.primary { border: none; background: #4a89dc; color: #fff; }
.plain { border: 1px solid var(--line); background: #fff; color: #4e5561; }
.danger { border: 1px solid #f0c0bd; background: #fff; color: #d0453e; }
.badge { font-size: 11px; padding: 2px 8px; border-radius: 10px; color: #fff; }
.badge.log { background: #4a89dc; }
.badge.note { background: #5ab48a; }
.error { color: #d0453e; font-size: 14px; }
.links { display: flex; gap: 18px; margin-top: 26px; }
.link-col { flex: 1; min-width: 0; }
.link-title { font-size: 13px; color: var(--muted); margin: 0 0 8px; font-weight: 600; }
.link-row {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  padding: 8px 10px;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 6px;
  margin-bottom: 6px;
  font-size: 13px;
}
.link-row a { color: inherit; text-decoration: none; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.link-empty { font-size: 12px; color: var(--muted); }
.link-btn { border: none; background: none; color: #4a89dc; font-size: 12px; cursor: pointer; }
.add-link { margin-top: 18px; }
.candidates { margin-top: 8px; }
.candidate {
  display: flex;
  justify-content: space-between;
  padding: 7px 10px;
  border: 1px dashed var(--line);
  border-radius: 6px;
  margin-bottom: 5px;
  font-size: 13px;
}
.candidate-title { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
</style>

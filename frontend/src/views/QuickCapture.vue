<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '@/api/client'
import type { EntryType } from '@/types'

const quickRef = ref<HTMLInputElement | null>(null)
const quick = ref('')
const type = ref<EntryType>('log')
const expanded = ref(false)
const title = ref('')
const body = ref('')
const saving = ref(false)
const lastSavedId = ref<number | null>(null)
const lastSavedTitle = ref('')

function focus() {
  nextTick(() => quickRef.value?.focus())
}

function onHotkey(e: KeyboardEvent) {
  if (e.ctrlKey && e.shiftKey && e.code === 'Space') {
    e.preventDefault()
    focus()
  }
}

/**
 * 输入法组合期间的 Enter 只是把候选词上屏，不是提交，必须放行。
 */
function isImeComposing(e: KeyboardEvent) {
  return e.isComposing || e.keyCode === 229
}

function onQuickKeydown(e: KeyboardEvent) {
  if (e.key !== 'Enter' || e.shiftKey || isImeComposing(e)) return
  e.preventDefault()
  void submitQuick()
}

function onBodyKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && (e.ctrlKey || e.metaKey) && !isImeComposing(e)) {
    e.preventDefault()
    void submitExpanded()
  }
}

async function submitQuick() {
  const text = quick.value.trim()
  if (!text || saving.value) return
  await save({ contentMd: text })
  quick.value = ''
  focus()
}

async function submitExpanded() {
  const text = body.value.trim()
  if (!text || saving.value) return
  await save({ title: title.value.trim() || undefined, contentMd: text })
  body.value = ''
  title.value = ''
  expanded.value = false
  focus()
}

async function save(payload: { title?: string; contentMd: string }) {
  saving.value = true
  try {
    const created = await api.createEntry({ type: type.value, ...payload })
    lastSavedId.value = created.id
    lastSavedTitle.value = created.title
  } catch (err) {
    ElMessage.error((err as Error).message)
  } finally {
    saving.value = false
  }
}

async function undoLast() {
  if (lastSavedId.value === null) return
  try {
    await api.deleteEntry(lastSavedId.value)
    lastSavedId.value = null
    lastSavedTitle.value = ''
    ElMessage.success('已撤销')
  } catch (err) {
    ElMessage.error((err as Error).message)
  }
}

onMounted(() => {
  window.addEventListener('keydown', onHotkey)
  focus()
})
onBeforeUnmount(() => window.removeEventListener('keydown', onHotkey))
</script>

<template>
  <section>
    <div class="type-switch">
      <label><input type="radio" value="log" v-model="type" @change="focus" /> 今天学了什么</label>
      <label><input type="radio" value="note" v-model="type" @change="focus" /> 某个知识点的理解</label>
    </div>

    <input
      ref="quickRef"
      v-model="quick"
      class="quick-input"
      :placeholder="type === 'log' ? '记下今天学到的，回车即保存' : '写下这个知识点现在的理解，回车即保存'"
      @keydown="onQuickKeydown"
    />

    <div class="row">
      <button class="link-btn" @click="expanded = !expanded">
        {{ expanded ? '收起长文' : '写长一点' }}
      </button>
      <span class="hint">回车保存 · Shift+Enter 换行 · Ctrl+Shift+Space 回到输入框</span>
    </div>

    <div v-if="expanded" class="expanded">
      <input v-model="title" class="title-input" placeholder="标题（留空则取正文首行）" />
      <textarea
        v-model="body"
        class="body-input"
        rows="10"
        placeholder="支持 Markdown，Ctrl+Enter 保存"
        @keydown="onBodyKeydown"
      ></textarea>
      <button class="primary" :disabled="saving || !body.trim()" @click="submitExpanded">保存</button>
    </div>

    <p v-if="lastSavedId !== null" class="saved">
      已记下「{{ lastSavedTitle }}」
      <RouterLink :to="`/entry/${lastSavedId}`">查看</RouterLink>
      <button class="link-btn" @click="undoLast">撤销</button>
    </p>
  </section>
</template>

<style scoped>
.type-switch { display: flex; gap: 20px; margin-bottom: 12px; font-size: 14px; }
.type-switch label { display: flex; align-items: center; gap: 6px; cursor: pointer; }
.quick-input {
  width: 100%;
  padding: 14px 16px;
  font-size: 16px;
  border: 1px solid var(--line);
  border-radius: 10px;
  background: #fff;
}
.quick-input:focus { outline: none; border-color: #4a89dc; }
.row { display: flex; justify-content: space-between; align-items: center; margin-top: 10px; }
.hint { font-size: 12px; color: var(--muted); }
.link-btn {
  border: none;
  background: none;
  color: #4a89dc;
  font-size: 13px;
  cursor: pointer;
  padding: 0;
}
.expanded { display: flex; flex-direction: column; gap: 10px; margin-top: 14px; }
.title-input, .body-input {
  padding: 10px 12px;
  border: 1px solid var(--line);
  border-radius: 8px;
  font-size: 14px;
  font-family: inherit;
}
.body-input { resize: vertical; }
.primary {
  align-self: flex-start;
  padding: 8px 20px;
  border: none;
  border-radius: 6px;
  background: #4a89dc;
  color: #fff;
  cursor: pointer;
}
.primary:disabled { opacity: 0.5; cursor: not-allowed; }
.saved { margin-top: 18px; font-size: 13px; color: #4e5561; display: flex; gap: 12px; align-items: center; }
</style>

<script setup lang="ts">
import type { Dictation } from '@/composables/useDictation'

defineProps<{ d: Dictation }>()
</script>

<template>
  <span class="voice">
    <!-- mousedown.prevent：点按钮不能把焦点从输入框抢走，否则连续录入就断了 -->
    <button
      type="button"
      class="mic"
      :class="d.state"
      :disabled="!d.supported || d.state === 'stopping'"
      :title="d.hint"
      :aria-label="`语音输入：${d.hint}`"
      @mousedown.prevent
      @click="d.toggle()"
    >
      <svg viewBox="0 0 24 24" width="15" height="15" aria-hidden="true">
        <path fill="currentColor" d="M12 15a3 3 0 0 0 3-3V6a3 3 0 0 0-6 0v6a3 3 0 0 0 3 3z" />
        <path
          fill="currentColor"
          d="M17 11a5 5 0 0 1-10 0H5a7 7 0 0 0 6 6.94V21h2v-3.06A7 7 0 0 0 19 11h-2z"
        />
      </svg>
    </button>
    <span v-if="d.preview" class="preview">{{ d.preview }}</span>
    <span v-else-if="d.errorText" class="error">{{ d.errorText }}</span>
    <span v-else-if="d.state === 'listening' || d.state === 'requesting'" class="hint">{{ d.hint }}</span>
  </span>
</template>

<style scoped>
.voice { display: inline-flex; align-items: center; gap: 8px; }
.mic {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  padding: 0;
  border: 1px solid var(--line);
  border-radius: 50%;
  background: #fff;
  color: #6b7280;
  cursor: pointer;
}
.mic:hover:not(:disabled) { border-color: #b9c0ca; color: #3c424d; }
.mic:disabled { cursor: not-allowed; opacity: 0.5; }
.mic.requesting { color: #b7791f; border-color: #f0dfba; }
.mic.listening {
  color: #fff;
  background: #d0453e;
  border-color: #d0453e;
  animation: pulse 1.2s ease-in-out infinite;
}
@keyframes pulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(208, 69, 62, 0.35); }
  50% { box-shadow: 0 0 0 6px rgba(208, 69, 62, 0); }
}
.preview, .hint { font-size: 12px; color: var(--muted); }
.error { font-size: 12px; color: #d0453e; }
</style>

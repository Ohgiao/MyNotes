import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import type { Ref } from 'vue'
import { acceptFinal, applyFinal } from '../dictation/merge'
import type { FinalGuard, InsertMode } from '../dictation/merge'
import { initialState, nextState } from '../dictation/machine'
import type { DictationEvent, DictationState } from '../dictation/machine'
import type { SpeechErrorEvent, SpeechRecognizer, SpeechResultEvent } from '../dictation/types'

export interface DictationOptions {
  /** 唯一写入目标；听写不引入第二份文本状态 */
  target: Ref<string>
  /** 用于结束后归还焦点，以及监听输入法组合事件 */
  el?: Ref<HTMLElement | null>
  mode?: InsertMode
  lang?: string
  /** 测试注入口，默认真实构造函数（DEV 下可被 window.__dictationFactory 覆盖） */
  factory?: () => SpeechRecognizer
}

export interface Dictation {
  supported: boolean
  state: DictationState
  hint: string
  errorText: string
  preview: string
  toggle(): void
  stop(): void
  cancel(): void
}

export const STATE_HINT: Record<DictationState, string> = {
  unsupported: '当前浏览器不支持语音识别，请用 Edge 打开',
  idle: '开始听写',
  requesting: '等待麦克风授权…',
  listening: '听写中，再点一下结束',
  stopping: '收尾中…'
}

const ERROR_TEXT: Record<string, string> = {
  notAllowed: '权限被拒，去站点设置里允许麦克风',
  'not-allowed': '权限被拒，去站点设置里允许麦克风',
  serviceNotAllowed: '浏览器拒绝了语音服务，检查 Edge 的语音权限',
  'service-not-allowed': '浏览器拒绝了语音服务，检查 Edge 的语音权限',
  network: 'Edge 语音走微软云端，需要联网',
  noSpeech: '没听到声音，再点一次',
  'no-speech': '没听到声音，再点一次',
  audioCapture: '未找到麦克风',
  'audio-capture': '未找到麦克风',
  aborted: '',
  error: '识别失败，再点一次试试'
}

const PERMISSION_HINT_DELAY = 8000
const STUCK_RECOVERY_DELAY = 500

export function useDictation(options: DictationOptions): Dictation {
  const mode: InsertMode = options.mode ?? 'append'
  const lang = options.lang ?? 'zh-CN'

  const ctor = window.SpeechRecognition ?? window.webkitSpeechRecognition
  const devFactory = import.meta.env.DEV ? window.__dictationFactory : undefined
  const factory = options.factory ?? devFactory ?? (ctor ? () => new ctor() : undefined)
  const supported = factory !== undefined

  const state = ref<DictationState>(initialState(supported))
  const errorText = ref('')
  const preview = ref('')

  let recognizer: SpeechRecognizer | null = null
  let guard: FinalGuard = { lastText: '', lastAtMs: 0 }
  let composing = false
  let permissionTimer: ReturnType<typeof setTimeout> | undefined
  const queued: string[] = []

  function send(event: DictationEvent) {
    state.value = nextState(state.value, event)
  }

  function write(text: string) {
    options.target.value = applyFinal(options.target.value, text, mode)
  }

  function flushQueued() {
    while (queued.length) {
      write(queued.shift() as string)
    }
  }

  function commit(final: string) {
    const next = acceptFinal(guard, final, Date.now())
    if (!next) {
      return
    }
    guard = next
    if (composing) {
      queued.push(final.trim())
      return
    }
    flushQueued()
    write(final)
  }

  function handleResult(event: SpeechResultEvent) {
    // 有些实现不触发 onstart，用第一个结果兜底
    send({ type: 'started' })
    let interim = ''
    for (let index = event.resultIndex; index < event.results.length; index++) {
      const result = event.results[index]
      const text = result[0]?.transcript ?? ''
      if (result.isFinal) {
        commit(text)
      } else {
        interim += text
      }
    }
    preview.value = interim
  }

  function clearTimers() {
    if (permissionTimer) {
      clearTimeout(permissionTimer)
      permissionTimer = undefined
    }
  }

  function begin() {
    if (!factory || recognizer) {
      return
    }
    errorText.value = ''
    preview.value = ''
    guard = { lastText: '', lastAtMs: 0 }
    queued.length = 0

    const instance = factory()
    instance.lang = lang
    instance.continuous = false
    instance.interimResults = true
    instance.maxAlternatives = 1
    // 每个回调都先确认"还是当前这个识别器"：cancel/error 之后晚到的事件不能再改状态或写文本
    instance.onstart = () => {
      if (recognizer === instance) {
        send({ type: 'started' })
      }
    }
    instance.onresult = (event: SpeechResultEvent) => {
      if (recognizer === instance) {
        handleResult(event)
      }
    }
    instance.onerror = (event: SpeechErrorEvent) => {
      if (recognizer !== instance) {
        return
      }
      errorText.value = ERROR_TEXT[event.error] ?? ERROR_TEXT.error
      // 规范上 error 之后一定跟着 end，但不能把 UI 卡在 listening 上赌它触发；
      // 更要紧的是必须释放 recognizer，否则下一次点按钮会被 begin() 的"已在进行中"挡掉。
      recognizer = null
      clearTimers()
      send({ type: 'error' })
    }
    instance.onend = () => {
      if (recognizer !== instance) {
        return
      }
      recognizer = null
      preview.value = ''
      clearTimers()
      send({ type: 'ended' })
      options.el?.value?.focus()
    }
    recognizer = instance

    send({ type: 'begin' })
    try {
      instance.start()
    } catch {
      recognizer = null
      send({ type: 'ended' })
      errorText.value = '没能启动识别，再点一次试试'
      return
    }
    permissionTimer = setTimeout(() => {
      if (state.value === 'requesting') {
        errorText.value = '还在等授权，看一下地址栏的麦克风权限提示'
      }
    }, PERMISSION_HINT_DELAY)
  }

  /** 优雅收尾：还在途的定稿仍然落框 */
  function stop() {
    if (!recognizer) {
      if (state.value !== 'idle') {
        send({ type: 'ended' })
      }
      return
    }
    send({ type: 'manual-stop' })
    recognizer.stop()
  }

  /** 丢弃在途结果：调用方马上要清空输入框时用，否则晚到的定稿会回填回去 */
  function cancel() {
    queued.length = 0
    preview.value = ''
    if (!recognizer) {
      if (state.value !== 'idle') {
        send({ type: 'ended' })
      }
      return
    }
    const target = recognizer
    recognizer = null
    send({ type: 'cancel' })
    target.abort()
    send({ type: 'ended' })
  }

  function toggle() {
    if (!supported) {
      return
    }
    if (state.value === 'idle') {
      begin()
    } else if (state.value === 'listening') {
      stop()
    } else if (state.value === 'requesting') {
      cancel()
    }
  }

  function onCompositionStart() {
    composing = true
  }

  function onCompositionEnd() {
    composing = false
    flushQueued()
  }

  let bound: HTMLElement | null = null
  watch(
    () => options.el?.value,
    (element) => {
      if (bound === element) {
        return
      }
      if (bound) {
        bound.removeEventListener('compositionstart', onCompositionStart)
        bound.removeEventListener('compositionend', onCompositionEnd)
      }
      bound = element ?? null
      if (bound) {
        bound.addEventListener('compositionstart', onCompositionStart)
        bound.addEventListener('compositionend', onCompositionEnd)
      }
    },
    { immediate: true }
  )

  onBeforeUnmount(() => {
    if (bound) {
      bound.removeEventListener('compositionstart', onCompositionStart)
      bound.removeEventListener('compositionend', onCompositionEnd)
      bound = null
    }
    if (permissionTimer) {
      clearTimeout(permissionTimer)
    }
    recognizer?.abort()
    recognizer = null
  })

  return reactive({
    supported,
    state,
    hint: computed(() => STATE_HINT[state.value]),
    errorText,
    preview,
    toggle,
    stop,
    cancel
  }) as Dictation
}

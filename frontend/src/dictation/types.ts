export interface SpeechAlternative {
  transcript: string
  confidence: number
}

export interface SpeechRecognitionResult {
  isFinal: boolean
  length: number
  [index: number]: SpeechAlternative
}

export interface SpeechResultList {
  length: number
  [index: number]: SpeechRecognitionResult
}

export interface SpeechResultEvent extends Event {
  resultIndex: number
  results: SpeechResultList
}

export interface SpeechErrorEvent extends Event {
  error: string
  message: string
}

/**
 * Web Speech API 里我们真正用到的子集。Edge 上构造函数是 webkitSpeechRecognition，
 * 且 TS 的 lib.dom 目前没有这些类型，所以自己声明而不引 @types 包——
 * 那个包会全局同名声明，等 TS 补进 DOM lib 就会两份定义打架。
 */
export interface SpeechRecognizer {
  lang: string
  continuous: boolean
  interimResults: boolean
  maxAlternatives: number
  start(): void
  stop(): void
  abort(): void
  onstart: (() => void) | null
  onresult: ((event: SpeechResultEvent) => void) | null
  onerror: ((event: SpeechErrorEvent) => void) | null
  onend: (() => void) | null
}

export interface SpeechRecognizerConstructor {
  new (): SpeechRecognizer
}

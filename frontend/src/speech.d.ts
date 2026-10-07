import type { SpeechRecognizer, SpeechRecognizerConstructor } from './dictation/types'

declare global {
  interface Window {
    SpeechRecognition?: SpeechRecognizerConstructor
    webkitSpeechRecognition?: SpeechRecognizerConstructor

    /**
     * 仅 DEV 构建生效的测试后门：注入一个假识别器，好在没麦克风时把 UI 各状态走一遍。
     */
    __dictationFactory?: () => SpeechRecognizer
  }
}

export {}

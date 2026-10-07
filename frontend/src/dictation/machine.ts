export type DictationState = 'unsupported' | 'idle' | 'requesting' | 'listening' | 'stopping'

export type DictationEvent =
  | { type: 'begin' }
  | { type: 'started' }
  | { type: 'manual-stop' }
  | { type: 'cancel' }
  | { type: 'ended' }
  | { type: 'error' }

export function initialState(supported: boolean): DictationState {
  return supported ? 'idle' : 'unsupported'
}

/**
 * 纯状态机，不碰 vue、不碰 window，方便逐条断言转移。
 * 非法转移一律保持原状态，所以重复事件（例如连点两次）不会把状态打乱。
 */
export function nextState(current: DictationState, event: DictationEvent): DictationState {
  if (current === 'unsupported') {
    return 'unsupported'
  }
  switch (event.type) {
    case 'begin':
      return current === 'idle' ? 'requesting' : current
    case 'started':
      return current === 'requesting' || current === 'idle' ? 'listening' : current
    case 'manual-stop':
      return current === 'listening' ? 'stopping' : current
    case 'cancel':
      return current === 'listening' || current === 'requesting' ? 'stopping' : current
    case 'ended':
    case 'error':
      return 'idle'
    default:
      return current
  }
}

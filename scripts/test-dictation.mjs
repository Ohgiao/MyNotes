// 语音输入的纯逻辑验证：不依赖浏览器、不碰麦克风。
// Node 24 可直接 import .ts（type stripping），所以这里测的是 frontend 里那两个纯模块本身。
import { acceptFinal, applyFinal, applyFinals } from '../frontend/src/dictation/merge.ts'
import { initialState, nextState } from '../frontend/src/dictation/machine.ts'

const rows = []
let failed = 0

function check(name, actual, expected) {
  const ok = JSON.stringify(actual) === JSON.stringify(expected)
  if (!ok) failed++
  rows.push(`${ok ? 'PASS' : 'FAIL'}  ${name}${ok ? '' : `  → 期望 ${JSON.stringify(expected)}，实得 ${JSON.stringify(actual)}`}`)
}

// ---- 定稿并入规则 ----
check('中文直接连写不补空格', applyFinal('今天学了', '连接池', 'append'), '今天学了连接池')
check('两边都是西文才补空格', applyFinal('use', 'MyBatis', 'append'), 'use MyBatis')
check('中文结尾接西文不补空格', applyFinal('第1章', 'MyBatis', 'append'), '第1章MyBatis')
check('西文结尾接中文不补空格', applyFinal('pool2', '参数', 'append'), 'pool2参数')
check('空框时直接落文本', applyFinal('', '  连接池  ', 'append'), '连接池')
check('空白定稿不改原文', applyFinal('原有内容', '   ', 'append'), '原有内容')
check('替换模式整体覆盖', applyFinal('旧关键词', '新关键词', 'replace'), '新关键词')
check('替换模式剥首尾空白', applyFinal('旧', '  新  ', 'replace'), '新')
check('多段定稿按顺序累积', applyFinals('今天', ['学了', '连接池', '默认 10'], 'append'), '今天学了连接池默认 10')
check('空数组保持原样', applyFinals('今天', [], 'append'), '今天')

// ---- 重复投递保护 ----
const t0 = 1_000_000
const first = { lastText: '', lastAtMs: 0 }
const guarded = acceptFinal(first, '连接池', t0)
check('首次投递被接受', guarded !== null && guarded.lastText === '连接池', true)
check('500ms 内同一串被丢弃', acceptFinal(guarded, '连接池', t0 + 400), null)
check('超过 500ms 的同一串被接受', acceptFinal(guarded, '连接池', t0 + 600)?.lastAtMs, t0 + 600)
check('不同文本不受去重影响', acceptFinal(guarded, '事务', t0 + 100)?.lastText, '事务')
check('空白文本原样返回 guard', acceptFinal(guarded, '  ', t0 + 100) === guarded, true)

// ---- 状态机 ----
check('支持时初始为 idle', initialState(true), 'idle')
check('不支持时初始为 unsupported', initialState(false), 'unsupported')
check('idle 点按钮 → requesting', nextState('idle', { type: 'begin' }), 'requesting')
check('requesting 收到 started → listening', nextState('requesting', { type: 'started' }), 'listening')
check('无 onstart 实现：requesting 收到首个结果也能进 listening',
  nextState(nextState('idle', { type: 'begin' }), { type: 'started' }), 'listening')
check('listening 点按钮 → stopping', nextState('listening', { type: 'manual-stop' }), 'stopping')
check('stopping 收到 ended → idle', nextState('stopping', { type: 'ended' }), 'idle')
check('listening 被 cancel → stopping', nextState('listening', { type: 'cancel' }), 'stopping')
check('requesting 被 cancel → stopping', nextState('requesting', { type: 'cancel' }), 'stopping')
check('idle 收到 ended 仍是 idle', nextState('idle', { type: 'ended' }), 'idle')
check('任何态出错都回 idle', ['requesting', 'listening', 'stopping'].map((s) => nextState(s, { type: 'error' })),
  ['idle', 'idle', 'idle'])
check('unsupported 是终态', ['begin', 'started', 'ended', 'error'].map((type) => nextState('unsupported', { type })),
  ['unsupported', 'unsupported', 'unsupported', 'unsupported'])
check('连点两次 begin 不会跳过 requesting', nextState('requesting', { type: 'begin' }), 'requesting')
check('listening 时重复 begin 无效', nextState('listening', { type: 'begin' }), 'listening')
check('idle 时 manual-stop 无效', nextState('idle', { type: 'manual-stop' }), 'idle')

console.log(rows.join('\n'))
console.log(`\n${failed === 0 ? 'ALL PASS' : failed + ' FAILED'}  (${rows.length} 项)`)
process.exit(failed === 0 ? 0 : 1)

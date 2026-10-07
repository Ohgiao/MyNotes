// 需要后端带 --mynotes.ai.* 指向 scripts/fake-llm.mjs 启动，见 README「不花钱验证 RAG 链路」
const BASE = process.env.MYNOTES_BASE ?? 'http://localhost:8080'
const STAMP = `m3-${Date.now()}`

async function req(method, path, body) {
  const res = await fetch(BASE + path, {
    method,
    headers: { 'Content-Type': 'application/json' },
    body: body === undefined ? undefined : JSON.stringify(body)
  })
  const text = await res.text()
  let json = null
  try {
    json = JSON.parse(text)
  } catch {
    /* 忽略非 JSON */
  }
  return { status: res.status, json, text }
}

const rows = []
let failed = 0

function check(name, ok, detail = '') {
  if (!ok) failed++
  rows.push(`${ok ? 'PASS' : 'FAIL'}  ${name}${detail ? `  → ${detail}` : ''}`)
}

const topic = `${STAMP} 事务隔离级别`
await req('POST', '/api/entries', {
  type: 'note',
  title: topic,
  contentMd: '读完了事务隔离级别：读未提交会脏读，可重复读能挡住不可重复读，但幻读要靠快照读加间隙锁。'
})
await req('POST', '/api/entries', {
  type: 'log',
  contentMd: `${STAMP} 顺手记了连接池超时和事务超时的区别，前者是等连接、后者是等锁`
})

const status = await req('GET', '/api/ai/status')
check('状态显示已配置模型', status.json?.configured === true, JSON.stringify(status.json))
check('状态为 rag 模式', status.json?.mode === 'rag', String(status.json?.mode))
check('有待索引条目', status.json?.pending > 0, `pending=${status.json?.pending}`)

const reindex = await req('POST', '/api/ai/reindex?limit=200')
check('重建索引写入向量', reindex.status === 200 && reindex.json?.embedded > 0,
  JSON.stringify(reindex.json))

const afterReindex = await req('GET', '/api/ai/status')
check('重建后无待索引条目', afterReindex.json?.pending === 0, `pending=${afterReindex.json?.pending}`)
check('已索引条数与总数一致', afterReindex.json?.embedded > 0, `embedded=${afterReindex.json?.embedded}`)

const again = await req('POST', '/api/ai/reindex?limit=200')
check('内容未变时不重复调用模型', again.json?.embedded === 0, JSON.stringify(again.json))

const ask = await req('POST', '/api/ai/ask', { question: '幻读要怎么处理', topK: 5 })
check('问答返回 200', ask.status === 200, `${ask.status} ${ask.text}`)
check('走的是生成式回答', (ask.json?.answer ?? '').includes('【fake-model】'), ask.json?.answer)
check('返回了引用列表', (ask.json?.citations ?? []).length > 0, `引用 ${ask.json?.citations?.length} 条`)
check('引用带可跳转的 entryId', ask.json?.citations?.every((c) => typeof c.entryId === 'number'))
const vectorHit = (ask.json?.citations ?? []).find((c) => c.matchedBy !== 'keyword')
check('至少一条来自语义召回（不只是关键词）', !!vectorHit, vectorHit?.matchedBy)
const topicCited = (ask.json?.citations ?? []).some((c) => (c.title ?? '').includes('事务隔离级别'))
check('问幻读时召回了事务隔离级别那条笔记', topicCited,
  (ask.json?.citations ?? []).map((c) => c.title).join(' | '))
check('引用编号从 1 连续递增',
  ask.json?.citations?.every((c, i) => c.index === i + 1), JSON.stringify(ask.json?.citations?.map((c) => c.index)))

const emptyAsk = await req('POST', '/api/ai/ask', { question: '' })
check('空问题被拒 400', emptyAsk.status === 400, emptyAsk.json?.error ?? String(emptyAsk.status))

const updated = await req('PUT', `/api/entries/${ask.json.citations[0].entryId}`, {
  contentMd: '内容改动了，向量需要重算'
})
const afterEdit = await req('GET', '/api/ai/status')
check('改内容后重新变为待索引（hash 生效）', updated.status === 200 && afterEdit.json?.pending > 0,
  `pending=${afterEdit.json?.pending}`)

console.log(rows.join('\n'))
console.log(`\n${failed === 0 ? 'ALL PASS' : failed + ' FAILED'}  (${rows.length} 项)`)
console.log(`提示：本次数据带 ${STAMP} 前缀。`)
process.exit(failed === 0 ? 0 : 1)

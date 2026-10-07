const BASE = process.env.MYNOTES_BASE ?? 'http://localhost:8080'

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
    /* 非 JSON 响应保留原文 */
  }
  return { status: res.status, json, text }
}

const rows = []
let failed = 0

function check(name, ok, detail = '') {
  if (!ok) failed++
  rows.push(`${ok ? 'PASS' : 'FAIL'}  ${name}${detail ? `  → ${detail}` : ''}`)
}

const created = await req('POST', '/api/entries', {
  type: 'log',
  contentMd: '今天研究了 MyBatis 的动态 SQL，用 <if> 标签拼 where 条件'
})
check('POST /api/entries 返回 201', created.status === 201, `${created.status} ${created.text}`)
const logId = created.json?.id
check('返回自增 id', typeof logId === 'number', String(logId))
check('标题留空时从正文首行补齐', (created.json?.title ?? '').includes('MyBatis'), created.json?.title)
check('日志不进入复习队列', created.json?.reviewStatus === 'none', created.json?.reviewStatus)

const note = await req('POST', '/api/entries', {
  type: 'note',
  title: 'MyBatis 映射机制',
  contentMd: 'resultMap 与驼峰自动映射的取舍，暂时理解到一半'
})
const noteId = note.json?.id
check('笔记创建后进入学习状态', note.json?.reviewStatus === 'learning', note.json?.reviewStatus)
check('笔记排出一天后复习', !!note.json?.nextReviewAt, String(note.json?.nextReviewAt))

const twoChar = await req('GET', '/api/search?q=' + encodeURIComponent('动态'))
check('双字中文命中', twoChar.json?.length === 1, `命中 ${twoChar.json?.length ?? twoChar.text}`)

const oneChar = await req('GET', '/api/search?q=' + encodeURIComponent('拼'))
check('单字中文命中（ngram 方案会失效的场景）', oneChar.json?.length === 1, `命中 ${oneChar.json?.length}`)
check('搜索返回命中片段', (oneChar.json?.[0]?.snippet ?? '').includes('拼'), oneChar.json?.[0]?.snippet)

const titleHit = await req('GET', '/api/search?q=' + encodeURIComponent('映射'))
check('标题命中被标记并排前', titleHit.json?.[0]?.titleHit === true, titleHit.json?.[0]?.title)

const wildcard = await req('GET', '/api/search?q=' + encodeURIComponent('%'))
check('用户输入的 % 当字面量而不是通配符', wildcard.json?.length === 0, `命中 ${wildcard.json?.length}`)

const miss = await req('GET', '/api/search?q=' + encodeURIComponent('不存在的词'))
check('无匹配返回空数组', miss.status === 200 && miss.json?.length === 0, miss.text)

const updated = await req('PUT', `/api/entries/${logId}`, {
  title: '',
  contentMd: '改成：动态 SQL 的 foreach 用于批量插入'
})
check('PUT 更新生效', updated.json?.contentMd?.includes('foreach') === true, updated.json?.title)
check('更新后标题重新从正文推导', (updated.json?.title ?? '').includes('foreach'), updated.json?.title)
check('updated_at 发生变化', updated.json?.updatedAt !== created.json?.updatedAt, updated.json?.updatedAt)

const goneAfterEdit = await req('GET', '/api/search?q=' + encodeURIComponent('where'))
check('旧内容不再被搜到（content_plain 已重抽）', goneAfterEdit.json?.length === 0, `命中 ${goneAfterEdit.json?.length}`)

const timeline = await req('GET', '/api/timeline?size=10')
check('时间线返回 200', timeline.status === 200, `${timeline.status} ${timeline.text}`)
const dayCount = timeline.json?.days?.length ?? 0
const entryCount = (timeline.json?.days ?? []).reduce((n, d) => n + d.entries.length, 0)
check('时间线按天分组且含两条新记录', dayCount >= 1 && entryCount === 2, `${dayCount} 天 / ${entryCount} 条`)
check('列表项带 preview 摘要', !!timeline.json?.days?.[0]?.entries?.[0]?.preview, timeline.json?.days?.[0]?.entries?.[0]?.preview)

const filtered = await req('GET', '/api/timeline?type=log')
check('type=log 只返回日志', (filtered.json?.days ?? []).flatMap((d) => d.entries).every((e) => e.type === 'log'))

const badType = await req('POST', '/api/entries', { type: 'memo', contentMd: 'x' })
check('非法 type 被拒 400', badType.status === 400, `${badType.status} ${badType.json?.errors?.[0]?.message ?? badType.text}`)

const emptyBody = await req('POST', '/api/entries', { type: 'log', contentMd: '  ' })
check('空内容被拒 400', emptyBody.status === 400, emptyBody.json?.errors?.[0]?.message ?? emptyBody.text)

const missing = await req('GET', '/api/entries/99999999')
check('不存在的 id 返回 404', missing.status === 404, `${missing.status} ${missing.json?.error}`)

const deleted = await req('DELETE', `/api/entries/${logId}`)
check('DELETE 返回 204', deleted.status === 204, String(deleted.status))
const afterDelete = await req('GET', '/api/search?q=' + encodeURIComponent('foreach'))
check('软删后搜索不再命中', afterDelete.json?.length === 0, `命中 ${afterDelete.json?.length}`)
const getDeleted = await req('GET', `/api/entries/${logId}`)
check('软删后详情 404', getDeleted.status === 404, String(getDeleted.status))

const restored = await req('POST', `/api/entries/${logId}/restore`)
check('restore 返回 204', restored.status === 204, String(restored.status))
const afterRestore = await req('GET', '/api/search?q=' + encodeURIComponent('foreach'))
check('恢复后重新可搜', afterRestore.json?.length === 1, `命中 ${afterRestore.json?.length}`)

const selfParent = await req('PUT', `/api/entries/${noteId}`, { contentMd: '自我引用测试', parentId: noteId })
check('父节点指向自己被拒 400', selfParent.status === 400, selfParent.json?.error ?? String(selfParent.status))

const logParent = await req('PUT', `/api/entries/${logId}`, { contentMd: '日志挂树测试', parentId: noteId })
check('日志不允许挂主题树 400', logParent.status === 400, logParent.json?.error ?? String(logParent.status))

console.log(rows.join('\n'))
console.log(`\n${failed === 0 ? 'ALL PASS' : failed + ' FAILED'}  (${rows.length} 项)`)
process.exit(failed === 0 ? 0 : 1)

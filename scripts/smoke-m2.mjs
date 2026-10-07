const BASE = process.env.MYNOTES_BASE ?? 'http://localhost:8080'
const STAMP = `m2-${Date.now()}`

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

function daysFromNow(iso) {
  return (new Date(iso).getTime() - Date.now()) / 86_400_000
}

const logs = []
for (let i = 0; i < 3; i++) {
  const created = await req('POST', '/api/entries', {
    type: 'log',
    contentMd: `${STAMP} 日志 ${i}：关于连接池与事务的第 ${i} 条观察`
  })
  logs.push(created.json)
}
check('建 3 条日志', logs.every((l) => l?.id), logs.map((l) => l?.id).join(','))

const root = await req('POST', '/api/entries', {
  type: 'note',
  title: `${STAMP} 数据库连接池`,
  contentMd: '池大小的确定依据，标签：连接池, 性能',
  tagNames: ['连接池', '性能']
})
const rootTags = (root.json?.tags ?? []).slice().sort().join(',')
check('笔记返回标签', rootTags === '性能,连接池', rootTags)

const child = await req('POST', '/api/entries', {
  type: 'note',
  title: `${STAMP} HikariCP 参数`,
  contentMd: 'maximum-pool-size 默认 10',
  parentId: root.json?.id,
  tagNames: ['连接池']
})
check('子笔记建立父子关系', child.json?.parentId === root.json?.id, String(child.json?.parentId))

const links = []
for (const log of logs) {
  links.push(await req('POST', `/api/entries/${child.json.id}/links`, { dstId: log.id }))
}
check('建立 3 条引用', links.every((r) => r.status === 204), links.map((r) => r.status).join(','))

const dupLink = await req('POST', `/api/entries/${child.json.id}/links`, { dstId: logs[0].id })
check('重复引用不报错且不产生第二条', dupLink.status === 204)

const childDetail = await req('GET', `/api/entries/${child.json.id}`)
check('详情含 3 条出向引用', childDetail.json?.relations?.outgoing?.length === 3,
  String(childDetail.json?.relations?.outgoing?.length))

const logDetail = await req('GET', `/api/entries/${logs[1].id}`)
check('被引用方能反查到入向引用', logDetail.json?.relations?.incoming?.some((i) => i.id === child.json?.id),
  JSON.stringify(logDetail.json?.relations?.incoming))

const selfLink = await req('POST', `/api/entries/${child.json.id}/links`, { dstId: child.json.id })
check('自引用被拒 400', selfLink.status === 400, selfLink.json?.error ?? String(selfLink.status))

const missingLink = await req('POST', `/api/entries/${child.json.id}/links`, { dstId: 99999999 })
check('引用不存在的条目被拒 404', missingLink.status === 404, String(missingLink.status))

const removed = await req('DELETE', `/api/entries/${child.json.id}/links/${logs[2].id}`)
const afterRemove = await req('GET', `/api/entries/${child.json.id}`)
check('移除引用后剩 2 条', removed.status === 204 && afterRemove.json?.relations?.outgoing?.length === 2,
  String(afterRemove.json?.relations?.outgoing?.length))

const tree = await req('GET', '/api/tree')
const rootNode = (tree.json ?? []).find((n) => n.id === root.json?.id)
check('主题树里根节点存在', !!rootNode, String(tree.json?.length))
check('主题树嵌套了子节点', rootNode?.children?.some((c) => c.id === child.json?.id),
  JSON.stringify(rootNode?.children?.map((c) => c.id)))

const badParent = await req('PUT', `/api/entries/${root.json.id}`, {
  contentMd: '尝试把自己的父节点设成自己的后代',
  parentId: child.json?.id
})
check('挂到自己下级被拒 400', badParent.status === 400, badParent.json?.error ?? String(badParent.status))

const logParent = await req('PUT', `/api/entries/${child.json.id}`, {
  contentMd: '尝试挂到日志下面',
  parentId: logs[0].id
})
check('父节点必须是笔记 400', logParent.status === 400, logParent.json?.error ?? String(logParent.status))

const retagged = await req('PUT', `/api/entries/${child.json.id}`, {
  contentMd: '换标签测试',
  tagNames: ['事务', '连接池']
})
check('标签可整体替换', retagged.json?.tags?.length === 2, JSON.stringify(retagged.json?.tags))

const cleared = await req('PUT', `/api/entries/${child.json.id}`, { contentMd: '清空标签', tagNames: [] })
check('传空数组即清空标签', cleared.json?.tags?.length === 0, JSON.stringify(cleared.json?.tags))

const fresh = await req('GET', '/api/reviews/today?limit=50')
check('新建笔记不会立刻出现在复习队列（next_review_at 是一天后）',
  !(fresh.json ?? []).some((i) => i.id === child.json?.id), `队列 ${fresh.json?.length} 条`)

const first = await req('POST', `/api/reviews/${child.json.id}/result`, { outcome: 'known' })
check('记得 → 熟练度 1、约 7 天后再见',
  first.json?.mastery === 1 && Math.abs(daysFromNow(first.json?.nextReviewAt) - 7) < 0.2,
  `mastery=${first.json?.mastery} 间隔=${daysFromNow(first.json?.nextReviewAt).toFixed(2)}天`)

const second = await req('POST', `/api/reviews/${child.json.id}/result`, { outcome: 'forgot' })
check('忘了 → 熟练度回落、约 1 天后再见',
  second.json?.mastery === 0 && Math.abs(daysFromNow(second.json?.nextReviewAt) - 1) < 0.2,
  `mastery=${second.json?.mastery} 间隔=${daysFromNow(second.json?.nextReviewAt).toFixed(2)}天`)

// 忘了把熟练度扣回 0，所以要连续三次"记得"才到 3 分毕业线
await req('POST', `/api/reviews/${child.json.id}/result`, { outcome: 'known' })
await req('POST', `/api/reviews/${child.json.id}/result`, { outcome: 'known' })
const third = await req('POST', `/api/reviews/${child.json.id}/result`, { outcome: 'known' })
check('熟练度到 3 判定已掌握', third.json?.reviewStatus === 'mastered', `mastery=${third.json?.mastery}`)

const afterMastered = await req('GET', '/api/reviews/today?limit=50')
check('已掌握的条目退出队列', !(afterMastered.json ?? []).some((i) => i.id === child.json?.id))

const badOutcome = await req('POST', `/api/reviews/${child.json.id}/result`, { outcome: 'maybe' })
check('非法 outcome 被拒 400', badOutcome.status === 400, badOutcome.json?.error ?? String(badOutcome.status))

const logReview = await req('POST', `/api/reviews/${logs[0].id}/result`, { outcome: 'known' })
check('日志不能进复习 404', logReview.status === 404, String(logReview.status))

const trashed = await req('DELETE', `/api/entries/${logs[2].id}`)
const trashList = await req('GET', '/api/trash?limit=100')
check('回收站能看到刚删的条目', trashed.status === 204
  && (trashList.json ?? []).some((i) => i.id === logs[2].id), `回收站 ${trashList.json?.length} 条`)

const restored = await req('POST', `/api/entries/${logs[2].id}/restore`)
const trashAgain = await req('GET', '/api/trash?limit=100')
check('恢复后离开回收站', restored.status === 204
  && !(trashAgain.json ?? []).some((i) => i.id === logs[2].id))

const orphaned = await req('DELETE', `/api/entries/${root.json.id}`)
const childAfterParentGone = await req('GET', `/api/entries/${child.json.id}`)
const treeAfter = await req('GET', '/api/tree')
check('父节点被软删后子节点仍在树里（成为根）', orphaned.status === 204
  && childAfterParentGone.status === 200
  && (treeAfter.json ?? []).some((n) => n.id === child.json?.id),
  `树根数 ${( treeAfter.json ?? []).length}`)

console.log(rows.join('\n'))
console.log(`\n${failed === 0 ? 'ALL PASS' : failed + ' FAILED'}  (${rows.length} 项)`)
console.log(`提示：本次测试数据带 ${STAMP} 前缀，可用来清理。`)
process.exit(failed === 0 ? 0 : 1)

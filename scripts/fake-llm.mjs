// 本地假模型服务：模拟 OpenAI 兼容的 /embeddings 与 /chat/completions，
// 用来在没有 API key 的情况下验证 RAG 链路（写入 pgvector → 余弦检索 → RRF 融合 → 生成回答）。
// 用法：node scripts/fake-llm.mjs   然后让 mynotes.ai.base-url 指向 http://127.0.0.1:8930/v1
import http from 'node:http'

const PORT = Number(process.env.FAKE_LLM_PORT ?? 8930)
const DIM = Number(process.env.FAKE_EMBEDDING_DIM ?? 1024)

function vectorize(text) {
  const vector = new Array(DIM).fill(0)
  const source = String(text)
  for (let i = 0; i < source.length; i++) {
    const gram = source.slice(i, i + 2)
    let hash = 0
    for (const ch of gram) hash = (hash * 131 + ch.codePointAt(0)) >>> 0
    vector[hash % DIM] += hash & 1 ? 1 : -1
  }
  const norm = Math.hypot(...vector) || 1
  return vector.map((value) => value / norm)
}

http
  .createServer((req, res) => {
    let raw = ''
    req.on('data', (chunk) => (raw += chunk))
    req.on('end', () => {
      const body = JSON.parse(raw || '{}')
      res.setHeader('Content-Type', 'application/json')

      if (req.url?.endsWith('/embeddings')) {
        const inputs = Array.isArray(body.input) ? body.input : [body.input]
        res.end(JSON.stringify({
          data: inputs.map((text, index) => ({ index, object: 'embedding', embedding: vectorize(text) }))
        }))
        return
      }

      if (req.url?.endsWith('/chat/completions')) {
        const user = body.messages?.find((m) => m.role === 'user')?.content ?? ''
        const passages = user.match(/^\[\d+\]/gm) ?? []
        res.end(JSON.stringify({
          choices: [{
            index: 0,
            message: {
              role: 'assistant',
              content: `【fake-model】收到 ${passages.length} 段笔记，据此回答。引用编号示例 [1]。`
            }
          }]
        }))
        return
      }

      res.statusCode = 404
      res.end(JSON.stringify({ error: { message: `unknown path ${req.url}` } }))
    })
  })
  .listen(PORT, '127.0.0.1', () => console.log(`fake llm listening on http://127.0.0.1:${PORT}/v1 (dim=${DIM})`))

# MyNotes · 学习记录与笔记

自用工具，回答两个问题：**今天学了什么**（日志）和**某个知识点我理解到哪了**（主题笔记）。
两者共用一张 `entry` 表、靠 `type` 区分，因此搜索、关联、复习只需要一套实现。

## 技术栈

Vue 3 + TypeScript + Element Plus｜Spring Boot 3.5 + MyBatis + Flyway｜PostgreSQL 18 + pgvector + pg_trgm

## 初始化

1. 建库建角色（只需一次，密码用 `\password` 交互输入，不会留在文件或命令历史里）：

   ```bash
   "D:/PostgreSQL/18/bin/psql.exe" -U postgres -f scripts/init-db.sql
   ```

   脚本内容：创建 `mynotes` 角色与同名库、装 `vector` / `pg_trgm` 扩展、给 `public` schema 放建表权
   （PG15 起默认收回，否则 Flyway 建表直接失败）。

2. 填密码：

   ```bash
   cp backend/src/main/resources/application-example.yml \
      backend/src/main/resources/application-local.yml
   # 编辑 application-local.yml 的 spring.datasource.password
   ```

   `application-local.yml` 已在 `.gitignore` 里，不要提交。表结构由 Flyway 在首次启动时自动建，
   不需要手工执行 SQL。

## 启动

需要两个终端：

```bash
mvn -f backend/pom.xml spring-boot:run          # API :8080
npm --prefix frontend run dev                   # 页面 :5173
```

打开 http://localhost:5173 。Vite 把 `/api` 代理到 8080，所以开发期同源、不需要配 CORS。

## HTTP 接口

| 方法 路径 | 用途 |
|---|---|
| `POST /api/entries` | 新建，`{type,title,contentMd,sourceDate,parentId}` |
| `GET /api/entries/{id}` | 详情 |
| `PUT /api/entries/{id}` | 更新 |
| `DELETE /api/entries/{id}` | 软删除 |
| `POST /api/entries/{id}/restore` | 恢复 |
| `GET /api/timeline?type&cursor&size` | 时间线，游标为上一页末条的 `sourceDate:id` |
| `GET /api/search?q&type&limit` | 中文关键词检索 |
| `GET /api/tree` | 主题树（仅笔记，父子由 `parent_id` 决定） |
| `POST /api/entries/{id}/links` · `DELETE /api/entries/{id}/links/{dstId}` | 建立/移除引用（详情响应里同时带回反向引用） |
| `GET /api/reviews/today` · `POST /api/reviews/{id}/result` | 复习队列与三档结果 `known\|vague\|forgot` |
| `GET /api/trash` | 回收站（软删除条目） |
| `GET /api/ai/status` · `POST /api/ai/reindex` · `POST /api/ai/ask` | 问答索引状态、重建向量、提问 |

## 配置模型（可选，问答用）

在 `application-local.yml` 里填任意 OpenAI 兼容网关：

```yaml
mynotes:
  ai:
    api-key: "sk-..."
    base-url: "https://dashscope.aliyuncs.com/compatible-mode/v1"
    chat-model: "qwen-plus"
    embedding-model: "text-embedding-v3"
    embedding-dim: 1024        # 必须与 embedding 模型输出维度一致
```

- **不填也能用**：问答自动降级为"关键词检索 + 笔记原文引用"，接口结构不变。
- `embedding-dim` 只在**首次启动前**生效——它通过 Flyway 占位符写进 `vector(dim)` 列。
  已经建过库之后换不同维度的模型，要再写一个 `V3` 迁移改列，而不是改这个值。
- 向量不是保存时自动算的（避免把外部 HTTP 调用挂在数据库事务里）。改过内容后到「问答」页点
  **现在重建**，或 `POST /api/ai/reindex`；按 `md5(content_plain)` 比对，内容没变不会重复调用模型。

### 不花钱验证 RAG 链路

```bash
node scripts/fake-llm.mjs        # 本地假模型：/embeddings + /chat/completions
mvn -f backend/pom.xml spring-boot:run \
  -Dspring-boot.run.arguments="--mynotes.ai.api-key=fake --mynotes.ai.base-url=http://127.0.0.1:8930/v1 --mynotes.ai.chat-model=fake-chat --mynotes.ai.embedding-model=fake-embed"
node scripts/smoke-m3.mjs        # 16 项：写向量→余弦召回→RRF 融合→引用编号
```

假模型用字符二元组哈希造向量，只能验证**链路通不通**（维度、HNSW 索引、排序、融合、引用），
不代表语义检索质量——那要接真模型才能判断。

## 冒烟测试

后端起来后依次跑，覆盖三层接口与前端会打的每个端点：

```bash
node scripts/smoke.mjs      # M1 记录 / 搜索 / 软删（30 项）
node scripts/smoke-m2.mjs   # M2 主题树 / 关联 / 标签 / 复习 / 回收站（26 项）
node scripts/smoke-m3.mjs   # M3 问答（需按上一节指向假模型）
```

测试数据带 `m2-` / `m3-` 时间戳前缀，方便事后清掉。


## 设计上几个刻意的选择

**中文检索用 `ILIKE` + `pg_trgm`，不用 `tsvector`。** 本机没有中文分词扩展（zhparser/pg_jieba 都没装），
`tsvector` 配 `simple` 词典会把一整串汉字当一个词，结果是搜索框静默返回空。`ILIKE` 对任意长度中文都成立，
包括单个汉字；三字以上的查询由 `gin_trgm_ops` 索引兜住。个人数据量下这个组合够用，等真到了几万条再换方案。

**向量存在同一张表里。** `entry.embedding vector(dim)` + HNSW 索引，检索就是一句
`ORDER BY embedding <=> :q LIMIT k`，不需要独立向量库。这一列在 M3 的 `V2__embedding.sql` 里才加，
因为 `vector(N)` 的维度写死在 schema 里，而 embedding 模型还没定。

**复习机制只做最小版。** 三个字段（`review_status` / `mastery` / `next_review_at`）加三个按钮
（记得 / 模糊 / 忘了 → 顺延 7 / 3 / 1 天），不实现 SM-2：个人数据量下算法参数带来的噪声大于收益。
字段名故意不绑算法，将来升级 SM-2 只需 `ALTER TABLE ADD COLUMN ease/interval/reps`。

**没有登录鉴权。** 单用户本机工具；要部署到公网之前必须先补鉴权。

## 目录

```
backend/src/main/java/com/mynotes/
  controller/  service/ + service/impl/  mapper/     # 三层
  entity/  dto/（入参）  vo/（出参）  common/  config/  client/
backend/src/main/resources/
  mapper/*.xml  db/migration/*.sql  application*.yml
frontend/src/
  views/  components/  api/  router/
scripts/
  init-db.sql  smoke.mjs  smoke-m2.mjs  smoke-m3.mjs  fake-llm.mjs
```

## 里程碑

- M1 记录 + 搜索：快速记录、时间线、中文检索、软删除
- M2 双视图 + 复习：主题树、条目关联、标签、复习队列、回收站
- M3 笔记问答：pgvector 混合召回 + 引用式回答（未配模型 key 时自动降级为关键词检索 + 原文引用）

-- 维度由 spring.flyway.placeholders.embedding-dim 注入，与 mynotes.ai.embedding-dim 保持一致。
-- 换用不同维度的 embedding 模型时，这里已经执行过了，需要再写一个 V3 迁移改列，而不是改本文件。
ALTER TABLE entry ADD COLUMN embedding vector(${embedding_dim});
ALTER TABLE entry ADD COLUMN embedded_hash CHAR(32);

-- HNSW 只索引已写入的向量；个人量级下 ef_construction 用默认值即可。
CREATE INDEX idx_entry_embedding ON entry USING hnsw (embedding vector_cosine_ops);

-- 用 PostgreSQL superuser 执行一次（密码走 \password 交互输入，不落文件、不进 shell 历史）：
--   "D:/PostgreSQL/18/bin/psql.exe" -U postgres -f scripts/init-db.sql
\set ON_ERROR_STOP on

CREATE ROLE mynotes LOGIN;
\password mynotes

CREATE DATABASE mynotes OWNER mynotes ENCODING 'UTF8' TEMPLATE template0;

\c mynotes

-- pgvector 不是 trusted extension，必须由 superuser 建；应用角色之后只用不建。
CREATE EXTENSION vector;
CREATE EXTENSION pg_trgm;

-- PG15 起 public schema 默认不给普通角色建表权，Flyway 需要它。
GRANT CREATE, USAGE ON SCHEMA public TO mynotes;

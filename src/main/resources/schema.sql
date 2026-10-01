-- pgvector must be enabled once per database; safe to run on every startup
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS document_chunk (
    id            BIGSERIAL PRIMARY KEY,
    document_id   VARCHAR(64)  NOT NULL,
    document_name VARCHAR(255) NOT NULL,
    chunk_index   INT          NOT NULL,
    content       TEXT         NOT NULL,
    embedding     VECTOR(768) NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT now()
);

-- ivfflat index dramatically speeds up cosine-similarity search once you have
-- more than a few thousand rows. Requires ANALYZE after bulk inserts.
CREATE INDEX IF NOT EXISTS idx_document_chunk_embedding
    ON document_chunk
    USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);

CREATE INDEX IF NOT EXISTS idx_document_chunk_document_id
    ON document_chunk (document_id);

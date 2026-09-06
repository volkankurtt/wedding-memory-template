CREATE TABLE memories (
    id UUID NOT NULL PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    message VARCHAR(600) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_memories_created_at ON memories (created_at DESC);

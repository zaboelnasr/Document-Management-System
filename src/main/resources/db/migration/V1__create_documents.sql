CREATE TABLE IF NOT EXISTS documents (
    id BIGSERIAL PRIMARY KEY,
    file_name VARCHAR(255),
    summary TEXT,
    bucket VARCHAR(255),
    object_key VARCHAR(255),
    content_type VARCHAR(255),
    size BIGINT,
    content OID,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    ocr_status VARCHAR(32) DEFAULT 'PENDING'
);

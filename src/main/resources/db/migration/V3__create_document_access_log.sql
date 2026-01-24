CREATE TABLE document_access_log (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL,
    access_date DATE NOT NULL,
    access_count INT DEFAULT 0,
    UNIQUE (document_id, access_date),
    CONSTRAINT fk_document_access_log_document
        FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE
);

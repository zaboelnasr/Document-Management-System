CREATE TABLE document_reviews (
  id BIGSERIAL PRIMARY KEY,
  document_id BIGINT NOT NULL UNIQUE,
  status VARCHAR(32) NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  CONSTRAINT fk_document_reviews_document
      FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE
);
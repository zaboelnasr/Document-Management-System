CREATE TABLE IF NOT EXISTS document_access_log (
                                                   id BIGSERIAL PRIMARY KEY,
                                                   document_name VARCHAR(255) NOT NULL,
    access_date DATE NOT NULL,
    access_count INT DEFAULT 0,
    UNIQUE (document_name, access_date)
    );

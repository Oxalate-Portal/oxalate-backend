CREATE TABLE certificate_documents
(
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    file_name      VARCHAR(255) NOT NULL,
    user_id        BIGINT       NOT NULL CONSTRAINT fk_certificate_documents_users_id REFERENCES users (id),
    certificate_id BIGINT       NOT NULL CONSTRAINT fk_certificate_documents_certificates_id REFERENCES certificates (id),
    created_at     TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

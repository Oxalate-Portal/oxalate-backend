CREATE TABLE certificate_classifications
(
    id          BIGSERIAL PRIMARY KEY,
    description TEXT
);

CREATE TABLE certificate_classification_translations
(
    id                BIGSERIAL PRIMARY KEY,
    classification_id BIGINT       NOT NULL REFERENCES certificate_classifications (id) ON DELETE CASCADE,
    language          VARCHAR(2)   NOT NULL,
    title             VARCHAR(255) NOT NULL,
    UNIQUE (classification_id, language)
);

ALTER TABLE certificates
    ADD COLUMN classification_id BIGINT REFERENCES certificate_classifications (id) ON DELETE SET NULL;

CREATE INDEX idx_certificates_classification_id ON certificates (classification_id);

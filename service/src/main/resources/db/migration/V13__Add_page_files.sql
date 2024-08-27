CREATE TABLE page_files
(
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    file_name     VARCHAR(255) NOT NULL,
    language      VARCHAR(255) NOT NULL,
    page_id       BIGINT       NOT NULL CONSTRAINT fk_upload_files_pages_id REFERENCES pages (id),
    mime_type     VARCHAR(255) NOT NULL,
    file_size     BIGINT       NOT NULL,
    file_checksum VARCHAR(255) NOT NULL,
    status        VARCHAR(255) NOT NULL,
    creator       BIGINT       NOT NULL CONSTRAINT fk_upload_files_users_id REFERENCES users (id),
    created_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT uk_upload_files_file_name_page_id_language UNIQUE (file_name, page_id, language)
);

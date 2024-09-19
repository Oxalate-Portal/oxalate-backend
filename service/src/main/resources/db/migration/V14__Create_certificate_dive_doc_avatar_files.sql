CREATE TABLE document_files
(
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    file_name     VARCHAR(255) NOT NULL,
    mime_type     VARCHAR(255) NOT NULL,
    file_size     BIGINT       NOT NULL,
    file_checksum VARCHAR(255) NOT NULL,
    status        VARCHAR(255) NOT NULL,
    creator       BIGINT       NOT NULL CONSTRAINT fk_upload_files_users_id REFERENCES users (id),
    created_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT uk_document_files_file_name UNIQUE (file_name)
);

CREATE TABLE dive_files
(
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_id      BIGINT       NOT NULL,
    dive_group_id BIGINT       NOT NULL,
    file_name     VARCHAR(255) NOT NULL,
    mime_type     VARCHAR(255) NOT NULL,
    file_size     BIGINT       NOT NULL,
    file_checksum VARCHAR(255) NOT NULL,
    status        VARCHAR(255) NOT NULL,
    creator       BIGINT       NOT NULL CONSTRAINT fk_upload_files_users_id REFERENCES users (id),
    created_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT uk_dive_files_file_name_event_id_dive_group_id UNIQUE (file_name, event_id, dive_group_id)
);

CREATE TABLE avatar_files
(
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    file_name     VARCHAR(255) NOT NULL,
    mime_type     VARCHAR(255) NOT NULL,
    file_size     BIGINT       NOT NULL,
    file_checksum VARCHAR(255) NOT NULL,
    creator       BIGINT       NOT NULL CONSTRAINT fk_certificate_files_users_id REFERENCES users (id),
    created_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

CREATE TABLE certificate_files
(
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    file_name      VARCHAR(255) NOT NULL,
    certificate_id BIGINT       NOT NULL CONSTRAINT fk_certificate_files_certificates_id REFERENCES certificates (id),
    mime_type      VARCHAR(255) NOT NULL,
    file_size      BIGINT       NOT NULL,
    file_checksum  VARCHAR(255) NOT NULL,
    creator        BIGINT       NOT NULL CONSTRAINT fk_certificate_files_users_id REFERENCES users (id),
    created_at     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT uk_certificate_files_file_name_certificate_id UNIQUE (file_name, certificate_id)
);

CREATE TABLE page_groups
(
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY
);

CREATE TABLE page_group_versions
(
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    page_group_id BIGINT     NOT NULL
        CONSTRAINT fk_page_group_versions_id REFERENCES page_groups (id) ON DELETE CASCADE,
    language      VARCHAR(2) NOT NULL DEFAULT 'en', -- ISO 639-1 language code
    title         TEXT       NOT NULL DEFAULT ''
);

CREATE TABLE pages
(
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    status        VARCHAR(64)                 NOT NULL DEFAULT 'PUBLIC',
    page_group_id BIGINT                      NOT NULL
        CONSTRAINT fk_pages_group_id REFERENCES page_groups (id) ON DELETE CASCADE,
    creator       BIGINT                      NOT NULL
        CONSTRAINT fk_pages_created_users_id REFERENCES users (id),
    created_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    modifier      BIGINT                               DEFAULT NULL
        CONSTRAINT fk_pages_updated_users_id REFERENCES users (id),
    modified_at   TIMESTAMP WITHOUT TIME ZONE          DEFAULT NULL
);

CREATE TABLE page_versions
(
    id       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    page_id  BIGINT     NOT NULL
        CONSTRAINT fk_page_versions_pages_id REFERENCES pages (id) ON DELETE CASCADE,
    language VARCHAR(2) NOT NULL DEFAULT 'en', -- ISO 639-1 language code
    title    TEXT       NOT NULL DEFAULT '',
    ingress  TEXT       NOT NULL DEFAULT '',
    body     TEXT       NOT NULL DEFAULT '',
    UNIQUE (page_id, language)
);

CREATE TABLE page_role_access
(
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    page_id          BIGINT      NOT NULL
        CONSTRAINT fk_page_role_access_pages_id REFERENCES pages (id) ON DELETE CASCADE,
    role             VARCHAR(64) NOT NULL DEFAULT 'ROLE_ANONYMOUS',
    read_permission  BOOLEAN     NOT NULL DEFAULT 'true',
    write_permission BOOLEAN     NOT NULL DEFAULT 'false',
    UNIQUE (page_id, role)
);

-- We don't allow both permissions to be false as this is the default for any page and role with no entry
ALTER TABLE page_role_access
    ADD CONSTRAINT page_role_access_permissions_not_null CHECK ( read_permission OR write_permission );
-- If the write permission is true, the read permission must also be true
ALTER TABLE page_role_access
    ADD CONSTRAINT page_role_access_write_permission_implies_read CHECK ( (write_permission AND NOT read_permission) OR read_permission );

-- Page group ID 1 will be reserved for special purposes like the front page and terms of use
INSERT INTO page_groups (id) OVERRIDING SYSTEM VALUE
VALUES (1);

INSERT INTO page_group_versions (title, language, page_group_id)
VALUES ('Reserved', 'en', 1),
       ('Varattu', 'fi', 1),
       ('Reserverade', 'sv', 1),
       ('Reserviert', 'de', 1);

SELECT pg_catalog.setval('page_groups_id_seq', 2, true);

SELECT pg_catalog.setval('page_group_versions_id_seq', 200, true);

-- We also reserve the 20 first page IDs for special purposes
SELECT pg_catalog.setval('pages_id_seq', 20, true);

-- 200 first page version IDs
SELECT pg_catalog.setval('page_versions_id_seq', 200, true);

-- 100 first page role IDs
SELECT pg_catalog.setval('page_role_access_id_seq', 200, true);

-- Add default info pages group
INSERT INTO page_groups (id) OVERRIDING SYSTEM VALUE
VALUES (2);

SELECT pg_catalog.setval('page_groups_id_seq', 3, true);

INSERT INTO page_group_versions (title, language, page_group_id)
VALUES ('Info', 'en', 2),
       ('Info', 'fi', 2),
       ('Info', 'sv', 2),
       ('Info', 'de', 2);

-- Finally we add the language selection to the user table
ALTER TABLE users
    ADD language VARCHAR(2) NOT NULL DEFAULT 'en';

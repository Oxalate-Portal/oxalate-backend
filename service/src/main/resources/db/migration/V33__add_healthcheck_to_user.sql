ALTER TABLE users
    ADD COLUMN healthcheck_id BIGINT DEFAULT NULL;

-- Insert page for healthcheck page
INSERT INTO pages (id, status, page_group_id, creator, created_at)
    OVERRIDING SYSTEM VALUE
VALUES (3, 'PUBLISHED', 1, 1, NOW());

-- Only admin has rw access to healthcheck page
INSERT INTO page_role_access (id, page_id, role, read_permission, write_permission)
    OVERRIDING SYSTEM VALUE
VALUES (5, 3, 'ROLE_ADMIN', true, true),
       (6, 3, 'ROLE_ANONYMOUS', true, false);

INSERT INTO page_versions (id, page_id, language, title, ingress, body)
    OVERRIDING SYSTEM VALUE
VALUES (11, 3, 'en', 'Healthcheck', 'This is a healthcheck page.', 'This is a healthcheck page.'),
       (12, 3, 'de', 'Gesundheitsprüfung', 'Dies ist eine Gesundheitsprüfungsseite.', 'Dies ist eine Gesundheitsprüfungsseite.'),
       (13, 3, 'fi', 'Terveystarkistus', 'Tämä on terveystarkistussivu.', 'Tämä on terveystarkistussivu.'),
       (14, 3, 'es', 'Chequeo de salud', 'Esta es una página de chequeo de salud.', 'Esta es una página de chequeo de salud.'),
       (15, 3, 'sv', 'Hälsokontroll', 'Detta är en hälsokontrollsida.', 'Detta är en hälsokontrollsida.');

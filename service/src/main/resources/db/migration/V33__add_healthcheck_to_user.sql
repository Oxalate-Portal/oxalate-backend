ALTER TABLE users
    ADD COLUMN health_statement_id BIGINT DEFAULT NULL;

-- Insert page for health statement page
INSERT INTO pages (id, status, page_group_id, creator, created_at)
    OVERRIDING SYSTEM VALUE
VALUES (3, 'PUBLISHED', 1, 1, NOW());

-- Only admin has rw access to health statement page
INSERT INTO page_role_access (id, page_id, role, read_permission, write_permission)
    OVERRIDING SYSTEM VALUE
VALUES (5, 3, 'ROLE_ADMIN', true, true),
       (6, 3, 'ROLE_ANONYMOUS', true, false);

INSERT INTO page_versions (id, page_id, language, title, ingress, body)
    OVERRIDING SYSTEM VALUE
VALUES (11, 3, 'en', 'Health statement', 'Confirmation of diving fitness before participating in diving activities.',
        'Confirmation of diving fitness before participating in diving activities.'),
       (12, 3, 'de', 'Gesundheitserklärung', 'Bestätigung der Tauchtauglichkeit vor der Teilnahme an Tauchaktivitäten.',
        'Bestätigung der Tauchtauglichkeit vor der Teilnahme an Tauchaktivitäten.'),
       (13, 3, 'fi', 'Terveysselvitys', 'Sukelluskelpoisuuden vahvistaminen ennen sukellustoimintaan osallistumista.',
        'Sukelluskelpoisuuden vahvistaminen ennen sukellustoimintaan osallistumista.'),
       (14, 3, 'es', 'Declaración de salud', 'Confirmación de la aptitud para el buceo antes de participar en actividades de buceo.',
        'Confirmación de la aptitud para el buceo antes de participar en actividades de buceo.'),
       (15, 3, 'sv', 'Hälsodeklaration', 'Bekräftelse av dyklämplighet innan deltagande i dykeraktiviteter.',
        'Bekräftelse av dyklämplighet innan deltagande i dykeraktiviteter.');

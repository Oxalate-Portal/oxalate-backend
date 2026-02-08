INSERT INTO page_group_versions (id, page_group_id, language, title)
    OVERRIDING SYSTEM VALUE
VALUES (5, 1, 'es', 'Pagina reservada'),
       (205, 2, 'es', 'informacion');

INSERT INTO page_versions (id, page_id, language, title, ingress, body)
    OVERRIDING SYSTEM VALUE
VALUES (9, 1, 'es', 'Página delantera', '', 'Bienvenido al portal de Oxalate'),
       (10, 2, 'es', 'Términos y condiciones', '', 'Texto de términos y condiciones');

-- After the forceful insertion of the page group versions and page versions, we need to make sure the sequences are up to date
SELECT setval('page_group_versions_id_seq', (SELECT MAX(id) FROM page_group_versions));
SELECT setval('page_versions_id_seq', (SELECT MAX(id) FROM page_versions));

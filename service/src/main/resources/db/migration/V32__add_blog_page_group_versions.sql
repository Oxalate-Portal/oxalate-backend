INSERT INTO page_groups (id, status)
    OVERRIDING SYSTEM VALUE
VALUES (3, 'PUBLISHED');

-- Insert de, de, fi, es, sv translations for the blog page group
INSERT INTO page_group_versions (page_group_id, language, title)
VALUES (3, 'en', 'Blog'),
       (3, 'de', 'Blog'),
       (3, 'fi', 'Blogi'),
       (3, 'es', 'Blog'),
       (3, 'sv', 'Blogg');

-- After the forceful insertion of the page group, we need to make sure the sequence is up to date
SELECT setval('page_groups_id_seq', (SELECT MAX(id) FROM page_groups));
SELECT setval('page_group_versions_id_seq', (SELECT MAX(id) FROM page_group_versions));

INSERT INTO portal_configuration (group_key, setting_key, value_type, default_value, required_runtime, description)
VALUES ('general', 'blog-enabled', 'boolean', 'false', false, 'Enable or disable the blog page group');

UPDATE portal_configuration
SET default_value = 'event,page,forum,blog'
WHERE group_key = 'commenting'
  AND setting_key = 'commenting-enabled-features';

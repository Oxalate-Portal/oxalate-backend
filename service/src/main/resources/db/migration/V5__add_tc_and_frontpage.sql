-- This is a shitty kluge, but we need it to be able to add the roles later:
INSERT INTO users (id, username, first_name, last_name, password, status, phone_number, privacy, next_of_kin, approved_terms, registered,
                   language) OVERRIDING SYSTEM VALUE
VALUES (1, 'admin@test.tld', 'Administrator', 'Account', 'NOT_SET', 'ACTIVE', '1234567890123', false, 'NOT_SET', true, NOW(), 'en')
ON CONFLICT (id)
DO NOTHING;

-- If V5 created an admin in an empty database, then this will bump the user id so that consequent inserts will not conflict with this one
DO
$$
    DECLARE
        last_val INTEGER;
    BEGIN
        SELECT last_value INTO last_val FROM users_id_seq;

        IF last_val <> 1 THEN
            RAISE NOTICE 'The sequence is currently at %', last_val;
        ELSE
            PERFORM pg_catalog.setval('users_id_seq', 2, false); -- Note the hardcoded value here
            SELECT last_value INTO last_val FROM users_id_seq;
            RAISE NOTICE 'The sequence has been reset to the next value: %', last_val;
        END IF;
    END
$$;

-- Insert page for front and t&c page
INSERT INTO pages (id, status, page_group_id, creator, created_at) OVERRIDING SYSTEM VALUE
VALUES (1, 'PUBLIC', 1, 1, NOW()), (2, 'PUBLIC', 1, 1, NOW());

-- Only admin has rw access to front and t&c page
INSERT INTO page_role_access (id, page_id, role, read_permission, write_permission) OVERRIDING SYSTEM VALUE
VALUES (1, 1, 'ROLE_ADMIN', true, true), (2, 1, 'ROLE_ANONYMOUS', true, false), (3, 2, 'ROLE_ADMIN', true, true), (4, 2, 'ROLE_ANONYMOUS', true, false);

INSERT INTO page_versions (id, page_id, language, title, ingress, body) OVERRIDING SYSTEM VALUE
VALUES (1, 1, 'de', 'Titel seite', '', 'Willkommen im Oxalate Portal'), (2, 1, 'en', 'Front page', '', 'Welcome to Oxalate portal'), (3, 1, 'fi', 'Etusivu', '', 'Tervetuloa Oxalate-portaaliin'), (4, 1, 'sv', 'Hemsidan', '', 'Välkommen till Oxalate-portalen'), (5, 2, 'de', 'Geschäftsbedingungen', '', 'Geschäftsbedingungen Text'), (6, 2, 'en', 'Terms and conditions', '', 'Terms and conditions text'), (7, 2, 'fi', 'Käyttöehdot', '', 'Käyttöehtoteksti'), (8, 2, 'sv', 'Vilkor', '', 'Vilkor-teksten');

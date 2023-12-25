-- ######################################### Generate test data
-- We use the full db.schema.table annotation here to ensure that we get the stuff in the right place
-- A couple of users, the password is qweqwe in all cases
INSERT INTO oxdb.public.users (first_name, last_name, "password", username, status, phone_number, privacy, next_of_kin, registered)
VALUES ('First', 'Name', '\$2a\$10\$7C3bQ5SKkKJ3ZzeqBVQ2v.qnu8xNFjOzo4bP1OcgCtP.3N8PuMCUC', 'test@a.tld', 'ACTIVE', '460455297375', false, 'My Mother 369520664546', NOW()),
       ('Eka', 'Toka', '\$2a\$10\$foJUN1cRitVgI/73Hu4c9eZFJkhfD.xrxJ9EvEZ2tYQMe5Vhvof0a', 'test@b.tld', 'ACTIVE', '765200231558', false, 'My Father 991227370791', NOW()),
       ('Erste', 'Zweite', '\$2a\$10\$foJUN1cRitVgI/73Hu4c9eZFJkhfD.xrxJ9EvEZ2tYQMe5Vhvof0a', 'test@c.tld', 'ACTIVE', '295242191657', true, 'My Brother 503121856561', NOW()),
       ('Första', 'Andra', '\$2a\$10\$foJUN1cRitVgI/73Hu4c9eZFJkhfD.xrxJ9EvEZ2tYQMe5Vhvof0a', 'test@d.tld', 'ACTIVE', '213155539137', true, 'My Sister 450161180752', NOW()),
       ('Un', 'Dö', '\$2a\$10\$foJUN1cRitVgI/73Hu4c9eZFJkhfD.xrxJ9EvEZ2tYQMe5Vhvof0a', 'test@e.tld', 'ACTIVE', '324729435718', false, 'My Hamster 632210265896', NOW()),
       ('<Anonymized>', '<Anonymized>', '<Anonymized>', 'cd13f66d-863e-43cc-b290-cf756bdefc51@anonymized.tld', 'ANONYMIZED', 'ANONYMIZED', false, 'ANONYMIZED', NOW());

-- First user, test@a.b has all roles, second is user
INSERT INTO oxdb.public.user_roles (user_id, role_id)
VALUES (1, 1),
       (1, 2),
       (1, 3),
       (2, 2),
       (2, 3),
       (3, 3),
       (4, 3),
       (5, 3),
       (6, 3);

-- Add a couple of events in the past
INSERT INTO oxdb.public.events (description, event_duration, max_depth, max_duration, max_participants, published,
                                 start_time, title, type, organizer_id)
VALUES ('Vuoden 2020 hedelmäisin mangosukellus, oma hedelmä mukaan.', 8, 90, 120, 12, true,
        '2020-06-12 10:00:00.000000', 'Mangosukellusta', 'Luola / Avo', 1),
       ('Vuoden 2021 ensimmäinen kiwisukellus, oma hedelmä mukaan.', 6, 40, 60, 18, true,
        '2021-01-01 10:00:00.000000', 'Kiwisukellusta', 'Luola / Avo', 1);

-- Add participants to future events
INSERT INTO oxdb.public.event_participants (user_id, event_id)
VALUES (3, 1),
       (4, 1),
       (5, 1),
       (2, 2),
       (4, 2);

-- Add a couple of events in the future
INSERT INTO oxdb.public.events (description, event_duration, max_depth, max_duration, max_participants, published,
                                 start_time, title, type, organizer_id)
VALUES ('Vuoden 2025 viimeinen banaanisukellus, oma hedelmä mukaan.', 8, 90, 120, 12, true,
        '2025-12-31 10:00:00.000000', 'Banaanisukellusta', 'Luola / Avo', 1),
       ('Vuoden 2026 viimeinen appelsiinisukellus, oma hedelmä mukaan.', 6, 40, 60, 18, true,
        '2026-12-31 10:00:00.000000', 'Appelsiinisukellusta', 'Luola / Avo', 1);

-- Add participants to future events
INSERT INTO oxdb.public.event_participants (user_id, event_id)
VALUES (3, 3),
       (4, 3),
       (5, 3),
       (2, 4),
       (4, 4);

-- Insert certificates for the users
INSERT INTO oxdb.public.certificates (user_id, organization, certificate_name, certificate_id, diver_id, certification_date)
VALUES (1,'IANTD', 'CCR full cave diver', '123456', null, '2012-06-21'),
       (1,'GUE', 'Cave diver 3', null, '131AA12', '2017-06-30'),
       (2,'CMAS', 'Advanced Trimix Diver', '234567', null, '2017-07-22'),
       (3,'NAUI', 'Closed Circuit Rebreather', '56345A', null, '2017-02-11'),
       (4,'BSAC', 'Sport mixed gas', 'Y12345', null, '2017-11-03'),
       (4,'SDI', 'Open water', '22XX22', null, '2017-08-01'),
       (4,'TDI', 'CCR DPV', '412123', null, '2017-12-02'),
       (5,'NSS-CDS', 'Cave rescue', null, '321', '2012-11-09'),
       (6,'PADI', 'AOWD', '1', null, '1984-01-01');

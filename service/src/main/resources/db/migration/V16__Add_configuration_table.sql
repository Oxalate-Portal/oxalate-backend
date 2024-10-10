CREATE TABLE portal_configuration
(
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    group_key        VARCHAR(255) NOT NULL,
    setting_key      VARCHAR(255) NOT NULL,
    value_type       VARCHAR(255) NOT NULL,
    default_value    VARCHAR(255) NOT NULL,
    runtime_value    VARCHAR(255),
    required_runtime BOOLEAN      NOT NULL,
    description      TEXT         NOT NULL,
    CONSTRAINT uk_portal_configuration_group_key_setting_key UNIQUE (group_key, setting_key)
);

INSERT INTO portal_configuration (value_type, group_key, setting_key, default_value, runtime_value, required_runtime,description)
    VALUES
    ('string',  'general',  'org-name', 'Oxalate', NULL, false, 'Name of the organization'),
    ('string',  'general',  'default-language', 'en', NULL, false,'Default language for the portal, must be one fo the enabled languages, ISO-639 format'),
    ('array',   'general',  'enabled-language', 'de,en,fi,sv', NULL, false,'Which languages are enabled, ISO-639 format'),
    ('number',  'general',  'top-divers-list-size', '100', NULL, false,'What is the count of top divers (counted by dives)'),
    ('email',   'email',    'org-email', 'org@non-existing.tld', NULL, true, 'Organization email address'),
    ('email',   'email',    'support-email', 'support@non-existing.tld', NULL, true, 'Support email address'),
    ('email',   'email',    'system-email', 'system@non-existing.tld', NULL, true, 'System email adddress, usually a no-reply address'),
    ('boolean', 'email',    'email-enabled', 'false', NULL, false, 'Whether the email sending is enabled by default'),
    ('array',   'email',    'email-notifications', 'event-new,event-updated,event-removed,page-new,page-updated,page-removed', NULL, false, 'Which email notifications are sent'),
    ('number',  'email',    'email-notification-retries', '5', NULL, false, 'How many times should a message sending be retried'),
    ('number',  'payment',  'start-month', '1', NULL, false, 'Which month will the period payment start, 1 = January'),
    ('boolean', 'payment',  'event-require-payment', 'false', NULL, false, 'Does a diver have to have a valid payment to be able to join a dive event'),
    ('array',   'payment',  'enabled-payment-methods', 'period,one-time', NULL, false, 'What type of payments are allowed'),
    ('number',  'frontend', 'min-event-length', '2', NULL, false, 'Minimum length of an event, in hours'),
    ('number',  'frontend', 'max-event-length', '12', NULL, false, 'Maximum length of an event, in hours'),
    ('number',  'frontend', 'max-dive-length', '240', NULL, false, 'Maximum length of a dive, in minutes'),
    ('number',  'frontend', 'min-participants', '3', NULL, false, 'Minimum number of simultaneous participants for an dive event to take place'),
    ('number',  'frontend', 'max-participants', '30', NULL, false, 'Maximum number of simultaneous participants'),
    ('number',  'frontend', 'max-depth', '60', NULL, false, 'Maximum depth of dives'),
    ('array',   'frontend', 'types-of-event', 'open-water,cave,open-and-cave,surface,boat,current', NULL, false, 'Maximum depth of dives'),
    ('number',  'frontend', 'max-certificates', '50', NULL, false, 'Maximum number of certificates a diver can add')
    ;

UPDATE events SET type = 'open-water' WHERE type = 'Avo';
UPDATE events SET type = 'open-water' WHERE type = 'DIVE';
UPDATE events SET type = 'open-and-cave' WHERE type = 'Luola / Avo';
UPDATE events SET type = 'cave' WHERE type = 'Luola';
UPDATE events SET type = 'surface' WHERE type = 'Vain pintatoimintaa';
UPDATE events SET type = 'surface' WHERE type = 'Muu';
-- ('', '', NULL, false, ''),

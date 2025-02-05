INSERT INTO portal_configuration (value_type, group_key, setting_key, default_value, runtime_value, required_runtime, description)
VALUES ('enum', 'payment', 'one-time-expiration-type', 'disabled,perpetual,periodical,durational', NULL, false,
        'If one-time payments expire, what type of expiration is used?'),
       ('enum', 'payment', 'one-time-expiration-unit', 'YEARS', NULL, false, 'If one-time payments expire, what calendar unit is used for expiration?'),
       ('number', 'payment', 'one-time-expiration-length', '1', NULL, false, 'If one-time payments expire, after how many calendar units?');

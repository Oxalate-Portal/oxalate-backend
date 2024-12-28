INSERT INTO portal_configuration (value_type, group_key, setting_key, default_value, runtime_value, required_runtime, description)
VALUES ('timezone', 'general', 'timezone', 'Europe/Helsinki', NULL, false, 'Portal timezone in which all date times should be displayed');

-- Zero the seconds and milliseconds from all timestamps
UPDATE events SET start_time = date_trunc('minute', start_time);

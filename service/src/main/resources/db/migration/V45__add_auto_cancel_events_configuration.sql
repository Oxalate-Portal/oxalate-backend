INSERT INTO portal_configuration (value_type, group_key, setting_key, default_value, runtime_value, required_runtime, description)
VALUES ('boolean', 'general', 'auto-cancel-events', 'false', NULL, false,
        'Whether published dive events with fewer participants than frontend.min-participants are cancelled automatically once their start time has passed');

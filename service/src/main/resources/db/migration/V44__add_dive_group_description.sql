ALTER TABLE dive_groups
    ADD COLUMN description TEXT;

INSERT INTO portal_configuration (value_type, group_key, setting_key, default_value, runtime_value, required_runtime, description)
VALUES ('number', 'frontend', 'dive-group-description-max-length', '8000', NULL, false, 'Maximum length of a dive group description, in characters');

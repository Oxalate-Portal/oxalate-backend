ALTER TABLE event_participants
    ADD COLUMN IF NOT EXISTS notified_at TIMESTAMP WITHOUT TIME ZONE;

INSERT INTO portal_configuration (value_type, group_key, setting_key, default_value, runtime_value, required_runtime, description)
VALUES ('number', 'general', 'waiting-list-hours', '12', NULL, false, 'How long a waiting list offer remains valid in hours')
ON CONFLICT (group_key, setting_key) DO NOTHING;


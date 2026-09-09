CREATE TABLE dive_groups
(
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_id   BIGINT                      NOT NULL
        CONSTRAINT fk_dive_groups_events_id REFERENCES events (id),
    name       VARCHAR(255)                NOT NULL,
    owner_id   BIGINT                      NOT NULL
        CONSTRAINT fk_dive_groups_users_id REFERENCES users (id),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE
);

-- A user may only own one dive group per dive event
ALTER TABLE dive_groups
    ADD CONSTRAINT dive_groups_event_owner_unique UNIQUE (event_id, owner_id);

CREATE INDEX dive_groups_event_id_idx ON dive_groups (event_id);

ALTER TABLE event_participants
    ADD COLUMN IF NOT EXISTS dive_group_id BIGINT
        CONSTRAINT fk_event_participants_dive_groups_id REFERENCES dive_groups (id);

ALTER TABLE event_participants
    ADD COLUMN IF NOT EXISTS dive_group_joined_at TIMESTAMP WITHOUT TIME ZONE;

CREATE INDEX event_participants_dive_group_id_idx ON event_participants (dive_group_id);

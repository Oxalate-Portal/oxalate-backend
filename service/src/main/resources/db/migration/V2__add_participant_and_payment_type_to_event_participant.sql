ALTER TABLE event_participants
    ADD participant_type VARCHAR(255) NOT NULL DEFAULT 'USER';

ALTER TABLE event_participants
    ADD payment_type VARCHAR(255) NOT NULL DEFAULT 'ONE_TIME';

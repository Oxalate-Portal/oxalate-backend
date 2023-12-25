-- Users
CREATE TABLE users
(
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username       VARCHAR(80)                 NOT NULL,
    first_name     VARCHAR(120)                NOT NULL,
    last_name      VARCHAR(120)                NOT NULL,
    password       VARCHAR(120)                NOT NULL,
    status         VARCHAR(255)                NOT NULL,
    phone_number   VARCHAR(255)                NOT NULL,
    privacy        boolean                     NOT NULL DEFAULT false,
    next_of_kin    VARCHAR(255),
    approved_terms BOOLEAN                     NOT NULL DEFAULT false,
    registered     TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

ALTER TABLE users
    ADD CONSTRAINT uk_users_id UNIQUE (username);

-- Events
CREATE TABLE events
(
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    description      VARCHAR(255),
    event_duration   INTEGER                     NOT NULL,
    max_depth        INTEGER                     NOT NULL,
    max_duration     INTEGER                     NOT NULL,
    max_participants INTEGER                     NOT NULL,
    published        BOOLEAN                     NOT NULL,
    start_time       TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    title            VARCHAR(255)                NOT NULL,
    type             VARCHAR(255)                NOT NULL,
    organizer_id     BIGINT                      NOT NULL
        CONSTRAINT fk_organizer_users_id REFERENCES users (id)
);

CREATE TABLE event_participants
(
    user_id    BIGINT  NOT NULL
        CONSTRAINT fk_event_users_id REFERENCES users (id),
    event_id   BIGINT  NOT NULL
        CONSTRAINT fk_event_events_id REFERENCES events (id),
    dive_count INTEGER NOT NULL DEFAULT 1
);

ALTER TABLE event_participants
    ADD CONSTRAINT event_participants_pkey PRIMARY KEY (user_id, event_id);

-- Roles
CREATE TABLE roles
(
    id   INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(20)
);

INSERT INTO roles (name)
VALUES ('ROLE_ADMIN');
INSERT INTO roles (name)
VALUES ('ROLE_ORGANIZER');
INSERT INTO roles (name)
VALUES ('ROLE_USER');

CREATE TABLE user_roles
(
    user_id BIGINT  NOT NULL
        CONSTRAINT fk_userroles_users_id REFERENCES users (id),
    role_id INTEGER NOT NULL
        CONSTRAINT fk_userroles_role_id REFERENCES roles (id)
);

ALTER TABLE user_roles
    ADD CONSTRAINT user_roles_pkey PRIMARY KEY (user_id, role_id);

-- Certificates
CREATE TABLE certificates
(
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id            BIGINT       NOT NULL
        CONSTRAINT fk_certificates_users_id REFERENCES users (id),
    organization       VARCHAR(255) NOT NULL,
    certificate_name   VARCHAR(512) NOT NULL,
    certificate_id     VARCHAR(255),
    diver_id           VARCHAR(255),
    certification_date TIMESTAMP WITHOUT TIME ZONE
);

ALTER TABLE certificates
    ADD CONSTRAINT user_org_cert_uk UNIQUE (user_id, organization, certificate_name);
-- Either certificate_id or diver_id must be set
ALTER TABLE certificates
    ADD CONSTRAINT either_cert_or_dive_id CHECK (certificate_id IS NOT NULL OR diver_id IS NOT NULL);

-- Tokens
CREATE TABLE tokens
(
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    token       VARCHAR(255)                NOT NULL,
    token_type  VARCHAR(255)                NOT NULL,
    user_id     BIGINT                      NOT NULL
        CONSTRAINT fk_tokens_users_id REFERENCES users (id),
    retry_count INTEGER                     NOT NULL DEFAULT 0,
    created_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    expires_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

-- Payments
CREATE TABLE payments
(
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id       BIGINT                      NOT NULL
        CONSTRAINT fk_payments_users_id REFERENCES users (id),
    payment_type  VARCHAR(255)                NOT NULL,
    payment_count INTEGER                     NOT NULL DEFAULT 0,
    created_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    expires_at    TIMESTAMP WITHOUT TIME ZONE          DEFAULT NULL
);

-- Messages

CREATE TABLE messages
(
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    description TEXT                        NOT NULL,
    title       VARCHAR(255)                NOT NULL,
    message     TEXT                        NOT NULL,
    creator     BIGINT                      NOT NULL
        CONSTRAINT fk_messages_users_id_2 REFERENCES users (id),
    created_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

-- Message receivers
CREATE TABLE message_receivers
(
    message_id BIGINT  NOT NULL
        CONSTRAINT fk_message_receivers_messages_id REFERENCES messages (id),
    user_id    BIGINT  NOT NULL
        CONSTRAINT fk_message_receivers_users_id REFERENCES users (id),
    read       BOOLEAN NOT NULL DEFAULT false
);

-- Application audit

CREATE TABLE application_audit_event
(
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    trace_id   VARCHAR(255)                NOT NULL,
    level      VARCHAR(255)                NOT NULL,
    user_id    BIGINT       DEFAULT NULL,
    ip_address VARCHAR(255) DEFAULT NULL,
    source     VARCHAR(255)                NOT NULL,
    message    TEXT                        NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

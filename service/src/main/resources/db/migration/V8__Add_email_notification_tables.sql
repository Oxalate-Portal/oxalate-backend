CREATE TABLE email_notification_subscriptions
(
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id           BIGINT      NOT NULL CONSTRAINT fk_email_notifications_users_id REFERENCES users (id),
    notification_type VARCHAR(20) NOT NULL
);

CREATE TABLE email_queue
(
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id             BIGINT        NOT NULL CONSTRAINT fk_email_queue_users_id REFERENCES users (id),
    email_type          VARCHAR(255)  NOT NULL,
    email_detail        VARCHAR(255)  NOT NULL,
    type_id             BIGINT        NOT NULL,
    status              VARCHAR(255)  NOT NULL,
    created_at          TIMESTAMP     NOT NULL,
    counter             INT DEFAULT 0 NOT NULL,
    next_send_timestamp TIMESTAMP     NOT NULL
);

CREATE INDEX idx_email_queue_status ON email_queue (status);
CREATE INDEX idx_email_queue_next_send_timestamp ON email_queue (next_send_timestamp);

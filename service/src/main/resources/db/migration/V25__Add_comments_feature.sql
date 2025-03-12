-- Add configurations for comments feature
INSERT INTO portal_configuration (value_type, group_key, setting_key, default_value, runtime_value, required_runtime, description)
VALUES ('boolean', 'commenting', 'commenting-enabled', 'false', NULL, false, 'Enable commenting');

INSERT INTO portal_configuration (value_type, group_key, setting_key, default_value, runtime_value, required_runtime, description)
VALUES ('array', 'commenting', 'commenting-enabled-features', 'event,page,forum', NULL, false, 'Which types of commenting are enabled');

INSERT INTO portal_configuration (value_type, group_key, setting_key, default_value, runtime_value, required_runtime, description)
VALUES ('boolean', 'commenting', 'commenting-allow-editing', 'false', NULL, false, 'Allow comments to be edited by their authors');

INSERT INTO portal_configuration (value_type, group_key, setting_key, default_value, runtime_value, required_runtime, description)
VALUES ('boolean', 'commenting', 'comments-require-review', 'false', NULL, false, 'All comments have to be reviewed before being published');

INSERT INTO portal_configuration (value_type, group_key, setting_key, default_value, runtime_value, required_runtime, description)
VALUES ('number', 'commenting', 'comments-report-trigger-level', '5', NULL, false, 'How many user reports are required before a comment is set to be reviewed');

CREATE TABLE comments
(
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title             VARCHAR(255) NOT NULL,
    body              TEXT         NOT NULL,
    user_id           BIGINT       NOT NULL,
    parent_comment_id BIGINT,
    comment_type      VARCHAR(20)  NOT NULL,
    comment_status    VARCHAR(20)  NOT NULL,
    cancel_reason     TEXT,
    created_at        TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    modified_at       TIMESTAMP WITHOUT TIME ZONE DEFAULT NULL,
    CONSTRAINT fk_comments_users FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_parent_comment FOREIGN KEY (parent_comment_id) REFERENCES comments (id) ON DELETE CASCADE
);

CREATE TABLE event_comments
(
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_id   BIGINT NOT NULL,
    comment_id BIGINT NOT NULL,
    CONSTRAINT fk_event_comments_event FOREIGN KEY (event_id) REFERENCES events (id) ON DELETE CASCADE,
    CONSTRAINT fk_event_comments_comment FOREIGN KEY (comment_id) REFERENCES comments (id) ON DELETE CASCADE
);

CREATE TABLE page_comments
(
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    page_id    BIGINT NOT NULL,
    comment_id BIGINT NOT NULL,
    CONSTRAINT fk_page_comments_page FOREIGN KEY (page_id) REFERENCES pages (id) ON DELETE CASCADE,
    CONSTRAINT fk_page_comments_comment FOREIGN KEY (comment_id) REFERENCES comments (id) ON DELETE CASCADE
);

CREATE TABLE forum_topics
(
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    comment_id BIGINT NOT NULL,
    CONSTRAINT fk_forum_topics_comment FOREIGN KEY (comment_id) REFERENCES comments (id)
);

CREATE TABLE comment_reports
(
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    comment_id BIGINT NOT NULL,
    user_id    BIGINT NOT NULL,
    reason     TEXT,
    status     VARCHAR(20)  NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_comment_reports_comment FOREIGN KEY (comment_id) REFERENCES comments (id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_reports_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- These are the root topics
INSERT INTO comments (id, title, body, user_id, parent_comment_id, comment_type, comment_status, cancel_reason)
    OVERRIDING SYSTEM VALUE
VALUES (1, 'Event root topic', 'All event threads are children to this topic', 1, NULL, 'TOPIC', 'PUBLISHED', NULL),
       (2, 'Page root topic', 'All page threads are children to this topic', 1, NULL, 'TOPIC', 'PUBLISHED', NULL),
       (3, 'Forum root topic', 'All forum threads are children to this topic', 1, NULL, 'TOPIC', 'PUBLISHED', NULL),
       (4, 'Shared past event topic', 'All past events use this topic', 1, 1, 'TOPIC', 'PUBLISHED', NULL);

SELECT setval(pg_get_serial_sequence('comments', 'id'), (SELECT MAX(id) FROM comments));

-- Link past events to the shared past event topic (comment ID 4)
INSERT INTO event_comments (event_id, comment_id)
SELECT e.id, 4
FROM events e
         LEFT JOIN event_comments ec ON e.id = ec.event_id AND ec.comment_id = 4
WHERE e.start_time < NOW()
  AND ec.event_id IS NULL;
-- Avoid duplicate entries

-- Create a root topic comment for future events and link them
WITH new_comments AS (
    INSERT INTO comments (title, body, user_id, comment_type, comment_status, parent_comment_id, created_at, modified_at)
        SELECT 'Root topic comment for event ID: ' || e.id,
               'Root topic comment for event ID: ' || e.id,
               1,
               'TOPIC',
               'PUBLISHED',
               1,
               NOW(),
               NULL
        FROM events e
                 LEFT JOIN event_comments ec ON e.id = ec.event_id
        WHERE e.start_time > NOW()
          AND ec.event_id IS NULL -- Avoid duplicate entries
        RETURNING id, title)
INSERT
INTO event_comments (event_id, comment_id)
SELECT e.id,
       nc.id
FROM new_comments nc
         JOIN events e ON nc.title = 'Root topic comment for event ID: ' || e.id;

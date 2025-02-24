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
    modified_at       TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_comments_users FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_comments_parent_comment FOREIGN KEY (parent_comment_id) REFERENCES comments (id)
);

CREATE TABLE event_comments
(
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_id            BIGINT                      NOT NULL,
    comment_id          BIGINT                      NOT NULL,
    expiration_datetime TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT fk_event_comments_event FOREIGN KEY (event_id) REFERENCES events (id),
    CONSTRAINT fk_event_comments_comment FOREIGN KEY (comment_id) REFERENCES comments (id)
);

CREATE TABLE page_comments
(
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    page_id    BIGINT NOT NULL,
    comment_id BIGINT NOT NULL,
    CONSTRAINT fk_page_comments_page FOREIGN KEY (page_id) REFERENCES pages (id),
    CONSTRAINT fk_page_comments_comment FOREIGN KEY (comment_id) REFERENCES comments (id)
);

CREATE TABLE forum_topics
(
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    comment_id BIGINT NOT NULL,
    CONSTRAINT fk_forum_topics_comment FOREIGN KEY (comment_id) REFERENCES comments (id)
);

-- These are the root topics
INSERT INTO comments (title, body, user_id, parent_comment_id, comment_type, comment_status, cancel_reason)
VALUES ('Event root topic', 'All event threads are children to this topic', 1, NULL, 'TOPIC', 'PUBLISHED', NULL),
       ( 'Page root topic', 'All page threads are children to this topic', 1, NULL, 'TOPIC', 'PUBLISHED', NULL),
       ( 'Forum root topic', 'All forum threads are children to this topic', 1, NULL, 'TOPIC', 'PUBLISHED', NULL);

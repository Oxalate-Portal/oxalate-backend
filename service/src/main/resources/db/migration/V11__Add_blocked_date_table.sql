CREATE TABLE blocked_dates
(
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    blocked_date DATE   NOT NULL,
    created_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    creator      BIGINT NOT NULL CONSTRAINT fk_blocked_dates_users_id REFERENCES users (id)
);

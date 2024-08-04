ALTER TABLE events
    ADD COLUMN status VARCHAR(255);
UPDATE events
SET status = 'PUBLISHED';
UPDATE events
SET status = 'DRAFTED'
WHERE published = false;
UPDATE events
SET status = 'HELD'
WHERE (start_time + (event_duration * interval '1 hour')) < NOW();
ALTER TABLE events
    DROP COLUMN published;

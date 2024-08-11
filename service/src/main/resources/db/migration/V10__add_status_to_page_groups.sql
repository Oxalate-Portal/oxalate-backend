ALTER TABLE page_groups ADD COLUMN status VARCHAR(255);
UPDATE page_groups SET status = 'PUBLISHED';

ALTER TABLE pages ALTER COLUMN status SET DEFAULT 'PUBLISHED';
UPDATE pages SET status = 'PUBLISHED' WHERE status = 'PUBLIC';
UPDATE pages SET status = 'DRAFTED' WHERE status = 'HIDDEN';

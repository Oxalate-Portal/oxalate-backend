UPDATE application_audit_event
SET user_id = -1
WHERE user_id IS NULL;

ALTER TABLE application_audit_event
    ALTER COLUMN user_id SET DEFAULT -1;

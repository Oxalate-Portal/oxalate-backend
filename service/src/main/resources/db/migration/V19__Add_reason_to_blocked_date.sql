ALTER TABLE blocked_dates
    ADD COLUMN reason TEXT DEFAULT 'Not specified' NOT NULL;

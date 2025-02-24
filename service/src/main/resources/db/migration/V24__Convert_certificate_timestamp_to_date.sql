ALTER TABLE certificates ALTER COLUMN certification_date TYPE DATE USING certification_date::DATE;

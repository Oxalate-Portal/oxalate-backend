ALTER TABLE certificate_classifications
    ADD COLUMN classification_order INTEGER NOT NULL DEFAULT 0;

WITH ordered AS (SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS position
                 FROM certificate_classifications)
UPDATE certificate_classifications c
SET classification_order = ordered.position
FROM ordered
WHERE c.id = ordered.id;

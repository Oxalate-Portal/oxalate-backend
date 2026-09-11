ALTER TABLE dive_groups
    ADD COLUMN group_order INTEGER NOT NULL DEFAULT 0;

-- The default order of the existing dive groups is their creation order within the dive event
WITH ordered AS (SELECT id, ROW_NUMBER() OVER (PARTITION BY event_id ORDER BY created_at, id) AS position
                 FROM dive_groups)
UPDATE dive_groups d
SET group_order = ordered.position
FROM ordered
WHERE d.id = ordered.id;

CREATE INDEX dive_groups_event_id_group_order_idx ON dive_groups (event_id, group_order);

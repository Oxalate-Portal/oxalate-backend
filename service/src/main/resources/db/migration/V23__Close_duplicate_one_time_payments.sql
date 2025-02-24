WITH ordered_payments AS (SELECT id,
                                 user_id,
                                 created_at,
                                 LEAD(created_at) OVER (PARTITION BY user_id ORDER BY created_at) AS next_created_at
                          FROM payments
                          WHERE payment_type = 'ONE_TIME'
                            AND expires_at IS NULL)
UPDATE payments p
SET expires_at = o.next_created_at
FROM ordered_payments o
WHERE p.id = o.id
  AND o.next_created_at IS NOT NULL;

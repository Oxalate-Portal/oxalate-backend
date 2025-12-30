ALTER TABLE payments
    ADD COLUMN start_date DATE;

ALTER TABLE payments
    ADD COLUMN end_date DATE;

UPDATE payments
SET end_date = expires_at::date
WHERE expires_at IS NOT NULL;

UPDATE payments
SET start_date = created_at::date
WHERE created_at IS NOT NULL;

UPDATE payments
SET payment_type = 'PERIODICAL'
WHERE payment_type = 'PERIOD';

ALTER TABLE payments
    DROP COLUMN expires_at;

ALTER TABLE payments
    ALTER COLUMN start_date SET NOT NULL;

ALTER TABLE payments
    RENAME COLUMN created_at TO created;

ALTER TABLE membership
    ADD COLUMN created TIMESTAMP;

UPDATE membership
SET created = start_date;

UPDATE portal_configuration
SET default_value = 'DISABLED,PERPETUAL,PERIODICAL,DURATIONAL'
WHERE group_key = 'membership'
  AND setting_key = 'membership-type';

UPDATE portal_configuration
SET default_value = 'DISABLED,PERPETUAL,PERIODICAL,DURATIONAL'
WHERE group_key = 'payment'
  AND setting_key = 'one-time-expiration-type';

UPDATE portal_configuration
SET default_value = 'DISABLED,PERPETUAL,PERIODICAL,DURATIONAL'
WHERE group_key = 'payment'
  AND setting_key = 'periodical-payment-method-type';

CREATE TABLE membership
(
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    type       VARCHAR(255) NOT NULL,
    start_date DATE         NOT NULL,
    end_date   DATE,
    status     VARCHAR(255) NOT NULL,
    CONSTRAINT fk_membership_user_id FOREIGN KEY (user_id) REFERENCES users (id)
);

INSERT INTO portal_configuration (value_type, group_key, setting_key, default_value, runtime_value, required_runtime, description)
VALUES ('enum', 'membership', 'membership-type', 'disabled,perpetual,periodical,durational', NULL, false, 'If enabled, defines the type of membership');

INSERT INTO portal_configuration (value_type, group_key, setting_key, default_value, runtime_value, required_runtime, description)
VALUES ('enum', 'membership', 'membership-period-unit', 'YEARS', NULL, false,
        'If membership type is set to periodical or durational, then this defines the time unit of the period. This uses the ChronoUnit enum.');

INSERT INTO portal_configuration (value_type, group_key, setting_key, default_value, runtime_value, required_runtime, description)
VALUES ('number', 'membership', 'membership-period-length', '1', NULL, false,
        'If membership type is set to periodical or durational, then this defines the number of time units in the period');

INSERT INTO portal_configuration (value_type, group_key, setting_key, default_value, runtime_value, required_runtime, description)
VALUES ('number', 'membership', 'membership-period-start-point', '1', NULL, false,
        'If membership type is set to periodical, then this defines the nth calendar unit of the next period');

-- Add single payment method as its own configuration as periodical and durational payments are not both supported simultaneously and are separate of single payments
INSERT INTO portal_configuration (value_type, group_key, setting_key, default_value, runtime_value, required_runtime, description)
VALUES ('boolean', 'payment', 'single-payment-enabled', 'false', NULL, false, 'Is single payment enabled');

-- Rename enabled-payment-methods to periodical-payment-method-type, change type to enum and
UPDATE portal_configuration
SET default_value = 'disabled,periodical,durational',
    value_type    = 'enum',
    setting_key   = 'periodical-payment-method-type'
WHERE group_key = 'payment'
  AND setting_key = 'enabled-payment-methods';

-- If the old runtime value of enabled-payment-methods contains one-time, then we enable the onw-time setting
UPDATE portal_configuration
SET runtime_value = 'true'
WHERE group_key = 'payment'
  AND setting_key = 'single-payment-enabled'
  AND EXISTS (SELECT 1
              FROM portal_configuration AS pc
              WHERE pc.setting_key = 'periodical-payment-method-type'
                AND pc.runtime_value LIKE '%one-time%');

-- If the old runtime value of enabled-payment-methods contains period, then we update it to the new value of periodical
UPDATE portal_configuration
SET runtime_value = 'periodical'
WHERE group_key = 'payment'
  AND setting_key = 'periodical-payment-method-type'
  AND runtime_value LIKE '%period%';

-- Add course to the list of event types
UPDATE portal_configuration
SET default_value = 'open-water,cave,open-and-cave,surface,boat,current,course'
WHERE group_key = 'frontend'
  AND setting_key = 'types-of-event';

-- Rename payment start-month to period-start-point as the period can be other than a year
UPDATE portal_configuration
SET setting_key = 'period-start-point'
WHERE group_key = 'payment'
  AND setting_key = 'start-month';
-- Add calendar unit configuration for periodical payments
INSERT INTO portal_configuration (value_type, group_key, setting_key, default_value, runtime_value, required_runtime, description)
VALUES ('enum', 'payment', 'periodical-payment-method-unit', 'YEARS', NULL, false, 'What calendar unit is used for periodical payments');

INSERT INTO portal_configuration (value_type, group_key, setting_key, default_value, runtime_value, required_runtime, description)
VALUES ('number', 'payment', 'payment-period-length', '1', NULL, false,
        'If periodical or durational payment type is enabled, then this defines the number of time units in the period');

-- Add separate switch to control if payment handling is enabled
INSERT INTO portal_configuration (value_type, group_key, setting_key, default_value, runtime_value, required_runtime, description)
VALUES ('boolean', 'payment', 'payment-enabled', 'true', NULL, false, 'Is payment handling enabled');

-- Register when the periodical payment counting has started
INSERT INTO portal_configuration (value_type, group_key, setting_key, default_value, runtime_value, required_runtime, description)
VALUES ('date', 'payment', 'payment-period-start', CURRENT_DATE, NULL, false, 'Date when the periodical payment counting has started');

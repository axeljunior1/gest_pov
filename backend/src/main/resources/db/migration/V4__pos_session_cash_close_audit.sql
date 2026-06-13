-- Clôture caisse : audit écarts et validation manager

ALTER TABLE pos_sessions ADD COLUMN IF NOT EXISTS closed_by varchar(120);
ALTER TABLE pos_sessions ADD COLUMN IF NOT EXISTS difference_reason varchar(50);
ALTER TABLE pos_sessions ADD COLUMN IF NOT EXISTS difference_comment varchar(500);
ALTER TABLE pos_sessions ADD COLUMN IF NOT EXISTS manager_validated_by varchar(120);
ALTER TABLE pos_sessions ADD COLUMN IF NOT EXISTS manager_validated_at timestamp without time zone;

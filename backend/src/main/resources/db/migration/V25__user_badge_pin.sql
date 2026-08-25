ALTER TABLE users ADD COLUMN badge_code VARCHAR(60);
ALTER TABLE users ADD COLUMN pin_hash VARCHAR(255);
ALTER TABLE users ADD COLUMN pin_failed_attempts INT NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN pin_locked_until TIMESTAMPTZ;

CREATE UNIQUE INDEX IF NOT EXISTS uk_users_badge_code ON users (badge_code) WHERE badge_code IS NOT NULL;

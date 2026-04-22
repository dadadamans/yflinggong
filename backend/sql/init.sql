-- Add is_read column to message table
ALTER TABLE message ADD COLUMN IF NOT EXISTS is_read BOOLEAN NOT NULL DEFAULT FALSE;

-- Add code_created_at column to bind_relation table
ALTER TABLE bind_relation ADD COLUMN IF NOT EXISTS code_created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Add enabled column to app_user table
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS enabled BOOLEAN NOT NULL DEFAULT TRUE;

-- Run DbInit
-- ALTER TABLE message ADD COLUMN IF NOT EXISTS is_read BOOLEAN NOT NULL DEFAULT FALSE;
-- ALTER TABLE bind_relation ADD COLUMN IF NOT EXISTS code_created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
-- ALTER TABLE app_user ADD COLUMN IF NOT EXISTS enabled BOOLEAN NOT NULL DEFAULT TRUE;
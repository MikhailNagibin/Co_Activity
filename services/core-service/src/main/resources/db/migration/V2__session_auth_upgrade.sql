ALTER TABLE users
  ADD COLUMN IF NOT EXISTS email_normalized VARCHAR(255),
  ADD COLUMN IF NOT EXISTS status VARCHAR(32),
  ADD COLUMN IF NOT EXISTS email_verified_at TIMESTAMP;

DO $$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_name = 'users' AND column_name = 'password'
  ) AND NOT EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_name = 'users' AND column_name = 'password_hash'
  ) THEN
    ALTER TABLE users RENAME COLUMN password TO password_hash;
  END IF;
END $$;

UPDATE users
SET email_normalized = lower(trim(login))
WHERE email_normalized IS NULL;

UPDATE users
SET status = 'ACTIVE'
WHERE status IS NULL;

UPDATE users
SET email_verified_at = CURRENT_TIMESTAMP
WHERE email_verified_at IS NULL;

ALTER TABLE users
  ALTER COLUMN login TYPE VARCHAR(255),
  ALTER COLUMN email_normalized SET NOT NULL,
  ALTER COLUMN password_hash TYPE VARCHAR(255),
  ALTER COLUMN password_hash SET NOT NULL,
  ALTER COLUMN status SET NOT NULL;

DO $$
BEGIN
  IF EXISTS (
    SELECT email_normalized
    FROM users
    GROUP BY email_normalized
    HAVING COUNT(*) > 1
  ) THEN
    RAISE EXCEPTION 'Cannot create unique index for users.email_normalized because duplicate emails already exist in users.login';
  END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uq_users_email_normalized
  ON users (email_normalized);

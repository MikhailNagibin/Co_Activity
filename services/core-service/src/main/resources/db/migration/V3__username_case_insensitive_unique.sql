DO $$
BEGIN
  IF EXISTS (
    SELECT lower(username)
    FROM users
    GROUP BY lower(username)
    HAVING COUNT(*) > 1
  ) THEN
    RAISE EXCEPTION 'Cannot create case-insensitive unique index for users.username because duplicates already exist';
  END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uq_users_username_lower
  ON users (lower(username));

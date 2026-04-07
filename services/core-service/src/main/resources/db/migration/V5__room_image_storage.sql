ALTER TABLE pictures
  ADD COLUMN IF NOT EXISTS storage_key VARCHAR(255),
  ADD COLUMN IF NOT EXISTS original_filename VARCHAR(255),
  ADD COLUMN IF NOT EXISTS content_type VARCHAR(100),
  ADD COLUMN IF NOT EXISTS size_bytes BIGINT,
  ADD COLUMN IF NOT EXISTS sort_order INT,
  ADD COLUMN IF NOT EXISTS created_at TIMESTAMP;

ALTER TABLE pictures
  ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP;

UPDATE pictures
SET created_at = CURRENT_TIMESTAMP
WHERE created_at IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_pictures_storage_key_not_null
  ON pictures (storage_key)
  WHERE storage_key IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_pictures_room_sort_order_not_null
  ON pictures (room_id, sort_order)
  WHERE storage_key IS NOT NULL AND sort_order IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_pictures_room_sort_order
  ON pictures (room_id, sort_order);

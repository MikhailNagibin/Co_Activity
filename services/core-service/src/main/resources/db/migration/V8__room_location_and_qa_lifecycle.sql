ALTER TABLE rooms
  ADD COLUMN IF NOT EXISTS city VARCHAR(100),
  ADD COLUMN IF NOT EXISTS country VARCHAR(100);

CREATE INDEX IF NOT EXISTS idx_rooms_city_lower
  ON rooms (LOWER(city))
  WHERE city IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_rooms_country_lower
  ON rooms (LOWER(country))
  WHERE country IS NOT NULL;

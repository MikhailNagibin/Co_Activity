CREATE TABLE IF NOT EXISTS user_follows (
  follower_id INT NOT NULL,
  followed_id INT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (follower_id, followed_id),
  CONSTRAINT fk_user_follows_follower
    FOREIGN KEY (follower_id) REFERENCES users(id),
  CONSTRAINT fk_user_follows_followed
    FOREIGN KEY (followed_id) REFERENCES users(id),
  CONSTRAINT chk_user_follows_not_self
    CHECK (follower_id <> followed_id)
);

CREATE INDEX IF NOT EXISTS idx_user_follows_follower_id
  ON user_follows (follower_id);

CREATE INDEX IF NOT EXISTS idx_user_follows_followed_id
  ON user_follows (followed_id);

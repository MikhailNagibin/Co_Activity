CREATE TABLE IF NOT EXISTS room_invitations (
  room_id INT NOT NULL,
  invited_user_id INT NOT NULL,
  invited_by_user_id INT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (room_id, invited_user_id),
  CONSTRAINT fk_room_invitations_room
    FOREIGN KEY (room_id) REFERENCES rooms(id),
  CONSTRAINT fk_room_invitations_invited_user
    FOREIGN KEY (invited_user_id) REFERENCES users(id),
  CONSTRAINT fk_room_invitations_invited_by_user
    FOREIGN KEY (invited_by_user_id) REFERENCES users(id),
  CONSTRAINT chk_room_invitations_not_self
    CHECK (invited_user_id <> invited_by_user_id)
);

CREATE INDEX IF NOT EXISTS idx_room_invitations_room_id
  ON room_invitations (room_id);

CREATE INDEX IF NOT EXISTS idx_room_invitations_invited_user_id
  ON room_invitations (invited_user_id);

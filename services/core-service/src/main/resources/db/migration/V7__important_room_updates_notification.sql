INSERT INTO notifications (notification) VALUES
('ImportantRoomUpdates')
ON CONFLICT DO NOTHING;

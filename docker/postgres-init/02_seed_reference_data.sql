INSERT INTO roles (role) VALUES
('Owner'),
('Admin'),
('Participant')
ON CONFLICT DO NOTHING;

INSERT INTO categories (name) VALUES
('Sport'),
('Music'),
('Art'),
('Entertainments'),
('Business'),
('Education'),
('ActiveRecreation'),
('PassiveRecreation'),
('MassEvent'),
('Other'),
('NotSpecified')
ON CONFLICT DO NOTHING;

INSERT INTO request_statuses (status_info) VALUES
('Consideration'),
('Accepted'),
('Refused'),
('RefusedWithBan')
ON CONFLICT DO NOTHING;

INSERT INTO notifications (notification) VALUES
('MembershipAccepted'),
('MembershipRejected'),
('ActivityClosed'),
('NewJoinRequest')
ON CONFLICT DO NOTHING;

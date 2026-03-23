INSERT INTO Roles (role) VALUES
('Owner'),
('Admin'),
('Participant')
ON CONFLICT DO NOTHING;

INSERT INTO Categories (name) VALUES
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

INSERT INTO RequestStatuses (status_info) VALUES
('Consideration'),
('Accepted'),
('Refused'),
('RefusedWithBan')
ON CONFLICT DO NOTHING;

INSERT INTO Notifications (notification) VALUES
('MembershipAccepted'),
('MembershipRejected'),
('ActivityClosed'),
('NewJoinRequest')
ON CONFLICT DO NOTHING;

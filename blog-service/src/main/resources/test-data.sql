DELETE FROM users u WHERE u.id = 1;
INSERT INTO users (id, username, password, display_name, authority) VALUES (1, 'admin', 'password', 'ADMIN', 'ADMIN')
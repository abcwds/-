INSERT IGNORE INTO user (username, password, nickname, phone, email, role, status) VALUES
('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjzqAKL9xL5jvMFVdNJHvGCgTq/VEq', 'Admin', '13800138000', 'admin@pet.com', 1, 1),
('user1', '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjzqAKL9xL5jvMFVdNJHvGCgTq/VEq', 'User', '13900139000', 'user1@pet.com', 0, 1);

INSERT INTO announcement (title, content, author, type, status, sort_order)
SELECT 'Adoption Notice', 'Welcome to the pet adoption platform! Please read the adoption guidelines carefully.', 'Admin', 'notice', 1, 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM announcement WHERE title = 'Adoption Notice');

INSERT INTO announcement (title, content, author, type, status, sort_order)
SELECT 'Spring Health Tips', 'Spring is a high-risk period for pet diseases. Please pay attention to pet health.', 'Admin', 'news', 1, 2
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM announcement WHERE title = 'Spring Health Tips');

INSERT INTO announcement (title, content, author, type, status, sort_order)
SELECT 'New Pets Coming', 'New pets will arrive this week. Welcome to visit!', 'Admin', 'news', 1, 3
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM announcement WHERE title = 'New Pets Coming');

-- 시드가 id를 직접 넣었으므로 시퀀스는 아직 1에 있다.
-- 맞춰 두지 않으면 POST /posts가 첫 요청부터 PK 충돌로 실패한다.
SELECT setval(pg_get_serial_sequence('boards',           'id'), coalesce((SELECT max(id) FROM boards),           1));
SELECT setval(pg_get_serial_sequence('users',            'id'), coalesce((SELECT max(id) FROM users),            1));
SELECT setval(pg_get_serial_sequence('posts',            'id'), coalesce((SELECT max(id) FROM posts),            1));
SELECT setval(pg_get_serial_sequence('comments',         'id'), coalesce((SELECT max(id) FROM comments),         1));
SELECT setval(pg_get_serial_sequence('post_likes',       'id'), coalesce((SELECT max(id) FROM post_likes),       1));
SELECT setval(pg_get_serial_sequence('post_view_events', 'id'), coalesce((SELECT max(id) FROM post_view_events), 1));

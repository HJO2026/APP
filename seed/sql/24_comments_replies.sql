\echo '        대댓글'
-- 대댓글. 앱 규칙: parent_id는 "같은 글의 최상위 댓글"만 가리키고 깊이는 1단계다.
--
-- 그래서 순서를 뒤집는다. 글을 고르고 그 글의 댓글을 찾는 게 아니라,
-- **부모 댓글을 먼저 고르고 post_id를 그대로 물려받는다.**
-- 이러면 "같은 글" 규칙이 구조적으로 보장되고, 조인도 PK 조인이라 빠르다.
-- 부모는 1 .. :top_comments 범위이므로 항상 최상위 댓글이다(깊이 1단계 보장).
INSERT INTO comments (id, post_id, user_id, parent_id, content, created_at, updated_at)
SELECT s.g,
       p.post_id,
       1 + floor(_seed.u('ru' || s.g) * :users_seed)::bigint,
       p.id,
       substr(repeat(md5('rc' || s.g), 8), 1, 100),
       p.created_at + (_seed.u('rd' || s.g) * 86400) * interval '1 second',
       p.created_at + (_seed.u('rd' || s.g) * 86400) * interval '1 second'
FROM (
  SELECT g, 1 + floor(_seed.u('rp' || g) * :top_comments)::bigint AS pid
  FROM generate_series(:top_comments + 1, :comments) g
) s
JOIN comments p ON p.id = s.pid;

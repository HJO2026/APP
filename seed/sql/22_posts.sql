\echo '        posts' :posts
-- created_at은 최근 쪽으로 치우치게 한다(설계서 v3 6.1의 recency_skew 2.0).
INSERT INTO posts (id, board_id, author_id, title, content, created_at, updated_at)
SELECT s.g,
       1 + floor(_seed.u('b' || s.g) * :boards)::bigint,
       1 + floor(_seed.u('a' || s.g) * :users_seed)::bigint,
       'post-' || s.g || ' ' || substr(md5('t' || s.g), 1, 16),
       substr(repeat(md5('c' || s.g), 64), 1, :content_bytes),
       s.ts, s.ts
FROM (
  SELECT g, _seed.epoch() - (power(_seed.u('d' || g), 2.0) * :post_days * 86400) * interval '1 second' AS ts
  FROM generate_series(1, :posts) g
) s;

\echo '        최상위 댓글' :top_comments
-- 최상위 댓글 (parent_id NULL). id는 1 .. :top_comments 를 쓴다.
-- 댓글도 Zipf로 쏠려야 한다. 글당 평균 3개로 고르게 깔리면
-- GET /posts/{id}의 "최신 댓글 20개"가 아무 일도 하지 않는다.
INSERT INTO comments (id, post_id, user_id, parent_id, content, created_at, updated_at)
SELECT s.g,
       _seed.zipf('cp' || s.g, :posts, :comment_zipf_s),
       1 + floor(_seed.u('cu' || s.g) * :users_seed)::bigint,
       NULL,
       substr(repeat(md5('cc' || s.g), 8), 1, 100),
       s.ts, s.ts
FROM (
  SELECT g, _seed.epoch() - (_seed.u('cd' || g) * :post_days * 86400) * interval '1 second' AS ts
  FROM generate_series(1, :top_comments) g
) s;

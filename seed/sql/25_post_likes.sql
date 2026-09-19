\echo '        좋아요' :likes
-- UNIQUE(post_id, user_id) 때문에 후보를 넉넉히 만들어 중복을 걸러낸 뒤 목표 수만큼 자른다.
-- 후보 순서(g)로 자르므로 결과는 결정적이다.
INSERT INTO post_likes (post_id, user_id, created_at)
SELECT d.post_id, d.user_id, d.created_at
FROM (
  SELECT DISTINCT ON (post_id, user_id) post_id, user_id, created_at, g
  FROM (
    SELECT _seed.zipf('lp' || g, :posts, :zipf_s) AS post_id,
           1 + floor(_seed.u('lu' || g) * :users_seed)::bigint AS user_id,
           _seed.epoch() - (_seed.u('ld' || g) * :post_days * 86400) * interval '1 second' AS created_at,
           g
    FROM generate_series(1, :like_candidates) g
  ) c
  ORDER BY post_id, user_id, g
) d
ORDER BY d.g
LIMIT :likes;

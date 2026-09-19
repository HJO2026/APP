-- 테이블별 행 수 + 순서 무관 해시 합.
-- timestamptz는 seed_meta.epoch로부터의 상대 초로 정규화한다.
-- 빌드 시각이 사람마다 달라도 같은 값이 나와야 하기 때문이다.
WITH e AS (SELECT extract(epoch FROM epoch) AS z FROM seed_meta),
h AS (SELECT 4611686018427387904::numeric AS m)
SELECT t, n, (s % (SELECT m FROM h))::bigint FROM (
  SELECT 'boards' t, count(*) n,
         coalesce(sum(('x'||substr(md5(id||'|'||name),1,7))::bit(28)::int::numeric),0) s FROM boards
  UNION ALL
  SELECT 'users', count(*),
         coalesce(sum(('x'||substr(md5(id||'|'||nickname||'|'||round(extract(epoch FROM created_at)-(SELECT z FROM e))),1,7))::bit(28)::int::numeric),0) FROM users
  UNION ALL
  SELECT 'posts', count(*),
         coalesce(sum(('x'||substr(md5(id||'|'||board_id||'|'||author_id||'|'||title||'|'||md5(content)||'|'||round(extract(epoch FROM created_at)-(SELECT z FROM e))),1,7))::bit(28)::int::numeric),0) FROM posts
  UNION ALL
  SELECT 'post_stats', count(*),
         coalesce(sum(('x'||substr(md5(post_id||'|'||view_count||'|'||like_count),1,7))::bit(28)::int::numeric),0) FROM post_stats
  UNION ALL
  SELECT 'comments', count(*),
         coalesce(sum(('x'||substr(md5(id||'|'||post_id||'|'||user_id||'|'||coalesce(parent_id::text,'-')||'|'||md5(content)||'|'||round(extract(epoch FROM created_at)-(SELECT z FROM e))),1,7))::bit(28)::int::numeric),0) FROM comments
  UNION ALL
  SELECT 'post_likes', count(*),
         coalesce(sum(('x'||substr(md5(post_id||'|'||user_id||'|'||round(extract(epoch FROM created_at)-(SELECT z FROM e))),1,7))::bit(28)::int::numeric),0) FROM post_likes
  UNION ALL
  SELECT 'post_view_events', count(*),
         coalesce(sum(('x'||substr(md5(event_id||'|'||post_id||'|'||user_id||'|'||round(extract(epoch FROM created_at)-(SELECT z FROM e))),1,7))::bit(28)::int::numeric),0) FROM post_view_events
) x ORDER BY t;

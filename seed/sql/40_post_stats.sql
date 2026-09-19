-- 카운터는 직접 넣지 않고 원천 테이블 집계로 채운다.
-- 직접 넣으면 "카운터가 실제 데이터와 맞는가"를 검증할 수단이 사라진다.
INSERT INTO post_stats (post_id, view_count, like_count)
SELECT p.id, coalesce(v.c, 0), coalesce(l.c, 0)
FROM posts p
LEFT JOIN (SELECT post_id, count(*) AS c FROM post_view_events GROUP BY post_id) v ON v.post_id = p.id
LEFT JOIN (SELECT post_id, count(*) AS c FROM post_likes      GROUP BY post_id) l ON l.post_id = p.id;

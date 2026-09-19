\set ON_ERROR_STOP on

-- psql 변수는 $$ 안에서 치환되지 않으므로 먼저 표로 옮긴다
CREATE TEMP TABLE _want(k text, v bigint);
INSERT INTO _want VALUES
  ('boards', :boards), ('users', :users_total), ('posts', :posts),
  ('post_stats', :posts), ('comments', :comments),
  ('post_likes', :likes), ('post_view_events', :views),
  ('hot_window', :hot_window), ('replies', :comments - :top_comments);

CREATE TEMP TABLE _wantt(k text, t text);
INSERT INTO _wantt VALUES ('event_ns', :'event_ns');

DO $$
DECLARE r record; n bigint; bad bigint; share numeric; want bigint;
BEGIN
  -- 1. 행 수
  FOR r IN SELECT k, v FROM _want WHERE k NOT IN ('hot_window','replies') LOOP
    EXECUTE format('SELECT count(*) FROM %I', r.k) INTO n;
    IF n <> r.v THEN
      RAISE EXCEPTION '[검증 실패] % 행 수 불일치: 실제 % / 목표 %', r.k, n, r.v;
    END IF;
  END LOOP;

  -- 2. post_stats가 원천 집계와 일치하는가 (직접 넣지 않고 집계로 채웠는지)
  SELECT count(*) INTO bad FROM post_stats s
  LEFT JOIN (SELECT post_id, count(*) c FROM post_view_events GROUP BY post_id) v ON v.post_id = s.post_id
  LEFT JOIN (SELECT post_id, count(*) c FROM post_likes GROUP BY post_id) l ON l.post_id = s.post_id
  WHERE s.view_count <> coalesce(v.c,0) OR s.like_count <> coalesce(l.c,0);
  IF bad > 0 THEN
    RAISE EXCEPTION '[검증 실패] post_stats가 원천 집계와 다름: 불일치 %건', bad;
  END IF;

  -- 3. 시퀀스가 최대 id보다 큰가 (안 맞으면 POST /posts가 첫 요청부터 PK 충돌)
  FOR r IN SELECT unnest(ARRAY['boards','users','posts','comments','post_likes','post_view_events']) AS t LOOP
    EXECUTE format('SELECT last_value FROM %s', pg_get_serial_sequence(r.t,'id')) INTO n;
    EXECUTE format('SELECT coalesce(max(id),0) FROM %I', r.t) INTO want;
    IF n < want THEN
      RAISE EXCEPTION '[검증 실패] % 시퀀스(%)가 최대 id(%)보다 작다', r.t, n, want;
    END IF;
  END LOOP;

  -- 4. autovacuum 원복 확인 (테이블 속성이라 템플릿에 복사된다)
  SELECT count(*) INTO bad FROM pg_class c JOIN pg_namespace ns ON ns.oid = c.relnamespace
  WHERE ns.nspname = 'public' AND c.relkind = 'r'
    AND c.reloptions::text LIKE '%autovacuum_enabled=off%';
  IF bad > 0 THEN
    RAISE EXCEPTION '[검증 실패] autovacuum이 꺼진 테이블 %개. 원복되지 않았다', bad;
  END IF;

  -- 5. event_id 네임스페이스 (k6 구간과 겹치면 안 된다)
  SELECT count(*) INTO bad FROM post_view_events
  WHERE substr(event_id::text, 1, 2) <> (SELECT t FROM _wantt WHERE k = 'event_ns');
  IF bad > 0 THEN
    RAISE EXCEPTION '[검증 실패] event_id 네임스페이스가 다른 행 %건', bad;
  END IF;

  -- 6. 대댓글 규칙 (앱: parent_id는 같은 글의 최상위 댓글만, 깊이 1단계)
  SELECT count(*) INTO bad FROM comments c JOIN comments p ON p.id = c.parent_id
  WHERE p.parent_id IS NOT NULL OR p.post_id <> c.post_id;
  IF bad > 0 THEN
    RAISE EXCEPTION '[검증 실패] 대댓글 규칙 위반 %건 (부모가 대댓글이거나 다른 글의 댓글)', bad;
  END IF;

  SELECT count(*) INTO n FROM comments WHERE parent_id IS NOT NULL;
  SELECT v INTO want FROM _want WHERE k = 'replies';
  IF n <> want THEN
    RAISE EXCEPTION '[검증 실패] 대댓글 수 불일치: 실제 % / 목표 %', n, want;
  END IF;

  -- 7. updated_at = created_at (비워 두면 now()라 빌드 시각마다 값이 달라진다)
  SELECT (SELECT count(*) FROM boards   WHERE updated_at <> created_at)
       + (SELECT count(*) FROM users    WHERE updated_at <> created_at)
       + (SELECT count(*) FROM posts    WHERE updated_at <> created_at)
       + (SELECT count(*) FROM comments WHERE updated_at <> created_at) INTO bad;
  IF bad > 0 THEN
    RAISE EXCEPTION '[검증 실패] updated_at이 created_at과 다른 행 %건', bad;
  END IF;

  RAISE NOTICE '필수 검증 7/7 통과';
END $$;

-- 분포와 창 크기는 경고만 낸다 (ZIPF_EXP 조정 대상)
\echo ''
\echo '--- 분포 ---'
SELECT round(100.0 * sum(c) FILTER (WHERE rn <= greatest(1, (:posts)/100)) / sum(c), 1) AS "상위 1% 조회 점유율(%)"
FROM (SELECT count(*) c, row_number() OVER (ORDER BY count(*) DESC) rn
      FROM post_view_events GROUP BY post_id) t;

SELECT count(*) AS "최근 24시간 이벤트", :hot_window AS "목표"
FROM post_view_events WHERE created_at >= (SELECT epoch FROM seed_meta) - interval '24 hours';

SELECT round(avg(c),1) AS "글당 평균 댓글", max(c) AS "최다 댓글 글"
FROM (SELECT count(*) c FROM comments GROUP BY post_id) t;

SELECT count(*) FILTER (WHERE parent_id IS NULL) AS "최상위 댓글",
       count(*) FILTER (WHERE parent_id IS NOT NULL) AS "대댓글" FROM comments;

\echo ''
\echo '--- 크기 ---'
SELECT relname AS "테이블", pg_size_pretty(pg_total_relation_size(c.oid)) AS "크기"
FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
WHERE n.nspname='public' AND c.relkind='r' ORDER BY pg_total_relation_size(c.oid) DESC;

SELECT pg_size_pretty(sum(pg_total_relation_size(c.oid))) AS "합계"
FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
WHERE n.nspname='public' AND c.relkind='r';

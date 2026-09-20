\echo '        조회 이벤트' :views
-- event_id 앞 2자리를 네임스페이스로 예약한다. 시드는 :event_ns, k6는 '1'로 시작하는 구간을 쓴다.
-- 겹치면 k6의 신규 요청이 ON CONFLICT로 조용히 무시되어 조회수가 오르지 않고
-- 사후 검증이 유실로 오판한다.
--
-- created_at은 :event_days에 걸쳐 균등하게 뿌린다. 그래서 최근 24시간 창에는
-- 전체 / :event_days 만큼 들어간다. run.sh가 event_days = views / hot_window로 잡으므로
-- 창 크기는 프로파일이 정한 값이 되고, M과 L이 같은 창을 갖는다.
INSERT INTO post_view_events (id, event_id, post_id, user_id, client_ts, created_at)
SELECT s.g,
       (:'event_ns' || substr(md5('ev' || s.g), 3, 30))::uuid,
       _seed.zipf('vp' || s.g, :posts, :zipf_s),
       1 + floor(_seed.u('vu' || s.g) * :users_seed)::bigint,
       s.ts,
       s.ts
FROM (
  SELECT g,
         _seed.epoch() - (_seed.u('vd' || g) * :event_days * 86400) * interval '1 second' AS ts
  FROM generate_series(1, :views) g
) s;

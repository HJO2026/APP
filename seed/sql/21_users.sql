\echo '        users' :users_total
-- 앞 구간(1 .. :users_seed): 시드 활동 사용자. 글·댓글·좋아요를 이 사람들이 만든다.
-- 뒤 구간(:users_seed+1 ..): k6 전용 풀. 시드는 여기에 좋아요·댓글을 붙이지 않는다.
--   안 나누면 k6의 좋아요가 기존 조합에 걸려 핫 로우 경합이 재현되지 않는다(에러도 안 난다).
--   FK 때문에 k6가 쓸 사용자도 시드가 미리 만들어야 한다. 이 풀이 곧 JWT sub 목록이다.
-- updated_at은 created_at과 같게 넣는다. 비워 두면 now()라 빌드 시각마다 값이 달라진다.
INSERT INTO users (id, nickname, created_at, updated_at)
SELECT g, 'user-' || g, ts, ts
FROM (
  SELECT g, _seed.epoch() - (_seed.u('u' || g) * 730 * 86400) * interval '1 second' AS ts
  FROM generate_series(1, :users_total) g
) s;

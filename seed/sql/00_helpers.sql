-- 결정적 난수 헬퍼. md5 기반이라 어느 환경·버전에서도 같은 값이 나온다.
-- hashint8/hashtext는 PostgreSQL 내부 함수라 버전 의존성이 있어 쓰지 않는다.
CREATE SCHEMA IF NOT EXISTS _seed;

-- 키 문자열 -> [0,1) 균등값
CREATE OR REPLACE FUNCTION _seed.u(k text) RETURNS double precision
LANGUAGE sql IMMUTABLE PARALLEL SAFE AS
$$ SELECT ('x' || substr(md5(k), 1, 7))::bit(28)::int::double precision / 268435456.0 $$;

-- Zipf 분포의 역함수. 순위 r의 빈도가 r^-s에 비례한다.
--
-- 연속 근사에서 F(r) = (r^(1-s) - 1) / (N^(1-s) - 1) 이고, 이를 뒤집으면 아래가 된다.
-- s = 1이면 극한값 r = N^u 로, 밀도가 정확히 1/r 이 된다(설계서 v3의 Zipf s=1.0).
--
-- 이전에는 floor(N * u^e) 를 썼는데, 상위 1% 점유율은 맞출 수 있어도 1등이 너무 뾰족했다.
-- e=8, N=54만에서 난수의 18.8%가 통째로 1등으로 몰려 한 글에 댓글이 31만 개 붙었다.
-- 진짜 Zipf s=1이면 1등의 몫은 1/ln(N) ≈ 7.6%다.
CREATE OR REPLACE FUNCTION _seed.zipf(k text, n bigint, s double precision) RETURNS bigint
LANGUAGE sql IMMUTABLE PARALLEL SAFE AS
$$
  SELECT greatest(1, least(n, CASE
    WHEN abs(s - 1.0) < 1e-9
      THEN floor(power(n::double precision, _seed.u(k)))::bigint
    ELSE floor(power(1.0 + _seed.u(k) * (power(n::double precision, 1.0 - s) - 1.0),
                     1.0 / (1.0 - s)))::bigint
  END))
$$;

-- 시각 기준점. 모든 timestamptz를 여기서 상대적으로 만든다.
-- 빌드 시각이 사람마다 달라도 fingerprint가 어긋나지 않도록, 기준점을 기록해 두고
-- fingerprint는 기준점으로부터의 상대 초로 계산한다.
CREATE TABLE IF NOT EXISTS _seed_epoch (t timestamptz NOT NULL);
TRUNCATE _seed_epoch;
INSERT INTO _seed_epoch VALUES (date_trunc('second', now()));

CREATE OR REPLACE FUNCTION _seed.epoch() RETURNS timestamptz
LANGUAGE sql STABLE AS $$ SELECT t FROM _seed_epoch $$;

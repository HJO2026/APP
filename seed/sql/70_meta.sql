-- 템플릿을 복제하면 이 표식도 같이 복사된다. bench에 붙어 이것만 읽으면
-- 지금 어느 프로파일이 붙어 있고 어떤 파라미터로 만들어졌는지 알 수 있다.
CREATE TABLE IF NOT EXISTS seed_meta (
  profile     text        NOT NULL,
  inputs_hash text        NOT NULL,
  epoch       timestamptz NOT NULL,   -- 모든 timestamptz의 기준점
  built_at    timestamptz NOT NULL,
  pg_version  text        NOT NULL,
  params      jsonb       NOT NULL
);
TRUNCATE seed_meta;
INSERT INTO seed_meta
SELECT :'profile', :'inputs_hash', _seed.epoch(), now(), current_setting('server_version'), :'params'::jsonb;

-- 헬퍼는 템플릿에 남기지 않는다
DROP TABLE _seed_epoch CASCADE;
DROP SCHEMA _seed CASCADE;

-- FK와 조회용 인덱스를 떼어 두고 적재한다. 정의는 카탈로그에서 그대로 떠서 보관하므로
-- V1/V2 파일과 중복 관리되지 않는다. PK와 UNIQUE는 남긴다.
CREATE TABLE IF NOT EXISTS _seed_stash (kind text, name text, tbl text, def text);
TRUNCATE _seed_stash;

DO $$
DECLARE r record;
BEGIN
  FOR r IN SELECT conname, conrelid::regclass::text AS tbl, pg_get_constraintdef(oid) AS def
           FROM pg_constraint
           WHERE contype = 'f' AND connamespace = 'public'::regnamespace
  LOOP
    INSERT INTO _seed_stash VALUES ('fk', r.conname, r.tbl, r.def);
    EXECUTE format('ALTER TABLE %s DROP CONSTRAINT %I', r.tbl, r.conname);
  END LOOP;

  FOR r IN SELECT indexname, tablename, indexdef FROM pg_indexes
           WHERE schemaname = 'public' AND indexname LIKE 'idx\_%'
  LOOP
    INSERT INTO _seed_stash VALUES ('idx', r.indexname, r.tablename, r.indexdef);
    EXECUTE format('DROP INDEX %I', r.indexname);
  END LOOP;
END $$;

-- 적재 중 autovacuum이 I/O를 뺏지 않게 끈다. 30_restore.sql에서 반드시 되켠다.
-- (테이블 속성이라 템플릿에 복사된다. 원복을 잊으면 측정 내내 꺼진 채로 돈다)
DO $$
DECLARE r record;
BEGIN
  FOR r IN SELECT tablename FROM pg_tables
           WHERE schemaname = 'public' AND tablename <> 'flyway_schema_history'
  LOOP
    EXECUTE format('ALTER TABLE %I SET (autovacuum_enabled = off)', r.tablename);
  END LOOP;
END $$;

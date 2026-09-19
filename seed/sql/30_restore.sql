-- 떼어 둔 인덱스와 FK를 되돌린다. FK 추가 시 전체 검사가 한 번 돌면서
-- 부모 없는 자식 행이 있으면 여기서 실패한다(정합성 1차 관문).
DO $$
DECLARE r record;
BEGIN
  FOR r IN SELECT * FROM _seed_stash WHERE kind = 'idx' LOOP
    EXECUTE r.def;
  END LOOP;
  FOR r IN SELECT * FROM _seed_stash WHERE kind = 'fk' LOOP
    EXECUTE format('ALTER TABLE %s ADD CONSTRAINT %I %s', r.tbl, r.name, r.def);
  END LOOP;
END $$;

DROP TABLE _seed_stash;

-- autovacuum 원복. 잊으면 템플릿에 꺼진 채로 복사되어 측정 내내 dead tuple이 쌓인다.
DO $$
DECLARE r record;
BEGIN
  FOR r IN SELECT tablename FROM pg_tables
           WHERE schemaname = 'public' AND tablename <> 'flyway_schema_history'
  LOOP
    EXECUTE format('ALTER TABLE %I RESET (autovacuum_enabled)', r.tablename);
  END LOOP;
END $$;

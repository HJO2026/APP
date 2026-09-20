-- 적재용 설정. 세션 단위로만 건다 (서버 설정을 바꾸지 않으므로 측정용에 섞일 수 없다).
-- psql 호출이 나뉘면 세션도 나뉘므로, 무거운 작업을 하는 모든 호출에 이 파일을 함께 넣는다.
-- 특히 인덱스 재구축과 VACUUM은 maintenance_work_mem에 크게 좌우된다.
SET maintenance_work_mem = '512MB';
SET synchronous_commit = off;
SET work_mem = '64MB';

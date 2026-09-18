-- =====================================================================
-- baseline 조회 인덱스 (잠정)
-- TODO(team): 인덱스 미정. 조사 안 = PK만 / v3 안 = 계약의 정렬·조회 조건을 지원하는 인덱스 (이 파일).
-- 이 파일은 별도 location(classpath:db/migration-indexes)에 있어서 FLYWAY_LOCATIONS로 뺄 수 있다.
--   PK만:    FLYWAY_LOCATIONS=classpath:db/migration
--   이 파일 포함(기본): FLYWAY_LOCATIONS=classpath:db/migration,classpath:db/migration-indexes
-- 전환할 때는 DB 볼륨을 새로 만들어야 한다 (이미 적용된 V2를 되돌리지 않는다).
-- =====================================================================

-- GET /posts?sort=latest : ORDER BY created_at DESC, id DESC
CREATE INDEX idx_posts_created_at_id ON posts (created_at DESC, id DESC);

-- GET /posts?sort=popular : ORDER BY like_count DESC, id DESC
CREATE INDEX idx_post_stats_like_count ON post_stats (like_count DESC, post_id DESC);

-- GET /posts/{id} : 최신 댓글 20개
CREATE INDEX idx_comments_post_created ON comments (post_id, created_at DESC, id DESC);

-- GET /posts/trending : 최근 24시간 창
CREATE INDEX idx_view_events_created_at ON post_view_events (created_at);

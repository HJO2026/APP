package com.example.HJO.dao;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.HJO.dto.response.CommentResponse;

/**
 * 댓글 조회 SQL. 조회 인덱스가 없으므로 "이 글의 댓글"을 찾으려면 comments 전체를 스캔한다(의도한 baseline 동작).
 */
@Repository
public class CommentDao {

	/**
	 * 상세 조회의 최신 최상위 댓글 + 대댓글 수. 한 쿼리로 끝낸다(N+1 없음).
	 * CTE c가 두 번 참조되므로 PostgreSQL이 한 번만 계산해 둔다(materialize). 그래서 comments 전체 스캔은 1번이다.
	 * 상관 서브쿼리로 comments를 직접 세면 댓글마다 전체 스캔이 반복된다(최대 21번).
	 */
	private static final String TOP_LEVEL_WITH_REPLY_COUNT_SQL = """
			WITH c AS (
			    SELECT id, user_id, parent_id, content, created_at
			    FROM comments
			    WHERE post_id = :postId
			)
			SELECT t.id, t.user_id, u.nickname, t.content, t.created_at,
			       (SELECT count(*) FROM c r WHERE r.parent_id = t.id) AS reply_count
			FROM c t
			JOIN users u ON u.id = t.user_id
			WHERE t.parent_id IS NULL
			ORDER BY t.created_at DESC, t.id DESC
			LIMIT :limit
			""";

	private final NamedParameterJdbcTemplate jdbc;

	public CommentDao(NamedParameterJdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	public List<CommentResponse> findLatestTopLevel(long postId, int limit) {
		return jdbc.query(TOP_LEVEL_WITH_REPLY_COUNT_SQL, Map.of("postId", postId, "limit", limit),
				(rs, rowNum) -> new CommentResponse(
						rs.getLong("id"),
						rs.getLong("user_id"),
						rs.getString("nickname"),
						rs.getString("content"),
						rs.getLong("reply_count"),
						PostDao.instant(rs, "created_at")));
	}

}

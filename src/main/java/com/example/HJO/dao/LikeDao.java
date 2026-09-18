package com.example.HJO.dao;

import java.util.Map;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 좋아요 기록. (post_id, user_id) UNIQUE 제약으로 한 사용자는 한 글에 한 번만 들어간다.
 * 같은 사용자가 동시에 여러 번 눌러도 첫 INSERT만 들어가고, 나머지는 그 커밋을 기다렸다가 DO NOTHING으로 넘어간다.
 */
@Repository
public class LikeDao {

	private static final String INSERT_IF_ABSENT_SQL = """
			INSERT INTO post_likes (post_id, user_id)
			VALUES (:postId, :userId)
			ON CONFLICT (post_id, user_id) DO NOTHING
			""";

	private final NamedParameterJdbcTemplate jdbc;

	public LikeDao(NamedParameterJdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	/** 새 좋아요면 true, 이미 눌렀으면 false. 없는 게시글이면 FK 위반(→ 404) */
	public boolean insertIfAbsent(long postId, long userId) {
		return jdbc.update(INSERT_IF_ABSENT_SQL, Map.of("postId", postId, "userId", userId)) == 1;
	}

}

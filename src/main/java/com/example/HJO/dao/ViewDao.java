package com.example.HJO.dao;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 조회 이벤트 기록. event_id의 UNIQUE 제약으로 같은 이벤트는 한 번만 들어간다.
 * 동시에 같은 event_id가 오면 PostgreSQL이 먼저 들어간 행의 커밋을 기다렸다가 DO NOTHING으로 넘긴다.
 */
@Repository
public class ViewDao {

	private static final String INSERT_IF_ABSENT_SQL = """
			INSERT INTO post_view_events (event_id, post_id, user_id, client_ts)
			VALUES (:eventId, :postId, :userId, :clientTs)
			ON CONFLICT (event_id) DO NOTHING
			""";

	private final NamedParameterJdbcTemplate jdbc;

	public ViewDao(NamedParameterJdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	/** 새 이벤트면 true, 이미 있던 event_id면 false. 없는 게시글이면 FK 위반(→ 404) */
	public boolean insertIfAbsent(UUID eventId, long postId, long userId, Instant clientTs) {
		int inserted = jdbc.update(INSERT_IF_ABSENT_SQL, Map.of(
				"eventId", eventId,
				"postId", postId,
				"userId", userId,
				"clientTs", clientTs.atOffset(ZoneOffset.UTC)));
		return inserted == 1;
	}

}

package com.example.HJO.support;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 테스트 준비 데이터. 만드는 API가 없는 것(게시판, 사용자)과, API로는 만들 수 없는 상태(과거 시각의 조회 이벤트)만 JDBC로 넣는다.
 * 앱에 테스트 전용 API를 만들지 않기 위한 장치다.
 */
public class TestData {

	private final JdbcTemplate jdbc;

	public TestData(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	public long newBoard() {
		return jdbc.queryForObject("INSERT INTO boards (name) VALUES ('test-board') RETURNING id", Long.class);
	}

	/** 새 사용자 1명 (테스트마다 새로 만들어 다른 테스트와 섞이지 않게 한다) */
	public long newUser() {
		return newUsers(1).get(0);
	}

	/** 새 사용자 n명의 id (오름차순) */
	public List<Long> newUsers(int n) {
		return jdbc.queryForList("""
				INSERT INTO users (nickname)
				SELECT 'user-' || g FROM generate_series(1, ?) g
				RETURNING id
				""", Long.class, n).stream().sorted().toList();
	}

	/** 기록 시각(created_at)을 지정한 조회 이벤트. trending의 24시간 경계를 만들 때 쓴다 */
	public void viewEventAt(long postId, long userId, Instant createdAt, int count) {
		for (int i = 0; i < count; i++) {
			jdbc.update("""
					INSERT INTO post_view_events (event_id, post_id, user_id, client_ts, created_at)
					VALUES (?, ?, ?, ?, ?)
					""", UUID.randomUUID(), postId, userId, Timestamp.from(createdAt), Timestamp.from(createdAt));
		}
	}

}

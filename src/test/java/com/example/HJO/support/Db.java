package com.example.HJO.support;

import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 검증용 조회. API 응답만 믿지 않고 공통 테이블을 직접 본다(블랙박스 + JDBC).
 */
public class Db {

	private final JdbcTemplate jdbc;

	public Db(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	public long viewCount(long postId) {
		return jdbc.queryForObject("SELECT view_count FROM post_stats WHERE post_id = ?", Long.class, postId);
	}

	public long likeCount(long postId) {
		return jdbc.queryForObject("SELECT like_count FROM post_stats WHERE post_id = ?", Long.class, postId);
	}

	public long likeRows(long postId) {
		return jdbc.queryForObject("SELECT count(*) FROM post_likes WHERE post_id = ?", Long.class, postId);
	}

	public long viewEventRows(UUID eventId) {
		return jdbc.queryForObject("SELECT count(*) FROM post_view_events WHERE event_id = ?", Long.class, eventId);
	}

	public long viewEventRowsOfPost(long postId) {
		return jdbc.queryForObject("SELECT count(*) FROM post_view_events WHERE post_id = ?", Long.class, postId);
	}

	public long replyRows(long parentId) {
		return jdbc.queryForObject("SELECT count(*) FROM comments WHERE parent_id = ?", Long.class, parentId);
	}

	public Long parentIdOf(long commentId) {
		return jdbc.queryForObject("SELECT parent_id FROM comments WHERE id = ?", Long.class, commentId);
	}

}

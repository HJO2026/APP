package com.example.HJO.schema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.example.HJO.support.IntegrationTest;

/**
 * S1: 기본 설정(V1 + V2)으로 Flyway가 적용되고 ddl-auto=validate 컨텍스트가 뜬다.
 * S3: 정합성 제약(UNIQUE, FK)이 동작한다.
 */
@IntegrationTest
class SchemaMigrationTest {

	@Autowired
	JdbcTemplate jdbc;

	@AfterEach
	void clean() {
		jdbc.execute("TRUNCATE boards, users, posts, post_stats, comments, post_likes, post_view_events RESTART IDENTITY CASCADE");
	}

	@Test
	void appliesV1AndV2() {
		List<String> versions = jdbc.queryForList(
				"SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank", String.class);

		assertThat(versions).containsExactly("1", "2");
	}

	@Test
	void createsAllTables() {
		List<String> tables = jdbc.queryForList(
				"SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' AND table_name <> 'flyway_schema_history'",
				String.class);

		assertThat(tables).containsExactlyInAnyOrder(
				"boards", "users", "posts", "post_stats", "comments", "post_likes", "post_view_events");
	}

	@Test
	void createsNamedConstraints() {
		List<String> constraints = jdbc.queryForList(
				"SELECT conname FROM pg_constraint WHERE contype IN ('u', 'f') AND connamespace = 'public'::regnamespace",
				String.class);

		assertThat(constraints).containsExactlyInAnyOrder(
				"uq_post_likes_post_user", "uq_post_view_events_event_id",
				"fk_posts_board", "fk_posts_author", "fk_post_stats_post",
				"fk_comments_post", "fk_comments_user",
				"fk_post_likes_post", "fk_post_likes_user",
				"fk_post_view_events_post", "fk_post_view_events_user");
	}

	@Test
	void createsBaselineIndexesFromV2() {
		List<String> indexes = jdbc.queryForList(
				"SELECT indexname FROM pg_indexes WHERE schemaname = 'public' AND indexname LIKE 'idx_%'", String.class);

		assertThat(indexes).containsExactlyInAnyOrder(
				"idx_posts_created_at_id", "idx_post_stats_like_count",
				"idx_comments_post_created", "idx_view_events_created_at");
	}

	@Test
	void rejectsDuplicateViewEventId() {
		long postId = insertPost();
		UUID eventId = UUID.randomUUID();
		insertViewEvent(eventId, postId);

		assertThatThrownBy(() -> insertViewEvent(eventId, postId)).isInstanceOf(DuplicateKeyException.class);
	}

	@Test
	void rejectsDuplicateLikeForSameUserAndPost() {
		long postId = insertPost();
		insertLike(postId, 1L);

		assertThatThrownBy(() -> insertLike(postId, 1L)).isInstanceOf(DuplicateKeyException.class);
	}

	@Test
	void rejectsViewEventForMissingPost() {
		insertPost();

		assertThatThrownBy(() -> insertViewEvent(UUID.randomUUID(), 999_999L))
				.isInstanceOf(DataIntegrityViolationException.class)
				.hasMessageContaining("fk_post_view_events_post");
	}

	@Test
	void postStatsDefaultsToZero() {
		long postId = insertPost();
		jdbc.update("INSERT INTO post_stats (post_id) VALUES (?)", postId);

		assertThat(jdbc.queryForMap("SELECT view_count, like_count FROM post_stats WHERE post_id = ?", postId))
				.containsEntry("view_count", 0L)
				.containsEntry("like_count", 0L);
	}

	private long insertPost() {
		jdbc.update("INSERT INTO boards (id, name) VALUES (1, 'b') ON CONFLICT DO NOTHING");
		jdbc.update("INSERT INTO users (id, nickname) VALUES (1, 'u') ON CONFLICT DO NOTHING");
		return jdbc.queryForObject(
				"INSERT INTO posts (board_id, author_id, title, content) VALUES (1, 1, 't', 'c') RETURNING id", Long.class);
	}

	private void insertViewEvent(UUID eventId, long postId) {
		jdbc.update("INSERT INTO post_view_events (event_id, post_id, user_id, client_ts) VALUES (?, ?, 1, ?)",
				eventId, postId, Timestamp.from(Instant.now()));
	}

	private void insertLike(long postId, long userId) {
		jdbc.update("INSERT INTO post_likes (post_id, user_id) VALUES (?, ?)", postId, userId);
	}

}

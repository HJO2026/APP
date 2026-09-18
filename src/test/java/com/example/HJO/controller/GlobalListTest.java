package com.example.HJO.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import com.example.HJO.support.ApiTestSupport;

import io.restassured.path.json.JsonPath;

/**
 * 전체 데이터를 보는 케이스(목록 끝까지 스크롤, trending). 다른 테스트의 데이터가 섞이면 안 되므로
 * 별도 컨텍스트(= 새 DB 컨테이너)에서 돈다. 기대값은 같은 DB에서 계약의 정렬식으로 직접 계산해 비교한다.
 */
@Tag("global")
@TestPropertySource(properties = "test.context=global") // 컨텍스트를 분리해 깨끗한 DB를 쓴다
class GlobalListTest extends ApiTestSupport {

	long boardId;
	long userId;

	@BeforeEach
	void setUp() {
		boardId = data.newBoard();
		userId = data.newUser();
	}

	@Test
	@DisplayName("GL-01 latest를 cursor로 끝까지 스크롤하면 누락·중복 없이 created_at DESC, id DESC 순서다 (같은 시각 포함)")
	void latestScrollIsCompleteAndOrdered() {
		for (int i = 0; i < 45; i++) {
			api.newPost(userId, boardId);
		}
		// 같은 created_at을 가진 글 60개를 만들어 동점 정렬(id DESC)도 검증한다
		jdbc.update("""
				WITH p AS (
				    INSERT INTO posts (board_id, author_id, title, content, created_at, updated_at)
				    SELECT ?, ?, 'same-time-' || g, 'c', timestamptz '2026-01-01 00:00:00+00', now()
				    FROM generate_series(1, 60) g
				    RETURNING id
				)
				INSERT INTO post_stats (post_id) SELECT id FROM p
				""", boardId, userId);

		List<Long> expected = jdbc.queryForList(
				"SELECT id FROM posts ORDER BY created_at DESC, id DESC", Long.class);
		List<Long> scrolled = scrollAll("latest", 20);

		assertThat(expected).hasSizeGreaterThanOrEqualTo(105);
		assertThat(scrolled).doesNotHaveDuplicates().isEqualTo(expected);
	}

	@Test
	@DisplayName("GL-02 popular를 cursor로 끝까지 스크롤하면 누락·중복 없이 like_count DESC, id DESC 순서다 (같은 좋아요 수 포함)")
	void popularScrollIsCompleteAndOrdered() {
		List<Long> users = data.newUsers(3);
		for (int i = 0; i < 30; i++) {
			long postId = api.newPost(userId, boardId);
			for (int u = 0; u < i % 4 && u < users.size(); u++) {
				api.like(users.get(u), postId).then().statusCode(200);
			}
		}

		List<Long> expected = jdbc.queryForList("""
				SELECT p.id FROM posts p JOIN post_stats s ON s.post_id = p.id
				ORDER BY s.like_count DESC, p.id DESC
				""", Long.class);
		List<Long> scrolled = scrollAll("popular", 7);

		assertThat(scrolled).doesNotHaveDuplicates().isEqualTo(expected);
	}

	@Test
	@DisplayName("GL-03 trending: 최근 24시간 이벤트 수 DESC, id DESC, 상위 20개. 24시간 밖 이벤트는 세지 않는다")
	void trendingCountsOnlyRecentWindow() {
		Instant now = Instant.now();
		List<Long> posts = new ArrayList<>();
		for (int i = 0; i < 25; i++) {
			long postId = api.newPost(userId, boardId);
			posts.add(postId);
			data.viewEventAt(postId, userId, now.minus(Duration.ofHours(1)), i % 7);
		}
		long oldOnly = posts.get(0);
		data.viewEventAt(oldOnly, userId, now.minus(Duration.ofHours(25)), 100); // 창 밖: 세면 1위가 된다

		JsonPath body = api.trending().then().statusCode(200).extract().jsonPath();
		List<Long> ids = body.getList("items.id", Long.class);

		List<Long> expected = jdbc.queryForList("""
				SELECT post_id FROM post_view_events
				WHERE created_at >= now() - interval '24 hours'
				GROUP BY post_id ORDER BY count(*) DESC, post_id DESC LIMIT 20
				""", Long.class);
		assertThat(ids).hasSize(20).isEqualTo(expected);
		assertThat(ids).doesNotContain(oldOnly);
		assertThat(body.getList("items.recentViewCount", Long.class)).isSortedAccordingTo((a, b) -> Long.compare(b, a));
	}

	private List<Long> scrollAll(String sort, int size) {
		List<Long> ids = new ArrayList<>();
		String cursor = null;
		int guard = 0;
		do {
			JsonPath page = api.listPosts(sort, cursor, size).then().statusCode(200).extract().jsonPath();
			ids.addAll(page.getList("items.id", Long.class));
			cursor = page.getString("nextCursor");
		}
		while (cursor != null && ++guard < 1_000);
		return ids;
	}

}

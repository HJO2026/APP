package com.example.HJO.concurrency;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;

import com.example.HJO.support.ApiTestSupport;
import com.example.HJO.support.Concurrently;

import io.restassured.response.Response;

/**
 * 정합성·동시성 (2회차 규칙: 연쇄 문제는 테스트 코드로 검증).
 * - 요청은 CountDownLatch로 동시에 출발시키고, 레이스는 확률적이라 각 케이스를 반복한다
 * - @Transactional을 쓰지 않는다(요청 스레드마다 실제 커밋된 상태를 봐야 한다)
 * - 테스트마다 새 게시글·사용자로 격리한다
 * TODO: bench-infra에 공통 계약 테스트(TP-xx)가 생기면 겹치는 케이스를 확인하고 이 레포에서 지운다 (사용자 결정 2026-09-19)
 */
class ConsistencyTest extends ApiTestSupport {

	static final int REPEAT = 3;

	long boardId;
	long authorId;

	@BeforeEach
	void setUp() {
		boardId = data.newBoard();
		authorId = data.newUser();
	}

	@RepeatedTest(REPEAT)
	@DisplayName("CC-01 같은 eventId를 순차 3번 + 동시 50번 보내도 1회만 반영된다")
	void sameEventIdIsCountedOnce() {
		long postId = api.newPost(authorId, boardId);
		UUID eventId = UUID.randomUUID();

		List<Response> sequential = List.of(
				api.view(authorId, postId, eventId), api.view(authorId, postId, eventId), api.view(authorId, postId, eventId));
		List<Response> concurrent = Concurrently.run(50, i -> api.view(authorId, postId, eventId));

		List<Response> all = new java.util.ArrayList<>(sequential);
		all.addAll(concurrent);
		assertThat(all).extracting(Response::statusCode).containsOnly(200);
		assertThat(all).filteredOn(r -> r.jsonPath().getBoolean("counted")).hasSize(1);
		assertThat(db.viewEventRows(eventId)).isEqualTo(1);
		assertThat(db.viewCount(postId)).isEqualTo(1);
	}

	@RepeatedTest(REPEAT)
	@DisplayName("CC-02 서로 다른 사용자 200명이 동시에 좋아요하면 like_count 200")
	void distinctUsersLikeConcurrently() {
		long postId = api.newPost(authorId, boardId);
		List<Long> users = data.newUsers(200);

		List<Response> responses = Concurrently.run(200, i -> api.like(users.get(i), postId));

		assertThat(responses).extracting(Response::statusCode).containsOnly(200);
		assertThat(responses).extracting(r -> r.jsonPath().getLong("likeCount")).contains(200L);
		assertThat(db.likeRows(postId)).isEqualTo(200);
		assertThat(db.likeCount(postId)).isEqualTo(200);
	}

	@RepeatedTest(REPEAT)
	@DisplayName("CC-03 같은 사용자가 동시에 50번 좋아요해도 1")
	void sameUserLikesConcurrently() {
		long postId = api.newPost(authorId, boardId);
		long userId = data.newUser();

		List<Response> responses = Concurrently.run(50, i -> api.like(userId, postId));

		assertThat(responses).extracting(Response::statusCode).containsOnly(200);
		assertThat(responses).extracting(r -> r.jsonPath().getLong("likeCount")).containsOnly(1L);
		assertThat(db.likeRows(postId)).isEqualTo(1);
		assertThat(db.likeCount(postId)).isEqualTo(1);
	}

	@RepeatedTest(REPEAT)
	@DisplayName("CC-04 서로 다른 eventId 100개를 동시에 보내면 view_count 100 (lost update 없음)")
	void distinctEventsConcurrently() {
		long postId = api.newPost(authorId, boardId);
		List<Long> users = data.newUsers(10);

		List<Response> responses = Concurrently.run(100, i -> api.view(users.get(i % 10), postId, UUID.randomUUID()));

		assertThat(responses).extracting(Response::statusCode).containsOnly(200);
		assertThat(db.viewEventRowsOfPost(postId)).isEqualTo(100);
		assertThat(db.viewCount(postId)).isEqualTo(100);
	}

	@RepeatedTest(REPEAT)
	@DisplayName("CC-05 같은 댓글에 대댓글 30개를 동시에 달면 모두 저장되고 replyCount 30")
	void concurrentRepliesToSameComment() {
		long postId = api.newPost(authorId, boardId);
		long commentId = api.newComment(authorId, postId, "댓글", null);
		List<Long> users = data.newUsers(30);

		List<Response> responses = Concurrently.run(30, i -> api.comment(users.get(i), postId, "reply-" + i, commentId));

		assertThat(responses).extracting(Response::statusCode).containsOnly(201);
		assertThat(db.replyRows(commentId)).isEqualTo(30);
		assertThat(api.getPost(postId).jsonPath().getLong("comments[0].replyCount")).isEqualTo(30);
	}

}

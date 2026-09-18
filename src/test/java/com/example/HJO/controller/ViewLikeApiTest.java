package com.example.HJO.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.HJO.support.ApiTestSupport;

import io.restassured.http.ContentType;

class ViewLikeApiTest extends ApiTestSupport {

	long userId;
	long postId;

	@BeforeEach
	void setUp() {
		userId = data.newUser();
		postId = api.newPost(userId, data.newBoard());
	}

	@Test
	@DisplayName("VW-01 조회: 처음은 counted=true, 같은 eventId 재전송은 false. 이벤트 1행, view_count 1")
	void viewIsCountedOnce() {
		UUID eventId = UUID.randomUUID();

		api.view(userId, postId, eventId).then().statusCode(200).body("counted", equalTo(true));
		api.view(userId, postId, eventId).then().statusCode(200).body("counted", equalTo(false));

		assertThat(db.viewEventRows(eventId)).isEqualTo(1);
		assertThat(db.viewCount(postId)).isEqualTo(1);
		api.getPost(postId).then().body("viewCount", equalTo(1));
	}

	@Test
	@DisplayName("VW-01 조회: 같은 eventId를 다른 글로 보내도 counted=false, 처음 글에만 반영된다")
	void sameEventIdForAnotherPostIsNotCounted() {
		long otherPostId = api.newPost(userId, data.newBoard());
		UUID eventId = UUID.randomUUID();

		api.view(userId, postId, eventId).then().body("counted", equalTo(true));
		api.view(userId, otherPostId, eventId).then().statusCode(200).body("counted", equalTo(false));

		assertThat(db.viewCount(postId)).isEqualTo(1);
		assertThat(db.viewCount(otherPostId)).isZero();
	}

	@Test
	@DisplayName("VW-02 조회 실패: eventId 누락·형식 오류 400, 없는 글 404, 토큰 없음 401")
	void viewErrors() {
		api.as(userId).contentType(ContentType.JSON).body(Map.of("clientTs", "2026-09-18T00:00:00Z"))
				.post("/posts/{id}/views", postId)
				.then().statusCode(400).body("errors.field", hasItem("eventId"));
		api.as(userId).contentType(ContentType.JSON)
				.body(Map.of("eventId", "not-a-uuid", "clientTs", "2026-09-18T00:00:00Z"))
				.post("/posts/{id}/views", postId)
				.then().statusCode(400).body("code", equalTo("INVALID_INPUT"));
		api.view(userId, 999_999_999L, UUID.randomUUID())
				.then().statusCode(404).body("code", equalTo("POST_NOT_FOUND"));
		api.anonymous().contentType(ContentType.JSON)
				.body(Map.of("eventId", UUID.randomUUID().toString(), "clientTs", "2026-09-18T00:00:00Z"))
				.post("/posts/{id}/views", postId)
				.then().statusCode(401);

		assertThat(db.viewCount(postId)).isZero();
	}

	@Test
	@DisplayName("LK-01 좋아요: 처음 likeCount 1, 재요청도 200과 1(멱등), 다른 사용자는 2")
	void likeIsIdempotentPerUser() {
		long otherUser = data.newUser();

		api.like(userId, postId).then().statusCode(200)
				.body("liked", equalTo(true)).body("likeCount", equalTo(1));
		api.like(userId, postId).then().statusCode(200)
				.body("liked", equalTo(true)).body("likeCount", equalTo(1));
		api.like(otherUser, postId).then().statusCode(200).body("likeCount", equalTo(2));

		assertThat(db.likeRows(postId)).isEqualTo(2);
		assertThat(db.likeCount(postId)).isEqualTo(2);
	}

	@Test
	@DisplayName("LK-02 좋아요 실패: 없는 글 404, 토큰 없음 401, 토큰의 사용자가 없으면 401")
	void likeErrors() {
		api.like(userId, 999_999_999L).then().statusCode(404).body("code", equalTo("POST_NOT_FOUND"));
		api.anonymous().post("/posts/{id}/likes", postId).then().statusCode(401);
		api.like(999_999_999L, postId).then().statusCode(401).body("code", equalTo("UNAUTHORIZED"));

		assertThat(db.likeCount(postId)).isZero();
	}

}

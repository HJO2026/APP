package com.example.HJO.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.HJO.global.pagination.OffsetCursor;
import com.example.HJO.support.ApiTestSupport;

import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;

class PostApiTest extends ApiTestSupport {

	long boardId;
	long userId;

	@BeforeEach
	void setUp() {
		boardId = data.newBoard();
		userId = data.newUser();
	}

	@Test
	@DisplayName("PA-01 글 작성: 201 + Location, posts와 post_stats(0, 0)가 생기고 created_at·updated_at이 기록된다")
	void createPost() {
		var response = api.createPost(userId, boardId, "제목", "본문")
				.then().statusCode(201)
				.header("Location", notNullValue())
				.extract();
		long id = response.jsonPath().getLong("id");

		assertThat(response.header("Location")).isEqualTo("/posts/" + id);
		Map<String, Object> row = jdbc.queryForMap("""
				SELECT p.board_id, p.author_id, p.title, p.created_at, p.updated_at, s.view_count, s.like_count
				FROM posts p JOIN post_stats s ON s.post_id = p.id WHERE p.id = ?
				""", id);
		assertThat(row).containsEntry("board_id", boardId).containsEntry("author_id", userId)
				.containsEntry("title", "제목").containsEntry("view_count", 0L).containsEntry("like_count", 0L);
		assertThat(row.get("created_at")).as("JPA Auditing").isNotNull();
		assertThat(row.get("updated_at")).as("JPA Auditing").isNotNull();
	}

	@Test
	@DisplayName("PA-02 글 작성 실패: 검증 400(필드 정보), 없는 게시판 404, 토큰 없음 401, 잘못된 Content-Type 415")
	void createPostErrors() {
		api.createPost(userId, boardId, " ", "")
				.then().statusCode(400)
				.body("code", equalTo("INVALID_INPUT"))
				.body("errors.field", hasItem("title"))
				.body("errors.field", hasItem("content"));

		api.createPost(userId, boardId, "a".repeat(201), "c")
				.then().statusCode(400).body("errors.field", hasItem("title"));

		api.createPost(userId, 999_999L, "t", "c")
				.then().statusCode(404).body("code", equalTo("BOARD_NOT_FOUND"));

		api.anonymous().contentType(ContentType.JSON)
				.body(Map.of("boardId", boardId, "title", "t", "content", "c"))
				.post("/posts")
				.then().statusCode(401).body("code", equalTo("UNAUTHORIZED"));

		api.as(userId).contentType(ContentType.TEXT).body("plain").post("/posts")
				.then().statusCode(415).body("code", equalTo("UNSUPPORTED_MEDIA_TYPE"));
	}

	@Test
	@DisplayName("PA-03 상세: 최상위 댓글 25개 중 최신 20개(작성자 nickname, replyCount), 대댓글은 목록에 없다")
	void detailWithLatestTopLevelComments() {
		long postId = api.newPost(userId, boardId);
		List<Long> commentIds = new java.util.ArrayList<>();
		for (int i = 1; i <= 25; i++) {
			commentIds.add(api.newComment(userId, postId, "comment-" + i, null));
		}
		long first = commentIds.get(0);
		long last = commentIds.get(24);
		for (int i = 1; i <= 3; i++) {
			api.newComment(userId, postId, "reply-" + i, last);
		}

		JsonPath body = api.getPost(postId).then().statusCode(200).extract().jsonPath();

		assertThat(body.getLong("id")).isEqualTo(postId);
		assertThat(body.getLong("authorId")).isEqualTo(userId);
		assertThat(body.getString("authorNickname")).startsWith("user-");
		assertThat(body.getString("createdAt")).isNotNull();
		assertThat(body.getString("updatedAt")).isNotNull();
		List<Long> ids = body.getList("comments.id", Long.class);
		assertThat(ids).hasSize(20)
				.first().isEqualTo(last);
		assertThat(ids).doesNotContain(first)
				.isEqualTo(commentIds.reversed().subList(0, 20));
		assertThat(body.getList("comments.content", String.class)).noneMatch(c -> c.startsWith("reply-"));
		assertThat(body.getLong("comments[0].replyCount")).isEqualTo(3);
		assertThat(body.getLong("comments[1].replyCount")).isZero();
		assertThat(body.getString("comments[0].authorNickname")).startsWith("user-");
	}

	@Test
	@DisplayName("PA-03 상세: 없는 글은 404 POST_NOT_FOUND, 숫자가 아닌 id는 400")
	void detailErrors() {
		api.getPost(999_999_999L).then().statusCode(404).body("code", equalTo("POST_NOT_FOUND"));
		api.anonymous().get("/posts/abc").then().statusCode(400).body("errors.field", hasItem("id"));
	}

	@Test
	@DisplayName("PA-04 목록: 본문 미리보기는 100자(한글 기준 글자 수)다")
	void listPreviewIs100Characters() {
		long postId = api.createPost(userId, boardId, "긴 글", "가".repeat(150))
				.then().statusCode(201).extract().jsonPath().getLong("id");

		JsonPath body = api.listPosts("latest", null, 50).then().statusCode(200).extract().jsonPath();

		String preview = body.getString("items.find { it.id == " + postId + " }.preview");
		assertThat(preview).isEqualTo("가".repeat(100));
	}

	@Test
	@DisplayName("PA-04 목록 실패: size 0·51은 400(필드 정보), 깨진 cursor·다른 정렬의 cursor는 400 INVALID_CURSOR")
	void listErrors() {
		api.listPosts(null, null, 0).then().statusCode(400).body("errors.field", hasItem("size"));
		api.listPosts(null, null, 51).then().statusCode(400).body("errors.field", hasItem("size"));
		api.listPosts("bad", null, null).then().statusCode(400).body("code", equalTo("INVALID_INPUT"));
		api.listPosts("latest", "@@@", null).then().statusCode(400).body("code", equalTo("INVALID_CURSOR"));
		api.listPosts("popular", OffsetCursor.encode("latest", 20), null)
				.then().statusCode(400).body("code", equalTo("INVALID_CURSOR"));
	}

	@Test
	@DisplayName("AU-01 인증 범위: GET은 토큰 없이 200, GET에 잘못된 토큰을 붙이면 401(알려진 동작), 쓰기는 토큰 필요")
	void authenticationScope() {
		long postId = api.newPost(userId, boardId);

		api.anonymous().get("/posts").then().statusCode(200);
		api.anonymous().get("/posts/{id}", postId).then().statusCode(200);
		api.anonymous().get("/posts/trending").then().statusCode(200);

		api.anonymous().auth().oauth2("not.a.jwt").get("/posts")
				.then().statusCode(401).header("WWW-Authenticate", containsString("Bearer"));

		api.anonymous().post("/posts/{id}/likes", postId).then().statusCode(401);
	}

}

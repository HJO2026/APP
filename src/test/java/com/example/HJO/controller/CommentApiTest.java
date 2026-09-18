package com.example.HJO.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.HJO.global.pagination.OffsetCursor;
import com.example.HJO.support.ApiTestSupport;

import io.restassured.path.json.JsonPath;

class CommentApiTest extends ApiTestSupport {

	long boardId;
	long userId;
	long postId;

	@BeforeEach
	void setUp() {
		boardId = data.newBoard();
		userId = data.newUser();
		postId = api.newPost(userId, boardId);
	}

	@Test
	@DisplayName("CM-01 댓글 작성: 최상위 댓글과 대댓글 모두 201, DB의 parent_id가 맞다")
	void createCommentAndReply() {
		long commentId = api.comment(userId, postId, "댓글", null)
				.then().statusCode(201).extract().jsonPath().getLong("id");
		long replyId = api.comment(userId, postId, "대댓글", commentId)
				.then().statusCode(201).extract().jsonPath().getLong("id");

		assertThat(db.parentIdOf(commentId)).isNull();
		assertThat(db.parentIdOf(replyId)).isEqualTo(commentId);
	}

	@Test
	@DisplayName("CM-02 댓글 작성 실패: 대댓글의 대댓글 400, 다른 글의 부모·없는 부모 404, 없는 글 404, 1,001자·공백 400, 토큰 없음 401")
	void createCommentErrors() {
		long commentId = api.newComment(userId, postId, "댓글", null);
		long replyId = api.newComment(userId, postId, "대댓글", commentId);
		long otherPostComment = api.newComment(userId, api.newPost(userId, boardId), "다른 글", null);

		api.comment(userId, postId, "대대댓글", replyId)
				.then().statusCode(400).body("code", equalTo("REPLY_DEPTH_EXCEEDED"));
		api.comment(userId, postId, "x", otherPostComment)
				.then().statusCode(404).body("code", equalTo("COMMENT_NOT_FOUND"));
		api.comment(userId, postId, "x", 999_999_999L)
				.then().statusCode(404).body("code", equalTo("COMMENT_NOT_FOUND"));
		api.comment(userId, 999_999_999L, "x", null)
				.then().statusCode(404).body("code", equalTo("POST_NOT_FOUND"));
		api.comment(userId, postId, "a".repeat(1_001), null)
				.then().statusCode(400).body("errors.field", hasItem("content"));
		api.comment(userId, postId, "  ", null)
				.then().statusCode(400).body("errors.field", hasItem("content"));
		api.anonymous().contentType("application/json").body("{\"content\":\"x\"}")
				.post("/posts/{postId}/comments", postId)
				.then().statusCode(401);
	}

	@Test
	@DisplayName("CM-03 대댓글 목록: 오래된 순, size=2로 끝까지 넘겨도 누락·중복 없다")
	void repliesInOldestFirstOrderWithoutGaps() {
		long commentId = api.newComment(userId, postId, "댓글", null);
		List<Long> replyIds = new ArrayList<>();
		for (int i = 1; i <= 5; i++) {
			replyIds.add(api.newComment(userId, postId, "reply-" + i, commentId));
		}

		List<Long> scrolled = new ArrayList<>();
		String cursor = null;
		int pages = 0;
		do {
			JsonPath page = api.replies(postId, commentId, cursor, 2).then().statusCode(200).extract().jsonPath();
			scrolled.addAll(page.getList("items.id", Long.class));
			cursor = page.getString("nextCursor");
			pages++;
		}
		while (cursor != null && pages < 10);

		assertThat(scrolled).isEqualTo(replyIds);
		assertThat(pages).isEqualTo(3);
	}

	@Test
	@DisplayName("CM-03 대댓글 목록: 대댓글이 없는 댓글은 빈 목록, 없는 댓글·다른 글 경로는 404, 다른 댓글의 cursor는 400")
	void repliesErrors() {
		long commentId = api.newComment(userId, postId, "댓글", null);
		long otherPostId = api.newPost(userId, boardId);

		api.replies(postId, commentId, null, null)
				.then().statusCode(200).body("items.size()", equalTo(0)).body("nextCursor", nullValue());
		api.replies(postId, 999_999_999L, null, null)
				.then().statusCode(404).body("code", equalTo("COMMENT_NOT_FOUND"));
		api.replies(otherPostId, commentId, null, null)
				.then().statusCode(404).body("code", equalTo("COMMENT_NOT_FOUND"));
		api.replies(postId, commentId, OffsetCursor.encode("replies-" + (commentId + 1), 2), null)
				.then().statusCode(400).body("code", equalTo("INVALID_CURSOR"));
	}

}

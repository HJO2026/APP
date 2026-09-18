package com.example.HJO.support;

import static io.restassured.RestAssured.given;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

/**
 * API 호출 래퍼. 사용자 id를 주면 그 사용자의 토큰을 붙인다. 응답은 상태 코드 검사 없이 그대로 돌려준다.
 * 여러 스레드에서 동시에 호출해도 된다(동시성 테스트).
 */
public class ApiClient {

	private final JwtTestTokens tokens;

	// 사용자별 토큰을 한 번만 서명한다(1시간 유효). 동시성 테스트에서 요청마다 서명하는 비용을 없앤다
	private final Map<Long, String> tokenCache = new ConcurrentHashMap<>();

	public ApiClient(JwtTestTokens tokens) {
		this.tokens = tokens;
	}

	/** 인증 없는 요청 */
	public RequestSpecification anonymous() {
		return given().accept(ContentType.JSON);
	}

	/** userId의 토큰을 붙인 요청 */
	public RequestSpecification as(long userId) {
		return anonymous().auth().oauth2(tokenCache.computeIfAbsent(userId, tokens::valid));
	}

	// ---- 게시글 ----

	public Response createPost(long userId, long boardId, String title, String content) {
		return as(userId).contentType(ContentType.JSON)
				.body(Map.of("boardId", boardId, "title", title, "content", content))
				.post("/posts");
	}

	/** 게시글을 만들고 id를 돌려준다(201이 아니면 실패) */
	public long newPost(long userId, long boardId) {
		return createPost(userId, boardId, "title", "content")
				.then().statusCode(201)
				.extract().jsonPath().getLong("id");
	}

	public Response getPost(long postId) {
		return anonymous().get("/posts/{id}", postId);
	}

	public Response listPosts(String sort, String cursor, Integer size) {
		RequestSpecification spec = anonymous();
		if (sort != null) {
			spec = spec.queryParam("sort", sort);
		}
		if (cursor != null) {
			spec = spec.queryParam("cursor", cursor);
		}
		if (size != null) {
			spec = spec.queryParam("size", size);
		}
		return spec.get("/posts");
	}

	public Response trending() {
		return anonymous().get("/posts/trending");
	}

	// ---- 댓글 ----

	public Response comment(long userId, long postId, String content, Long parentId) {
		Map<String, Object> body = new HashMap<>();
		body.put("content", content);
		if (parentId != null) {
			body.put("parentId", parentId);
		}
		return as(userId).contentType(ContentType.JSON).body(body).post("/posts/{postId}/comments", postId);
	}

	/** 댓글을 만들고 id를 돌려준다(201이 아니면 실패) */
	public long newComment(long userId, long postId, String content, Long parentId) {
		return comment(userId, postId, content, parentId)
				.then().statusCode(201)
				.extract().jsonPath().getLong("id");
	}

	public Response replies(long postId, long commentId, String cursor, Integer size) {
		RequestSpecification spec = anonymous();
		if (cursor != null) {
			spec = spec.queryParam("cursor", cursor);
		}
		if (size != null) {
			spec = spec.queryParam("size", size);
		}
		return spec.get("/posts/{postId}/comments/{commentId}/replies", postId, commentId);
	}

	// ---- 조회수·좋아요 ----

	public Response view(long userId, long postId, UUID eventId) {
		return as(userId).contentType(ContentType.JSON)
				.body(Map.of("eventId", eventId.toString(), "clientTs", Instant.now().toString()))
				.post("/posts/{id}/views", postId);
	}

	public Response like(long userId, long postId) {
		return as(userId).post("/posts/{id}/likes", postId);
	}

}

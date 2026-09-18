package com.example.HJO.global.error;

import org.springframework.http.HttpStatus;

/**
 * API 에러 코드와 메시지의 단일 출처. 응답 본문의 code는 enum 이름을 그대로 쓴다.
 */
public enum ErrorCode {

	// 400
	INVALID_INPUT(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
	INVALID_CURSOR(HttpStatus.BAD_REQUEST, "cursor 값이 올바르지 않습니다."),
	REPLY_DEPTH_EXCEEDED(HttpStatus.BAD_REQUEST, "대댓글에는 답글을 달 수 없습니다."),

	// 401
	UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요하거나 토큰이 유효하지 않습니다."),

	// 404
	POST_NOT_FOUND(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."),
	COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다."),
	RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 경로를 찾을 수 없습니다."),

	// 405
	METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다."),

	// 500
	INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

	private final HttpStatus status;
	private final String message;

	ErrorCode(HttpStatus status, String message) {
		this.status = status;
		this.message = message;
	}

	public HttpStatus getStatus() {
		return status;
	}

	public String getCode() {
		return name();
	}

	public String getMessage() {
		return message;
	}

}

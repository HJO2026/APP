package com.example.HJO.global.error;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 에러 응답 본문. { "code": "...", "message": "...", "errors": [{ "field": "...", "reason": "..." }] }
 * errors는 검증 실패일 때만 들어간다.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(String code, String message, List<FieldError> errors) {

	public static ErrorResponse of(ErrorCode errorCode) {
		return new ErrorResponse(errorCode.getCode(), errorCode.getMessage(), List.of());
	}

	public static ErrorResponse of(ErrorCode errorCode, List<FieldError> errors) {
		return new ErrorResponse(errorCode.getCode(), errorCode.getMessage(), errors);
	}

	public record FieldError(String field, String reason) {
	}

}

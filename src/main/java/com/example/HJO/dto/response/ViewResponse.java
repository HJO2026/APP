package com.example.HJO.dto.response;

/**
 * counted가 false면 이미 반영된 eventId다(중복 요청도 성공으로 본다).
 * 조회수는 넣지 않는다. 가장 많이 들어오는 쓰기라 응답을 최소로 둔다(현재 조회수는 상세 API에 있다).
 */
public record ViewResponse(boolean counted) {
}

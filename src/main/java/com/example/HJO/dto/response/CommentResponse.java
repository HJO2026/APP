package com.example.HJO.dto.response;

import java.time.Instant;

/**
 * 상세 조회의 최상위 댓글 한 건. replyCount는 조회 시점에 실시간으로 센 대댓글 수다.
 */
public record CommentResponse(
		Long id,
		Long authorId,
		String authorNickname,
		String content,
		long replyCount,
		Instant createdAt) {
}

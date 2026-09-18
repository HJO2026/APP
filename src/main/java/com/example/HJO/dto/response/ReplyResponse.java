package com.example.HJO.dto.response;

import java.time.Instant;

/**
 * 대댓글 목록의 한 건.
 */
public record ReplyResponse(
		Long id,
		Long authorId,
		String authorNickname,
		String content,
		Instant createdAt) {
}

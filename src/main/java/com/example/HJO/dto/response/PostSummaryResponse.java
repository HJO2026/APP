package com.example.HJO.dto.response;

import java.time.Instant;

/**
 * 목록의 게시글 한 건. preview는 본문 앞 100자다. 작성자는 authorId만 준다(nickname 없음).
 */
public record PostSummaryResponse(
		Long id,
		Long boardId,
		Long authorId,
		String title,
		String preview,
		long viewCount,
		long likeCount,
		Instant createdAt) {
}

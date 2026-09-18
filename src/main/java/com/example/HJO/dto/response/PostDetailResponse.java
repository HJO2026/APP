package com.example.HJO.dto.response;

import java.time.Instant;
import java.util.List;

/**
 * 게시글 상세. comments는 최신 최상위 댓글 20개(대댓글은 별도 API로 조회)다.
 */
public record PostDetailResponse(
		Long id,
		Long boardId,
		Long authorId,
		String authorNickname,
		String title,
		String content,
		long viewCount,
		long likeCount,
		Instant createdAt,
		Instant updatedAt,
		List<CommentResponse> comments) {
}

package com.example.HJO.dto.response;

import java.time.Instant;

/**
 * trending의 게시글 한 건. recentViewCount는 집계 기간(기본 24시간) 안의 조회 이벤트 수(정렬 기준)이고,
 * viewCount는 전체 누적 조회수다. 작성자는 목록과 같이 authorId만 준다.
 */
public record TrendingPostResponse(
		Long id,
		Long boardId,
		Long authorId,
		String title,
		String preview,
		long recentViewCount,
		long viewCount,
		long likeCount,
		Instant createdAt) {
}

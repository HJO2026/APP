package com.example.HJO.dto.response;

/**
 * 좋아요 결과. 처음 누를 때와 다시 누를 때 모두 현재 상태를 돌려준다(멱등). 취소 API는 없다.
 */
public record LikeResponse(boolean liked, long likeCount) {

	public static LikeResponse of(long likeCount) {
		return new LikeResponse(true, likeCount);
	}

}

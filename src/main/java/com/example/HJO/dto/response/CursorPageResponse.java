package com.example.HJO.dto.response;

import java.util.List;

/**
 * cursor 기반 목록의 한 페이지. nextCursor가 null이면 마지막 페이지다.
 */
public record CursorPageResponse<T>(List<T> items, String nextCursor) {

	/**
	 * size + 1건을 읽은 결과로 페이지를 만든다. 한 건이 더 있으면 다음 페이지가 있다고 보고 nextCursor를 붙인다.
	 */
	public static <T> CursorPageResponse<T> fromOverfetched(List<T> fetched, int size, String nextCursor) {
		boolean hasNext = fetched.size() > size;
		return new CursorPageResponse<>(hasNext ? fetched.subList(0, size) : fetched, hasNext ? nextCursor : null);
	}

}

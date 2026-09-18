package com.example.HJO.domain;

import java.util.Locale;

/**
 * 게시글 목록 정렬. 요청 파라미터는 소문자(latest, popular)다.
 */
public enum PostSort {

	/** created_at DESC, id DESC */
	LATEST,

	/** like_count DESC, id DESC */
	POPULAR;

	public String key() {
		return name().toLowerCase(Locale.ROOT);
	}

	/** 대소문자를 무시한다. 알 수 없는 값이면 IllegalArgumentException(→ 400) */
	public static PostSort from(String value) {
		return valueOf(value.trim().toUpperCase(Locale.ROOT));
	}

}

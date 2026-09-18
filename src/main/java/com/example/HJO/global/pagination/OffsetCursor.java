package com.example.HJO.global.pagination;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import com.example.HJO.global.error.BusinessException;
import com.example.HJO.global.error.ErrorCode;

/**
 * 불투명 cursor. 내부는 offset이다(baseline은 offset 페이지네이션).
 * 형식: base64url("v1:{scope}:{offset}"). scope는 cursor를 만든 목록(예: 정렬 기준)이며, 다른 목록의 cursor는 거부한다.
 */
public final class OffsetCursor {

	private static final String VERSION = "v1";
	private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
	private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

	private OffsetCursor() {
	}

	public static String encode(String scope, long offset) {
		String raw = VERSION + ":" + scope + ":" + offset;
		return ENCODER.encodeToString(raw.getBytes(StandardCharsets.UTF_8));
	}

	/** cursor가 없으면 0(첫 페이지). 형식이 틀리거나 scope가 다르면 INVALID_CURSOR(400) */
	public static long decode(String cursor, String expectedScope) {
		if (cursor == null || cursor.isBlank()) {
			return 0;
		}
		try {
			String[] parts = new String(DECODER.decode(cursor), StandardCharsets.UTF_8).split(":", -1);
			if (parts.length != 3 || !VERSION.equals(parts[0]) || !expectedScope.equals(parts[1])) {
				throw new BusinessException(ErrorCode.INVALID_CURSOR);
			}
			long offset = Long.parseLong(parts[2]);
			if (offset < 0) {
				throw new BusinessException(ErrorCode.INVALID_CURSOR);
			}
			return offset;
		}
		catch (IllegalArgumentException ex) { // Base64 형식 오류, 숫자 형식 오류
			throw new BusinessException(ErrorCode.INVALID_CURSOR, ex);
		}
	}

}

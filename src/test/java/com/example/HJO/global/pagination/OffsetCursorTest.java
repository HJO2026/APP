package com.example.HJO.global.pagination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.example.HJO.global.error.BusinessException;
import com.example.HJO.global.error.ErrorCode;

class OffsetCursorTest {

	@Test
	@DisplayName("인코딩한 cursor를 같은 scope로 디코딩하면 offset이 그대로 나온다")
	void roundTrip() {
		String cursor = OffsetCursor.encode("latest", 40);

		assertThat(OffsetCursor.decode(cursor, "latest")).isEqualTo(40);
	}

	@Test
	@DisplayName("cursor는 URL에 그대로 넣을 수 있다 (base64url, 패딩 없음)")
	void encodedCursorIsUrlSafe() {
		String cursor = OffsetCursor.encode("replies-123", 999_999);

		assertThat(cursor).matches("[A-Za-z0-9_-]+");
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { "   " })
	@DisplayName("cursor가 없으면 첫 페이지(offset 0)다")
	void missingCursorIsFirstPage(String cursor) {
		assertThat(OffsetCursor.decode(cursor, "latest")).isZero();
	}

	@Test
	@DisplayName("다른 목록(scope)에서 받은 cursor는 거부한다")
	void rejectsCursorFromAnotherScope() {
		String latestCursor = OffsetCursor.encode("latest", 20);

		assertInvalidCursor(() -> OffsetCursor.decode(latestCursor, "popular"));
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"@@@",            // base64 아님
			"djE6bGF0ZXN0",   // "v1:latest" (부분 개수 부족)
	})
	@DisplayName("형식이 깨진 cursor는 INVALID_CURSOR")
	void rejectsMalformedCursor(String cursor) {
		assertInvalidCursor(() -> OffsetCursor.decode(cursor, "latest"));
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"v2:latest:20",         // 모르는 버전
			"v1:latest:abc",        // 숫자 아님
			"v1:latest:-1",         // 음수
			"v1:latest:20:extra",   // 부분이 너무 많음
			"v1:latest:99999999999999999999", // long 범위 초과
	})
	@DisplayName("내용이 잘못된 cursor는 INVALID_CURSOR")
	void rejectsInvalidContent(String raw) {
		String cursor = Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));

		assertInvalidCursor(() -> OffsetCursor.decode(cursor, "latest"));
	}

	private static void assertInvalidCursor(org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
		assertThatThrownBy(call)
				.isInstanceOf(BusinessException.class)
				.extracting(ex -> ((BusinessException) ex).getErrorCode())
				.isEqualTo(ErrorCode.INVALID_CURSOR);
	}

}

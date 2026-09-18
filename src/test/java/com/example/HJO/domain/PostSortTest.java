package com.example.HJO.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class PostSortTest {

	@ParameterizedTest(name = "\"{0}\" → {1}")
	@CsvSource({ "latest, LATEST", "popular, POPULAR", "LATEST, LATEST", "Popular, POPULAR", "' latest ', LATEST" })
	@DisplayName("요청 파라미터는 대소문자와 앞뒤 공백을 무시한다")
	void parsesCaseInsensitively(String value, PostSort expected) {
		assertThat(PostSort.from(value)).isEqualTo(expected);
	}

	@ParameterizedTest
	@ValueSource(strings = { "bad", "", "trending" })
	@DisplayName("모르는 값은 IllegalArgumentException (→ 400)")
	void rejectsUnknownValue(String value) {
		assertThatThrownBy(() -> PostSort.from(value)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("key는 소문자 이름이다 (cursor scope로 쓴다)")
	void keyIsLowercaseName() {
		assertThat(PostSort.LATEST.key()).isEqualTo("latest");
		assertThat(PostSort.POPULAR.key()).isEqualTo("popular");
	}

}

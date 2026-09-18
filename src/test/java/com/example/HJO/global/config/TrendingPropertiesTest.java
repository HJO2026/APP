package com.example.HJO.global.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TrendingPropertiesTest {

	@Test
	@DisplayName("계약 기본값(24시간, 20개)은 유효하다")
	void acceptsContractDefaults() {
		assertThatCode(() -> new TrendingProperties(Duration.ofHours(24), 20)).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("기간이 없거나 0 이하면 기동 실패")
	void rejectsNonPositiveWindow() {
		assertThatThrownBy(() -> new TrendingProperties(null, 20)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new TrendingProperties(Duration.ZERO, 20)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new TrendingProperties(Duration.ofHours(-1), 20))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("개수가 1 미만이면 기동 실패")
	void rejectsLimitBelowOne() {
		assertThatThrownBy(() -> new TrendingProperties(Duration.ofHours(24), 0))
				.isInstanceOf(IllegalArgumentException.class);
	}

}

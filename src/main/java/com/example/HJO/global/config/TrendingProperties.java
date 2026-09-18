package com.example.HJO.global.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * trending 집계 설정. 기본값(24시간, 20개)은 API 계약과 같다. 실험할 때 재빌드 없이 환경변수로 바꿀 수 있다.
 *
 * @param window 집계 기간 (TRENDING_WINDOW, 예: 24h)
 * @param limit  돌려줄 글 수 (TRENDING_LIMIT)
 */
@ConfigurationProperties("app.trending")
public record TrendingProperties(Duration window, int limit) {

	public TrendingProperties {
		if (window == null || window.isNegative() || window.isZero()) {
			throw new IllegalArgumentException("app.trending.window must be positive, but was " + window);
		}
		if (limit < 1) {
			throw new IllegalArgumentException("app.trending.limit must be at least 1, but was " + limit);
		}
	}

}

package com.example.HJO.global.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class JwtDecoderConfigTest {

	@Test
	void rejectsMissingOrShortSecret() {
		assertThatThrownBy(() -> JwtDecoderConfig.hs256Key(null)).isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> JwtDecoderConfig.hs256Key("")).isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> JwtDecoderConfig.hs256Key("x".repeat(31)))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("JWT_SECRET");
	}

	@Test
	void accepts32ByteSecret() {
		assertThatCode(() -> JwtDecoderConfig.hs256Key("x".repeat(32))).doesNotThrowAnyException();
	}

}

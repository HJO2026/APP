package com.example.HJO.global.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.oauth2.jwt.Jwt;

class NumericSubjectValidatorTest {

	private final NumericSubjectValidator validator = new NumericSubjectValidator();

	@Test
	@DisplayName("양의 정수 sub는 user id로 통과한다")
	void acceptsPositiveNumericSubject() {
		assertThat(NumericSubjectValidator.parseUserId("42")).isEqualTo(42L);
		assertThat(validator.validate(jwtWithSubject("42")).hasErrors()).isFalse();
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { "abc", "-1", "0", "4 2", "+42", "1.5", "99999999999999999999" })
	@DisplayName("숫자가 아니거나 0 이하이거나 long 범위를 넘으면 거부한다 (401)")
	void rejectsInvalidSubject(String subject) {
		assertThat(NumericSubjectValidator.parseUserId(subject)).isNull();
		assertThat(validator.validate(jwtWithSubject(subject)).hasErrors()).isTrue();
	}

	private static Jwt jwtWithSubject(String subject) {
		Jwt.Builder builder = Jwt.withTokenValue("token").header("alg", "HS256").claim("scope", "none");
		if (subject != null) {
			builder.subject(subject);
		}
		return builder.build();
	}

}

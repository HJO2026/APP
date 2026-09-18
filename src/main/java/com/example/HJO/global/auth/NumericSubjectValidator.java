package com.example.HJO.global.auth;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * sub가 양의 정수(user id)가 아니면 invalid_token으로 거부한다(401).
 */
public class NumericSubjectValidator implements OAuth2TokenValidator<Jwt> {

	private static final OAuth2Error INVALID_SUBJECT =
			new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, "sub must be a positive numeric user id", null);

	@Override
	public OAuth2TokenValidatorResult validate(Jwt jwt) {
		return parseUserId(jwt.getSubject()) != null
				? OAuth2TokenValidatorResult.success()
				: OAuth2TokenValidatorResult.failure(INVALID_SUBJECT);
	}

	public static Long parseUserId(String subject) {
		if (subject == null || subject.isEmpty() || !subject.chars().allMatch(Character::isDigit)) {
			return null;
		}
		try {
			long id = Long.parseLong(subject);
			return id > 0 ? id : null;
		}
		catch (NumberFormatException ex) { // long 범위 초과
			return null;
		}
	}

}

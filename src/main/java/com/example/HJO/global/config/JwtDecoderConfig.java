package com.example.HJO.global.config;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import com.example.HJO.global.auth.NumericSubjectValidator;

/**
 * 토큰은 검증만 한다(발급 API 없음, 토큰은 시드 단계에서 사전 발급).
 * HS256 서명, exp 필수(만료 검사), sub는 숫자 user id. 검증 과정에서 DB를 조회하지 않는다.
 */
@Configuration(proxyBeanMethods = false)
public class JwtDecoderConfig {

	static final int MIN_SECRET_BYTES = 32; // HS256은 256비트 이상의 키가 필요하다

	@Bean
	JwtDecoder jwtDecoder(@Value("${app.jwt.secret}") String secret) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(hs256Key(secret))
				.macAlgorithm(MacAlgorithm.HS256)
				.build();
		decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
				JwtValidators.createDefault(), // exp/nbf 시각 검사
				new JwtClaimValidator<Object>(JwtClaimNames.EXP, Objects::nonNull), // 기본 검사는 exp가 없으면 통과시킨다
				new NumericSubjectValidator()));
		return decoder;
	}

	static SecretKey hs256Key(String secret) {
		byte[] bytes = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
		if (bytes.length < MIN_SECRET_BYTES) {
			throw new IllegalStateException(
					"JWT_SECRET must be at least " + MIN_SECRET_BYTES + " bytes (UTF-8), but was " + bytes.length);
		}
		return new SecretKeySpec(bytes, "HmacSHA256");
	}

}

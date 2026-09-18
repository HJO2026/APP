package com.example.HJO.support;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

/**
 * 테스트 전용 토큰 발급 유틸. 운영 코드에는 발급 기능을 두지 않는다(토큰은 시드 단계에서 사전 발급).
 * 형식: HS256, sub = user id(문자열), exp 필수.
 */
@TestComponent
public class JwtTestTokens {

	private final String secret;

	public JwtTestTokens(@Value("${app.jwt.secret}") String secret) {
		this.secret = secret;
	}

	/** 1시간 유효한 정상 토큰 */
	public String valid(long userId) {
		return sign(secret, claims(String.valueOf(userId)).expiresAt(Instant.now().plus(Duration.ofHours(1))).build());
	}

	/** 기본 clock skew(60초)보다 충분히 전에 만료된 토큰 */
	public String expired(long userId) {
		Instant past = Instant.now().minus(Duration.ofMinutes(10));
		return sign(secret, claims(String.valueOf(userId)).issuedAt(past.minus(Duration.ofHours(1))).expiresAt(past).build());
	}

	public String withoutExp(long userId) {
		return sign(secret, claims(String.valueOf(userId)).build());
	}

	public String withSubject(String subject) {
		return sign(secret, claims(subject).expiresAt(Instant.now().plus(Duration.ofHours(1))).build());
	}

	public String signedWith(String otherSecret, long userId) {
		return sign(otherSecret, claims(String.valueOf(userId)).expiresAt(Instant.now().plus(Duration.ofHours(1))).build());
	}

	/** alg=none (서명 없음) 토큰 */
	public String unsigned(long userId) {
		Base64.Encoder b64 = Base64.getUrlEncoder().withoutPadding();
		String header = b64.encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
		long exp = Instant.now().plus(Duration.ofHours(1)).getEpochSecond();
		String payload = b64.encodeToString(
				("{\"sub\":\"" + userId + "\",\"exp\":" + exp + "}").getBytes(StandardCharsets.UTF_8));
		return header + "." + payload + ".";
	}

	private static JwtClaimsSet.Builder claims(String subject) {
		JwtClaimsSet.Builder builder = JwtClaimsSet.builder().issuedAt(Instant.now());
		return subject == null ? builder : builder.subject(subject);
	}

	private static String sign(String secret, JwtClaimsSet claims) {
		var key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
		var encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
		var header = JwsHeader.with(MacAlgorithm.HS256).build();
		return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}

}

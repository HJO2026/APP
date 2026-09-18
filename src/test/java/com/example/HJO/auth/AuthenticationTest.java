package com.example.HJO.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.HJO.common.auth.CurrentUserId;
import com.example.HJO.support.IntegrationTest;
import com.example.HJO.support.JwtTestTokens;

/**
 * C5: 인증 없음이나 잘못된 토큰 → 401. 실제 서버(RANDOM_PORT)로 필터 체인과 에러 포워드까지 검증한다.
 * API가 아직 없으므로 테스트 전용 엔드포인트(/test/me)로 인증 결과를 확인한다.
 */
@IntegrationTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(AuthenticationTest.AuthProbeController.class)
@AutoConfigureObservability // 테스트는 기본으로 지표 내보내기를 꺼서 prometheus 엔드포인트가 없다
class AuthenticationTest {

	@Autowired
	TestRestTemplate rest;

	@Autowired
	JwtTestTokens tokens;

	@Test
	void validTokenPassesAndInjectsUserIdFromSub() {
		ResponseEntity<String> response = get("/test/me", tokens.valid(42L));

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isEqualTo("42");
	}

	@Test
	void missingTokenIs401() {
		ResponseEntity<String> response = get("/test/me", null);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		assertThat(response.getHeaders().getFirst(HttpHeaders.WWW_AUTHENTICATE)).startsWith("Bearer");
	}

	@Test
	void apiPathsRequireAuthentication() {
		assertThat(get("/posts", null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		assertThat(get("/posts/1", null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void malformedTokenIs401() {
		assertThat(get("/test/me", "not-a-jwt").getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void wrongSignatureIs401() {
		String token = tokens.signedWith("another-secret-that-is-32-bytes-long!!", 42L);

		assertThat(get("/test/me", token).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void unsignedAlgNoneTokenIs401() {
		assertThat(get("/test/me", tokens.unsigned(42L)).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void expiredTokenIs401() {
		assertThat(get("/test/me", tokens.expired(42L)).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void tokenWithoutExpIs401() {
		assertThat(get("/test/me", tokens.withoutExp(42L)).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void nonNumericOrMissingSubjectIs401() {
		assertThat(get("/test/me", tokens.withSubject("abc")).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		assertThat(get("/test/me", tokens.withSubject("-1")).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		assertThat(get("/test/me", tokens.withSubject("0")).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		assertThat(get("/test/me", tokens.withSubject("99999999999999999999")).getStatusCode())
				.isEqualTo(HttpStatus.UNAUTHORIZED);
		assertThat(get("/test/me", tokens.withSubject(null)).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void unknownPathWithValidTokenIs404NotUnauthorized() {
		// 에러 포워드(/error)에서 인증이 사라져 401로 바뀌지 않는지 확인
		assertThat(get("/no-such-path", tokens.valid(42L)).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void healthAndPrometheusAreOpenWithoutToken() {
		ResponseEntity<String> health = get("/actuator/health", null);
		ResponseEntity<String> prometheus = get("/actuator/prometheus", null);

		assertThat(health.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(health.getBody()).contains("\"status\":\"UP\"");
		assertThat(prometheus.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(prometheus.getBody()).contains("jvm_memory_used_bytes");
	}

	private ResponseEntity<String> get(String path, String bearerToken) {
		HttpHeaders headers = new HttpHeaders();
		if (bearerToken != null) {
			headers.setBearerAuth(bearerToken);
		}
		return rest.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), String.class);
	}

	/** 테스트 클래스의 직계 중첩 클래스라 컴포넌트 스캔에서 제외되고, 이 테스트에서만 @Import로 등록된다 */
	@RestController
	static class AuthProbeController {

		@GetMapping("/test/me")
		String me(@CurrentUserId Long userId) {
			return String.valueOf(userId);
		}

	}

}
